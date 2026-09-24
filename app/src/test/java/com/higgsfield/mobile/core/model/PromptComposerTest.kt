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

        val prompt = PromptComposer.compose(brief, "  Make it red  ")

        assertEquals(
            "Subject: a green apple\nStyle: product photography\nRequirements: single object\nMake it red",
            prompt,
        )
        assertFalse(prompt.contains("Exclusions"))
    }

    @Test
    fun `empty brief returns only current instruction`() {
        assertEquals("Create an apple", PromptComposer.compose(CreativeBrief(), " Create an apple "))
    }
}
