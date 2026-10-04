package com.higgsfield.mobile.core.network

import com.higgsfield.mobile.core.model.DirectModelRoutes
import com.higgsfield.mobile.core.model.GenerationAttachment
import com.higgsfield.mobile.core.model.GenerationDraft
import com.higgsfield.mobile.core.model.GenerationOptions
import com.higgsfield.mobile.core.model.CreativeBrief
import com.higgsfield.mobile.core.model.MediaKind
import com.higgsfield.mobile.core.model.MediaRole
import com.higgsfield.mobile.core.model.WorkflowRegistry
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DirectModelRoutesTest {
    private fun draft(model: com.higgsfield.mobile.core.model.WorkflowDescriptor, vararg attachments: GenerationAttachment) =
        GenerationDraft("A camera move", CreativeBrief(), model.id, attachments.toList())

    private fun media(id: String, kind: MediaKind, role: MediaRole) =
        GenerationAttachment(id, "content://$id", kind, role, "https://example.com/$id")

    private fun adapter(model: com.higgsfield.mobile.core.model.WorkflowDescriptor) =
        SchemaWorkflowAdapter(model, requireNotNull(WorkflowRequestSchemas.find(model.id)))

    @Test fun `every enabled direct route has a matching request schema`() {
        DirectModelRoutes.all.forEach { model ->
            assertEquals(model.endpointPath, WorkflowRegistry.find(model.id)?.endpointPath)
            assertEquals(model.id, WorkflowRequestSchemas.find(model.id)?.workflowId)
        }
        assertEquals(DirectModelRoutes.all.size, DirectModelRoutes.all.map { it.endpointPath }.toSet().size)
    }

    @Test fun `image to video maps ordered frames and audio to their distinct fields`() {
        val model = DirectModelRoutes.wan2_7Image
        val request = adapter(model).toRequest(draft(model,
            media("first.jpg", MediaKind.IMAGE, MediaRole.START_FRAME),
            media("last.jpg", MediaKind.IMAGE, MediaRole.END_FRAME),
            media("sound.mp3", MediaKind.AUDIO, MediaRole.AUDIO)))
        assertEquals(JsonPrimitive("https://example.com/first.jpg"), request["image_url"])
        assertEquals(JsonPrimitive("https://example.com/last.jpg"), request["end_image_url"])
        assertEquals(JsonPrimitive("https://example.com/sound.mp3"), request["audio_url"])
        assertFalse(request.containsKey("image_urls"))
    }

    @Test fun `genjutsu requires video and one to eight image references`() {
        val model = DirectModelRoutes.genjutsuMotion
        val source = media("source.mp4", MediaKind.VIDEO, MediaRole.SOURCE)
        val first = media("first.jpg", MediaKind.IMAGE, MediaRole.REFERENCE)
        assertFalse(adapter(model).validate(draft(model, source)).isValid)
        val request = adapter(model).toRequest(draft(model, source, first))
        assertEquals(JsonPrimitive("https://example.com/source.mp4"), request["video_url"])
        assertEquals(JsonArray(listOf(JsonPrimitive("https://example.com/first.jpg"))), request["image_urls"])
        val tooMany = (1..9).map { media("$it.jpg", MediaKind.IMAGE, MediaRole.REFERENCE) }
        assertFalse(adapter(model).validate(draft(model, *(listOf(source) + tooMany).toTypedArray())).isValid)
    }

    @Test fun `prime reference limit counts image and video files together`() {
        val model = DirectModelRoutes.wan3PrimeReference
        val references = (1..6).map { media("$it.jpg", MediaKind.IMAGE, MediaRole.REFERENCE) } +
            (1..5).map { media("$it.mp4", MediaKind.VIDEO, MediaRole.VIDEO_REFERENCE) }
        assertFalse(adapter(model).validate(draft(model, *references.toTypedArray())).isValid)
        assertTrue(adapter(model).validate(draft(model, *references.dropLast(1).toTypedArray())).isValid)
    }

    @Test fun `route specific controls are typed and constrained`() {
        val model = DirectModelRoutes.ideogram
        val options = GenerationOptions(modelOptions = mapOf("image_weight" to "75", "rendering_speed" to "QUALITY"))
        val request = adapter(model).toRequest(draft(model).copy(options = options))
        assertEquals(JsonPrimitive(75), request["image_weight"])
        assertEquals(JsonPrimitive("QUALITY"), request["rendering_speed"])
        assertFalse(adapter(model).validate(draft(model).copy(options = options.copy(modelOptions = mapOf("image_weight" to "101")))).isValid)
    }

    @Test fun `Kling 3 maps last image and numeric CFG scale`() {
        val model = DirectModelRoutes.kling3ProImage
        val draft = draft(model, media("first.jpg", MediaKind.IMAGE, MediaRole.START_FRAME),
            media("last.jpg", MediaKind.IMAGE, MediaRole.END_FRAME)).copy(
            options = GenerationOptions(modelOptions = mapOf("cfg_scale" to "0.5", "sound" to "off")))

        val request = adapter(model).toRequest(draft)

        assertEquals(JsonPrimitive("https://example.com/last.jpg"), request["last_image_url"])
        assertEquals(JsonPrimitive(0.5), request["cfg_scale"])
        assertEquals(JsonPrimitive("off"), request["sound"])
    }
}
