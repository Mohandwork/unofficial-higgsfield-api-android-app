package com.higgsfield.mobile.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class PromptComposerTest {
    @Test
    fun `composition is deterministic and omits empty fields`() {
        val brief = CreativeBrief(
            subject = "  a green apple  ",
            style = "product photography",
            mood = "",
            requirements = "single object",
            exclusions = "text",
        )

        val prompt = PromptComposer.compose(
            brief,
            "  Change only the apple skin from green to red; preserve the composition, lighting, background, and single-object framing  ",
        )

        assertEquals(
            "Subject: a green apple\nStyle: product photography\nRequirements: single object\nChange only the apple skin from green to red; preserve the composition, lighting, background, and single-object framing",
            prompt,
        )
        assertFalse(prompt.contains("Exclusions"))
    }

    @Test
    fun `empty brief returns only current instruction`() {
        assertEquals(
            "Studio product photo of one ripe green apple centered on a matte cream background, soft daylight, no text",
            PromptComposer.compose(
                CreativeBrief(),
                " Studio product photo of one ripe green apple centered on a matte cream background, soft daylight, no text ",
            ),
        )
    }
}
