package com.higgsfield.mobile.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkflowRegistryTest {
    @Test
    fun `unverified workflows cannot be submitted`() {
        assertTrue(WorkflowRegistry.all.isNotEmpty())
        WorkflowRegistry.all.forEach { workflow ->
            assertFalse(workflow.isSubmissionEnabled)
            assertNull(workflow.endpointPath)
            assertNull(workflow.schemaVerifiedOn)
        }
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
        assertTrue(WorkflowRegistry.compatibleEditors(MediaKind.IMAGE).isEmpty())
        assertTrue(WorkflowRegistry.compatibleEditors(MediaKind.IMAGE, submissionReadyOnly = false).isNotEmpty())
    }
}
