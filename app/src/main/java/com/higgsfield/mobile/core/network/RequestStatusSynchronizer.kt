package com.higgsfield.mobile.core.network

import com.higgsfield.mobile.core.database.GenerationDao
import com.higgsfield.mobile.core.database.GenerationEntity
import com.higgsfield.mobile.core.database.LocalGenerationStore
import com.higgsfield.mobile.core.database.OutputEntity
import com.higgsfield.mobile.core.database.PersistedGenerationStatus
import com.higgsfield.mobile.core.model.MediaKind
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import retrofit2.HttpException

/** Applies a verified remote status response to an already accepted local generation. */
@Singleton
class RequestStatusSynchronizer @Inject constructor(
    private val service: HiggsfieldService,
    private val generationDao: GenerationDao,
    private val localStore: LocalGenerationStore,
) {
    suspend fun refreshAcceptedRequests(): Boolean {
        val pending = localStore.acceptedPending()
        for (generation in pending) refresh(generation)
        return pending.isNotEmpty()
    }

    suspend fun refresh(generationId: String) {
        generationDao.get(generationId)?.let { generation -> refresh(generation) }
    }

    suspend fun refresh(generation: GenerationEntity) {
        val statusUrl = generation.statusUrl ?: return markProtocolFailure(generation, "Accepted request is missing a status URL")
        require(HiggsfieldUrlValidator.isApiUrl(statusUrl)) { "Refusing to poll a non-Higgsfield URL" }
        val remote = try {
            service.getRequestStatus(statusUrl)
        } catch (error: HttpException) {
            when (error.code()) {
                401 -> markProtocolFailure(generation, "Higgsfield credentials were rejected")
                404 -> markProtocolFailure(generation, "Higgsfield could not find this request")
                else -> throw error
            }
            return
        }
        require(remote.requestId == generation.requestId) { "Status response request ID does not match the local generation" }
        val now = System.currentTimeMillis()
        when (remote.status) {
            "queued" -> update(generation.id, PersistedGenerationStatus.QUEUED, now)
            "in_progress" -> update(generation.id, PersistedGenerationStatus.IN_PROGRESS, now)
            "completed" -> {
                val outputs = remote.toOutputs(generation.id, now)
                if (outputs.isEmpty()) markProtocolFailure(generation, "Completed request returned no media outputs")
                else localStore.applyCompleted(generation.conversationId, generation.id, outputs, now)
            }
            "failed" -> update(generation.id, PersistedGenerationStatus.FAILED, now, "remote_failed", remote.error)
            "nsfw" -> update(generation.id, PersistedGenerationStatus.NSFW, now, "nsfw", remote.error)
            "canceled" -> update(generation.id, PersistedGenerationStatus.CANCELED, now)
            else -> markProtocolFailure(generation, "Unknown remote request status: ${remote.status}")
        }
    }

    suspend fun cancel(generationId: String) {
        val generation = generationDao.get(generationId) ?: return
        require(generation.status == PersistedGenerationStatus.QUEUED) { "Only queued requests can be canceled" }
        val cancellationUrl = generation.cancellationUrl
            ?: return markProtocolFailure(generation, "Queued request is missing a cancellation URL")
        require(HiggsfieldUrlValidator.isApiUrl(cancellationUrl)) { "Refusing to cancel through a non-Higgsfield URL" }
        service.cancelRequest(cancellationUrl)
        update(generationId, PersistedGenerationStatus.CANCELED, System.currentTimeMillis())
    }

    private suspend fun markProtocolFailure(generation: GenerationEntity, message: String) =
        update(generation.id, PersistedGenerationStatus.FAILED, System.currentTimeMillis(), "remote_protocol", message)

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
