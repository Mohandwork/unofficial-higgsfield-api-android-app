package com.higgsfield.mobile.core.network

import com.higgsfield.mobile.core.model.WorkflowCatalog

object WorkflowRequestSchemas {
    val soulStandard = WorkflowRequestSchema(
        workflowId = WorkflowCatalog.SOUL.id,
        fields = listOf(WorkflowRequestValues.requiredComposedPrompt()),
    )

    val soulV2Standard = WorkflowRequestSchema(
        workflowId = WorkflowCatalog.SOUL_V2.id,
        fields = listOf(WorkflowRequestValues.requiredComposedPrompt()),
    )

    val all: List<WorkflowRequestSchema> = listOf(soulStandard, soulV2Standard)

    fun find(workflowId: com.higgsfield.mobile.core.model.WorkflowId): WorkflowRequestSchema? =
        all.firstOrNull { it.workflowId == workflowId }
}
