package com.promptstudio.app.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelStartingRatesTest {
    @Test
    fun `every selectable route has an editable main model rate entry`() {
        val unmapped = WorkflowRegistry.all.filter { ModelStartingRates.modelIdFor(it.id) == null }

        assertTrue("Unmapped model routes: ${unmapped.map { it.id.value }}", unmapped.isEmpty())
    }

    @Test
    fun `variants inherit the most specific main model rate`() {
        assertEquals(ModelStartingRates.priceFor(WorkflowId("wan-3-prime")), ModelStartingRates.priceFor(WorkflowId("wan-3-prime-image")))
        assertEquals(ModelStartingRates.priceFor(WorkflowId("wan-3")), ModelStartingRates.priceFor(WorkflowId("wan-3-reference")))
        assertEquals(ModelStartingRates.priceFor(WorkflowId("kling-2-6-motion")), ModelStartingRates.priceFor(WorkflowId("kling-2-6-motion-std")))
    }

    @Test
    fun `unknown model rates remain unavailable`() {
        assertEquals("Pricing unavailable", ModelStartingRates.priceFor(WorkflowId("unknown-model")))
    }
}
