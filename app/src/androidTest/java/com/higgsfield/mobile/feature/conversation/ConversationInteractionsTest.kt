package com.higgsfield.mobile.feature.conversation

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.higgsfield.mobile.R
import com.higgsfield.mobile.core.model.CreativeBrief
import com.higgsfield.mobile.core.model.GenerationDraft
import com.higgsfield.mobile.core.model.GenerationOutput
import com.higgsfield.mobile.core.model.GenerationRecord
import com.higgsfield.mobile.core.model.GenerationStatus
import com.higgsfield.mobile.core.model.MediaKind
import com.higgsfield.mobile.core.model.MediaRole
import com.higgsfield.mobile.core.model.WorkflowCatalog
import com.higgsfield.mobile.core.model.WorkflowRegistry
import com.higgsfield.mobile.ui.theme.HiggsfieldTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ConversationInteractionsTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun promptInputEmitsTypedChangeEvent() {
        val events = mutableListOf<ConversationUiEvent>()
        compose.setContent {
            HiggsfieldTheme {
                ComposerDock(
                    state = ConversationUiState(composer = ComposerUiState(selectedWorkflow = previewWorkflow)),
                    motionEnabled = false,
                    onEvent = events::add,
                )
            }
        }

        compose.onNodeWithTag("conversation_prompt").performTextInput("A red apple")

        compose.runOnIdle { assertTrue(ConversationUiEvent.ChangePrompt("A red apple") in events) }
    }

    @Test
    fun headerNewChatInvokesCreationAction() {
        var createCount = 0
        compose.setContent {
            HiggsfieldTheme {
                ConversationHeader(ConversationUiState(), onEvent = {}, modelMenuOpen = false) { createCount++ }
            }
        }

        compose.onNodeWithTag("new_conversation").performClick()

        compose.runOnIdle { assertEquals(1, createCount) }
    }

    @Test
    fun composerShowsTypeAndModelSpecificReferenceCounts() {
        val workflow = WorkflowRegistry.find(WorkflowCatalog.SEEDANCE_2_REFERENCE.id)!!
        compose.setContent {
            HiggsfieldTheme {
                ComposerDock(
                    state = ConversationUiState(composer = ComposerUiState(
                        selectedWorkflow = workflow,
                        attachmentSlots = workflow.mediaRequirements,
                        attachments = listOf(
                            DraftMediaAttachment(MediaRole.REFERENCE, MediaKind.IMAGE, "content://photo/one", "one.jpg"),
                            DraftMediaAttachment(MediaRole.REFERENCE, MediaKind.IMAGE, "content://photo/two", "two.jpg"),
                            DraftMediaAttachment(MediaRole.VIDEO_REFERENCE, MediaKind.VIDEO, "content://video/one", "one.mp4"),
                        ),
                    )),
                    motionEnabled = false,
                    onEvent = {},
                )
            }
        }

        compose.onNodeWithText("Reference image: 2 of 9").assertExists()
        compose.onNodeWithText("Reference video: 1 of 3").assertExists()
    }

    @Test
    fun resultActionsKeepEditImageDistinctFromReuseParameters() {
        val events = mutableListOf<ConversationUiEvent>()
        val output = GenerationOutput("output-1", "https://example.test/apple.png", MediaKind.IMAGE)
        val draft = GenerationDraft("A red apple", CreativeBrief(), previewWorkflow.id)
        val item = TimelineItem(
            id = "generation-1",
            prompt = draft.instruction,
            modelName = previewWorkflow.displayName,
            stateLabel = ConversationText.Resource(R.string.status_completed),
            outputLabel = ConversationText.Resource(R.string.restored_image),
            output = output,
            sourceOutputId = output.id,
            lifecycle = GenerationStatus.Completed(listOf(output)),
            record = GenerationRecord("generation-1", draft = draft, status = GenerationStatus.Completed(listOf(output))),
        )
        compose.setContent {
            HiggsfieldTheme { TimelineCard(item, active = false, canEditImage = true, onEvent = events::add) }
        }

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        compose.onNodeWithText(context.getString(R.string.edit_image)).performClick()
        compose.onNodeWithText(context.getString(R.string.reuse_parameters)).performClick()

        compose.runOnIdle {
            assertEquals(listOf(ConversationUiEvent.EditImage(item), ConversationUiEvent.ReuseParameters(item)), events)
        }
    }
}
