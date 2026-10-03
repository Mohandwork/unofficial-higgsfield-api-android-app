package com.higgsfield.mobile.core.model

import kotlinx.serialization.Serializable
import com.higgsfield.mobile.core.error.AppError

@JvmInline
@Serializable
value class WorkflowId(val value: String)

enum class MediaKind { IMAGE, VIDEO, AUDIO }
enum class WorkflowFamily { SOUL, MARKETING_STUDIO, QWEN, SEEDANCE, KLING, CINEMA_STUDIO, WAN, HAPPY_HORSE, Z_IMAGE, GROK, IDEOGRAM, RECRAFT, GENJUTSU, MINIMAX, PIXVERSE }
enum class WorkflowCapability {
    TEXT_TO_IMAGE, IMAGE_TO_IMAGE, TEXT_TO_VIDEO, IMAGE_TO_VIDEO,
    REFERENCE_IMAGE, NEGATIVE_PROMPT, SEED, AUDIO,
}

enum class WorkflowOption { ASPECT_RATIO, RESOLUTION, DURATION, SEED, NEGATIVE_PROMPT, GENERATE_AUDIO, AIGC_WATERMARK, ENABLE_THINKING, PROMPT_EXTEND, RENDERING_SPEED, IMAGE_WEIGHT, OUTPUT_FORMAT, QUALITY, SOUND, MODE, CFG_SCALE, KEEP_ORIGINAL_SOUND, CHARACTER_ORIENTATION }

data class OptionConstraint(
    val choices: List<String> = emptyList(),
    val minimum: Int? = null,
    val maximum: Int? = null,
)

data class StaticEstimateMetadata(
    val fromPrice: String? = null,
    val creditGuidance: String? = null,
    val expectedLatency: String? = null,
    val maximumResolution: String? = null,
    val supportedDurations: String? = null,
    val sourceLabel: String? = null,
    val sourceUrl: String = "",
    val verifiedOn: String = "",
)

enum class MediaRole { SOURCE, START_FRAME, END_FRAME, REFERENCE, VIDEO_REFERENCE, MOTION_REFERENCE, AUDIO }

data class MediaRequirement(
    val role: MediaRole,
    val kind: MediaKind,
    val minimumCount: Int = 0,
    /** Null means the published workflow schema does not state a numeric limit. */
    val maximumCount: Int? = 1,
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
    val supportedOptions: Set<WorkflowOption> = emptySet(),
    val optionConstraints: Map<WorkflowOption, OptionConstraint> = emptyMap(),
    /** A shared maximum across image and video reference roles, when the API states one. */
    val maximumCombinedReferences: Int? = null,
    val promptRequired: Boolean = true,
    val staticEstimate: StaticEstimateMetadata? = null,
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
    val role: MediaRole = MediaRole.REFERENCE,
    val remoteUrl: String? = null,
    val isGeneratedOutput: Boolean = false,
)

data class GenerationOptions(
    val aspectRatio: String = DEFAULT_ASPECT_RATIO,
    val resolution: String? = null,
    val durationSeconds: Int? = null,
    val seed: Long? = null,
    val negativePrompt: String? = null,
    /** Optional route-specific controls, stored by API field name. */
    val modelOptions: Map<String, String> = emptyMap(),
)

data class GenerationDraft(
    val instruction: String,
    val creativeBrief: CreativeBrief,
    val workflowId: WorkflowId,
    val attachments: List<GenerationAttachment> = emptyList(),
    val activeSourceId: String? = null,
    val options: GenerationOptions = GenerationOptions(),
    /** Exact prompt captured at the original submission; used for a faithful retry. */
    val composedPromptOverride: String? = null,
)

sealed interface GenerationStatus {
    data object Draft : GenerationStatus
    data object Queued : GenerationStatus
    data class InProgress(val progress: Float? = null) : GenerationStatus
    data class Completed(val outputs: List<GenerationOutput>) : GenerationStatus
    data class Failed(val userMessage: String, val retryable: Boolean) : GenerationStatus
    data class UnknownSubmissionOutcome(val userMessage: String) : GenerationStatus
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
    val errorCode: String? = null,
)

sealed interface EstimateState {
    data object Idle : EstimateState
    data object Loading : EstimateState
    data class Available(val credits: String, val note: String? = null) : EstimateState
    data class Unavailable(val reason: String) : EstimateState
}

data class DraftValidation(
    val errors: List<AppError> = emptyList(),
    val compatibleAlternatives: List<WorkflowId> = emptyList(),
) { val isValid: Boolean get() = errors.isEmpty() }

interface WorkflowAdapter<Request : Any> {
    val descriptor: WorkflowDescriptor
    fun validate(draft: GenerationDraft): DraftValidation
    fun toRequest(draft: GenerationDraft): Request
}

private const val DEFAULT_ASPECT_RATIO = "1:1"
