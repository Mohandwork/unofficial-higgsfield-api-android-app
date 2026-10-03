package com.higgsfield.mobile.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.higgsfield.mobile.core.model.MediaKind
import com.higgsfield.mobile.core.model.MediaRole
import com.higgsfield.mobile.core.model.CreativeBrief
import com.higgsfield.mobile.core.model.GenerationOptions
import com.higgsfield.mobile.core.model.WorkflowCatalog
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.flow.first
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PersistenceDatabaseTest {
    private lateinit var database: HiggsfieldDatabase
    private lateinit var store: LocalGenerationStore

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, HiggsfieldDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        store = LocalGenerationStore(
            database,
            database.conversationDao(),
            database.generationDao(),
            database.mediaDao(),
        )
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun `conversation upsert preserves child history`() = runTest {
        val conversation = conversation("conversation")
        database.conversationDao().upsert(conversation)
        database.generationDao().insert(generation("generation", conversation.id))

        database.conversationDao().upsert(conversation.copy(title = "Renamed"))

        assertEquals("Renamed", database.conversationDao().get(conversation.id)?.title)
        assertNotNull(database.generationDao().get("generation"))
    }

    @Test
    fun `accepted update reports missing generation`() = runTest {
        val changed = database.generationDao().markAccepted(
            generationId = "missing",
            status = PersistedGenerationStatus.QUEUED,
            requestId = "request",
            statusUrl = "https://example.invalid/status",
            cancellationUrl = null,
            correlationId = null,
            now = 2L,
        )
        assertEquals(0, changed)
    }

    @Test
    fun `multiple completed images retain current source and require selection`() = runTest {
        val conversation = conversation("conversation")
        database.conversationDao().upsert(conversation)
        store.insertDraft(generation("first", conversation.id), emptyList())
        store.applyCompleted(
            conversation.id,
            "first",
            listOf(output("first-output", "first")),
            now = 2L,
        )
        store.insertDraft(generation("second", conversation.id, parent = "first"), emptyList())
        store.applyCompleted(
            conversation.id,
            "second",
            listOf(output("choice-a", "second"), output("choice-b", "second")),
            now = 3L,
        )

        val restored = database.conversationDao().get(conversation.id)!!
        assertEquals("first-output", restored.activeSourceOutputId)
        assertTrue(restored.requiresSourceSelection)
    }

    @Test
    fun `failed completion transaction does not persist mismatched outputs`() = runTest {
        val conversation = conversation("conversation")
        database.conversationDao().upsert(conversation)
        store.insertDraft(generation("generation", conversation.id), emptyList())

        runCatching {
            store.applyCompleted(
                conversation.id,
                "generation",
                listOf(output("wrong", "different-generation")),
                now = 2L,
            )
        }

        assertNull(database.mediaDao().getOutput("wrong"))
        assertEquals(PersistedGenerationStatus.DRAFT, database.generationDao().get("generation")?.status)
    }

    @Test
    fun `composer draft is restored per conversation and deleted with its chat`() = runTest {
        val persistence = RoomConversationPersistence(
            database, database.conversationDao(), database.generationDao(), store,
        )
        persistence.ensureConversation("image-chat", MediaKind.IMAGE, WorkflowCatalog.SOUL.id)
        persistence.ensureConversation("video-chat", MediaKind.VIDEO, null)
        val draft = PersistedComposerDraft(
            prompt = "A red apple",
            options = GenerationOptions(aspectRatio = "4:5", seed = 42L),
            attachments = listOf(PersistedDraftAttachment(MediaRole.REFERENCE, MediaKind.IMAGE, "content://media/apple", "apple.jpg", "https://example.test/apple.jpg")),
        )

        persistence.saveDraft("image-chat", draft)

        assertEquals(draft, persistence.observe("image-chat").first()?.draft)
        assertEquals(PersistedComposerDraft(), persistence.observe("video-chat").first()?.draft)
        persistence.deleteConversation("image-chat")
        assertNull(database.conversationDao().get("image-chat"))
    }

    @Test
    fun `reusing parameters saves model brief draft and detached source together`() = runTest {
        val persistence = RoomConversationPersistence(database, database.conversationDao(), database.generationDao(), store)
        persistence.ensureConversation("image-chat", MediaKind.IMAGE, WorkflowCatalog.SOUL.id)
        database.conversationDao().setActiveSource("image-chat", "old-output", false, 2L)
        val brief = CreativeBrief(mood = "Warm")
        val draft = PersistedComposerDraft(prompt = "Use warm light", options = GenerationOptions(seed = 42L))

        persistence.reuseParameters("image-chat", WorkflowCatalog.QWEN_IMAGE_3_EDIT.id, brief, draft)

        val snapshot = persistence.observe("image-chat").first()!!
        assertEquals(WorkflowCatalog.QWEN_IMAGE_3_EDIT.id, snapshot.selectedWorkflowId)
        assertEquals(brief, snapshot.brief)
        assertEquals(draft, snapshot.draft)
        assertNull(snapshot.activeSourceOutputId)
    }

    @Test
    fun `failed draft write rolls back reused model brief and source`() = runTest {
        val persistence = RoomConversationPersistence(database, database.conversationDao(), database.generationDao(), store)
        persistence.ensureConversation("image-chat", MediaKind.IMAGE, WorkflowCatalog.SOUL.id)
        database.conversationDao().setActiveSource("image-chat", "old-output", false, 2L)
        database.openHelper.writableDatabase.execSQL("""
            CREATE TRIGGER fail_reused_draft BEFORE INSERT ON conversation_drafts
            BEGIN SELECT RAISE(ABORT, 'draft write failed'); END
        """.trimIndent())

        val result = runCatching {
            persistence.reuseParameters("image-chat", WorkflowCatalog.QWEN_IMAGE_3_EDIT.id,
                CreativeBrief(mood = "Warm"), PersistedComposerDraft(prompt = "New prompt"))
        }

        assertTrue(result.isFailure)
        val snapshot = persistence.observe("image-chat").first()!!
        assertEquals(WorkflowCatalog.SOUL.id, snapshot.selectedWorkflowId)
        assertEquals(CreativeBrief(), snapshot.brief)
        assertEquals("old-output", snapshot.activeSourceOutputId)
        assertEquals(PersistedComposerDraft(), snapshot.draft)
    }

    @Test
    fun `looking for a recent chat does not create one`() = runTest {
        val persistence = RoomConversationPersistence(database, database.conversationDao(), database.generationDao(), store)

        assertNull(persistence.mostRecentConversation(MediaKind.IMAGE))
        assertTrue(persistence.observeConversations().first().isEmpty())
    }

    @Test
    fun `first prompt creates a chat with its initial composer state`() = runTest {
        val persistence = RoomConversationPersistence(database, database.conversationDao(), database.generationDao(), store)
        val brief = CreativeBrief(mood = "Warm")
        val draft = PersistedComposerDraft(prompt = "A red kite", options = GenerationOptions(seed = 42L))

        val id = persistence.createConversationFromDraft(MediaKind.IMAGE, WorkflowCatalog.QWEN_IMAGE_3_EDIT.id, brief, draft)

        val snapshot = persistence.observe(id).first()!!
        assertEquals(WorkflowCatalog.QWEN_IMAGE_3_EDIT.id, snapshot.selectedWorkflowId)
        assertEquals(brief, snapshot.brief)
        assertEquals(draft, snapshot.draft)
        assertEquals(id, persistence.mostRecentConversation(MediaKind.IMAGE))
    }

    @Test
    fun `failed initial draft insert does not leave an empty chat`() = runTest {
        val persistence = RoomConversationPersistence(database, database.conversationDao(), database.generationDao(), store)
        database.openHelper.writableDatabase.execSQL("""
            CREATE TRIGGER fail_initial_draft BEFORE INSERT ON conversation_drafts
            BEGIN SELECT RAISE(ABORT, 'draft write failed'); END
        """.trimIndent())

        val result = runCatching {
            persistence.createConversationFromDraft(MediaKind.IMAGE, WorkflowCatalog.SOUL.id,
                CreativeBrief(), PersistedComposerDraft(prompt = "A red kite"))
        }

        assertTrue(result.isFailure)
        assertTrue(persistence.observeConversations().first().isEmpty())
    }

    private fun conversation(id: String) = ConversationEntity(
        id = id,
        mediaKind = MediaKind.IMAGE.name,
        title = "Images",
        createdAtEpochMillis = 1L,
        updatedAtEpochMillis = 1L,
    )

    private fun generation(id: String, conversationId: String, parent: String? = null) = GenerationEntity(
        id = id,
        conversationId = conversationId,
        parentGenerationId = parent,
        branchRootId = parent ?: id,
        workflowId = WorkflowCatalog.QWEN_IMAGE_3_EDIT.id.value,
        instruction = "Change only the apple skin from green to red; preserve the composition, lighting, background, and single-object framing",
        composedPrompt = "Change only the apple skin from green to red; preserve the composition, lighting, background, and single-object framing",
        optionsSnapshotJson = "{}",
        createdAtEpochMillis = 1L,
        updatedAtEpochMillis = 1L,
    )

    private fun output(id: String, generationId: String) = OutputEntity(
        id = id,
        generationId = generationId,
        mediaKind = MediaKind.IMAGE.name,
        remoteUrl = "https://example.invalid/$id.jpg",
        createdAtEpochMillis = 1L,
    )
}
