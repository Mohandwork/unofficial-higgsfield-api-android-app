package com.higgsfield.mobile.core.model

object WorkflowRegistry {
    private val imageEdit = setOf(WorkflowCapability.TEXT_TO_IMAGE, WorkflowCapability.IMAGE_TO_IMAGE, WorkflowCapability.REFERENCE_IMAGE)
    private val videoCreate = setOf(WorkflowCapability.TEXT_TO_VIDEO, WorkflowCapability.IMAGE_TO_VIDEO, WorkflowCapability.REFERENCE_IMAGE)
    private val sourceImageRequired = listOf(MediaRequirement(MediaRole.SOURCE, MediaKind.IMAGE, minimumCount = 1))
    private val motionReferenceRequired = listOf(MediaRequirement(MediaRole.MOTION_REFERENCE, MediaKind.IMAGE, minimumCount = 1))

    val all: List<WorkflowDescriptor> = listOf(
        image(WorkflowCatalog.SOUL, WorkflowFamily.SOUL, setOf(WorkflowCapability.TEXT_TO_IMAGE)),
        image(WorkflowCatalog.SOUL_V2, WorkflowFamily.SOUL, setOf(WorkflowCapability.TEXT_TO_IMAGE)),
        image(WorkflowCatalog.SOUL_CINEMA, WorkflowFamily.SOUL, setOf(WorkflowCapability.TEXT_TO_IMAGE)),
        image(WorkflowCatalog.MARKETING_STUDIO_2_ALPHA, WorkflowFamily.MARKETING_STUDIO, imageEdit),
        image(WorkflowCatalog.MARKETING_STUDIO_2_5_FLARE, WorkflowFamily.MARKETING_STUDIO, imageEdit),
        image(WorkflowCatalog.MARKETING_STUDIO_2_5_SUNBURST, WorkflowFamily.MARKETING_STUDIO, imageEdit),
        image(WorkflowCatalog.QWEN_IMAGE_3, WorkflowFamily.QWEN, imageEdit + WorkflowCapability.NEGATIVE_PROMPT),
        image(WorkflowCatalog.QWEN_IMAGE_3_EDIT, WorkflowFamily.QWEN, imageEdit, sourceImageRequired),
        video(WorkflowCatalog.SEEDANCE_2, WorkflowFamily.SEEDANCE),
        video(WorkflowCatalog.SEEDANCE_2_5, WorkflowFamily.SEEDANCE),
        video(WorkflowCatalog.KLING_2_5_TURBO, WorkflowFamily.KLING),
        video(WorkflowCatalog.KLING_2_6, WorkflowFamily.KLING),
        video(WorkflowCatalog.KLING_2_6_MOTION, WorkflowFamily.KLING, motionReferenceRequired),
        video(WorkflowCatalog.KLING_3, WorkflowFamily.KLING),
        video(WorkflowCatalog.KLING_3_MOTION, WorkflowFamily.KLING, motionReferenceRequired),
        video(WorkflowCatalog.KLING_O3, WorkflowFamily.KLING),
        video(WorkflowCatalog.KLING_OMNI, WorkflowFamily.KLING, capabilities = videoCreate + WorkflowCapability.AUDIO),
        video(WorkflowCatalog.CINEMA_STUDIO_4, WorkflowFamily.CINEMA_STUDIO),
        video(WorkflowCatalog.WAN_2_6, WorkflowFamily.WAN),
        video(WorkflowCatalog.WAN_2_7, WorkflowFamily.WAN),
        video(WorkflowCatalog.WAN_3, WorkflowFamily.WAN),
        video(WorkflowCatalog.WAN_3_PRIME, WorkflowFamily.WAN, tier = "Prime"),
    )

    fun forKind(kind: MediaKind) = all.filter { it.mediaKind == kind }
    fun find(id: WorkflowId) = all.firstOrNull { it.id == id }
    fun compatibleEditors(kind: MediaKind, submissionReadyOnly: Boolean = true) = all.filter {
        it.mediaKind == kind && WorkflowCapability.IMAGE_TO_IMAGE in it.capabilities &&
            (!submissionReadyOnly || it.isSubmissionEnabled)
    }

    private fun image(key: WorkflowKey, family: WorkflowFamily, capabilities: Set<WorkflowCapability>, required: List<MediaRequirement> = emptyList()) =
        descriptor(key, family, MediaKind.IMAGE, capabilities, required)

    private fun video(key: WorkflowKey, family: WorkflowFamily, required: List<MediaRequirement> = emptyList(), tier: String? = null, capabilities: Set<WorkflowCapability> = videoCreate) =
        descriptor(key, family, MediaKind.VIDEO, capabilities, required, tier)

    private fun descriptor(key: WorkflowKey, family: WorkflowFamily, kind: MediaKind, capabilities: Set<WorkflowCapability>, required: List<MediaRequirement>, tier: String? = null) =
        WorkflowDescriptor(
            id = key.id, displayName = key.displayName, family = family, mediaKind = kind,
            tier = tier, capabilities = capabilities, mediaRequirements = required, endpointPath = null,
            pricingFactors = listOf("resolution", "duration", "model options"),
            documentationUrl = WorkflowCatalog.DOCUMENTATION_URL, schemaVerifiedOn = null, isSubmissionEnabled = false,
        )
}
