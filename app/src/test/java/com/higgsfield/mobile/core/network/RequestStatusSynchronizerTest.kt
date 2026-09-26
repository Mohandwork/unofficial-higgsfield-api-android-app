package com.higgsfield.mobile.core.network

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.higgsfield.mobile.core.database.ConversationEntity
import com.higgsfield.mobile.core.database.GenerationEntity
import com.higgsfield.mobile.core.database.HiggsfieldDatabase
import com.higgsfield.mobile.core.database.LocalGenerationStore
import com.higgsfield.mobile.core.database.PersistedGenerationStatus
import com.higgsfield.mobile.core.error.ErrorMapper
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import retrofit2.HttpException
import retrofit2.Response

@RunWith(RobolectricTestRunner::class)
class RequestStatusSynchronizerTest {
    private lateinit var database: HiggsfieldDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, HiggsfieldDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun `denied cancellation refreshes stale queued request without marking it canceled`() = runTest {
        val id = "generation-1"
        database.conversationDao().upsert(
            ConversationEntity(id = "conversation-1", mediaKind = "VIDEO", title = "Video", createdAtEpochMillis = 1, updatedAtEpochMillis = 1),
        )
        database.generationDao().insert(
            GenerationEntity(
                id = id,
                conversationId = "conversation-1",
                branchRootId = id,
                workflowId = "test-workflow",
                instruction = "test",
                composedPrompt = "test",
                optionsSnapshotJson = "{}",
                status = PersistedGenerationStatus.QUEUED,
                requestId = "request-1",
                statusUrl = "https://platform.higgsfield.ai/requests/request-1/status",
                cancellationUrl = "https://platform.higgsfield.ai/requests/request-1/cancel",
                createdAtEpochMillis = 1,
                updatedAtEpochMillis = 1,
            ),
        )
        val service = DeniedCancellationService()
        val synchronizer = RequestStatusSynchronizer(
            service,
            database.generationDao(),
            LocalGenerationStore(database, database.conversationDao(), database.generationDao(), database.mediaDao()),
        )

        val error = try {
            synchronizer.cancel(id)
            null
        } catch (rejected: HttpException) {
            rejected
        }

        assertEquals(400, error?.code())
        assertEquals("Request is in progress", error?.let(ErrorMapper::from)?.userMessage)
        assertTrue(service.statusFetched)
        assertEquals(PersistedGenerationStatus.IN_PROGRESS, database.generationDao().get(id)?.status)
    }

    private class DeniedCancellationService : HiggsfieldService {
        var statusFetched = false

        override suspend fun generateUploadUrl(request: UploadUrlRequest): UploadUrlResponse = error("Unexpected upload")
        override suspend fun submitWorkflow(endpointPath: String, request: JsonObject): RemoteRequestStatus = error("Unexpected submission")
        override suspend fun getRequestStatus(statusUrl: String): RemoteRequestStatus {
            statusFetched = true
            return RemoteRequestStatus(status = "in_progress", requestId = "request-1")
        }
        override suspend fun cancelRequest(cancelUrl: String): Unit = throw HttpException(
            Response.error<Unit>(400, "{\"detail\":\"Request is in progress\"}".toResponseBody()),
        )
    }
}
