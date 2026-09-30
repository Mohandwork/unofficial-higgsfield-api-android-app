package com.higgsfield.mobile.feature.conversation

import com.higgsfield.mobile.R
import com.higgsfield.mobile.core.model.GenerationOptions
import com.higgsfield.mobile.core.model.DirectModelRoutes
import com.higgsfield.mobile.core.model.MediaKind
import com.higgsfield.mobile.core.model.MediaRole
import com.higgsfield.mobile.core.model.WorkflowCatalog
import com.higgsfield.mobile.core.model.WorkflowRegistry
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
        assertEquals("local-attachment-reference-content://reference", draft.attachments.single().id)
        assertTrue(draft.attachments.single().uri.startsWith("content://"))
    }

    @Test
    fun `qwen edit exposes its three source photos without a second unlimited photo slot`() {
        val qwenEdit = WorkflowRegistry.find(WorkflowCatalog.QWEN_IMAGE_3_EDIT.id)!!

        assertEquals(listOf(MediaRole.SOURCE), useCase.slotsFor(qwenEdit).map { it.role })
        assertEquals(3, useCase.slotsFor(qwenEdit).single().maximumCount)
    }

    @Test
    fun `kling image reference does not claim an undocumented upload maximum`() {
        val kling = WorkflowRegistry.find(WorkflowCatalog.KLING_O3.id)!!

        assertEquals(null, useCase.slotsFor(kling).single().maximumCount)
    }

    @Test
    fun `switching to MiniMax keeps supported values and removes unsupported Wan controls`() {
        val options = GenerationOptions(aspectRatio = "adaptive", resolution = "1080p", durationSeconds = 20,
            modelOptions = mapOf("generate_audio" to "false", "aigc_watermark" to "true"))

        val retained = useCase.retainOptionsFor(options, DirectModelRoutes.miniImage)

        assertEquals("adaptive", retained.aspectRatio)
        assertEquals(null, retained.resolution)
        assertEquals(null, retained.durationSeconds)
        assertEquals(mapOf("aigc_watermark" to "true"), retained.modelOptions)
        assertEquals(listOf(MediaRole.START_FRAME, MediaRole.END_FRAME), useCase.slotsFor(DirectModelRoutes.miniImage).map { it.role })
    }
}
