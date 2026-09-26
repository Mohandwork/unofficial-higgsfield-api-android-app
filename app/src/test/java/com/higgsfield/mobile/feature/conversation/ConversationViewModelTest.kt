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
import com.higgsfield.mobile.core.model.GenerationAttachment
import com.higgsfield.mobile.core.model.GenerationOptions
import com.higgsfield.mobile.core.model.GenerationOutput
import com.higgsfield.mobile.core.model.GenerationRecord
import com.higgsfield.mobile.core.model.GenerationStatus
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
        assertEquals("", state.prompt)
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
    fun `restoring a completed image keeps the composer empty`() {
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
        assertEquals("", viewModel.state.value.prompt)
        assertEquals("image-1", viewModel.state.value.activeSourceId)

        viewModel.updatePrompt("Add mist between the trees")
        persistence.emit(snapshot.copy(title = "Renamed"))
        assertEquals("Add mist between the trees", viewModel.state.value.prompt)
    }

    @Test
    fun `selecting old output establishes branch parent`() {
        val viewModel = ConversationViewModel(FakeConversationPersistence())
        viewModel.initialize(MediaKind.IMAGE)
        viewModel.updatePrompt("First")
        viewModel.addDemoResult()
        val first = viewModel.state.value.timeline.single().let { item ->
            item.copy(output = GenerationOutput(item.sourceOutputId!!, "https://example.test/first.png", MediaKind.IMAGE))
        }
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
    fun `selecting an image keeps the composer ready for a new edit`() {
        val viewModel = ConversationViewModel(FakeConversationPersistence())
        viewModel.initialize(MediaKind.IMAGE)
        viewModel.updatePrompt("A green apple on a cream background")
        viewModel.addDemoResult()
        val image = viewModel.state.value.timeline.single().copy(
            output = GenerationOutput("output-1", "https://example.test/apple.png", MediaKind.IMAGE),
            sourceOutputId = "output-1",
        )
        viewModel.selectWorkflow(WorkflowRegistry.find(WorkflowCatalog.QWEN_IMAGE_3_EDIT.id)!!)
        viewModel.updatePrompt("")

        viewModel.useOutput(image)

        assertEquals("", viewModel.state.value.prompt)
        assertEquals("output-1", viewModel.currentDraft()?.activeSourceId)
        viewModel.updatePrompt("Keep my own edit")
        viewModel.useOutput(image)
        assertEquals("Keep my own edit", viewModel.state.value.prompt)
    }

    @Test
    fun `image edit selection requires a compatible model`() {
        val viewModel = ConversationViewModel(FakeConversationPersistence())
        viewModel.initialize(MediaKind.IMAGE)
        val item = TimelineItem(
            id = "image-1", prompt = "Apple", modelName = WorkflowCatalog.SOUL.displayName,
            stateLabel = ConversationText.Resource(R.string.status_completed),
            output = GenerationOutput("output-1", "https://example.test/apple.png", MediaKind.IMAGE),
            sourceOutputId = "output-1",
        )

        viewModel.useOutput(item)

        assertNull(viewModel.state.value.activeSourceId)
        assertEquals(ConversationText.Resource(R.string.message_choose_compatible_editor), viewModel.state.value.message)
    }

    @Test
    fun `video reuse restores the original prompt and model without treating video as an image source`() {
        val persistence = FakeConversationPersistence()
        val viewModel = ConversationViewModel(persistence)
        viewModel.initialize(MediaKind.VIDEO)
        val draft = GenerationDraft(
            instruction = "Orbit around a lighthouse",
            creativeBrief = CreativeBrief(mood = "Stormy"),
            workflowId = WorkflowCatalog.SEEDANCE_2_5.id,
            options = GenerationOptions(durationSeconds = 8),
        )
        val item = TimelineItem(
            id = "video-1",
            prompt = draft.instruction,
            modelName = WorkflowCatalog.SEEDANCE_2_5.displayName,
            stateLabel = ConversationText.Resource(R.string.status_completed),
            output = GenerationOutput("output-1", "https://example.test/video.mp4", MediaKind.VIDEO),
            sourceOutputId = "output-1",
            record = GenerationRecord("video-1", draft = draft, status = GenerationStatus.Completed(emptyList())),
        )

        viewModel.useOutput(item)
        assertNull(viewModel.state.value.activeSourceId)
        viewModel.reuseParameters(item)

        assertEquals(draft.instruction, viewModel.state.value.prompt)
        assertEquals(draft.workflowId, viewModel.state.value.selectedWorkflow?.id)
        assertEquals(8, viewModel.state.value.options.durationSeconds)
        assertEquals("Stormy", viewModel.state.value.brief.mood)
        assertNull(viewModel.state.value.activeSourceId)
        assertEquals(draft.instruction, persistence.savedDrafts["video-default"]?.prompt)
    }

    @Test
    fun `reuse parameters restores text and settings without old image references`() {
        val persistence = FakeConversationPersistence()
        val viewModel = ConversationViewModel(persistence)
        viewModel.initialize(MediaKind.IMAGE)
        persistence.selectedSourceOutputId = "old-output"
        val draft = GenerationDraft(
            instruction = "Make the light warmer",
            creativeBrief = CreativeBrief(mood = "Calm"),
            workflowId = WorkflowCatalog.QWEN_IMAGE_3_EDIT.id,
            attachments = listOf(
                GenerationAttachment("reference-1", "content://photos/original", MediaKind.IMAGE, MediaRole.REFERENCE),
                GenerationAttachment("reference-2", "content://photos/original", MediaKind.IMAGE, MediaRole.SOURCE),
            ),
            activeSourceId = "old-output",
            options = GenerationOptions(aspectRatio = "16:9"),
        )
        val item = TimelineItem(
            id = "result-1",
            prompt = draft.instruction,
            modelName = WorkflowCatalog.QWEN_IMAGE_3_EDIT.displayName,
            stateLabel = ConversationText.Resource(R.string.status_completed),
            output = GenerationOutput("latest-output", "https://example.test/latest.png", MediaKind.IMAGE),
            sourceOutputId = "latest-output",
            record = GenerationRecord("result-1", draft = draft, status = GenerationStatus.Completed(emptyList())),
        )

        viewModel.reuseParameters(item)

        assertEquals(draft.instruction, viewModel.state.value.prompt)
        assertEquals(draft.workflowId, viewModel.state.value.selectedWorkflow?.id)
        assertEquals("16:9", viewModel.state.value.options.aspectRatio)
        assertTrue(viewModel.state.value.attachments.isEmpty())
        assertNull(viewModel.state.value.activeSourceId)
        assertTrue(viewModel.currentDraft()?.attachments.orEmpty().isEmpty())
        assertNull(viewModel.currentDraft()?.activeSourceId)
        assertTrue(persistence.savedDrafts["image-default"]?.attachments.orEmpty().isEmpty())
        assertNull(persistence.selectedSourceOutputId)

        val staleSnapshot = PersistedConversationSnapshot(
            id = "image-default",
            title = "Image exploration",
            mediaKind = MediaKind.IMAGE,
            brief = draft.creativeBrief,
            selectedWorkflowId = draft.workflowId,
            activeSourceOutputId = "old-output",
            requiresSourceSelection = false,
            timeline = listOf(PersistedTimelineItem(
                "old-generation", "Original", draft.workflowId,
                PersistedGenerationStatus.COMPLETED, "old-output", MediaKind.IMAGE, null,
            )),
        )
        persistence.emit(staleSnapshot)
        assertNull(viewModel.state.value.activeSourceId)
        persistence.emit(staleSnapshot.copy(activeSourceOutputId = null))
        assertNull(viewModel.state.value.activeSourceId)
    }

    @Test
    fun `workspace transition ends only after its saved conversation arrives`() {
        val persistence = FakeConversationPersistence()
        val viewModel = ConversationViewModel(persistence)
        viewModel.initialize(MediaKind.IMAGE, "image-new")
        assertTrue(viewModel.state.value.isTransitioning)

        persistence.emit(PersistedConversationSnapshot(
            id = "image-new", title = "New image chat", mediaKind = MediaKind.IMAGE,
            brief = CreativeBrief(), selectedWorkflowId = WorkflowCatalog.SOUL.id,
            activeSourceOutputId = null, requiresSourceSelection = false, timeline = emptyList(),
        ))

        assertEquals("New image chat", viewModel.state.value.conversationTitle)
        assertEquals(false, viewModel.state.value.isTransitioning)
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

    @Test
    fun `accepted generation clears submitted prompt for the next edit`() {
        val originalPrompt = "A green apple on a cream background"
        val repository = FakeGenerationRepository(Result.success(GenerationRecord(
            id = "generation-1",
            draft = GenerationDraft(originalPrompt, CreativeBrief(), WorkflowCatalog.SOUL.id),
            status = GenerationStatus.Queued,
        )))
        val persistence = FakeConversationPersistence()
        val viewModel = ConversationViewModel(persistence = persistence, generationRepository = repository)
        viewModel.initialize(MediaKind.IMAGE)
        viewModel.updatePrompt(originalPrompt)

        viewModel.submitGeneration()

        assertEquals(originalPrompt, repository.draft?.instruction)
        assertEquals("", viewModel.state.value.prompt)
        assertEquals("", persistence.savedDrafts["image-default"]?.prompt)
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
    var selectedSourceOutputId: String? = null
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
    override suspend fun selectActiveSource(conversationId: String, outputId: String?) {
        selectedSourceOutputId = outputId
    }

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
