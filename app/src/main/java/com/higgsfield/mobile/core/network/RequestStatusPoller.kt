package com.higgsfield.mobile.core.network

import com.higgsfield.mobile.core.database.GenerationDao
import com.higgsfield.mobile.core.database.PersistedGenerationStatus
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException

/** Foreground polling for accepted work. Submission is deliberately outside this shared component. */
@Singleton
class RequestStatusPoller @Inject constructor(
    private val synchronizer: RequestStatusSynchronizer,
    private val generationDao: GenerationDao,
    private val policy: PollingPolicy = PollingPolicy(),
) {
    /** Reconcile every accepted request while the app is open, including chats not currently shown. */
    suspend fun pollAcceptedRequests() = supervisorScope {
        generationDao.getAcceptedPending().forEach { generation ->
            launch {
                try {
                    pollUntilTerminal(generation.id)
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Exception) {
                    // The persisted request remains available for a later foreground retry.
                }
            }
        }
    }

    suspend fun pollUntilTerminal(generationId: String) {
        var previousDelay: Long? = null
        while (currentCoroutineContext().isActive) {
            try {
                synchronizer.refresh(generationId)
            } catch (error: Throwable) {
                if (!error.isRetryableStatusFailure()) throw error
            }
            val status = generationDao.get(generationId)?.status ?: return
            if (status.isTerminal()) return
            previousDelay = policy.nextDelayMillis(previousDelay)
            delay(previousDelay)
        }
    }
}

private fun PersistedGenerationStatus.isTerminal(): Boolean = this in setOf(
    PersistedGenerationStatus.COMPLETED,
    PersistedGenerationStatus.FAILED,
    PersistedGenerationStatus.NSFW,
    PersistedGenerationStatus.CANCELED,
    PersistedGenerationStatus.UNKNOWN_SUBMISSION_OUTCOME,
)

private fun Throwable.isRetryableStatusFailure(): Boolean =
    this is IOException || (this is HttpException && code() >= 500)
