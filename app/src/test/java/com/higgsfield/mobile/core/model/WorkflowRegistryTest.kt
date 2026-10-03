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
                assertTrue(workflow.schemaVerifiedOn.orEmpty().isNotBlank())
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

    @Test
    fun `reference limits follow their selected API workflows`() {
        val seedance2 = WorkflowRegistry.find(WorkflowCatalog.SEEDANCE_2_REFERENCE.id)!!
        val seedance2_5 = WorkflowRegistry.find(WorkflowCatalog.SEEDANCE_2_5_REFERENCE.id)!!
        val qwenEdit = WorkflowRegistry.find(WorkflowCatalog.QWEN_IMAGE_3_EDIT.id)!!
        val marketing = WorkflowRegistry.find(WorkflowCatalog.MARKETING_STUDIO_2_ALPHA.id)!!

        assertEquals(9, seedance2.mediaRequirements.first { it.kind == MediaKind.IMAGE }.maximumCount)
        assertEquals(3, seedance2.mediaRequirements.first { it.kind == MediaKind.VIDEO }.maximumCount)
        assertEquals(30, seedance2_5.mediaRequirements.first { it.kind == MediaKind.IMAGE }.maximumCount)
        assertEquals(10, seedance2_5.mediaRequirements.first { it.kind == MediaKind.VIDEO }.maximumCount)
        assertEquals(3, qwenEdit.mediaRequirements.single().maximumCount)
        assertEquals(16, marketing.mediaRequirements.single().maximumCount)
    }
}
