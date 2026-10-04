package com.promptstudio.app.core.network

import com.promptstudio.app.core.error.AppError
import com.promptstudio.app.core.error.ErrorMapper
import com.promptstudio.app.core.model.DraftValidation
import com.promptstudio.app.core.model.GenerationDraft
import com.promptstudio.app.core.model.MediaKind
import com.promptstudio.app.core.model.PromptComposer
import com.promptstudio.app.core.model.WorkflowAdapter
import com.promptstudio.app.core.model.WorkflowDescriptor
import com.promptstudio.app.core.model.WorkflowId
import com.promptstudio.app.core.model.WorkflowCapability
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
                if (schema.requiresImageOrVideoReference && draft.attachments.none {
                        it.kind in setOf(MediaKind.IMAGE, MediaKind.VIDEO) && !it.remoteUrl.isNullOrBlank()
                    }) add(ErrorMapper.referenceImageRequired())
                descriptor.mediaRequirements.forEach { requirement ->
                    val count = draft.attachments.count { it.role == requirement.role && it.kind == requirement.kind && !it.remoteUrl.isNullOrBlank() }
                    if (count < requirement.minimumCount) add(ErrorMapper.protocol("${requirement.role.name.lowercase().replace('_', ' ')} requires ${requirement.minimumCount} ${requirement.kind.name.lowercase()} file(s)."))
                    if (requirement.maximumCount != null && count > requirement.maximumCount) add(ErrorMapper.protocol("Too many ${requirement.role.name.lowercase().replace('_', ' ')} files; maximum ${requirement.maximumCount}."))
                }
                descriptor.maximumCombinedReferences?.let { maximum ->
                    val count = draft.attachments.count { it.role in setOf(MediaRole.REFERENCE, MediaRole.VIDEO_REFERENCE) && !it.remoteUrl.isNullOrBlank() }
                    if (count > maximum) add(ErrorMapper.protocol("This model accepts up to $maximum reference files in total."))
                }
            },
        )
    }

    override fun toRequest(draft: GenerationDraft): JsonObject {
        check(validate(draft).isValid) { INVALID_DRAFT_MESSAGE }
        return JsonObject(resolveFields(draft).mapNotNull { (field, value) -> value?.let { field.name to it } }.toMap())
    }

    private fun resolveFields(draft: GenerationDraft) = schema.fields.map { field ->
        val prompt = composedPrompt(draft)
        field to if (field.name == NEGATIVE_PROMPT_FIELD) {
            (if (draft.composedPromptOverride != null) draft.options.negativePrompt
            else PromptComposer.composeNegativePrompt(draft.creativeBrief, draft.options.negativePrompt))?.let(::JsonPrimitive)
        } else field.value.resolve(draft, prompt)
    }

    private fun composedPrompt(draft: GenerationDraft) = draft.composedPromptOverride ?: PromptComposer.compose(
        draft.creativeBrief,
        draft.instruction,
        WorkflowCapability.NEGATIVE_PROMPT in descriptor.capabilities,
    )

    private companion object {
        const val WORKFLOW_MISMATCH_MESSAGE = "The schema belongs to a different workflow."
        const val WORKFLOW_DISABLED_MESSAGE = "The workflow is not verified for submission."
        const val INVALID_DRAFT_MESSAGE = "A valid generation draft is required."
        const val NEGATIVE_PROMPT_FIELD = "negative_prompt"
    }
}

data class WorkflowRequestSchema(
    val workflowId: WorkflowId,
    val fields: List<WorkflowRequestField>,
    val requiresImageOrVideoReference: Boolean = false,
)

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
    val duration = WorkflowRequestValue { draft, _ -> draft.options.durationSeconds?.let(::JsonPrimitive) }

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

    fun modelString(name: String) = WorkflowRequestValue { draft, _ ->
        draft.options.modelOptions[name]?.takeIf(String::isNotBlank)?.let(::JsonPrimitive)
    }

    fun modelBoolean(name: String) = WorkflowRequestValue { draft, _ ->
        draft.options.modelOptions[name]?.toBooleanStrictOrNull()?.let(::JsonPrimitive)
    }

    fun modelInteger(name: String) = WorkflowRequestValue { draft, _ ->
        draft.options.modelOptions[name]?.toIntOrNull()?.let(::JsonPrimitive)
    }

    fun modelDecimal(name: String) = WorkflowRequestValue { draft, _ ->
        draft.options.modelOptions[name]?.toDoubleOrNull()?.let(::JsonPrimitive)
    }

    fun uploadedUrl(role: MediaRole, kind: MediaKind) = WorkflowRequestValue { draft, _ ->
        draft.attachments.firstOrNull { it.role == role && it.kind == kind }
            ?.remoteUrl?.takeIf(String::isNotBlank)?.let(::JsonPrimitive)
    }

    fun uploadedUrls(role: MediaRole, kind: MediaKind) = WorkflowRequestValue { draft, _ ->
        draft.attachments.filter { it.role == role && it.kind == kind }
            .mapNotNull { it.remoteUrl?.takeIf(String::isNotBlank) }
            .takeIf(List<String>::isNotEmpty)?.let { urls -> JsonArray(urls.map(::JsonPrimitive)) }
    }

    fun requiredUploadedUrl(name: String, role: MediaRole, kind: MediaKind, error: AppError) = WorkflowRequestField(
        name = name, value = uploadedUrl(role, kind), validate = required(error),
    )

    val uploadedVideoUrls = WorkflowRequestValue { draft, _ ->
        draft.attachments
            .filter { it.kind == MediaKind.VIDEO }
            .mapNotNull { it.remoteUrl?.takeIf(String::isNotBlank) }
            .takeIf(List<String>::isNotEmpty)
            ?.let { urls -> JsonArray(urls.map(::JsonPrimitive)) }
    }

    val uploadedAudioUrls = WorkflowRequestValue { draft, _ ->
        draft.attachments.filter { it.kind == MediaKind.AUDIO }
            .mapNotNull { it.remoteUrl?.takeIf(String::isNotBlank) }
            .takeIf(List<String>::isNotEmpty)?.let { urls -> JsonArray(urls.map(::JsonPrimitive)) }
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

    fun optionalUploadedImageUrls(maximumCount: Int) = WorkflowRequestField(
        name = IMAGE_URLS_FIELD,
        value = uploadedImageUrls,
        validate = { value -> if (value is JsonArray && value.size > maximumCount) listOf(ErrorMapper.referenceImageLimit()) else emptyList() },
    )

    fun optionalUploadedVideoUrls(maximumCount: Int) = WorkflowRequestField(
        name = VIDEO_URLS_FIELD,
        value = uploadedVideoUrls,
        validate = { value -> if (value is JsonArray && value.size > maximumCount) listOf(ErrorMapper.protocol("Too many video references for this model.")) else emptyList() },
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
    private const val VIDEO_URLS_FIELD = "video_urls"
}
