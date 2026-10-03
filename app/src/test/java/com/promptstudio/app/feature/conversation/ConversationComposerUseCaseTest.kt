package com.promptstudio.app.feature.conversation

import com.promptstudio.app.R
import com.promptstudio.app.core.model.GenerationOptions
import com.promptstudio.app.core.model.MediaKind
import com.promptstudio.app.core.model.MediaRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationComposerUseCaseTest {
    private val useCase = ConversationComposerUseCase()

    @Test
    fun `workflow selection keeps only compatible reference slots and options`() {
        val state = ConversationUiState(
            composer = ComposerUiState(
                attachments = listOf(
                    DraftMediaAttachment(MediaRole.REFERENCE, MediaKind.IMAGE, "content://reference", "reference"),
                    DraftMediaAttachment(MediaRole.MOTION_REFERENCE, MediaKind.VIDEO, "content://motion", "motion"),
                ),
                options = GenerationOptions(aspectRatio = "16:9", resolution = "2k", durationSeconds = 8),
            ),
        )

        val selection = useCase.selectWorkflow(state, previewWorkflow)

        assertEquals(listOf(MediaRole.REFERENCE), selection.attachments.map { it.role })
        assertEquals("16:9", selection.options.aspectRatio)
        assertEquals("2k", selection.options.resolution)
        assertEquals(null, selection.options.durationSeconds)
    }

    @Test
    fun `draft contains one source output plus stable local attachment identity`() {
        val source = TimelineItem(
            id = "generation-1",
            prompt = "Original",
            modelName = previewWorkflow.displayName,
            stateLabel = ConversationText.Resource(R.string.status_completed),
            sourceOutputId = "output-1",
        )
        val state = ConversationUiState(
            composer = ComposerUiState(
                selectedWorkflow = previewWorkflow,
                prompt = "Change the sky",
                activeSourceId = source.id,
                attachments = listOf(DraftMediaAttachment(MediaRole.REFERENCE, MediaKind.IMAGE, "content://reference", "reference")),
            ),
            generation = GenerationUiState(timeline = listOf(source)),
        )

        val draft = useCase.draftFor(state, persistedSourceOutputId = null)!!

        assertEquals("output-1", draft.activeSourceId)
        assertEquals("local-attachment-reference", draft.attachments.single().id)
        assertTrue(draft.attachments.single().uri.startsWith("content://"))
    }
}
