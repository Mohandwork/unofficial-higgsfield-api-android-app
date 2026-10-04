package com.promptstudio.app.core.network

import com.promptstudio.app.R
import com.promptstudio.app.core.model.CreativeBrief
import com.promptstudio.app.core.model.GenerationDraft
import com.promptstudio.app.core.model.WorkflowCatalog
import com.promptstudio.app.core.model.WorkflowRegistry
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class SchemaWorkflowAdapterTest {
    private val adapter = SchemaWorkflowAdapter(
        descriptor = WorkflowRegistry.find(WorkflowCatalog.SOUL_V2.id)!!,
        schema = WorkflowRequestSchemas.soulV2Standard,
    )

    @Test
    fun `soul v2 fixture maps through the generic schema adapter`() {
        val request = adapter.toRequest(
            GenerationDraft(
                instruction = "Make the light soft.",
                creativeBrief = CreativeBrief(subject = "A ceramic bird", style = "Editorial still life"),
                workflowId = WorkflowCatalog.SOUL_V2.id,
            ),
        )

        assertEquals(
            "{\"prompt\":\"Subject: A ceramic bird\\nStyle: Editorial still life\\nMake the light soft.\"}",
            Json.encodeToString(request),
        )
    }

    @Test
    fun `missing generic schema input is represented by a central application error`() {
        val validation = adapter.validate(GenerationDraft(" ", CreativeBrief(), WorkflowCatalog.SOUL_V2.id))

        assertFalse(validation.isValid)
        assertEquals("instruction_required", validation.errors.single().code)
        assertEquals(R.string.error_instruction_required, validation.errors.single().messageResId)
    }

    @Test
    fun `schema registry finds each verified workflow without a dedicated adapter`() {
        assertEquals(WorkflowRequestSchemas.soulStandard, WorkflowRequestSchemas.find(WorkflowCatalog.SOUL.id))
        assertEquals(WorkflowRequestSchemas.soulV2Standard, WorkflowRequestSchemas.find(WorkflowCatalog.SOUL_V2.id))
    }

    @Test
    fun `seedance reference workflow sends photo and video arrays with selected controls`() {
        val referenceAdapter = SchemaWorkflowAdapter(
            WorkflowRegistry.find(WorkflowCatalog.SEEDANCE_2_REFERENCE.id)!!,
            WorkflowRequestSchemas.seedance2Reference,
        )
        val draft = GenerationDraft(
            instruction = "Move slowly",
            creativeBrief = CreativeBrief(),
            workflowId = WorkflowCatalog.SEEDANCE_2_REFERENCE.id,
            attachments = listOf(
                GenerationAttachment("photo-1", "content://one", MediaKind.IMAGE, remoteUrl = "https://example.com/one.jpg"),
                GenerationAttachment("photo-2", "content://two", MediaKind.IMAGE, remoteUrl = "https://example.com/two.jpg"),
                GenerationAttachment("video-1", "content://clip", MediaKind.VIDEO, remoteUrl = "https://example.com/clip.mp4"),
            ),
            options = GenerationOptions(durationSeconds = 8, resolution = "1080p", aspectRatio = "16:9"),
        )

        assertEquals(
            "{\"prompt\":\"Move slowly\",\"image_urls\":[\"https://example.com/one.jpg\",\"https://example.com/two.jpg\"],\"video_urls\":[\"https://example.com/clip.mp4\"],\"duration\":8,\"resolution\":\"1080p\",\"aspect_ratio\":\"16:9\"}",
            Json.encodeToString(referenceAdapter.toRequest(draft)),
        )
        assertFalse(referenceAdapter.validate(draft.copy(attachments = emptyList())).isValid)
        assertFalse(referenceAdapter.toRequest(draft.copy(instruction = "")).containsKey("prompt"))
    }

    @Test
    fun `seedance 2_5 reference sends audio and selected output controls`() {
        val referenceAdapter = SchemaWorkflowAdapter(
            WorkflowRegistry.find(WorkflowCatalog.SEEDANCE_2_5_REFERENCE.id)!!,
            WorkflowRequestSchemas.seedance2_5Reference,
        )
        val draft = GenerationDraft(
            instruction = "Animate the scene",
            creativeBrief = CreativeBrief(),
            workflowId = WorkflowCatalog.SEEDANCE_2_5_REFERENCE.id,
            attachments = listOf(
                GenerationAttachment("photo", "content://photo", MediaKind.IMAGE, remoteUrl = "https://example.com/photo.jpg"),
                GenerationAttachment("audio", "content://audio", MediaKind.AUDIO, MediaRole.AUDIO, "https://example.com/audio.mp3"),
            ),
            options = GenerationOptions(resolution = "1080p", modelOptions = mapOf("generate_audio" to "false", "output_format" to "mov")),
        )

        assertEquals(
            "{\"prompt\":\"Animate the scene\",\"image_urls\":[\"https://example.com/photo.jpg\"],\"audio_urls\":[\"https://example.com/audio.mp3\"],\"resolution\":\"1080p\",\"aspect_ratio\":\"1:1\",\"generate_audio\":false,\"output_format\":\"mov\"}",
            Json.encodeToString(referenceAdapter.toRequest(draft)),
        )
    }
}
