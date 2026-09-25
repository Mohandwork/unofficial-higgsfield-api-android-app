package com.higgsfield.mobile.core.network

import com.higgsfield.mobile.core.database.GenerationDao
import com.higgsfield.mobile.core.database.GenerationEntity
import com.higgsfield.mobile.core.database.LocalGenerationStore
import com.higgsfield.mobile.core.database.OutputEntity
import com.higgsfield.mobile.core.database.PersistedGenerationStatus
import com.higgsfield.mobile.core.error.AppError
import com.higgsfield.mobile.core.error.ErrorMapper
import com.higgsfield.mobile.core.model.MediaKind
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import retrofit2.HttpException

interface GenerationRequestSynchronizer {
    suspend fun refreshAcceptedRequests(): Boolean
    suspend fun refresh(generationId: String)
    suspend fun cancel(generationId: String)
}

/** Applies a verified remote status response to an already accepted local generation. */
@Singleton
class RequestStatusSynchronizer @Inject constructor(
    private val service: HiggsfieldService,
    private val generationDao: GenerationDao,
    private val localStore: LocalGenerationStore,
) : GenerationRequestSynchronizer {
    override suspend fun refreshAcceptedRequests(): Boolean {
        val pending = localStore.acceptedPending()
        for (generation in pending) refresh(generation)
        return pending.isNotEmpty()
    }

    override suspend fun refresh(generationId: String) {
        generationDao.get(generationId)?.let { generation -> refresh(generation) }
    }

    suspend fun refresh(generation: GenerationEntity) {
        val statusUrl = generation.statusUrl ?: return markFailure(generation, ErrorMapper.protocol(MISSING_STATUS_URL_MESSAGE))
        require(HiggsfieldUrlValidator.isApiUrl(statusUrl)) { NON_HIGGSFIELD_POLL_MESSAGE }
        val remote = try {
            service.getRequestStatus(statusUrl)
        } catch (error: HttpException) {
            when (error.code()) {
                401 -> markFailure(generation, ErrorMapper.credentialsRejected())
                404 -> markFailure(generation, ErrorMapper.requestNotFound())
                else -> throw error
            }
            return
        }
        require(remote.requestId == generation.requestId) { REQUEST_ID_MISMATCH_MESSAGE }
        val now = System.currentTimeMillis()
        when (remote.status) {
            STATUS_QUEUED -> update(generation.id, PersistedGenerationStatus.QUEUED, now)
            STATUS_IN_PROGRESS -> update(generation.id, PersistedGenerationStatus.IN_PROGRESS, now)
            STATUS_COMPLETED -> {
                val outputs = remote.toOutputs(generation.id, now)
                if (outputs.isEmpty()) markFailure(generation, ErrorMapper.protocol(MISSING_COMPLETED_OUTPUTS_MESSAGE))
                else localStore.applyCompleted(generation.conversationId, generation.id, outputs, now)
            }
            STATUS_FAILED -> markFailure(generation, ErrorMapper.generationFailed(remote.error), now)
            STATUS_NSFW -> markFailure(generation, ErrorMapper.moderated(remote.error), now, PersistedGenerationStatus.NSFW)
            STATUS_CANCELED -> update(generation.id, PersistedGenerationStatus.CANCELED, now)
            else -> markFailure(generation, ErrorMapper.protocol("$UNKNOWN_STATUS_PREFIX${remote.status}"))
        }
    }

    override suspend fun cancel(generationId: String) {
        val generation = generationDao.get(generationId) ?: return
        require(generation.status == PersistedGenerationStatus.QUEUED) { ONLY_QUEUED_CANCELLATION_MESSAGE }
        val cancellationUrl = generation.cancellationUrl
            ?: return markFailure(generation, ErrorMapper.protocol(MISSING_CANCELLATION_URL_MESSAGE))
        require(HiggsfieldUrlValidator.isApiUrl(cancellationUrl)) { NON_HIGGSFIELD_CANCEL_MESSAGE }
        service.cancelRequest(cancellationUrl)
        update(generationId, PersistedGenerationStatus.CANCELED, System.currentTimeMillis())
    }

    private suspend fun markFailure(
        generation: GenerationEntity,
        error: AppError,
        now: Long = System.currentTimeMillis(),
        status: PersistedGenerationStatus = PersistedGenerationStatus.FAILED,
    ) = update(generation.id, status, now, error.code, error.diagnosticMessage)

    private suspend fun update(
        generationId: String,
        status: PersistedGenerationStatus,
        now: Long,
        errorCode: String? = null,
        errorMessage: String? = null,
    ) {
        check(generationDao.updateStatus(generationId, status, errorCode, errorMessage, now) == 1)
    }
}

private const val STATUS_QUEUED = "queued"
private const val STATUS_IN_PROGRESS = "in_progress"
private const val STATUS_COMPLETED = "completed"
private const val STATUS_FAILED = "failed"
private const val STATUS_NSFW = "nsfw"
private const val STATUS_CANCELED = "canceled"
private const val MISSING_STATUS_URL_MESSAGE = "Accepted request is missing a status URL"
private const val NON_HIGGSFIELD_POLL_MESSAGE = "Refusing to poll a non-Higgsfield URL"
private const val REQUEST_ID_MISMATCH_MESSAGE = "Status response request ID does not match the local generation"
private const val MISSING_COMPLETED_OUTPUTS_MESSAGE = "Completed request returned no media outputs"
private const val UNKNOWN_STATUS_PREFIX = "Unknown remote request status: "
private const val ONLY_QUEUED_CANCELLATION_MESSAGE = "Only queued requests can be canceled"
private const val MISSING_CANCELLATION_URL_MESSAGE = "Queued request is missing a cancellation URL"
private const val NON_HIGGSFIELD_CANCEL_MESSAGE = "Refusing to cancel through a non-Higgsfield URL"

object HiggsfieldUrlValidator {
    fun isApiUrl(url: String): Boolean = runCatching {
        val parsed = url.toHttpUrlOrNull()
        parsed?.isHttps == true && parsed.host == HiggsfieldNetwork.API_HOST
    }.getOrDefault(false)
}

private fun RemoteRequestStatus.toOutputs(generationId: String, now: Long): List<OutputEntity> = buildList {
    images.forEachIndexed { index, output -> add(output.toEntity(generationId, MediaKind.IMAGE, index, now)) }
    video?.let { add(it.toEntity(generationId, MediaKind.VIDEO, size, now)) }
    audio?.let { add(it.toEntity(generationId, MediaKind.AUDIO, size, now)) }
    audios.forEachIndexed { index, output -> add(output.toEntity(generationId, MediaKind.AUDIO, size + index, now)) }
}

private fun RemoteMediaOutput.toEntity(
    generationId: String,
    kind: MediaKind,
    index: Int,
    now: Long,
) = OutputEntity(
    id = "$generationId-output-$index",
    generationId = generationId,
    mediaKind = kind.name,
    remoteUrl = url,
    createdAtEpochMillis = now,
)
