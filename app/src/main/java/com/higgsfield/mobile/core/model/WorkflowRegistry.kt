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
    private const val SEEDANCE_2_REFERENCE_ENDPOINT = "bytedance/seedance-2.0/reference-to-video"
    private const val SEEDANCE_2_5_REFERENCE_ENDPOINT = "bytedance/seedance-2.5/reference-to-video"
    private const val HAPPY_HORSE_1_ENDPOINT = "alibaba/happy-horse/text-to-video"
    private const val Z_IMAGE_TURBO_ENDPOINT = "z-image/turbo"
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
    private const val NEW_SCHEMA_VERIFIED_ON = "2026-09-30"
    private val PRICING_FACTORS = listOf("resolution", "duration", "model options")
    private val imageEdit = setOf(WorkflowCapability.TEXT_TO_IMAGE, WorkflowCapability.IMAGE_TO_IMAGE, WorkflowCapability.REFERENCE_IMAGE)
    private val textToVideo = setOf(WorkflowCapability.TEXT_TO_VIDEO)
    private val motionControl = setOf(WorkflowCapability.IMAGE_TO_VIDEO)
    private val imageReferenceVideo = setOf(WorkflowCapability.TEXT_TO_VIDEO, WorkflowCapability.REFERENCE_IMAGE)
    private val imageOptions = setOf(WorkflowOption.ASPECT_RATIO, WorkflowOption.RESOLUTION)
    private val imageOptionsWithSeed = imageOptions + WorkflowOption.SEED
    private val qwenOptions = imageOptionsWithSeed + WorkflowOption.NEGATIVE_PROMPT
    private val sourceImageRequired = listOf(MediaRequirement(MediaRole.SOURCE, MediaKind.IMAGE, minimumCount = 1, maximumCount = 3))
    private val motionControlRequired = listOf(
        MediaRequirement(MediaRole.SOURCE, MediaKind.IMAGE, minimumCount = 1),
        MediaRequirement(MediaRole.MOTION_REFERENCE, MediaKind.VIDEO, minimumCount = 1),
    )
    private val seedance2References = listOf(
        MediaRequirement(MediaRole.REFERENCE, MediaKind.IMAGE, maximumCount = 9),
        MediaRequirement(MediaRole.VIDEO_REFERENCE, MediaKind.VIDEO, maximumCount = 3),
        MediaRequirement(MediaRole.AUDIO, MediaKind.AUDIO, maximumCount = null),
    )
    private val seedance2_5References = listOf(
        MediaRequirement(MediaRole.REFERENCE, MediaKind.IMAGE, maximumCount = 30),
        MediaRequirement(MediaRole.VIDEO_REFERENCE, MediaKind.VIDEO, maximumCount = 10),
        MediaRequirement(MediaRole.AUDIO, MediaKind.AUDIO, maximumCount = null),
    )
    private val referenceVideoOptions = setOf(WorkflowOption.DURATION, WorkflowOption.RESOLUTION, WorkflowOption.ASPECT_RATIO)
    private val seedanceOptions = referenceVideoOptions + WorkflowOption.GENERATE_AUDIO
    private val seedance2_5Options = seedanceOptions + WorkflowOption.OUTPUT_FORMAT
    private val imageAspectRatios = listOf("1:1", "2:3", "3:2", "3:4", "4:3", "7:9", "9:7", "9:16", "16:9", "21:9")
    private val videoAspectRatios = listOf("16:9", "4:3", "1:1", "3:4", "9:16", "21:9")

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
        image(WorkflowCatalog.MARKETING_STUDIO_2_ALPHA, WorkflowFamily.MARKETING_STUDIO, imageEdit, listOf(MediaRequirement(MediaRole.REFERENCE, MediaKind.IMAGE, maximumCount = 16)), options = imageOptions, endpointPath = MARKETING_STUDIO_ALPHA_ENDPOINT, schemaVerifiedOn = SCHEMA_VERIFIED_ON),
        image(WorkflowCatalog.MARKETING_STUDIO_2_5_FLARE, WorkflowFamily.MARKETING_STUDIO, imageEdit, listOf(MediaRequirement(MediaRole.REFERENCE, MediaKind.IMAGE, maximumCount = 16)), options = imageOptions, endpointPath = MARKETING_STUDIO_FLARE_ENDPOINT, schemaVerifiedOn = SCHEMA_VERIFIED_ON),
        image(WorkflowCatalog.MARKETING_STUDIO_2_5_SUNBURST, WorkflowFamily.MARKETING_STUDIO, imageEdit, listOf(MediaRequirement(MediaRole.REFERENCE, MediaKind.IMAGE, maximumCount = 16)), options = imageOptions, endpointPath = MARKETING_STUDIO_SUNBURST_ENDPOINT, schemaVerifiedOn = SCHEMA_VERIFIED_ON),
        image(WorkflowCatalog.QWEN_IMAGE_3, WorkflowFamily.QWEN, setOf(WorkflowCapability.TEXT_TO_IMAGE, WorkflowCapability.NEGATIVE_PROMPT), options = qwenOptions, endpointPath = QWEN_IMAGE_3_ENDPOINT, schemaVerifiedOn = SCHEMA_VERIFIED_ON),
        image(WorkflowCatalog.QWEN_IMAGE_3_EDIT, WorkflowFamily.QWEN, imageEdit, sourceImageRequired, options = qwenOptions, endpointPath = QWEN_IMAGE_3_EDIT_ENDPOINT, schemaVerifiedOn = SCHEMA_VERIFIED_ON),
        image(WorkflowCatalog.Z_IMAGE_TURBO, WorkflowFamily.Z_IMAGE, setOf(WorkflowCapability.TEXT_TO_IMAGE), options = imageOptionsWithSeed + WorkflowOption.PROMPT_EXTEND, endpointPath = Z_IMAGE_TURBO_ENDPOINT, schemaVerifiedOn = NEW_SCHEMA_VERIFIED_ON),
        video(WorkflowCatalog.SEEDANCE_2, WorkflowFamily.SEEDANCE, options = seedanceOptions, endpointPath = SEEDANCE_2_ENDPOINT, schemaVerifiedOn = NEW_SCHEMA_VERIFIED_ON),
        video(WorkflowCatalog.SEEDANCE_2_5, WorkflowFamily.SEEDANCE, options = seedance2_5Options, endpointPath = SEEDANCE_2_5_ENDPOINT, schemaVerifiedOn = NEW_SCHEMA_VERIFIED_ON),
        video(WorkflowCatalog.SEEDANCE_2_REFERENCE, WorkflowFamily.SEEDANCE, seedance2References, capabilities = setOf(WorkflowCapability.REFERENCE_IMAGE), options = seedanceOptions, endpointPath = SEEDANCE_2_REFERENCE_ENDPOINT, schemaVerifiedOn = NEW_SCHEMA_VERIFIED_ON),
        video(WorkflowCatalog.SEEDANCE_2_5_REFERENCE, WorkflowFamily.SEEDANCE, seedance2_5References, capabilities = setOf(WorkflowCapability.REFERENCE_IMAGE), options = seedance2_5Options, endpointPath = SEEDANCE_2_5_REFERENCE_ENDPOINT, schemaVerifiedOn = NEW_SCHEMA_VERIFIED_ON),
        video(WorkflowCatalog.HAPPY_HORSE_1, WorkflowFamily.HAPPY_HORSE, options = referenceVideoOptions + WorkflowOption.SEED, endpointPath = HAPPY_HORSE_1_ENDPOINT, schemaVerifiedOn = NEW_SCHEMA_VERIFIED_ON),
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
        video(WorkflowCatalog.WAN_3, WorkflowFamily.WAN, options = setOf(WorkflowOption.ASPECT_RATIO, WorkflowOption.RESOLUTION, WorkflowOption.DURATION, WorkflowOption.SEED, WorkflowOption.GENERATE_AUDIO, WorkflowOption.ENABLE_THINKING), endpointPath = WAN_3_ENDPOINT, schemaVerifiedOn = SCHEMA_VERIFIED_ON),
        video(WorkflowCatalog.WAN_3_PRIME, WorkflowFamily.WAN, tier = PRIME_TIER, options = setOf(WorkflowOption.ASPECT_RATIO, WorkflowOption.RESOLUTION, WorkflowOption.DURATION, WorkflowOption.SEED, WorkflowOption.GENERATE_AUDIO, WorkflowOption.ENABLE_THINKING), endpointPath = WAN_3_PRIME_ENDPOINT, schemaVerifiedOn = SCHEMA_VERIFIED_ON),
    ) + DirectModelRoutes.all

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
        options: Set<WorkflowOption> = emptySet(),
        endpointPath: String? = null,
        schemaVerifiedOn: String? = null,
    ) = descriptor(
        key = key,
        family = family,
        kind = MediaKind.VIDEO,
        capabilities = capabilities,
        required = required,
        options = options,
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
            optionConstraints = when (key.id) {
                WorkflowCatalog.WAN_3.id, WorkflowCatalog.WAN_3_PRIME.id -> DirectModelRoutes.wan3Image.optionConstraints
                WorkflowCatalog.Z_IMAGE_TURBO.id -> mapOf(
                    WorkflowOption.RESOLUTION to OptionConstraint(listOf("1k", "2k")),
                    WorkflowOption.ASPECT_RATIO to OptionConstraint(imageAspectRatios),
                    WorkflowOption.SEED to OptionConstraint(minimum = 0, maximum = Int.MAX_VALUE),
                )
                WorkflowCatalog.SEEDANCE_2.id, WorkflowCatalog.SEEDANCE_2_REFERENCE.id,
                WorkflowCatalog.SEEDANCE_2_5.id, WorkflowCatalog.SEEDANCE_2_5_REFERENCE.id -> mapOf(
                    WorkflowOption.DURATION to OptionConstraint(minimum = 4, maximum = if (key.id in setOf(WorkflowCatalog.SEEDANCE_2.id, WorkflowCatalog.SEEDANCE_2_REFERENCE.id)) 15 else 30),
                    WorkflowOption.RESOLUTION to OptionConstraint(when (key.id) {
                        WorkflowCatalog.SEEDANCE_2.id, WorkflowCatalog.SEEDANCE_2_REFERENCE.id -> listOf("480p", "720p", "1080p", "4k")
                        WorkflowCatalog.SEEDANCE_2_5_REFERENCE.id -> listOf("480p", "720p", "1080p")
                        else -> listOf("480p", "720p")
                    }),
                    WorkflowOption.ASPECT_RATIO to OptionConstraint(videoAspectRatios),
                    WorkflowOption.OUTPUT_FORMAT to OptionConstraint(listOf("mp4", "mov")),
                )
                else -> emptyMap()
            },
            staticEstimate = staticMetadataFor(key, schemaVerifiedOn),
            documentationUrl = WorkflowCatalog.DOCUMENTATION_URL,
            schemaVerifiedOn = schemaVerifiedOn,
            isSubmissionEnabled = endpointPath != null,
        )

    private const val SCHEMA_VERIFIED_ON = "2026-09-25"

    /** Static pricing/specification transcription supplied by the user on 2026-09-26. */
    private fun staticMetadataFor(key: WorkflowKey, schemaVerifiedOn: String?): StaticEstimateMetadata {
        val (resolution, durations) = when (key.id) {
            WorkflowCatalog.SOUL.id, WorkflowCatalog.SOUL_V2.id -> "Up to 1080p" to null
            WorkflowCatalog.MARKETING_STUDIO_2_ALPHA.id,
            WorkflowCatalog.MARKETING_STUDIO_2_5_FLARE.id,
            WorkflowCatalog.MARKETING_STUDIO_2_5_SUNBURST.id -> "Up to 4K" to null
            WorkflowCatalog.QWEN_IMAGE_3.id, WorkflowCatalog.QWEN_IMAGE_3_EDIT.id,
            WorkflowCatalog.Z_IMAGE_TURBO.id -> "Up to 2K" to null
            WorkflowCatalog.SEEDANCE_2.id, WorkflowCatalog.SEEDANCE_2_REFERENCE.id -> "Up to 4K" to "4s / 15s"
            WorkflowCatalog.SEEDANCE_2_5.id, WorkflowCatalog.SEEDANCE_2_5_REFERENCE.id -> "Up to 1080p" to "4s / 30s"
            WorkflowCatalog.HAPPY_HORSE_1.id -> "Up to 1080p" to "3s / 15s"
            WorkflowCatalog.KLING_2_5_TURBO.id -> "Up to 1080p" to "5s / 10s"
            WorkflowCatalog.KLING_2_6.id, WorkflowCatalog.KLING_2_6_MOTION.id -> null to "1s / 5s / 10s"
            WorkflowCatalog.KLING_3.id, WorkflowCatalog.KLING_3_MOTION.id -> "Up to 1080p" to "1s / 3s / 15s"
            WorkflowCatalog.KLING_O3.id, WorkflowCatalog.KLING_OMNI.id -> null to "1s / 3s / 5s / 10s"
            WorkflowCatalog.CINEMA_STUDIO_4.id -> "Up to 720p" to "4s / 30s"
            WorkflowCatalog.WAN_2_6.id -> "Up to 1080p" to "5s / 10s / 15s"
            WorkflowCatalog.WAN_2_7.id -> "Up to 1080p" to "2s / 10s / 15s"
            WorkflowCatalog.WAN_3.id, WorkflowCatalog.WAN_3_PRIME.id -> "Up to 1080p" to "2s / 30s"
            else -> null to null
        }
        return StaticEstimateMetadata(
            fromPrice = ModelStartingRates.priceFor(key.id),
            maximumResolution = resolution,
            supportedDurations = durations,
            sourceLabel = if (key.id in setOf(
                    WorkflowCatalog.Z_IMAGE_TURBO.id,
                    WorkflowCatalog.SEEDANCE_2_REFERENCE.id,
                    WorkflowCatalog.SEEDANCE_2_5_REFERENCE.id,
                    WorkflowCatalog.HAPPY_HORSE_1.id,
                )) "Higgsfield model API documentation; price not verified" else "User-provided Higgsfield pricing and model-specification screenshots",
            sourceUrl = WorkflowCatalog.DOCUMENTATION_URL,
            verifiedOn = if (key.id in setOf(
                    WorkflowCatalog.Z_IMAGE_TURBO.id,
                    WorkflowCatalog.SEEDANCE_2_REFERENCE.id,
                    WorkflowCatalog.SEEDANCE_2_5_REFERENCE.id,
                    WorkflowCatalog.HAPPY_HORSE_1.id,
                )) "2026-09-30" else schemaVerifiedOn ?: "2026-09-26",
        )
    }
}
