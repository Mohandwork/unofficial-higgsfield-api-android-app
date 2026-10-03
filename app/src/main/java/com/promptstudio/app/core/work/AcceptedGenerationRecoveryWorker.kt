package com.promptstudio.app.core.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.Constraints
import com.promptstudio.app.core.network.RequestStatusSynchronizer
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/** Recovery only polls requests that were already accepted; it never submits a draft. */
@HiltWorker
class AcceptedGenerationRecoveryWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted parameters: WorkerParameters,
    private val synchronizer: RequestStatusSynchronizer,
) : CoroutineWorker(appContext, parameters) {
    override suspend fun doWork(): Result = try {
        if (synchronizer.refreshAcceptedRequests()) Result.retry() else Result.success()
    } catch (_: IOException) {
        Result.retry()
    }
}

class AcceptedGenerationRecoveryScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    fun enqueue() {
        val request = OneTimeWorkRequestBuilder<AcceptedGenerationRecoveryWorker>()
            .setConstraints(Constraints(NetworkType.CONNECTED))
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.KEEP, request)
    }

    private companion object {
        const val WORK_NAME = "accepted-generation-recovery"
    }
}
