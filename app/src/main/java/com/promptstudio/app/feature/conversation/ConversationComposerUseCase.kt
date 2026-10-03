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
        val referenceSlot = MediaRequirement(MediaRole.REFERENCE, MediaKind.IMAGE)
        return workflow.mediaRequirements + if (
            WorkflowCapability.REFERENCE_IMAGE in workflow.capabilities &&
                workflow.mediaRequirements.none { it.role == MediaRole.REFERENCE }
        ) listOf(referenceSlot) else emptyList()
    }

    fun selectWorkflow(state: ConversationUiState, workflow: WorkflowDescriptor): ComposerWorkflowSelection {
        val slots = slotsFor(workflow)
        return ComposerWorkflowSelection(
            slots = slots,
            attachments = state.attachments.filter { attachment ->
                slots.any { it.role == attachment.role && it.kind == attachment.kind }
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
                    id = "local-attachment-${attachment.role.name.lowercase()}",
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
        return options.copy(
            aspectRatio = if (WorkflowOption.ASPECT_RATIO in supported) options.aspectRatio else GenerationOptions().aspectRatio,
            resolution = options.resolution?.takeIf { WorkflowOption.RESOLUTION in supported },
            durationSeconds = options.durationSeconds?.takeIf { WorkflowOption.DURATION in supported },
            seed = options.seed?.takeIf { WorkflowOption.SEED in supported },
            negativePrompt = options.negativePrompt?.takeIf { WorkflowOption.NEGATIVE_PROMPT in supported },
        )
    }

    fun canEditOutput(state: ConversationUiState, item: TimelineItem): Boolean =
        item.output?.kind == MediaKind.IMAGE && item.sourceOutputId != null && state.mediaKind == MediaKind.IMAGE &&
            WorkflowCapability.IMAGE_TO_IMAGE in state.selectedWorkflow?.capabilities.orEmpty()
}
