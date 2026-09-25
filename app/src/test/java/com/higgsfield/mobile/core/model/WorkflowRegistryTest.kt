package com.higgsfield.mobile.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkflowRegistryTest {
    @Test
    fun `every catalog workflow has a verified endpoint configuration`() {
        assertTrue(WorkflowRegistry.all.isNotEmpty())
        WorkflowRegistry.all
            .forEach { workflow ->
                assertTrue(workflow.isSubmissionEnabled)
                assertTrue(workflow.endpointPath.orEmpty().isNotBlank())
                assertEquals("2026-09-25", workflow.schemaVerifiedOn)
        }
    }

    @Test
    fun `soul v2 uses the verified standard endpoint`() {
        val workflow = WorkflowRegistry.find(WorkflowCatalog.SOUL_V2.id)!!

        assertTrue(workflow.isSubmissionEnabled)
        assertEquals("higgsfield-ai/soul/v2/standard", workflow.endpointPath)
        assertEquals("2026-09-25", workflow.schemaVerifiedOn)
    }

    @Test
    fun `soul standard uses its verified endpoint`() {
        val workflow = WorkflowRegistry.find(WorkflowCatalog.SOUL.id)!!

        assertTrue(workflow.isSubmissionEnabled)
        assertEquals("higgsfield-ai/soul/standard", workflow.endpointPath)
        assertEquals("2026-09-25", workflow.schemaVerifiedOn)
    }

    @Test
    fun `qwen edit declares a required source image role`() {
        val workflow = WorkflowRegistry.find(WorkflowCatalog.QWEN_IMAGE_3_EDIT.id)!!
        val source = workflow.mediaRequirements.single()

        assertEquals(MediaRole.SOURCE, source.role)
        assertEquals(MediaKind.IMAGE, source.kind)
        assertEquals(1, source.minimumCount)
    }

    @Test
    fun `submission-ready editor search never offers catalog-only models`() {
        assertTrue(WorkflowRegistry.compatibleEditors(MediaKind.IMAGE).isNotEmpty())
        assertTrue(WorkflowRegistry.compatibleEditors(MediaKind.IMAGE).all { it.isSubmissionEnabled })
    }
}
