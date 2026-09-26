package com.higgsfield.mobile.feature.conversation

import com.higgsfield.mobile.R
import com.higgsfield.mobile.core.model.GenerationStatus
import com.higgsfield.mobile.core.model.MediaKind
import com.higgsfield.mobile.core.model.MediaRequirement
import com.higgsfield.mobile.core.model.MediaRole
import com.higgsfield.mobile.core.model.StaticEstimateMetadata
import com.higgsfield.mobile.core.model.WorkflowCapability
import com.higgsfield.mobile.core.model.WorkflowDescriptor
import com.higgsfield.mobile.core.model.WorkflowFamily
import com.higgsfield.mobile.core.model.WorkflowId
import com.higgsfield.mobile.core.model.WorkflowOption

internal val previewWorkflow = WorkflowDescriptor(
    id = WorkflowId("preview-cinema"),
    displayName = "Higgsfield Cinema v2.0",
    family = WorkflowFamily.CINEMA_STUDIO,
    mediaKind = MediaKind.IMAGE,
    capabilities = setOf(WorkflowCapability.TEXT_TO_IMAGE, WorkflowCapability.REFERENCE_IMAGE),
    mediaRequirements = listOf(MediaRequirement(MediaRole.REFERENCE, MediaKind.IMAGE)),
    supportedOptions = setOf(WorkflowOption.ASPECT_RATIO, WorkflowOption.RESOLUTION, WorkflowOption.SEED),
    staticEstimate = StaticEstimateMetadata("\$0.04", "12 credits / generation", "About 20 seconds", "https://open.higgsfield.ai/pricing", "2026-09-26"),
    documentationUrl = "https://docs.higgsfield.ai/docs",
    isSubmissionEnabled = true,
)

internal fun previewConversationState() = ConversationUiState(
    mediaKind = MediaKind.IMAGE,
    prompt = "A futuristic street market at night, neon rain, cinematic framing.",
    workflows = listOf(previewWorkflow),
    selectedWorkflow = previewWorkflow,
    attachmentSlots = previewWorkflow.mediaRequirements,
    attachments = listOf(DraftMediaAttachment(MediaRole.REFERENCE, MediaKind.IMAGE, "", "Neon-market-reference.png")),
)

internal fun previewGenerationItem() = TimelineItem(
    id = "preview-generation",
    prompt = "A futuristic street market at night, 8K cinematic framing.",
    modelName = previewWorkflow.displayName,
    stateLabel = ConversationText.Resource(R.string.status_generating),
    lifecycle = GenerationStatus.InProgress(0.58f),
)
