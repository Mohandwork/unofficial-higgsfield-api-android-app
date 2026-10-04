package com.promptstudio.app.feature.conversation

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.promptstudio.app.R
import com.promptstudio.app.core.model.CreativeBrief
import com.promptstudio.app.core.model.GenerationDraft
import com.promptstudio.app.core.model.GenerationOutput
import com.promptstudio.app.core.model.GenerationRecord
import com.promptstudio.app.core.model.GenerationStatus
import com.promptstudio.app.core.model.MediaKind
import com.promptstudio.app.ui.theme.PromptStudioTheme
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
            PromptStudioTheme {
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
            PromptStudioTheme {
                ConversationHeader(ConversationUiState(), onEvent = {}, modelMenuOpen = false) { createCount++ }
            }
        }

        compose.onNodeWithTag("new_conversation").performClick()

        compose.runOnIdle { assertEquals(1, createCount) }
    }

    @Test
    fun chatRowRenamesAndSwipeRequestsRemovalOfThatChat() {
        val events = mutableListOf<ConversationUiEvent>()
        val removals = mutableListOf<String>()
        val older = ConversationSummary("image-older", MediaKind.IMAGE, "Older chat", System.currentTimeMillis())
        compose.setContent {
            HiggsfieldTheme {
                ConversationRail(
                    state = ConversationUiState(chat = ChatUiState(
                        mediaKind = MediaKind.IMAGE,
                        id = "image-current",
                        conversations = listOf(older),
                    )),
                    onSelectConversation = { _, _ -> },
                    onCreateConversation = {},
                    onRemoveConversation = { id, _ -> removals += id },
                    onEvent = events::add,
                )
            }
        }

        compose.onNodeWithTag("rename_chat_image-older").performClick()
        compose.onNodeWithTag("rename_chat_input").performTextClearance()
        compose.onNodeWithTag("rename_chat_input").performTextInput("Renamed")
        compose.onNodeWithText("Done").performClick()
        compose.runOnIdle { assertTrue(ConversationUiEvent.RenameConversation("image-older", "Renamed") in events) }

        compose.onNodeWithTag("chat_row_image-older").performTouchInput { swipeLeft() }
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        compose.onNodeWithText(context.getString(R.string.remove_chat_confirm)).performClick()
        compose.runOnIdle { assertEquals(listOf("image-older"), removals) }
    }

    @Test
    fun modelPickerNavigatesFamiliesAndSelectsAWorkflow() {
        val events = mutableListOf<ConversationUiEvent>()
        val workflows = WorkflowRegistry.forKind(MediaKind.VIDEO)
        compose.setContent {
            HiggsfieldTheme {
                ConversationHeader(
                    state = ConversationUiState(
                        chat = ChatUiState(mediaKind = MediaKind.VIDEO),
                        composer = ComposerUiState(workflows = workflows, selectedWorkflow = workflows.first()),
                    ),
                    onEvent = events::add,
                    modelMenuOpen = true,
                    onCreateConversation = {},
                )
            }
        }

        compose.onNodeWithTag("model_kling-3").assertDoesNotExist()
        compose.onNodeWithTag("model_family_KLING").performClick()
        compose.onNodeWithTag("model_kling-3").assertExists()
        compose.onNodeWithTag("model_family_back").performClick()
        compose.onNodeWithTag("model_family_SEEDANCE").performClick()
        compose.onNodeWithTag("model_seedance-2-reference").performClick()

        compose.runOnIdle {
            assertTrue(ConversationUiEvent.SelectWorkflow(WorkflowRegistry.find(WorkflowCatalog.SEEDANCE_2_REFERENCE.id)!!) in events)
        }
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
            PromptStudioTheme { TimelineCard(item, active = false, canEditImage = true, onEvent = events::add) }
        }

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        compose.onNodeWithText(context.getString(R.string.edit_image)).performClick()
        compose.onNodeWithText(context.getString(R.string.reuse_parameters)).performClick()

        compose.runOnIdle {
            assertEquals(listOf(ConversationUiEvent.EditImage(item), ConversationUiEvent.ReuseParameters(item)), events)
        }
    }
}
