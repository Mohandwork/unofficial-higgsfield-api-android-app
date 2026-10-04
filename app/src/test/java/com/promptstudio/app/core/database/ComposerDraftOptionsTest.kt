package com.promptstudio.app.core.database

import com.promptstudio.app.core.model.GenerationOptions
import org.junit.Assert.assertEquals
import org.junit.Test

class ComposerDraftOptionsTest {
    @Test fun `model controls survive draft persistence`() {
        val original = PersistedComposerDraft(options = GenerationOptions(modelOptions = mapOf(
            "generate_audio" to "false", "rendering_speed" to "QUALITY", "image_weight" to "75")))

        assertEquals(original.options, original.toEntity("conversation").toDraft().options)
    }
}
