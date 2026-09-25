package com.higgsfield.mobile.core.network

import com.higgsfield.mobile.core.error.AppError
import com.higgsfield.mobile.core.error.ErrorMapper
import com.higgsfield.mobile.core.model.DraftValidation
import com.higgsfield.mobile.core.model.GenerationDraft
import com.higgsfield.mobile.core.model.PromptComposer
import com.higgsfield.mobile.core.model.WorkflowAdapter
import com.higgsfield.mobile.core.model.WorkflowDescriptor
import com.higgsfield.mobile.core.model.WorkflowId
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** Configurable mapper for verified model schemas; each model adds configuration, not a class. */
class SchemaWorkflowAdapter(
    override val descriptor: WorkflowDescriptor,
    private val schema: WorkflowRequestSchema,
) : WorkflowAdapter<JsonObject> {
    init {
        require(descriptor.id == schema.workflowId) { WORKFLOW_MISMATCH_MESSAGE }
        require(descriptor.isSubmissionEnabled) { WORKFLOW_DISABLED_MESSAGE }
    }

    override fun validate(draft: GenerationDraft): DraftValidation = DraftValidation(
        errors = buildList {
            if (draft.workflowId != descriptor.id) add(ErrorMapper.protocol(WORKFLOW_MISMATCH_MESSAGE))
            addAll(schema.fields.mapNotNull { field ->
                field.errorWhenMissing?.takeIf { field.value.resolve(draft, composedPrompt(draft)) == null }
            })
        },
    )

    override fun toRequest(draft: GenerationDraft): JsonObject {
        check(validate(draft).isValid) { INVALID_DRAFT_MESSAGE }
        return JsonObject(schema.fields.mapNotNull { field ->
            field.value.resolve(draft, composedPrompt(draft))?.let { field.name to it }
        }.toMap())
    }

    private fun composedPrompt(draft: GenerationDraft) = PromptComposer.compose(draft.creativeBrief, draft.instruction)

    private companion object {
        const val WORKFLOW_MISMATCH_MESSAGE = "The schema belongs to a different workflow."
        const val WORKFLOW_DISABLED_MESSAGE = "The workflow is not verified for submission."
        const val INVALID_DRAFT_MESSAGE = "A valid generation draft is required."
    }
}

data class WorkflowRequestSchema(val workflowId: WorkflowId, val fields: List<WorkflowRequestField>)

data class WorkflowRequestField(
    val name: String,
    val value: WorkflowRequestValue,
    val errorWhenMissing: AppError? = null,
)

fun interface WorkflowRequestValue {
    fun resolve(draft: GenerationDraft, composedPrompt: String): JsonElement?
}

object WorkflowRequestValues {
    val composedPrompt = WorkflowRequestValue { _, prompt ->
        prompt.takeIf(String::isNotBlank)?.let(::JsonPrimitive)
    }

    fun requiredComposedPrompt() = WorkflowRequestField(
        name = PROMPT_FIELD,
        value = composedPrompt,
        errorWhenMissing = ErrorMapper.instructionRequired(),
    )

    private const val PROMPT_FIELD = "prompt"
}
