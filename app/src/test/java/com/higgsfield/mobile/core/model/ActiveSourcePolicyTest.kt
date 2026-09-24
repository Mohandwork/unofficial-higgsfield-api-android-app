package com.higgsfield.mobile.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ActiveSourcePolicyTest {
    @Test
    fun `single completed image automatically replaces active source`() {
        val output = GenerationOutput("image-1", "https://example.invalid/image.jpg", MediaKind.IMAGE)
        assertEquals(
            ActiveSourceDecision.Replace("image-1"),
            ActiveSourcePolicy.after(GenerationStatus.Completed(listOf(output))),
        )
    }

    @Test
    fun `multiple completed images require explicit selection`() {
        val outputs = listOf(
            GenerationOutput("one", "https://example.invalid/one.jpg", MediaKind.IMAGE),
            GenerationOutput("two", "https://example.invalid/two.jpg", MediaKind.IMAGE),
        )
        assertTrue(ActiveSourcePolicy.after(GenerationStatus.Completed(outputs)) is ActiveSourceDecision.RequireSelection)
    }

    @Test
    fun `failed generation keeps current active source`() {
        assertTrue(
            ActiveSourcePolicy.after(GenerationStatus.Failed("Network unavailable", retryable = true))
                is ActiveSourceDecision.KeepCurrent,
        )
    }
}
