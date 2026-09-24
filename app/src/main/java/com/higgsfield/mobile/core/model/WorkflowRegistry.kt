package com.higgsfield.mobile.core.model

object WorkflowRegistry {
    private const val catalog = "https://docs.higgsfield.ai/docs/models"
    private val imageEdit = setOf(WorkflowCapability.TEXT_TO_IMAGE, WorkflowCapability.IMAGE_TO_IMAGE, WorkflowCapability.REFERENCE_IMAGE)
    private val videoCreate = setOf(WorkflowCapability.TEXT_TO_VIDEO, WorkflowCapability.IMAGE_TO_VIDEO, WorkflowCapability.REFERENCE_IMAGE)

    val all: List<WorkflowDescriptor> = listOf(
        image("soul", "SOUL", WorkflowFamily.SOUL, setOf(WorkflowCapability.TEXT_TO_IMAGE)),
        image("soul-v2", "SOUL V2", WorkflowFamily.SOUL, setOf(WorkflowCapability.TEXT_TO_IMAGE)),
        image("soul-cinema", "SOUL Cinema", WorkflowFamily.SOUL, setOf(WorkflowCapability.TEXT_TO_IMAGE)),
        image("marketing-studio-2-alpha", "Marketing Studio 2.0 Alpha", WorkflowFamily.MARKETING_STUDIO, imageEdit),
        image("marketing-studio-2-5-flare", "Marketing Studio 2.5 Flare", WorkflowFamily.MARKETING_STUDIO, imageEdit),
        image("marketing-studio-2-5-sunburst", "Marketing Studio 2.5 Sunburst", WorkflowFamily.MARKETING_STUDIO, imageEdit),
        image("qwen-image-3", "Qwen Image 3", WorkflowFamily.QWEN, imageEdit + WorkflowCapability.NEGATIVE_PROMPT),
        image("qwen-image-3-edit", "Qwen Image 3 Edit", WorkflowFamily.QWEN, imageEdit, sourceImageRequired),
        video("seedance-2", "Seedance 2.0", WorkflowFamily.SEEDANCE),
        video("seedance-2-5", "Seedance 2.5", WorkflowFamily.SEEDANCE),
        video("kling-2-5-turbo", "Kling 2.5 Turbo", WorkflowFamily.KLING),
        video("kling-2-6", "Kling 2.6", WorkflowFamily.KLING),
        video("kling-2-6-motion", "Kling 2.6 Motion Control", WorkflowFamily.KLING, motionReferenceRequired),
        video("kling-3", "Kling 3.0", WorkflowFamily.KLING),
        video("kling-3-motion", "Kling 3.0 Motion Control", WorkflowFamily.KLING, motionReferenceRequired),
        video("kling-o3", "Kling O3", WorkflowFamily.KLING),
        video("kling-omni", "Kling Omni", WorkflowFamily.KLING, capabilities = videoCreate + WorkflowCapability.AUDIO),
        video("cinema-studio-4", "Cinema Studio 4.0", WorkflowFamily.CINEMA_STUDIO),
        video("wan-2-6", "Wan 2.6", WorkflowFamily.WAN),
        video("wan-2-7", "Wan 2.7", WorkflowFamily.WAN),
        video("wan-3", "Wan 3.0", WorkflowFamily.WAN),
        video("wan-3-prime", "Wan 3.0 Prime", WorkflowFamily.WAN, tier = "Prime"),
    )

    fun forKind(kind: MediaKind) = all.filter { it.mediaKind == kind }
    fun find(id: WorkflowId) = all.firstOrNull { it.id == id }
    fun compatibleEditors(kind: MediaKind, submissionReadyOnly: Boolean = true) = all.filter {
        it.mediaKind == kind && WorkflowCapability.IMAGE_TO_IMAGE in it.capabilities &&
            (!submissionReadyOnly || it.isSubmissionEnabled)
    }

    private val sourceImageRequired = listOf(MediaRequirement(MediaRole.SOURCE, MediaKind.IMAGE, minimumCount = 1))
    private val motionReferenceRequired = listOf(MediaRequirement(MediaRole.MOTION_REFERENCE, MediaKind.IMAGE, minimumCount = 1))

    private fun image(id: String, name: String, family: WorkflowFamily, capabilities: Set<WorkflowCapability>, required: List<MediaRequirement> = emptyList()) =
        descriptor(id, name, family, MediaKind.IMAGE, capabilities, required)

    private fun video(id: String, name: String, family: WorkflowFamily, required: List<MediaRequirement> = emptyList(), tier: String? = null, capabilities: Set<WorkflowCapability> = videoCreate) =
        descriptor(id, name, family, MediaKind.VIDEO, capabilities, required, tier)

    private fun descriptor(id: String, name: String, family: WorkflowFamily, kind: MediaKind, capabilities: Set<WorkflowCapability>, required: List<MediaRequirement>, tier: String? = null) =
        WorkflowDescriptor(
            id = WorkflowId(id), displayName = name, family = family, mediaKind = kind,
            tier = tier, capabilities = capabilities, mediaRequirements = required, endpointPath = null,
            pricingFactors = listOf("resolution", "duration", "model options"),
            documentationUrl = catalog, schemaVerifiedOn = null, isSubmissionEnabled = false,
        )
}
