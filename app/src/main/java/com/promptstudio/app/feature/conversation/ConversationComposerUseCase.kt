package com.promptstudio.app.feature.conversation

import com.promptstudio.app.core.model.GenerationAttachment
import com.promptstudio.app.core.model.GenerationDraft
import com.promptstudio.app.core.model.GenerationOptions
import com.promptstudio.app.core.model.MediaKind
import com.promptstudio.app.core.model.MediaRequirement
import com.promptstudio.app.core.model.MediaRole
import com.promptstudio.app.core.model.WorkflowCapability
import com.promptstudio.app.core.model.WorkflowDescriptor
import com.promptstudio.app.core.model.WorkflowOption
import com.promptstudio.app.core.model.WorkflowRegistry
import javax.inject.Inject

data class ComposerWorkflowSelection(
    val slots: List<MediaRequirement>,
    val attachments: List<DraftMediaAttachment>,
    val options: GenerationOptions,
    val activeImageIncompatible: Boolean,
)

data class ReusableComposerParameters(
    val workflow: WorkflowDescriptor,
    val draft: GenerationDraft,
    val slots: List<MediaRequirement>,
)

/** Pure composer rules: capability filtering, request drafts, and reusable text/settings. */
class ConversationComposerUseCase @Inject constructor() {
    fun slotsFor(workflow: WorkflowDescriptor?): List<MediaRequirement> {
        if (workflow == null) return emptyList()
        val referenceSlot = MediaRequirement(MediaRole.REFERENCE, MediaKind.IMAGE, maximumCount = null)
        return workflow.mediaRequirements + if (
            WorkflowCapability.REFERENCE_IMAGE in workflow.capabilities &&
                workflow.mediaRequirements.none { it.kind == MediaKind.IMAGE }
        ) listOf(referenceSlot) else emptyList()
    }

    fun selectWorkflow(state: ConversationUiState, workflow: WorkflowDescriptor): ComposerWorkflowSelection {
        val slots = slotsFor(workflow)
        return ComposerWorkflowSelection(
            slots = slots,
            attachments = slots.flatMap { slot ->
                state.attachments.filter { it.role == slot.role && it.kind == slot.kind }.take(slot.maximumCount ?: Int.MAX_VALUE)
            },
            options = retainOptionsFor(state.options, workflow),
            activeImageIncompatible = state.activeSourceId != null && state.mediaKind == MediaKind.IMAGE &&
                WorkflowCapability.IMAGE_TO_IMAGE !in workflow.capabilities,
        )
    }

    fun draftFor(state: ConversationUiState, persistedSourceOutputId: String?): GenerationDraft? {
        val workflow = state.selectedWorkflow ?: return null
        return GenerationDraft(
            instruction = state.prompt,
            creativeBrief = state.brief,
            workflowId = workflow.id,
            attachments = state.attachments.map { attachment ->
                GenerationAttachment(
                    id = "local-attachment-${attachment.role.name.lowercase()}-${attachment.uri}",
                    uri = attachment.uri,
                    kind = attachment.kind,
                    role = attachment.role,
                    remoteUrl = attachment.remoteUrl,
                )
            },
            activeSourceId = state.activeSourceId?.let { activeId ->
                persistedSourceOutputId ?: state.timeline.firstOrNull { it.id == activeId }?.sourceOutputId
            },
            options = state.options,
        )
    }

    fun reusable(item: TimelineItem, mediaKind: MediaKind): ReusableComposerParameters? {
        val draft = item.record?.draft ?: return null
        val workflow = WorkflowRegistry.find(draft.workflowId) ?: return null
        if (workflow.mediaKind != mediaKind) return null
        return ReusableComposerParameters(workflow, draft, slotsFor(workflow))
    }

    fun retainOptionsFor(options: GenerationOptions, workflow: WorkflowDescriptor?): GenerationOptions {
        val supported = workflow?.supportedOptions.orEmpty()
        val constraints = workflow?.optionConstraints.orEmpty()
        fun allowed(option: WorkflowOption, value: String): Boolean =
            constraints[option]?.choices?.let { it.isEmpty() || value in it } ?: true
        fun allowedNumber(option: WorkflowOption, value: Long): Boolean =
            constraints[option]?.let { value >= (it.minimum?.toLong() ?: Long.MIN_VALUE) && value <= (it.maximum?.toLong() ?: Long.MAX_VALUE) } ?: true
        return options.copy(
            aspectRatio = if (WorkflowOption.ASPECT_RATIO in supported && allowed(WorkflowOption.ASPECT_RATIO, options.aspectRatio)) options.aspectRatio else GenerationOptions().aspectRatio,
            resolution = options.resolution?.takeIf { WorkflowOption.RESOLUTION in supported && allowed(WorkflowOption.RESOLUTION, it) },
            durationSeconds = options.durationSeconds?.takeIf { WorkflowOption.DURATION in supported && allowedNumber(WorkflowOption.DURATION, it.toLong()) },
            seed = options.seed?.takeIf { WorkflowOption.SEED in supported && allowedNumber(WorkflowOption.SEED, it) },
            negativePrompt = options.negativePrompt?.takeIf { WorkflowOption.NEGATIVE_PROMPT in supported },
            modelOptions = options.modelOptions.filter { (key, value) ->
                supported.any { it.name.lowercase() == key || when (it) {
                    WorkflowOption.GENERATE_AUDIO -> key == "generate_audio"
                    WorkflowOption.AIGC_WATERMARK -> key == "aigc_watermark"
                    WorkflowOption.ENABLE_THINKING -> key == "enable_thinking"
                    WorkflowOption.PROMPT_EXTEND -> key == "prompt_extend"
                    WorkflowOption.RENDERING_SPEED -> key == "rendering_speed"
                    WorkflowOption.IMAGE_WEIGHT -> key == "image_weight"
                    WorkflowOption.OUTPUT_FORMAT -> key == "output_format"
                    WorkflowOption.QUALITY -> key == "quality"
                    else -> false
                } } && supported.firstOrNull { it.name.lowercase() == key }?.let { option ->
                    val constraint = constraints[option]
                    (constraint?.choices.isNullOrEmpty() || value in constraint!!.choices) &&
                        (constraint?.minimum == null && constraint?.maximum == null || value.toIntOrNull()?.let { number ->
                            number >= (constraint.minimum ?: Int.MIN_VALUE) && number <= (constraint.maximum ?: Int.MAX_VALUE)
                        } == true)
                } != false
            },
        )
    }

    fun canEditOutput(state: ConversationUiState, item: TimelineItem): Boolean =
        item.output?.kind == MediaKind.IMAGE && item.sourceOutputId != null && state.mediaKind == MediaKind.IMAGE &&
            WorkflowCapability.IMAGE_TO_IMAGE in state.selectedWorkflow?.capabilities.orEmpty()
}
