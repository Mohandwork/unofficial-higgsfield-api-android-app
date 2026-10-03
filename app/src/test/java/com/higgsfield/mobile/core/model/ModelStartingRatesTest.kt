package com.higgsfield.mobile.core.model

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
        assertEquals("\$0.0476/s", ModelStartingRates.priceFor(WorkflowId("wan-3-prime-image")))
        assertEquals("\$0.025/s", ModelStartingRates.priceFor(WorkflowId("wan-3-reference")))
        assertEquals("\$0.0385/s", ModelStartingRates.priceFor(WorkflowId("kling-2-6-motion-std")))
    }

    @Test
    fun `unverified main model rates remain unavailable`() {
        assertEquals("Pricing unavailable", ModelStartingRates.priceFor(WorkflowId("minimax-h3-image")))
        assertEquals("Pricing unavailable", ModelStartingRates.priceFor(WorkflowId("unknown-model")))
    }
}
