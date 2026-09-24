package com.higgsfield.mobile.core.model

import kotlinx.serialization.Serializable

@JvmInline
@Serializable
value class WorkflowId(val value: String)

enum class MediaKind { IMAGE, VIDEO, AUDIO }
enum class WorkflowFamily { SOUL, MARKETING_STUDIO, QWEN, SEEDANCE, KLING, CINEMA_STUDIO, WAN }
enum class WorkflowCapability {
    TEXT_TO_IMAGE, IMAGE_TO_IMAGE, TEXT_TO_VIDEO, IMAGE_TO_VIDEO,
    REFERENCE_IMAGE, NEGATIVE_PROMPT, SEED, AUDIO,
}

enum class MediaRole { SOURCE, START_FRAME, END_FRAME, REFERENCE, MOTION_REFERENCE, AUDIO }

data class MediaRequirement(
    val role: MediaRole,
    val kind: MediaKind,
    val minimumCount: Int = 0,
    val maximumCount: Int = 1,
)

data class WorkflowDescriptor(
    val id: WorkflowId,
    val displayName: String,
    val family: WorkflowFamily,
    val mediaKind: MediaKind,
    val tier: String? = null,
    val capabilities: Set<WorkflowCapability>,
    val mediaRequirements: List<MediaRequirement> = emptyList(),
    val endpointPath: String? = null,
    val pricingFactors: List<String> = emptyList(),
    val documentationUrl: String,
    val schemaVerifiedOn: String? = null,
    val isSubmissionEnabled: Boolean = false,
)

data class CreativeBrief(
    val subject: String = "",
    val style: String = "",
    val mood: String = "",
    val cameraDirection: String = "",
    val requirements: String = "",
    val exclusions: String = "",
    val outputGoal: String = "",
)

data class GenerationAttachment(
    val id: String,
    val uri: String,
    val kind: MediaKind,
    val remoteUrl: String? = null,
    val isGeneratedOutput: Boolean = false,
)

data class GenerationOptions(
    val aspectRatio: String = "1:1",
    val resolution: String? = null,
    val durationSeconds: Int? = null,
    val seed: Long? = null,
    val negativePrompt: String? = null,
)

data class GenerationDraft(
    val instruction: String,
    val creativeBrief: CreativeBrief,
    val workflowId: WorkflowId,
    val attachments: List<GenerationAttachment> = emptyList(),
    val activeSourceId: String? = null,
    val options: GenerationOptions = GenerationOptions(),
)

sealed interface GenerationStatus {
    data object Draft : GenerationStatus
    data object Queued : GenerationStatus
    data class InProgress(val progress: Float? = null) : GenerationStatus
    data class Completed(val outputs: List<GenerationOutput>) : GenerationStatus
    data class Failed(val userMessage: String, val retryable: Boolean) : GenerationStatus
    data class Nsfw(val userMessage: String) : GenerationStatus
    data object Canceled : GenerationStatus
}

data class GenerationOutput(
    val id: String,
    val remoteUrl: String,
    val kind: MediaKind,
    val localUri: String? = null,
)

data class GenerationRecord(
    val id: String,
    val parentGenerationId: String? = null,
    val branchRootId: String = id,
    val draft: GenerationDraft,
    val status: GenerationStatus,
)

sealed interface EstimateState {
    data object Idle : EstimateState
    data object Loading : EstimateState
    data class Available(val credits: String, val note: String? = null) : EstimateState
    data class Unavailable(val reason: String) : EstimateState
}

data class DraftValidation(
    val errors: List<String> = emptyList(),
    val compatibleAlternatives: List<WorkflowId> = emptyList(),
) { val isValid: Boolean get() = errors.isEmpty() }

interface WorkflowAdapter<Request : Any> {
    val descriptor: WorkflowDescriptor
    fun validate(draft: GenerationDraft): DraftValidation
    fun toRequest(draft: GenerationDraft): Request
}
