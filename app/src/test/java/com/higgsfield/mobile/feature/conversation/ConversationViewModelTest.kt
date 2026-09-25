package com.higgsfield.mobile.feature.conversation

import com.higgsfield.mobile.core.model.MediaKind
import com.higgsfield.mobile.core.model.MediaRole
import com.higgsfield.mobile.core.connectivity.ConnectivityStatusProvider
import com.higgsfield.mobile.R
import com.higgsfield.mobile.core.model.WorkflowCatalog
import com.higgsfield.mobile.core.model.WorkflowId
import com.higgsfield.mobile.core.model.WorkflowRegistry
import com.higgsfield.mobile.core.database.ConversationPersistence
import com.higgsfield.mobile.core.database.PersistedConversationSnapshot
import com.higgsfield.mobile.core.model.CreativeBrief
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
        viewModel.updatePrompt("Create an apple")

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
        viewModel.updatePrompt("Create an apple")
        viewModel.addDemoResult()

        viewModel.detachSource()

        val state = viewModel.state.value
        assertNull(state.activeSourceId)
        assertEquals(1, state.timeline.size)
        assertEquals(ConversationText.Resource(R.string.message_fresh_generation_started), state.message)
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

        viewModel.attachMedia(MediaRole.SOURCE, MediaKind.IMAGE, "subject.png")
        viewModel.attachMedia(MediaRole.MOTION_REFERENCE, MediaKind.VIDEO, "motion.mp4")

        assertEquals(
            listOf(
                DraftMediaAttachment(MediaRole.SOURCE, MediaKind.IMAGE, "subject.png"),
                DraftMediaAttachment(MediaRole.MOTION_REFERENCE, MediaKind.VIDEO, "motion.mp4"),
            ),
            viewModel.state.value.attachments,
        )
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
    private val snapshot = MutableStateFlow<PersistedConversationSnapshot?>(null)
    override fun observe(conversationId: String): Flow<PersistedConversationSnapshot?> = snapshot
    override suspend fun ensureConversation(conversationId: String, kind: MediaKind, initialWorkflowId: WorkflowId?) = Unit
    override suspend fun saveBrief(conversationId: String, brief: CreativeBrief) = Unit
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
}
