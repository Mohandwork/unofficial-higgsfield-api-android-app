package com.higgsfield.mobile.core.data

import com.higgsfield.mobile.core.database.AttachmentEntity
import com.higgsfield.mobile.core.database.ConversationDao
import com.higgsfield.mobile.core.database.ConversationEntity
import com.higgsfield.mobile.core.database.GenerationDao
import com.higgsfield.mobile.core.database.GenerationEntity
import com.higgsfield.mobile.core.database.GenerationWithMedia
import com.higgsfield.mobile.core.database.LocalGenerationStore
import com.higgsfield.mobile.core.database.MediaDao
import com.higgsfield.mobile.core.database.OutputEntity
import com.higgsfield.mobile.core.database.PersistedGenerationStatus
import com.higgsfield.mobile.core.error.AppError
import com.higgsfield.mobile.core.error.ErrorMapper
import com.higgsfield.mobile.core.model.EstimateState
import com.higgsfield.mobile.core.model.GenerationAttachment
import com.higgsfield.mobile.core.model.GenerationDraft
import com.higgsfield.mobile.core.model.GenerationOptions
import com.higgsfield.mobile.core.model.GenerationOutput
import com.higgsfield.mobile.core.model.GenerationRecord
import com.higgsfield.mobile.core.model.GenerationStatus
import com.higgsfield.mobile.core.model.MediaKind
import com.higgsfield.mobile.core.model.MediaRole
import com.higgsfield.mobile.core.model.PromptComposer
import com.higgsfield.mobile.core.model.WorkflowRegistry
import com.higgsfield.mobile.core.model.WorkflowCapability
import com.higgsfield.mobile.core.network.AttachmentUploadResult
import com.higgsfield.mobile.core.network.HiggsfieldService
import com.higgsfield.mobile.core.network.HiggsfieldUrlValidator
import com.higgsfield.mobile.core.network.GenerationRequestSynchronizer
import com.higgsfield.mobile.core.network.SchemaWorkflowAdapter
import com.higgsfield.mobile.core.network.SecureAttachmentUploader
import com.higgsfield.mobile.core.network.WorkflowRequestSchemas
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/** A recoverable submission failure whose user-facing copy remains in [AppError]. */
class GenerationSubmissionException(val appError: AppError) : Exception(appError.code)

/**
 * The production request boundary: it persists a draft before any network work, uploads local
 * attachments, maps the verified schema once, and records an accepted Higgsfield request.
 */
@Singleton
class RoomGenerationRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val service: HiggsfieldService,
    private val attachmentUploader: SecureAttachmentUploader,
    private val localStore: LocalGenerationStore,
    private val conversationDao: ConversationDao,
    private val generationDao: GenerationDao,
    private val mediaDao: MediaDao,
    private val statusSynchronizer: GenerationRequestSynchronizer,
) : GenerationRepository {
    override fun observeConversation(conversationId: String): Flow<List<GenerationRecord>> =
        combine(
            conversationDao.observe(conversationId),
            generationDao.observeWithMedia(conversationId),
        ) { conversation, generations ->
            conversation?.let { entity -> generations.map { it.toRecord(entity.toBrief()) } } ?: emptyList()
        }

    /** Live estimates have not been authorized or verified for submission bodies yet. */
    override suspend fun estimate(draft: GenerationDraft): EstimateState = EstimateState.Idle

    override suspend fun submit(conversationId: String, draft: GenerationDraft): Result<GenerationRecord> {
        if ((draft.composedPromptOverride ?: PromptComposer.compose(draft.creativeBrief, draft.instruction)).isBlank()) {
            return Result.failure(GenerationSubmissionException(ErrorMapper.instructionRequired()))
        }
        val adapter = workflowAdapter(draft).getOrElse { return Result.failure(it) }
        val sourceOutput = draft.activeSourceId?.let { mediaDao.getOutput(it) }
        if (draft.activeSourceId != null) {
            val parentGeneration = sourceOutput?.let { generationDao.get(it.generationId) }
            if (sourceOutput == null || parentGeneration?.conversationId != conversationId ||
                sourceOutput.mediaKind != MediaKind.IMAGE.name || !sourceOutput.remoteUrl.startsWith("https://")) {
                return Result.failure(GenerationSubmissionException(ErrorMapper.activeImageUnavailable()))
            }
            if (WorkflowCapability.IMAGE_TO_IMAGE !in adapter.descriptor.capabilities ||
                WorkflowRequestSchemas.find(draft.workflowId)?.fields?.none { it.name == "image_urls" } != false) {
                return Result.failure(GenerationSubmissionException(ErrorMapper.imageEditUnsupported()))
            }
        }
        val submittedDraft = if (sourceOutput == null) draft else draft.copy(
            attachments = listOf(GenerationAttachment(
                    id = "source-${sourceOutput.id}",
                    uri = sourceOutput.remoteUrl,
                    kind = MediaKind.IMAGE,
                    role = MediaRole.SOURCE,
                    remoteUrl = sourceOutput.remoteUrl,
                    isGeneratedOutput = true,
                )) + draft.attachments.filterNot { it.role == MediaRole.SOURCE && it.remoteUrl == sourceOutput.remoteUrl },
        )
        val now = System.currentTimeMillis()
        val generationId = UUID.randomUUID().toString()
        val parent = sourceOutput
            ?.let { output -> generationDao.get(output.generationId) }
            ?.takeIf { generation -> generation.conversationId == conversationId }
        val entity = submittedDraft.toEntity(generationId, conversationId, parent, now)

        var generationPostStarted = false
        var generationResponseReceived = false
        return try {
            localStore.insertDraft(entity, submittedDraft.attachments.toEntities(generationId))
            val uploadedDraft = when (val upload = attachmentUploader.uploadAll(submittedDraft.attachments)) {
                is AttachmentUploadResult.Uploaded -> submittedDraft.copy(attachments = upload.attachments)
                is AttachmentUploadResult.Failed -> return fail(entity, upload.error)
            }
            val uploadedPlan = submissionPlan(uploadedDraft, adapter).getOrElse { return fail(entity, it.toAppError()) }
            mediaDao.upsertAttachments(uploadedDraft.attachments.toEntities(generationId))

            generationPostStarted = true
            val response = service.submitWorkflow(uploadedPlan.endpointPath, uploadedPlan.request)
            generationResponseReceived = true
            require(response.requestId.isNotBlank()) { MISSING_REQUEST_ID_MESSAGE }
            require(response.requestId.matches(REQUEST_ID_PATTERN)) { INVALID_REQUEST_ID_MESSAGE }

            // A successful POST is the durable boundary. Keep its ID before any client-side
            // interpretation or polling can fail, so a later app launch can reconcile it.
            val statusUrl = response.statusUrl
                ?.takeIf(HiggsfieldUrlValidator::isApiUrl)
                ?: canonicalStatusUrl(response.requestId)
            val cancellationUrl = response.cancelUrl
                ?.takeIf(HiggsfieldUrlValidator::isApiUrl)
            localStore.markAccepted(
                generationId = generationId,
                requestId = response.requestId,
                statusUrl = statusUrl,
                cancellationUrl = cancellationUrl,
                correlationId = null,
                now = System.currentTimeMillis(),
            )
            // A status refresh never repeats the generation POST. It only reconciles accepted work.
            runCatching { statusSynchronizer.refresh(generationId) }
            val persisted = generationDao.get(generationId)
            Result.success(
                persisted?.toRecord(
                    brief = submittedDraft.creativeBrief,
                    attachments = mediaDao.attachmentsFor(generationId),
                    outputs = mediaDao.outputsFor(generationId),
                ) ?: entity.toRecord(submittedDraft.creativeBrief),
            )
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

    override suspend fun downloadOutput(output: GenerationOutput, destinationUri: String): Result<Unit> = try {
        require(output.remoteUrl.startsWith("https://")) { "Output URL must use HTTPS" }
        withContext(Dispatchers.IO) {
            OkHttpClient().newCall(Request.Builder().url(output.remoteUrl).build()).execute().use { response ->
                check(response.isSuccessful) { "Output download failed: ${response.code}" }
                val body = requireNotNull(response.body) { "Output download returned an empty body" }
                context.contentResolver.openOutputStream(Uri.parse(destinationUri)).use { destination ->
                    requireNotNull(destination) { "Selected download location is unavailable" }
                    body.byteStream().use { input -> input.copyTo(destination) }
                }
            }
        }
        check(mediaDao.setOutputLocalUri(output.id, destinationUri) == 1) { "Output no longer exists" }
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
            error.userMessage ?: error.diagnosticMessage,
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
    sourceOutputId = activeSourceId,
    workflowId = workflowId.value,
    instruction = instruction.trim(),
    composedPrompt = composedPromptOverride ?: PromptComposer.compose(
        creativeBrief,
        instruction,
        WorkflowCapability.NEGATIVE_PROMPT in WorkflowRegistry.find(workflowId)?.capabilities.orEmpty(),
    ),
    negativePrompt = if (composedPromptOverride != null) options.negativePrompt else PromptComposer.composeNegativePrompt(creativeBrief, options.negativePrompt),
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

private fun GenerationEntity.toRecord(brief: com.higgsfield.mobile.core.model.CreativeBrief) = GenerationRecord(
    id = id,
    parentGenerationId = parentGenerationId,
    branchRootId = branchRootId,
    draft = GenerationDraft(
        instruction = instruction,
        creativeBrief = brief,
        workflowId = com.higgsfield.mobile.core.model.WorkflowId(workflowId),
        activeSourceId = sourceOutputId,
        options = optionsSnapshotJson.toOptions().copy(negativePrompt = negativePrompt),
        composedPromptOverride = composedPrompt,
    ),
    status = status.toDomainStatus(errorMessage, errorCode),
    errorCode = errorCode,
)

private fun GenerationWithMedia.toRecord(brief: com.higgsfield.mobile.core.model.CreativeBrief) =
    generation.toRecord(brief, attachments, outputs)

private fun GenerationEntity.toRecord(
    brief: com.higgsfield.mobile.core.model.CreativeBrief,
    attachments: List<AttachmentEntity>,
    outputs: List<OutputEntity>,
) = GenerationRecord(
    id = id,
    parentGenerationId = parentGenerationId,
    branchRootId = branchRootId,
    draft = GenerationDraft(
        instruction = instruction,
        creativeBrief = brief,
        workflowId = com.higgsfield.mobile.core.model.WorkflowId(workflowId),
        attachments = attachments.map(AttachmentEntity::toDomainAttachment),
        activeSourceId = sourceOutputId,
        options = optionsSnapshotJson.toOptions().copy(negativePrompt = negativePrompt),
        composedPromptOverride = composedPrompt,
    ),
    status = status.toDomainStatus(errorMessage, errorCode, outputs),
    errorCode = errorCode,
)

private fun PersistedGenerationStatus.toDomainStatus(
    errorMessage: String?,
    errorCode: String? = null,
    outputs: List<OutputEntity> = emptyList(),
): GenerationStatus = when (this) {
    PersistedGenerationStatus.DRAFT -> GenerationStatus.Draft
    PersistedGenerationStatus.QUEUED -> GenerationStatus.Queued
    PersistedGenerationStatus.IN_PROGRESS -> GenerationStatus.InProgress()
    PersistedGenerationStatus.COMPLETED -> GenerationStatus.Completed(outputs.map(OutputEntity::toDomainOutput))
    PersistedGenerationStatus.NSFW -> GenerationStatus.Nsfw(errorMessage.orEmpty())
    PersistedGenerationStatus.CANCELED -> GenerationStatus.Canceled
    PersistedGenerationStatus.FAILED ->
        GenerationStatus.Failed(errorMessage.orEmpty(), retryable = ErrorMapper.isRetryable(errorCode))
    PersistedGenerationStatus.UNKNOWN_SUBMISSION_OUTCOME -> GenerationStatus.UnknownSubmissionOutcome(errorMessage.orEmpty())
}

private fun AttachmentEntity.toDomainAttachment() = GenerationAttachment(
    id = id,
    uri = localUri ?: remoteUrl.orEmpty(),
    kind = MediaKind.valueOf(mediaKind),
    role = com.higgsfield.mobile.core.model.MediaRole.valueOf(role),
    remoteUrl = remoteUrl,
)

private fun OutputEntity.toDomainOutput() = GenerationOutput(
    id = id,
    remoteUrl = remoteUrl,
    kind = MediaKind.valueOf(mediaKind),
    localUri = localUri,
)

private fun ConversationEntity.toBrief() = com.higgsfield.mobile.core.model.CreativeBrief(
    subject = briefSubject,
    style = briefStyle,
    mood = briefMood,
    cameraDirection = briefCameraDirection,
    requirements = briefRequirements,
    exclusions = briefExclusions,
    outputGoal = briefOutputGoal,
)

private fun String.toOptions(): GenerationOptions = runCatching {
    val values = Json.parseToJsonElement(this).jsonObject
    GenerationOptions(
        aspectRatio = values[ASPECT_RATIO_FIELD]?.jsonPrimitive?.contentOrNull ?: DEFAULT_ASPECT_RATIO,
        resolution = values[RESOLUTION_FIELD]?.jsonPrimitive?.contentOrNull,
        durationSeconds = values[DURATION_SECONDS_FIELD]?.jsonPrimitive?.intOrNull,
        seed = values[SEED_FIELD]?.jsonPrimitive?.longOrNull,
        negativePrompt = values[NEGATIVE_PROMPT_FIELD]?.jsonPrimitive?.contentOrNull,
    )
}.getOrDefault(GenerationOptions())

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
private const val DEFAULT_ASPECT_RATIO = "1:1"
private const val RESOLUTION_FIELD = "resolution"
private const val DURATION_SECONDS_FIELD = "durationSeconds"
private const val SEED_FIELD = "seed"
private const val NEGATIVE_PROMPT_FIELD = "negativePrompt"
private const val UNKNOWN_WORKFLOW_MESSAGE = "The selected workflow is not registered"
private const val MISSING_SCHEMA_MESSAGE = "The selected workflow has no verified submission schema"
private const val MISSING_ENDPOINT_MESSAGE = "The selected workflow has no verified submission endpoint"
private const val MISSING_REQUEST_ID_MESSAGE = "Accepted response is missing a request ID"
private const val INVALID_REQUEST_ID_MESSAGE = "Accepted response has an invalid request ID"
private val REQUEST_ID_PATTERN = Regex("[A-Za-z0-9-]{1,128}")

private fun canonicalStatusUrl(requestId: String): String =
    "https://${com.higgsfield.mobile.core.network.HiggsfieldNetwork.PLATFORM_HOST}/requests/$requestId/status"
