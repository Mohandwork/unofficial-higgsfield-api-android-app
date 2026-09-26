package com.higgsfield.mobile.feature.conversation

import com.higgsfield.mobile.R
import com.higgsfield.mobile.core.connectivity.ConnectivityStatusProvider
import com.higgsfield.mobile.core.data.GenerationRepository
import com.higgsfield.mobile.core.data.GenerationSubmissionException
import com.higgsfield.mobile.core.database.ConversationPersistence
import com.higgsfield.mobile.core.database.ConversationSummary
import com.higgsfield.mobile.core.database.PersistedComposerDraft
import com.higgsfield.mobile.core.database.PersistedConversationSnapshot
import com.higgsfield.mobile.core.database.PersistedGenerationStatus
import com.higgsfield.mobile.core.database.PersistedTimelineItem
import com.higgsfield.mobile.core.error.ErrorMapper
import com.higgsfield.mobile.core.model.CreativeBrief
import com.higgsfield.mobile.core.model.EstimateState
import com.higgsfield.mobile.core.model.GenerationDraft
import com.higgsfield.mobile.core.model.GenerationOptions
import com.higgsfield.mobile.core.model.GenerationOutput
import com.higgsfield.mobile.core.model.GenerationRecord
import com.higgsfield.mobile.core.model.MediaKind
import com.higgsfield.mobile.core.model.MediaRole
import com.higgsfield.mobile.core.model.WorkflowCatalog
import com.higgsfield.mobile.core.model.WorkflowId
import com.higgsfield.mobile.core.model.WorkflowRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ConversationViewModelTest {
    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `successful image demo becomes active source for next instruction`() {
        val viewModel = ConversationViewModel(FakeConversationPersistence())
        viewModel.initialize(MediaKind.IMAGE)
        viewModel.updatePrompt("Studio product photo of one ripe green apple centered on a matte cream background, soft daylight, no text")

        viewModel.addDemoResult()

        val state = viewModel.state.value
        assertEquals(1, state.timeline.size)
        assertNotNull(state.activeSourceId)
        assertEquals(ConversationText.Resource(R.string.demo_image, listOf(1)), state.activeSourceLabel)
        assertEquals("Studio product photo of one ripe green apple centered on a matte cream background, soft daylight, no text", state.prompt)
    }

    @Test
    fun `detaching source starts a fresh generation without deleting history`() {
        val viewModel = ConversationViewModel(FakeConversationPersistence())
        viewModel.initialize(MediaKind.IMAGE)
        viewModel.updatePrompt("Studio product photo of one ripe green apple centered on a matte cream background, soft daylight, no text")
        viewModel.addDemoResult()

        viewModel.detachSource()

        val state = viewModel.state.value
        assertNull(state.activeSourceId)
        assertEquals(1, state.timeline.size)
        assertEquals(ConversationText.Resource(R.string.message_fresh_generation_started), state.message)
    }

    @Test
    fun `a restored failed item without output is not treated as an active source`() {
        val persistence = FakeConversationPersistence()
        val viewModel = ConversationViewModel(persistence)
        viewModel.initialize(MediaKind.IMAGE)

        persistence.emit(
            PersistedConversationSnapshot(
                id = "image-default",
                title = "Image exploration",
                mediaKind = MediaKind.IMAGE,
                brief = CreativeBrief(),
                selectedWorkflowId = WorkflowCatalog.SOUL.id,
                activeSourceOutputId = null,
                requiresSourceSelection = false,
                timeline = listOf(PersistedTimelineItem("failed", "Try", WorkflowCatalog.SOUL.id, PersistedGenerationStatus.FAILED, null, null, null)),
            ),
        )

        assertNull(viewModel.state.value.activeSourceId)
    }

    @Test
    fun `restoring a completed image carries its prompt once`() {
        val persistence = FakeConversationPersistence()
        val viewModel = ConversationViewModel(persistence)
        viewModel.initialize(MediaKind.IMAGE)
        val snapshot = PersistedConversationSnapshot(
            id = "image-default",
            title = "Image exploration",
            mediaKind = MediaKind.IMAGE,
            brief = CreativeBrief(),
            selectedWorkflowId = WorkflowCatalog.SOUL.id,
            activeSourceOutputId = "output-1",
            requiresSourceSelection = false,
            timeline = listOf(PersistedTimelineItem("image-1", "A quiet forest", WorkflowCatalog.SOUL.id, PersistedGenerationStatus.COMPLETED, "output-1", MediaKind.IMAGE, null)),
        )

        persistence.emit(snapshot)
        assertEquals("A quiet forest", viewModel.state.value.prompt)

        viewModel.updatePrompt("")
        persistence.emit(snapshot.copy(title = "Renamed"))
        assertEquals("", viewModel.state.value.prompt)
    }

    @Test
    fun `selecting old output establishes branch parent`() {
        val viewModel = ConversationViewModel(FakeConversationPersistence())
        viewModel.initialize(MediaKind.IMAGE)
        viewModel.updatePrompt("First")
        viewModel.addDemoResult()
        val first = viewModel.state.value.timeline.single()
        viewModel.selectWorkflow(WorkflowRegistry.find(WorkflowCatalog.QWEN_IMAGE_3_EDIT.id)!!)
        viewModel.updatePrompt("Second")
        viewModel.addDemoResult()

        viewModel.useOutput(first)
        viewModel.updatePrompt("Branch edit")
        viewModel.addDemoResult()

        val branch = viewModel.state.value.timeline.last()
        assertEquals(first.id, branch.parentId)
    }

    @Test
    fun `selecting an image carries its prompt into an empty composer`() {
        val viewModel = ConversationViewModel(FakeConversationPersistence())
        viewModel.initialize(MediaKind.IMAGE)
        viewModel.updatePrompt("A green apple on a cream background")
        viewModel.addDemoResult()
        val image = viewModel.state.value.timeline.single().copy(
            output = GenerationOutput("output-1", "https://example.test/apple.png", MediaKind.IMAGE),
            sourceOutputId = "output-1",
        )
        viewModel.updatePrompt("")

        viewModel.useOutput(image)

        assertEquals("A green apple on a cream background", viewModel.state.value.prompt)
        assertEquals("output-1", viewModel.currentDraft()?.activeSourceId)
        viewModel.updatePrompt("Keep my own edit")
        viewModel.useOutput(image)
        assertEquals("Keep my own edit", viewModel.state.value.prompt)
    }

    @Test
    fun `switching conversations restores each persisted composer draft`() {
        val persistence = FakeConversationPersistence()
        val viewModel = ConversationViewModel(persistence)
        viewModel.initialize(MediaKind.IMAGE)
        viewModel.selectWorkflow(WorkflowRegistry.find(WorkflowCatalog.MARKETING_STUDIO_2_ALPHA.id)!!)
        viewModel.updatePrompt("An apple")
        viewModel.attachMedia(MediaRole.REFERENCE, MediaKind.IMAGE, "content://draft/apple", "apple.jpg")
        viewModel.updateOptions(GenerationOptions(aspectRatio = "4:5"))

        viewModel.initialize(MediaKind.VIDEO)
        viewModel.updatePrompt("A walking dog")
        viewModel.initialize(MediaKind.IMAGE)
        persistence.emit(PersistedConversationSnapshot(
            id = "image-default", title = "Images", mediaKind = MediaKind.IMAGE,
            brief = CreativeBrief(), selectedWorkflowId = WorkflowCatalog.MARKETING_STUDIO_2_ALPHA.id,
            activeSourceOutputId = null, requiresSourceSelection = false, timeline = emptyList(),
            draft = persistence.savedDrafts.getValue("image-default"),
        ))

        assertEquals("An apple", viewModel.state.value.prompt)
        assertEquals("content://draft/apple", viewModel.state.value.attachments.single().uri)
        assertEquals("4:5", viewModel.state.value.options.aspectRatio)
        assertEquals("A walking dog", persistence.savedDrafts.getValue("video-default").prompt)
    }

    @Test
    fun `validated connectivity updates the workspace state`() {
        val connectivity = FakeConnectivityStatusProvider(isOnline = false)
        val viewModel = ConversationViewModel(FakeConversationPersistence(), connectivity)

        viewModel.initialize(MediaKind.IMAGE)
        assertEquals(false, viewModel.state.value.isOnline)

        connectivity.setOnline(true)
        assertEquals(true, viewModel.state.value.isOnline)
    }

    @Test
    fun `motion workflow assigns source image and motion video to separate draft slots`() {
        val viewModel = ConversationViewModel(FakeConversationPersistence())
        viewModel.initialize(MediaKind.VIDEO)
        viewModel.selectWorkflow(WorkflowRegistry.find(WorkflowCatalog.KLING_2_6_MOTION.id)!!)

        viewModel.attachMedia(MediaRole.SOURCE, MediaKind.IMAGE, "content://draft/subject", "subject.png")
        viewModel.attachMedia(MediaRole.MOTION_REFERENCE, MediaKind.VIDEO, "content://draft/motion", "motion.mp4")

        assertEquals(
            listOf(
                DraftMediaAttachment(MediaRole.SOURCE, MediaKind.IMAGE, "content://draft/subject", "subject.png"),
                DraftMediaAttachment(MediaRole.MOTION_REFERENCE, MediaKind.VIDEO, "content://draft/motion", "motion.mp4"),
            ),
            viewModel.state.value.attachments,
        )
        val draft = viewModel.currentDraft()!!
        assertEquals(MediaRole.SOURCE, draft.attachments.first().role)
        assertEquals("content://draft/motion", draft.attachments.last().uri)
    }

    @Test
    fun `generate submits the selected draft and shows the centralized authentication error`() {
        val repository = FakeGenerationRepository(Result.failure(GenerationSubmissionException(ErrorMapper.credentialsRejected())))
        val viewModel = ConversationViewModel(
            persistence = FakeConversationPersistence(),
            generationRepository = repository,
        )
        viewModel.initialize(MediaKind.IMAGE)
        viewModel.updatePrompt("Studio product photo of one ripe green apple centered on a matte cream background, soft daylight, no text")

        viewModel.submitGeneration()

        assertEquals("image-default", repository.conversationId)
        assertEquals(WorkflowCatalog.SOUL.id, repository.draft?.workflowId)
        assertEquals(
            ConversationText.Resource(R.string.error_credentials_rejected),
            viewModel.state.value.message,
        )
        assertTrue(!viewModel.state.value.isSubmitting)
    }
}

private class FakeConnectivityStatusProvider(isOnline: Boolean) : ConnectivityStatusProvider {
    private val mutableOnline = MutableStateFlow(isOnline)
    override val isOnline = mutableOnline

    fun setOnline(value: Boolean) {
        mutableOnline.value = value
    }
}

private class FakeConversationPersistence : ConversationPersistence {
    private val snapshots = mutableMapOf<String, MutableStateFlow<PersistedConversationSnapshot?>>()
    val savedDrafts = mutableMapOf<String, PersistedComposerDraft>()
    override fun observe(conversationId: String): Flow<PersistedConversationSnapshot?> =
        snapshots.getOrPut(conversationId) { MutableStateFlow(null) }
    override fun observeConversations(): Flow<List<ConversationSummary>> = emptyFlow()
    override suspend fun ensureConversation(conversationId: String, kind: MediaKind, initialWorkflowId: WorkflowId?) = Unit
    override suspend fun createConversation(kind: MediaKind, initialWorkflowId: WorkflowId?): String = "${kind.name.lowercase()}-new"
    override suspend fun mostRecentConversation(kind: MediaKind, initialWorkflowId: WorkflowId?): String = "${kind.name.lowercase()}-new"
    override suspend fun renameConversation(conversationId: String, title: String) = Unit
    override suspend fun deriveTitleFromFirstPrompt(conversationId: String, prompt: String) = Unit
    override suspend fun deleteConversation(conversationId: String) = Unit
    override suspend fun saveBrief(conversationId: String, brief: CreativeBrief) = Unit
    override suspend fun saveDraft(conversationId: String, draft: PersistedComposerDraft) {
        savedDrafts[conversationId] = draft
    }
    override suspend fun saveSelectedWorkflow(conversationId: String, workflowId: WorkflowId) = Unit
    override suspend fun saveCompletedDemo(
        conversationId: String,
        generationId: String,
        parentGenerationId: String?,
        workflowId: WorkflowId,
        instruction: String,
        outputKind: MediaKind,
    ) = Unit
    override suspend fun selectActiveSource(conversationId: String, outputId: String?) = Unit

    fun emit(value: PersistedConversationSnapshot) {
        snapshots.getOrPut(value.id) { MutableStateFlow(null) }.value = value
    }
}

private class FakeGenerationRepository(
    private val submitResult: Result<GenerationRecord>,
) : GenerationRepository {
    var conversationId: String? = null
    var draft: GenerationDraft? = null

    override fun observeConversation(conversationId: String) = emptyFlow<List<GenerationRecord>>()
    override suspend fun estimate(draft: GenerationDraft) = EstimateState.Idle
    override suspend fun submit(conversationId: String, draft: GenerationDraft): Result<GenerationRecord> {
        this.conversationId = conversationId
        this.draft = draft
        return submitResult
    }
    override suspend fun cancel(generationId: String): Result<Unit> = Result.success(Unit)
    override suspend fun downloadOutput(output: GenerationOutput, destinationUri: String): Result<Unit> = Result.success(Unit)
    override suspend fun resumeAcceptedRequests() = Unit
}
