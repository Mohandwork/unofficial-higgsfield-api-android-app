package com.promptstudio.app.core.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.promptstudio.app.core.database.ConversationEntity
import com.promptstudio.app.core.database.GenerationEntity
import com.promptstudio.app.core.database.PromptStudioDatabase
import com.promptstudio.app.core.database.LocalGenerationStore
import com.promptstudio.app.core.database.OutputEntity
import com.promptstudio.app.core.database.PersistedGenerationStatus
import com.promptstudio.app.core.model.CreativeBrief
import com.promptstudio.app.core.model.GenerationAttachment
import com.promptstudio.app.core.model.GenerationDraft
import com.promptstudio.app.core.model.GenerationOptions
import com.promptstudio.app.core.model.GenerationStatus
import com.promptstudio.app.core.model.MediaKind
import com.promptstudio.app.core.model.MediaRole
import com.promptstudio.app.core.model.WorkflowCatalog
import com.promptstudio.app.core.network.ApiCredentials
import com.promptstudio.app.core.network.AttachmentBinaryUploader
import com.promptstudio.app.core.network.GenerationRequestSynchronizer
import com.promptstudio.app.core.network.ProviderNetwork
import com.promptstudio.app.core.network.ProviderService
import com.promptstudio.app.core.network.LocalAttachmentSource
import com.promptstudio.app.core.network.SecureAttachmentUploader
import java.io.InputStream
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okhttp3.RequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RoomGenerationRepositoryTest {
    private lateinit var database: PromptStudioDatabase
    private lateinit var server: MockWebServer
    private lateinit var service: ProviderService
    private lateinit var synchronizer: RecordingSynchronizer
    private lateinit var repository: RoomGenerationRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, PromptStudioDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        server = MockWebServer().also(MockWebServer::start)
        service = ProviderNetwork.createService(
            credentials = ApiCredentials("test-key-id", "test-secret"),
            baseUrl = server.url("/").toString(),
            authorizedHosts = setOf(server.hostName),
        )
        synchronizer = RecordingSynchronizer()
        repository = RoomGenerationRepository(
            context = ApplicationProvider.getApplicationContext(),
            service = service,
            attachmentUploader = SecureAttachmentUploader(service, NoOpBinaryUploader, NoOpLocalSource),
            localStore = LocalGenerationStore(
                database,
                database.conversationDao(),
                database.generationDao(),
                database.mediaDao(),
            ),
            conversationDao = database.conversationDao(),
            generationDao = database.generationDao(),
            mediaDao = database.mediaDao(),
            statusSynchronizer = synchronizer,
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
        database.close()
    }

    @Test
    fun `accepted submission persists and reconstructs the complete draft`() = runTest {
        database.conversationDao().upsert(conversation(CONVERSATION_ID))
        server.enqueue(
            MockResponse().setBody(
                """{"status":"queued","request_id":"request-1","status_url":"https://api.higgsfield.ai/requests/request-1/status"}""",
            ),
        )
        val draft = GenerationDraft(
            instruction = INSTRUCTION,
            creativeBrief = CreativeBrief(subject = "green apple", style = "product photograph"),
            workflowId = WorkflowCatalog.SOUL_V2.id,
            attachments = listOf(
                GenerationAttachment(
                    id = ATTACHMENT_ID,
                    uri = "content://media/apple",
                    kind = MediaKind.IMAGE,
                    role = MediaRole.REFERENCE,
                    remoteUrl = "https://media.example.test/apple.png",
                ),
            ),
            options = GenerationOptions(aspectRatio = "4:5", seed = 7L),
        )

        val result = repository.submit(CONVERSATION_ID, draft)

        assertEquals("/higgsfield-ai/soul/v2/standard", server.takeRequest().path)
        assertNotNull(result.getOrNull())
        assertEquals("https://media.example.test/apple.png", result.getOrThrow().draft.attachments.single().remoteUrl)
        assertEquals(1, synchronizer.refreshedGenerationIds.size)
        val persisted = database.generationDao().get(result.getOrThrow().id)!!
        assertEquals(PersistedGenerationStatus.QUEUED, persisted.status)
        assertEquals("request-1", persisted.requestId)
        assertEquals(1, database.mediaDao().attachmentsFor(persisted.id).size)

        database.generationDao().update(persisted.copy(status = PersistedGenerationStatus.COMPLETED))
        database.mediaDao().upsertOutputs(
            listOf(OutputEntity("output-1", persisted.id, MediaKind.IMAGE.name, "https://media.example.test/result.png", createdAtEpochMillis = 2L)),
        )
        val restored = repository.observeConversation(CONVERSATION_ID).first().single()

        assertEquals("green apple", restored.draft.creativeBrief.subject)
        assertEquals("4:5", restored.draft.options.aspectRatio)
        assertEquals(7L, restored.draft.options.seed)
        assertEquals("https://media.example.test/apple.png", restored.draft.attachments.single().remoteUrl)
        assertTrue(restored.status is GenerationStatus.Completed)
        assertEquals("https://media.example.test/result.png", (restored.status as GenerationStatus.Completed).outputs.single().remoteUrl)
    }

    @Test
    fun `accepted submission keeps a canonical recovery URL when the returned URL is unusable`() = runTest {
        database.conversationDao().upsert(conversation(CONVERSATION_ID))
        server.enqueue(
            MockResponse().setBody(
                """{"status":"queued","request_id":"request-2","status_url":"https://storage.example.test/request-status"}""",
            ),
        )

        val result = repository.submit(
            CONVERSATION_ID,
            GenerationDraft(INSTRUCTION, CreativeBrief(), WorkflowCatalog.SOUL_V2.id),
        )

        assertTrue(result.isSuccess)
        val persisted = database.generationDao().get(result.getOrThrow().id)!!
        assertEquals(PersistedGenerationStatus.QUEUED, persisted.status)
        assertEquals("request-2", persisted.requestId)
        assertEquals("https://platform.higgsfield.ai/requests/request-2/status", persisted.statusUrl)
        assertEquals(listOf(persisted.id), synchronizer.refreshedGenerationIds)
    }

    @Test
    fun `ambiguous submission disconnect is persisted without a generation retry`() = runTest {
        database.conversationDao().upsert(conversation(CONVERSATION_ID))
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))

        val result = repository.submit(
            CONVERSATION_ID,
            GenerationDraft(INSTRUCTION, CreativeBrief(), WorkflowCatalog.SOUL_V2.id),
        )

        assertFalse(result.isSuccess)
        val persisted = database.generationDao().observeForConversation(CONVERSATION_ID).first().single()
        assertEquals(PersistedGenerationStatus.UNKNOWN_SUBMISSION_OUTCOME, persisted.status)
        assertEquals(1, server.requestCount)
        assertEquals(0, synchronizer.refreshedGenerationIds.size)
    }

    @Test
    fun `editing a completed image sends its URL and keeps the parent generation`() = runTest {
        database.conversationDao().upsert(conversation(CONVERSATION_ID))
        database.generationDao().insert(GenerationEntity(
            id = "original", conversationId = CONVERSATION_ID, branchRootId = "original",
            workflowId = WorkflowCatalog.MARKETING_STUDIO_2_ALPHA.id.value,
            instruction = "An apple", composedPrompt = "An apple", optionsSnapshotJson = "{}",
            status = PersistedGenerationStatus.COMPLETED, createdAtEpochMillis = 1L, updatedAtEpochMillis = 1L,
        ))
        database.mediaDao().upsertOutputs(listOf(OutputEntity(
            id = "source-output", generationId = "original", mediaKind = MediaKind.IMAGE.name,
            remoteUrl = "https://media.example.test/apple.png", createdAtEpochMillis = 1L,
        )))
        server.enqueue(MockResponse().setBody("""{"status":"queued","request_id":"edit-1"}"""))

        val result = repository.submit(CONVERSATION_ID, GenerationDraft(
            instruction = "Make the apple red", creativeBrief = CreativeBrief(),
            workflowId = WorkflowCatalog.MARKETING_STUDIO_2_ALPHA.id,
            activeSourceId = "source-output",
        ))

        assertTrue(result.isSuccess)
        assertTrue(server.takeRequest().body.readUtf8().contains("\"image_urls\":[\"https://media.example.test/apple.png\"]"))
        assertEquals("original", database.generationDao().get(result.getOrThrow().id)?.parentGenerationId)
        assertEquals("source-output", result.getOrThrow().draft.activeSourceId)
    }

    @Test
    fun `incompatible active image is rejected before a billable POST`() = runTest {
        database.conversationDao().upsert(conversation(CONVERSATION_ID))
        database.generationDao().insert(GenerationEntity(
            id = "original", conversationId = CONVERSATION_ID, branchRootId = "original",
            workflowId = WorkflowCatalog.SOUL.id.value,
            instruction = "An apple", composedPrompt = "An apple", optionsSnapshotJson = "{}",
            status = PersistedGenerationStatus.COMPLETED, createdAtEpochMillis = 1L, updatedAtEpochMillis = 1L,
        ))
        database.mediaDao().upsertOutputs(listOf(OutputEntity(
            id = "source-output", generationId = "original", mediaKind = MediaKind.IMAGE.name,
            remoteUrl = "https://media.example.test/apple.png", createdAtEpochMillis = 1L,
        )))

        val result = repository.submit(CONVERSATION_ID, GenerationDraft(
            instruction = "Make the apple red", creativeBrief = CreativeBrief(),
            workflowId = WorkflowCatalog.SOUL.id, activeSourceId = "source-output",
        ))

        assertTrue(result.isFailure)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `retry retains the original composed prompt after the conversation brief changes`() = runTest {
        database.conversationDao().upsert(conversation(CONVERSATION_ID))
        server.enqueue(MockResponse().setBody("""{"status":"queued","request_id":"first-1"}"""))
        server.enqueue(MockResponse().setBody("""{"status":"queued","request_id":"retry-1"}"""))
        val original = GenerationDraft(
            instruction = "Make a painting", creativeBrief = CreativeBrief(subject = "forest", exclusions = "people"),
            workflowId = WorkflowCatalog.QWEN_IMAGE_3.id,
        )
        assertTrue(repository.submit(CONVERSATION_ID, original).isSuccess)
        val firstBody = server.takeRequest().body.readUtf8()
        database.conversationDao().updateBrief(CONVERSATION_ID, "desert", "", "", "", "", "text", "", 2L)

        val restored = repository.observeConversation(CONVERSATION_ID).first().single()
        assertTrue(repository.submit(CONVERSATION_ID, restored.draft).isSuccess)

        assertEquals(firstBody, server.takeRequest().body.readUtf8())
    }

    private fun conversation(id: String) = ConversationEntity(
        id = id,
        mediaKind = MediaKind.IMAGE.name,
        title = "Image exploration",
        briefSubject = "green apple",
        briefStyle = "product photograph",
        createdAtEpochMillis = 1L,
        updatedAtEpochMillis = 1L,
    )
}

private class RecordingSynchronizer : GenerationRequestSynchronizer {
    val refreshedGenerationIds = mutableListOf<String>()

    override suspend fun refreshAcceptedRequests(): Boolean = false
    override suspend fun refresh(generationId: String) {
        refreshedGenerationIds += generationId
    }
    override suspend fun cancel(generationId: String) = Unit
}

private object NoOpBinaryUploader : AttachmentBinaryUploader {
    override fun upload(uploadUrl: String, headers: Map<String, String>, contentType: String, body: RequestBody) = Unit
}

private object NoOpLocalSource : LocalAttachmentSource {
    override fun contentType(uri: String): String? = null
    override fun contentLength(uri: String): Long? = null
    override fun open(uri: String): InputStream? = null
}

private const val CONVERSATION_ID = "conversation-1"
private const val ATTACHMENT_ID = "reference-1"
private const val INSTRUCTION = "Studio product photo of one ripe green apple centered on a matte cream background, soft daylight, no text"
