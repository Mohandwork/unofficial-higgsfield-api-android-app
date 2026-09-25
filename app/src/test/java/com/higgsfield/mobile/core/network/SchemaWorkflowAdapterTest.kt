package com.higgsfield.mobile.core.network

import com.higgsfield.mobile.R
import com.higgsfield.mobile.core.model.CreativeBrief
import com.higgsfield.mobile.core.model.GenerationDraft
import com.higgsfield.mobile.core.model.WorkflowCatalog
import com.higgsfield.mobile.core.model.WorkflowRegistry
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
}
