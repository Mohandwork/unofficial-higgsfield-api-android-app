package com.higgsfield.mobile.core.model

object WorkflowRegistry {
    private const val PRIME_TIER = "Prime"
    private const val SOUL_STANDARD_ENDPOINT = "higgsfield-ai/soul/standard"
    internal const val SOUL_V2_STANDARD_ENDPOINT = "higgsfield-ai/soul/v2/standard"
    private const val MARKETING_STUDIO_ALPHA_ENDPOINT = "marketing-studio/image"
    private const val MARKETING_STUDIO_FLARE_ENDPOINT = "marketing-studio/image/flare"
    private const val MARKETING_STUDIO_SUNBURST_ENDPOINT = "marketing-studio/image/sunburst"
    private const val QWEN_IMAGE_3_ENDPOINT = "alibaba/qwen-image-3/text-to-image"
    private const val QWEN_IMAGE_3_EDIT_ENDPOINT = "alibaba/qwen-image-3/edit"
    private const val SOUL_CINEMA_ENDPOINT = "higgsfield-ai/soul/cinema"
    private const val SEEDANCE_2_ENDPOINT = "bytedance/seedance-2.0/text-to-video"
    private const val SEEDANCE_2_5_ENDPOINT = "bytedance/seedance-2.5/text-to-video"
    private const val KLING_2_5_TURBO_ENDPOINT = "kling-video/v2.5-turbo/pro/text-to-video"
    private const val KLING_2_6_ENDPOINT = "kling-video/v2.6/pro/text-to-video"
    private const val KLING_3_ENDPOINT = "kling-video/v3.0/pro/text-to-video"
    private const val KLING_2_6_MOTION_ENDPOINT = "kling-video/motion-control/pro"
    private const val KLING_3_MOTION_ENDPOINT = "kling-video/v3/motion-control/pro"
    private const val KLING_O3_ENDPOINT = "kling-video/o3/image-reference"
    private const val KLING_OMNI_ENDPOINT = "kling-video/omni/image-reference"
    private const val CINEMA_STUDIO_4_ENDPOINT = "higgsfield/cinema-studio/4.0"
    private const val WAN_2_6_ENDPOINT = "wan/v2.6/text-to-video"
    private const val WAN_2_7_ENDPOINT = "wan/v2.7/text-to-video"
    private const val WAN_3_ENDPOINT = "alibaba/wan-3.0/text-to-video"
    private const val WAN_3_PRIME_ENDPOINT = "alibaba/wan-3.0-prime/text-to-video"
    private const val SOUL_STANDARD_SCHEMA_VERIFIED_ON = "2026-09-25"
    private const val SOUL_V2_SCHEMA_VERIFIED_ON = "2026-09-25"
    private val PRICING_FACTORS = listOf("resolution", "duration", "model options")
    private val imageEdit = setOf(WorkflowCapability.TEXT_TO_IMAGE, WorkflowCapability.IMAGE_TO_IMAGE, WorkflowCapability.REFERENCE_IMAGE)
    private val textToVideo = setOf(WorkflowCapability.TEXT_TO_VIDEO)
    private val motionControl = setOf(WorkflowCapability.IMAGE_TO_VIDEO)
    private val imageReferenceVideo = setOf(WorkflowCapability.TEXT_TO_VIDEO, WorkflowCapability.REFERENCE_IMAGE)
    private val imageOptions = setOf(WorkflowOption.ASPECT_RATIO, WorkflowOption.RESOLUTION)
    private val imageOptionsWithSeed = imageOptions + WorkflowOption.SEED
    private val qwenOptions = imageOptionsWithSeed + WorkflowOption.NEGATIVE_PROMPT
    private val sourceImageRequired = listOf(MediaRequirement(MediaRole.SOURCE, MediaKind.IMAGE, minimumCount = 1))
    private val motionControlRequired = listOf(
        MediaRequirement(MediaRole.SOURCE, MediaKind.IMAGE, minimumCount = 1),
        MediaRequirement(MediaRole.MOTION_REFERENCE, MediaKind.VIDEO, minimumCount = 1),
    )

    val all: List<WorkflowDescriptor> = listOf(
        image(
            WorkflowCatalog.SOUL,
            WorkflowFamily.SOUL,
            setOf(WorkflowCapability.TEXT_TO_IMAGE),
            endpointPath = SOUL_STANDARD_ENDPOINT,
            schemaVerifiedOn = SOUL_STANDARD_SCHEMA_VERIFIED_ON,
        ),
        image(
            WorkflowCatalog.SOUL_V2,
            WorkflowFamily.SOUL,
            setOf(WorkflowCapability.TEXT_TO_IMAGE),
            endpointPath = SOUL_V2_STANDARD_ENDPOINT,
            schemaVerifiedOn = SOUL_STANDARD_SCHEMA_VERIFIED_ON,
        ),
        image(WorkflowCatalog.SOUL_CINEMA, WorkflowFamily.SOUL, setOf(WorkflowCapability.TEXT_TO_IMAGE), options = imageOptionsWithSeed, endpointPath = SOUL_CINEMA_ENDPOINT, schemaVerifiedOn = SCHEMA_VERIFIED_ON),
        image(WorkflowCatalog.MARKETING_STUDIO_2_ALPHA, WorkflowFamily.MARKETING_STUDIO, imageEdit, options = imageOptions, endpointPath = MARKETING_STUDIO_ALPHA_ENDPOINT, schemaVerifiedOn = SCHEMA_VERIFIED_ON),
        image(WorkflowCatalog.MARKETING_STUDIO_2_5_FLARE, WorkflowFamily.MARKETING_STUDIO, imageEdit, options = imageOptions, endpointPath = MARKETING_STUDIO_FLARE_ENDPOINT, schemaVerifiedOn = SCHEMA_VERIFIED_ON),
        image(WorkflowCatalog.MARKETING_STUDIO_2_5_SUNBURST, WorkflowFamily.MARKETING_STUDIO, imageEdit, options = imageOptions, endpointPath = MARKETING_STUDIO_SUNBURST_ENDPOINT, schemaVerifiedOn = SCHEMA_VERIFIED_ON),
        image(WorkflowCatalog.QWEN_IMAGE_3, WorkflowFamily.QWEN, imageEdit + WorkflowCapability.NEGATIVE_PROMPT, options = qwenOptions, endpointPath = QWEN_IMAGE_3_ENDPOINT, schemaVerifiedOn = SCHEMA_VERIFIED_ON),
        image(WorkflowCatalog.QWEN_IMAGE_3_EDIT, WorkflowFamily.QWEN, imageEdit, sourceImageRequired, options = qwenOptions, endpointPath = QWEN_IMAGE_3_EDIT_ENDPOINT, schemaVerifiedOn = SCHEMA_VERIFIED_ON),
        video(WorkflowCatalog.SEEDANCE_2, WorkflowFamily.SEEDANCE, endpointPath = SEEDANCE_2_ENDPOINT, schemaVerifiedOn = SCHEMA_VERIFIED_ON),
        video(WorkflowCatalog.SEEDANCE_2_5, WorkflowFamily.SEEDANCE, endpointPath = SEEDANCE_2_5_ENDPOINT, schemaVerifiedOn = SCHEMA_VERIFIED_ON),
        video(WorkflowCatalog.KLING_2_5_TURBO, WorkflowFamily.KLING, endpointPath = KLING_2_5_TURBO_ENDPOINT, schemaVerifiedOn = SCHEMA_VERIFIED_ON),
        video(WorkflowCatalog.KLING_2_6, WorkflowFamily.KLING, endpointPath = KLING_2_6_ENDPOINT, schemaVerifiedOn = SCHEMA_VERIFIED_ON),
        video(WorkflowCatalog.KLING_2_6_MOTION, WorkflowFamily.KLING, motionControlRequired, capabilities = motionControl, endpointPath = KLING_2_6_MOTION_ENDPOINT, schemaVerifiedOn = SCHEMA_VERIFIED_ON),
        video(WorkflowCatalog.KLING_3, WorkflowFamily.KLING, endpointPath = KLING_3_ENDPOINT, schemaVerifiedOn = SCHEMA_VERIFIED_ON),
        video(WorkflowCatalog.KLING_3_MOTION, WorkflowFamily.KLING, motionControlRequired, capabilities = motionControl, endpointPath = KLING_3_MOTION_ENDPOINT, schemaVerifiedOn = SCHEMA_VERIFIED_ON),
        video(WorkflowCatalog.KLING_O3, WorkflowFamily.KLING, capabilities = imageReferenceVideo, endpointPath = KLING_O3_ENDPOINT, schemaVerifiedOn = SCHEMA_VERIFIED_ON),
        video(WorkflowCatalog.KLING_OMNI, WorkflowFamily.KLING, capabilities = imageReferenceVideo, endpointPath = KLING_OMNI_ENDPOINT, schemaVerifiedOn = SCHEMA_VERIFIED_ON),
        video(WorkflowCatalog.CINEMA_STUDIO_4, WorkflowFamily.CINEMA_STUDIO, endpointPath = CINEMA_STUDIO_4_ENDPOINT, schemaVerifiedOn = SCHEMA_VERIFIED_ON),
        video(WorkflowCatalog.WAN_2_6, WorkflowFamily.WAN, endpointPath = WAN_2_6_ENDPOINT, schemaVerifiedOn = SCHEMA_VERIFIED_ON),
        video(WorkflowCatalog.WAN_2_7, WorkflowFamily.WAN, endpointPath = WAN_2_7_ENDPOINT, schemaVerifiedOn = SCHEMA_VERIFIED_ON),
        video(WorkflowCatalog.WAN_3, WorkflowFamily.WAN, endpointPath = WAN_3_ENDPOINT, schemaVerifiedOn = SCHEMA_VERIFIED_ON),
        video(WorkflowCatalog.WAN_3_PRIME, WorkflowFamily.WAN, tier = PRIME_TIER, endpointPath = WAN_3_PRIME_ENDPOINT, schemaVerifiedOn = SCHEMA_VERIFIED_ON),
    )

    fun forKind(kind: MediaKind) = all.filter { it.mediaKind == kind }
    fun find(id: WorkflowId) = all.firstOrNull { it.id == id }
    fun compatibleEditors(kind: MediaKind, submissionReadyOnly: Boolean = true) = all.filter {
        it.mediaKind == kind && WorkflowCapability.IMAGE_TO_IMAGE in it.capabilities &&
            (!submissionReadyOnly || it.isSubmissionEnabled)
    }

    private fun image(
        key: WorkflowKey,
        family: WorkflowFamily,
        capabilities: Set<WorkflowCapability>,
        required: List<MediaRequirement> = emptyList(),
        options: Set<WorkflowOption> = emptySet(),
        endpointPath: String? = null,
        schemaVerifiedOn: String? = null,
    ) = descriptor(key, family, MediaKind.IMAGE, capabilities, required, options = options, endpointPath = endpointPath, schemaVerifiedOn = schemaVerifiedOn)

    private fun video(
        key: WorkflowKey,
        family: WorkflowFamily,
        required: List<MediaRequirement> = emptyList(),
        tier: String? = null,
        capabilities: Set<WorkflowCapability> = textToVideo,
        endpointPath: String? = null,
        schemaVerifiedOn: String? = null,
    ) = descriptor(
        key = key,
        family = family,
        kind = MediaKind.VIDEO,
        capabilities = capabilities,
        required = required,
        tier = tier,
        endpointPath = endpointPath,
        schemaVerifiedOn = schemaVerifiedOn,
    )

    private fun descriptor(
        key: WorkflowKey,
        family: WorkflowFamily,
        kind: MediaKind,
        capabilities: Set<WorkflowCapability>,
        required: List<MediaRequirement>,
        options: Set<WorkflowOption> = emptySet(),
        tier: String? = null,
        endpointPath: String? = null,
        schemaVerifiedOn: String? = null,
    ) =
        WorkflowDescriptor(
            id = key.id, displayName = key.displayName, family = family, mediaKind = kind,
            tier = tier, capabilities = capabilities, mediaRequirements = required, endpointPath = endpointPath,
            pricingFactors = PRICING_FACTORS,
            supportedOptions = options,
            staticEstimate = StaticEstimateMetadata(sourceUrl = WorkflowCatalog.DOCUMENTATION_URL, verifiedOn = schemaVerifiedOn ?: SCHEMA_VERIFIED_ON),
            documentationUrl = WorkflowCatalog.DOCUMENTATION_URL,
            schemaVerifiedOn = schemaVerifiedOn,
            isSubmissionEnabled = endpointPath != null,
        )

    private const val SCHEMA_VERIFIED_ON = "2026-09-25"
}
