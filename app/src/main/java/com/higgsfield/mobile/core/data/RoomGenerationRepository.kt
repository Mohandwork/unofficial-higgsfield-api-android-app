package com.higgsfield.mobile.core.data

import com.higgsfield.mobile.core.database.AttachmentEntity
import com.higgsfield.mobile.core.database.GenerationDao
import com.higgsfield.mobile.core.database.GenerationEntity
import com.higgsfield.mobile.core.database.LocalGenerationStore
import com.higgsfield.mobile.core.database.MediaDao
import com.higgsfield.mobile.core.database.PersistedGenerationStatus
import com.higgsfield.mobile.core.error.AppError
import com.higgsfield.mobile.core.error.ErrorMapper
import com.higgsfield.mobile.core.model.EstimateState
import com.higgsfield.mobile.core.model.GenerationAttachment
import com.higgsfield.mobile.core.model.GenerationDraft
import com.higgsfield.mobile.core.model.GenerationRecord
import com.higgsfield.mobile.core.model.GenerationStatus
import com.higgsfield.mobile.core.model.MediaKind
import com.higgsfield.mobile.core.model.PromptComposer
import com.higgsfield.mobile.core.model.WorkflowRegistry
import com.higgsfield.mobile.core.network.AttachmentUploadResult
import com.higgsfield.mobile.core.network.HiggsfieldService
import com.higgsfield.mobile.core.network.HiggsfieldUrlValidator
import com.higgsfield.mobile.core.network.RequestStatusSynchronizer
import com.higgsfield.mobile.core.network.SchemaWorkflowAdapter
import com.higgsfield.mobile.core.network.SecureAttachmentUploader
import com.higgsfield.mobile.core.network.WorkflowRequestSchemas
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import java.io.IOException

/** A recoverable submission failure whose user-facing copy remains in [AppError]. */
class GenerationSubmissionException(val appError: AppError) : Exception(appError.code)

/**
 * The production request boundary: it persists a draft before any network work, uploads local
 * attachments, maps the verified schema once, and records an accepted Higgsfield request.
 */
@Singleton
class RoomGenerationRepository @Inject constructor(
    private val service: HiggsfieldService,
    private val attachmentUploader: SecureAttachmentUploader,
    private val localStore: LocalGenerationStore,
    private val generationDao: GenerationDao,
    private val mediaDao: MediaDao,
    private val statusSynchronizer: RequestStatusSynchronizer,
) : GenerationRepository {
    override fun observeConversation(conversationId: String): Flow<List<GenerationRecord>> =
        generationDao.observeForConversation(conversationId).map { generations ->
            generations.map(GenerationEntity::toRecord)
        }

    /** Live estimates have not been authorized or verified for submission bodies yet. */
    override suspend fun estimate(draft: GenerationDraft): EstimateState = EstimateState.Idle

    override suspend fun submit(conversationId: String, draft: GenerationDraft): Result<GenerationRecord> {
        if (PromptComposer.compose(draft.creativeBrief, draft.instruction).isBlank()) {
            return Result.failure(GenerationSubmissionException(ErrorMapper.instructionRequired()))
        }
        val adapter = workflowAdapter(draft).getOrElse { return Result.failure(it) }
        val now = System.currentTimeMillis()
        val generationId = UUID.randomUUID().toString()
        val parent = draft.activeSourceId
            ?.let { outputId -> mediaDao.getOutput(outputId) }
            ?.let { output -> generationDao.get(output.generationId) }
            ?.takeIf { generation -> generation.conversationId == conversationId }
        val entity = draft.toEntity(generationId, conversationId, parent, now)

        var generationPostStarted = false
        var generationResponseReceived = false
        return try {
            localStore.insertDraft(entity, draft.attachments.toEntities(generationId))
            val uploadedDraft = when (val upload = attachmentUploader.uploadAll(draft.attachments)) {
                is AttachmentUploadResult.Uploaded -> draft.copy(attachments = upload.attachments)
                is AttachmentUploadResult.Failed -> return fail(entity, upload.error)
            }
            val uploadedPlan = submissionPlan(uploadedDraft, adapter).getOrElse { return fail(entity, it.toAppError()) }
            mediaDao.upsertAttachments(uploadedDraft.attachments.toEntities(generationId))

            generationPostStarted = true
            val response = service.submitWorkflow(uploadedPlan.endpointPath, uploadedPlan.request)
            generationResponseReceived = true
            require(response.requestId.isNotBlank()) { MISSING_REQUEST_ID_MESSAGE }
            val statusUrl = requireNotNull(response.statusUrl) { MISSING_STATUS_URL_MESSAGE }
            require(HiggsfieldUrlValidator.isApiUrl(statusUrl)) { INVALID_STATUS_URL_MESSAGE }
            response.cancelUrl?.let { cancelUrl ->
                require(HiggsfieldUrlValidator.isApiUrl(cancelUrl)) { INVALID_CANCEL_URL_MESSAGE }
            }
            localStore.markAccepted(
                generationId = generationId,
                requestId = response.requestId,
                statusUrl = statusUrl,
                cancellationUrl = response.cancelUrl,
                correlationId = null,
                now = System.currentTimeMillis(),
            )
            // A status refresh never repeats the generation POST. It only reconciles accepted work.
            runCatching { statusSynchronizer.refresh(generationId) }
            Result.success(generationDao.get(generationId)?.toRecord() ?: entity.toRecord())
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            val mapped = ErrorMapper.from(error)
            if (generationPostStarted && (error is IOException || generationResponseReceived)) {
                markUnknownSubmissionOutcome(entity, mapped)
            } else {
                fail(entity, mapped)
            }
        }
    }

    override suspend fun cancel(generationId: String): Result<Unit> = try {
        statusSynchronizer.cancel(generationId)
        Result.success(Unit)
    } catch (error: CancellationException) {
        throw error
    } catch (error: Throwable) {
        Result.failure(GenerationSubmissionException(ErrorMapper.from(error)))
    }

    override suspend fun resumeAcceptedRequests() {
        statusSynchronizer.refreshAcceptedRequests()
    }

    private suspend fun fail(entity: GenerationEntity, error: AppError): Result<GenerationRecord> {
        return updateFailure(entity, PersistedGenerationStatus.FAILED, error)
    }

    private suspend fun markUnknownSubmissionOutcome(
        entity: GenerationEntity,
        error: AppError,
    ): Result<GenerationRecord> = updateFailure(entity, PersistedGenerationStatus.UNKNOWN_SUBMISSION_OUTCOME, error)

    private suspend fun updateFailure(
        entity: GenerationEntity,
        status: PersistedGenerationStatus,
        error: AppError,
    ): Result<GenerationRecord> {
        check(generationDao.updateStatus(
            entity.id,
            status,
            error.code,
            error.diagnosticMessage,
            System.currentTimeMillis(),
        ) == 1)
        return Result.failure(GenerationSubmissionException(error))
    }

    private fun workflowAdapter(draft: GenerationDraft): Result<SchemaWorkflowAdapter> = runCatching {
        val descriptor = requireNotNull(WorkflowRegistry.find(draft.workflowId)) { UNKNOWN_WORKFLOW_MESSAGE }
        val schema = requireNotNull(WorkflowRequestSchemas.find(draft.workflowId)) { MISSING_SCHEMA_MESSAGE }
        requireNotNull(descriptor.endpointPath) { MISSING_ENDPOINT_MESSAGE }
        SchemaWorkflowAdapter(descriptor, schema)
    }

    private fun submissionPlan(
        draft: GenerationDraft,
        adapter: SchemaWorkflowAdapter,
    ): Result<SubmissionPlan> = runCatching {
        val validation = adapter.validate(draft)
        if (!validation.isValid) throw GenerationSubmissionException(validation.errors.first())
        SubmissionPlan(requireNotNull(adapter.descriptor.endpointPath), adapter.toRequest(draft))
    }

    private fun Throwable.toAppError(): AppError =
        (this as? GenerationSubmissionException)?.appError ?: ErrorMapper.from(this)
}

private data class SubmissionPlan(val endpointPath: String, val request: JsonObject)

private fun GenerationDraft.toEntity(
    generationId: String,
    conversationId: String,
    parent: GenerationEntity?,
    now: Long,
) = GenerationEntity(
    id = generationId,
    conversationId = conversationId,
    parentGenerationId = parent?.id,
    branchRootId = parent?.branchRootId ?: generationId,
    workflowId = workflowId.value,
    instruction = instruction.trim(),
    composedPrompt = PromptComposer.compose(creativeBrief, instruction),
    negativePrompt = options.negativePrompt?.takeIf(String::isNotBlank),
    optionsSnapshotJson = options.snapshotJson(),
    createdAtEpochMillis = now,
    updatedAtEpochMillis = now,
)

private fun List<GenerationAttachment>.toEntities(generationId: String) = map { attachment ->
    AttachmentEntity(
        id = "$generationId-${attachment.id}",
        generationId = generationId,
        role = attachment.role.name,
        mediaKind = attachment.kind.name,
        localUri = attachment.uri,
        remoteUrl = attachment.remoteUrl,
    )
}

private fun GenerationEntity.toRecord() = GenerationRecord(
    id = id,
    parentGenerationId = parentGenerationId,
    branchRootId = branchRootId,
    draft = GenerationDraft(
        instruction = instruction,
        creativeBrief = com.higgsfield.mobile.core.model.CreativeBrief(),
        workflowId = com.higgsfield.mobile.core.model.WorkflowId(workflowId),
    ),
    status = status.toDomainStatus(errorMessage),
)

private fun PersistedGenerationStatus.toDomainStatus(errorMessage: String?): GenerationStatus = when (this) {
    PersistedGenerationStatus.DRAFT -> GenerationStatus.Draft
    PersistedGenerationStatus.QUEUED -> GenerationStatus.Queued
    PersistedGenerationStatus.IN_PROGRESS -> GenerationStatus.InProgress()
    PersistedGenerationStatus.COMPLETED -> GenerationStatus.Completed(emptyList())
    PersistedGenerationStatus.NSFW -> GenerationStatus.Nsfw(errorMessage.orEmpty())
    PersistedGenerationStatus.CANCELED -> GenerationStatus.Canceled
    PersistedGenerationStatus.FAILED, PersistedGenerationStatus.UNKNOWN_SUBMISSION_OUTCOME ->
        GenerationStatus.Failed(errorMessage.orEmpty(), retryable = false)
}

private fun com.higgsfield.mobile.core.model.GenerationOptions.snapshotJson(): String = Json.encodeToString(
    JsonObject.serializer(),
    buildJsonObject {
        put(ASPECT_RATIO_FIELD, JsonPrimitive(aspectRatio))
        resolution?.let { put(RESOLUTION_FIELD, JsonPrimitive(it)) }
        durationSeconds?.let { put(DURATION_SECONDS_FIELD, JsonPrimitive(it)) }
        seed?.let { put(SEED_FIELD, JsonPrimitive(it)) }
        negativePrompt?.let { put(NEGATIVE_PROMPT_FIELD, JsonPrimitive(it)) }
    },
)

private const val ASPECT_RATIO_FIELD = "aspectRatio"
private const val RESOLUTION_FIELD = "resolution"
private const val DURATION_SECONDS_FIELD = "durationSeconds"
private const val SEED_FIELD = "seed"
private const val NEGATIVE_PROMPT_FIELD = "negativePrompt"
private const val UNKNOWN_WORKFLOW_MESSAGE = "The selected workflow is not registered"
private const val MISSING_SCHEMA_MESSAGE = "The selected workflow has no verified submission schema"
private const val MISSING_ENDPOINT_MESSAGE = "The selected workflow has no verified submission endpoint"
private const val MISSING_REQUEST_ID_MESSAGE = "Accepted response is missing a request ID"
private const val MISSING_STATUS_URL_MESSAGE = "Accepted response is missing a status URL"
private const val INVALID_STATUS_URL_MESSAGE = "Accepted response has an invalid status URL"
private const val INVALID_CANCEL_URL_MESSAGE = "Accepted response has an invalid cancellation URL"
