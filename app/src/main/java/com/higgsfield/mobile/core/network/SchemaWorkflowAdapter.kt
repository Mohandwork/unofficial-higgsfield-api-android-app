package com.higgsfield.mobile.core.network

import com.higgsfield.mobile.core.error.AppError
import com.higgsfield.mobile.core.error.ErrorMapper
import com.higgsfield.mobile.core.model.DraftValidation
import com.higgsfield.mobile.core.model.GenerationDraft
import com.higgsfield.mobile.core.model.MediaKind
import com.higgsfield.mobile.core.model.PromptComposer
import com.higgsfield.mobile.core.model.WorkflowAdapter
import com.higgsfield.mobile.core.model.WorkflowDescriptor
import com.higgsfield.mobile.core.model.WorkflowId
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonArray
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

    override fun validate(draft: GenerationDraft): DraftValidation {
        val resolvedFields = resolveFields(draft)
        return DraftValidation(
            errors = buildList {
                if (draft.workflowId != descriptor.id) add(ErrorMapper.protocol(WORKFLOW_MISMATCH_MESSAGE))
                resolvedFields.forEach { (field, value) -> addAll(field.validate(value)) }
            },
        )
    }

    override fun toRequest(draft: GenerationDraft): JsonObject {
        check(validate(draft).isValid) { INVALID_DRAFT_MESSAGE }
        return JsonObject(resolveFields(draft).mapNotNull { (field, value) -> value?.let { field.name to it } }.toMap())
    }

    private fun resolveFields(draft: GenerationDraft) = schema.fields.map { field ->
        field to field.value.resolve(draft, composedPrompt(draft))
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
    val validate: (JsonElement?) -> List<AppError> = { emptyList() },
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
        validate = required(ErrorMapper.instructionRequired()),
    )

    val resolution = WorkflowRequestValue { draft, _ ->
        draft.options.resolution?.takeIf(String::isNotBlank)?.let(::JsonPrimitive)
    }

    val aspectRatio = WorkflowRequestValue { draft, _ -> JsonPrimitive(draft.options.aspectRatio) }

    val seed = WorkflowRequestValue { draft, _ -> draft.options.seed?.let(::JsonPrimitive) }

    val negativePrompt = WorkflowRequestValue { draft, _ ->
        draft.options.negativePrompt?.takeIf(String::isNotBlank)?.let(::JsonPrimitive)
    }

    val uploadedImageUrls = WorkflowRequestValue { draft, _ ->
        draft.attachments
            .filter { it.kind == MediaKind.IMAGE }
            .mapNotNull { it.remoteUrl?.takeIf(String::isNotBlank) }
            .takeIf(List<String>::isNotEmpty)
            ?.let { urls -> JsonArray(urls.map(::JsonPrimitive)) }
    }

    fun requiredUploadedUrl(name: String, kind: MediaKind, error: AppError) = WorkflowRequestField(
        name = name,
        value = uploadedUrl(kind),
        validate = required(error),
    )

    fun requiredUploadedImageUrls(maximumCount: Int) = WorkflowRequestField(
        name = IMAGE_URLS_FIELD,
        value = uploadedImageUrls,
        validate = { value ->
            when {
                value == null -> listOf(ErrorMapper.referenceImageRequired())
                (value as JsonArray).size > maximumCount -> listOf(ErrorMapper.referenceImageLimit())
                else -> emptyList()
            }
        },
    )

    fun optionalUploadedImageUrls() = WorkflowRequestField(
        name = IMAGE_URLS_FIELD,
        value = uploadedImageUrls,
    )

    fun constantString(name: String, value: String) = WorkflowRequestField(
        name = name,
        value = constantStringValue(value),
    )

    fun constantStringValue(value: String) = WorkflowRequestValue { _, _ -> JsonPrimitive(value) }

    fun constantBoolean(name: String, value: Boolean) = WorkflowRequestField(
        name = name,
        value = WorkflowRequestValue { _, _ -> JsonPrimitive(value) },
    )

    fun optional(name: String, value: WorkflowRequestValue) = WorkflowRequestField(name, value)

    private fun uploadedUrl(kind: MediaKind) = WorkflowRequestValue { draft, _ ->
        draft.attachments
            .firstOrNull { it.kind == kind }
            ?.remoteUrl
            ?.takeIf(String::isNotBlank)
            ?.let(::JsonPrimitive)
    }

    private fun required(error: AppError): (JsonElement?) -> List<AppError> = { value ->
        if (value == null) listOf(error) else emptyList()
    }

    private const val PROMPT_FIELD = "prompt"
    private const val IMAGE_URLS_FIELD = "image_urls"
}
