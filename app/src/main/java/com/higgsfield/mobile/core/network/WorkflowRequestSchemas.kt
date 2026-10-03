package com.higgsfield.mobile.core.network

import com.higgsfield.mobile.core.error.ErrorMapper
import com.higgsfield.mobile.core.model.WorkflowCatalog
import com.higgsfield.mobile.core.model.WorkflowRegistry
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.intOrNull

object WorkflowRequestSchemas {
    val soulStandard = WorkflowRequestSchema(
        workflowId = WorkflowCatalog.SOUL.id,
        fields = listOf(WorkflowRequestValues.requiredComposedPrompt()),
    )

    val soulV2Standard = WorkflowRequestSchema(
        workflowId = WorkflowCatalog.SOUL_V2.id,
        fields = listOf(WorkflowRequestValues.requiredComposedPrompt()),
    )

    val soulCinema = WorkflowRequestSchema(
        workflowId = WorkflowCatalog.SOUL_CINEMA.id,
        fields = listOf(
            WorkflowRequestValues.requiredComposedPrompt(),
            WorkflowRequestValues.optional(RESOLUTION_FIELD, WorkflowRequestValues.resolution),
            WorkflowRequestValues.optional(ASPECT_RATIO_FIELD, WorkflowRequestValues.aspectRatio),
            WorkflowRequestValues.optional(SEED_FIELD, WorkflowRequestValues.seed),
            WorkflowRequestValues.constantBoolean(ENHANCE_PROMPT_FIELD, false),
        ),
    )

    val marketingStudioAlpha = marketingStudioSchema(WorkflowCatalog.MARKETING_STUDIO_2_ALPHA.id)

    val marketingStudioFlare = marketingStudioSchema(WorkflowCatalog.MARKETING_STUDIO_2_5_FLARE.id)

    val marketingStudioSunburst = marketingStudioSchema(WorkflowCatalog.MARKETING_STUDIO_2_5_SUNBURST.id)

    val qwenImage3 = WorkflowRequestSchema(
        workflowId = WorkflowCatalog.QWEN_IMAGE_3.id,
        fields = imageGenerationFields(),
    )

    val qwenImage3Edit = WorkflowRequestSchema(
        workflowId = WorkflowCatalog.QWEN_IMAGE_3_EDIT.id,
        fields = imageGenerationFields(requiredImageUrls = true),
    )

    val zImageTurbo = WorkflowRequestSchema(
        workflowId = WorkflowCatalog.Z_IMAGE_TURBO.id,
        fields = listOf(
            WorkflowRequestValues.requiredComposedPrompt(),
            choice(RESOLUTION_FIELD, WorkflowRequestValues.resolution, setOf("1k", "2k")),
            choice(ASPECT_RATIO_FIELD, WorkflowRequestValues.aspectRatio, setOf("1:1", "2:3", "3:2", "3:4", "4:3", "7:9", "9:7", "9:16", "16:9", "21:9")),
            integerRange(SEED_FIELD, WorkflowRequestValues.seed, 0, 2147483647),
            WorkflowRequestValues.optional("prompt_extend", WorkflowRequestValues.modelBoolean("prompt_extend")),
        ),
    )

    val seedance2 = seedanceTextSchema(WorkflowCatalog.SEEDANCE_2.id, 15, true)

    val seedance2_5 = seedanceTextSchema(WorkflowCatalog.SEEDANCE_2_5.id, 30, false)
    val seedance2Reference = referenceVideoSchema(WorkflowCatalog.SEEDANCE_2_REFERENCE.id, 9, 3, 15, true)
    val seedance2_5Reference = referenceVideoSchema(WorkflowCatalog.SEEDANCE_2_5_REFERENCE.id, 30, 10, 30, false)
    val happyHorse1 = WorkflowRequestSchema(
        workflowId = WorkflowCatalog.HAPPY_HORSE_1.id,
        fields = listOf(
            WorkflowRequestValues.requiredComposedPrompt(),
            integerRange(DURATION_FIELD, WorkflowRequestValues.duration, 3, 15),
            choice(RESOLUTION_FIELD, WorkflowRequestValues.resolution, setOf("720p", "1080p")),
            choice(ASPECT_RATIO_FIELD, WorkflowRequestValues.aspectRatio, setOf("16:9", "9:16", "1:1", "4:3", "3:4")),
            integerRange(SEED_FIELD, WorkflowRequestValues.seed, 1, 2147483646),
        ),
    )

    val kling2_5Turbo = textToVideoSchema(WorkflowCatalog.KLING_2_5_TURBO.id)

    val kling2_6 = textToVideoSchema(WorkflowCatalog.KLING_2_6.id)

    val kling3 = textToVideoSchema(WorkflowCatalog.KLING_3.id)

    val kling2_6Motion = motionControlSchema(WorkflowCatalog.KLING_2_6_MOTION.id)

    val kling3Motion = motionControlSchema(WorkflowCatalog.KLING_3_MOTION.id)

    val klingO3 = imageReferenceVideoSchema(WorkflowCatalog.KLING_O3.id)

    val klingOmni = imageReferenceVideoSchema(WorkflowCatalog.KLING_OMNI.id)

    val cinemaStudio4 = textToVideoSchema(WorkflowCatalog.CINEMA_STUDIO_4.id)

    val wan2_6 = textToVideoSchema(WorkflowCatalog.WAN_2_6.id)

    val wan2_7 = textToVideoSchema(WorkflowCatalog.WAN_2_7.id)

    val wan3 = DirectModelRequestSchemas.schemaFor(requireNotNull(WorkflowRegistry.find(WorkflowCatalog.WAN_3.id)))

    val wan3Prime = DirectModelRequestSchemas.schemaFor(requireNotNull(WorkflowRegistry.find(WorkflowCatalog.WAN_3_PRIME.id)))

    val all: List<WorkflowRequestSchema> = listOf(
        soulStandard,
        soulV2Standard,
        soulCinema,
        marketingStudioAlpha,
        marketingStudioFlare,
        marketingStudioSunburst,
        qwenImage3,
        qwenImage3Edit,
        zImageTurbo,
        seedance2,
        seedance2_5,
        seedance2Reference,
        seedance2_5Reference,
        happyHorse1,
        kling2_5Turbo,
        kling2_6,
        kling3,
        kling2_6Motion,
        kling3Motion,
        klingO3,
        klingOmni,
        cinemaStudio4,
        wan2_6,
        wan2_7,
        wan3,
        wan3Prime,
    ) + DirectModelRequestSchemas.all

    fun find(workflowId: com.higgsfield.mobile.core.model.WorkflowId): WorkflowRequestSchema? =
        all.firstOrNull { it.workflowId == workflowId }

    private fun marketingStudioSchema(workflowId: com.higgsfield.mobile.core.model.WorkflowId) =
        WorkflowRequestSchema(
            workflowId = workflowId,
            fields = listOf(
                WorkflowRequestValues.requiredComposedPrompt(),
                WorkflowRequestValues.constantString(QUALITY_FIELD, HIGH_QUALITY),
                WorkflowRequestValues.optional(RESOLUTION_FIELD, WorkflowRequestValues.resolution),
                WorkflowRequestValues.optional(ASPECT_RATIO_FIELD, WorkflowRequestValues.aspectRatio),
                WorkflowRequestValues.optionalUploadedImageUrls(MAXIMUM_MARKETING_IMAGES),
                WorkflowRequestValues.constantBoolean(ENHANCE_PROMPT_FIELD, false),
            ),
        )

    private fun imageGenerationFields(requiredImageUrls: Boolean = false) = buildList {
        add(WorkflowRequestValues.requiredComposedPrompt())
        if (requiredImageUrls) add(WorkflowRequestValues.requiredUploadedImageUrls(MAXIMUM_QWEN_EDIT_IMAGES))
        add(WorkflowRequestValues.optional(RESOLUTION_FIELD, WorkflowRequestValues.resolution))
        add(WorkflowRequestValues.optional(ASPECT_RATIO_FIELD, WorkflowRequestValues.aspectRatio))
        add(WorkflowRequestValues.optional(SEED_FIELD, WorkflowRequestValues.seed))
        add(WorkflowRequestValues.optional(NEGATIVE_PROMPT_FIELD, WorkflowRequestValues.negativePrompt))
    }

    /** Text-only schema used only for the documented text-to-video route of each listed family. */
    private fun textToVideoSchema(workflowId: com.higgsfield.mobile.core.model.WorkflowId) =
        WorkflowRequestSchema(
            workflowId = workflowId,
            fields = listOf(WorkflowRequestValues.requiredComposedPrompt()),
        )

    private fun referenceVideoSchema(
        workflowId: com.higgsfield.mobile.core.model.WorkflowId,
        maximumImages: Int,
        maximumVideos: Int,
        maximumDuration: Int,
        supports4k: Boolean,
    ) =
        WorkflowRequestSchema(
            workflowId = workflowId,
            fields = listOf(
                WorkflowRequestValues.optional(PROMPT_FIELD, WorkflowRequestValues.composedPrompt),
                WorkflowRequestValues.optionalUploadedImageUrls(maximumImages),
                WorkflowRequestValues.optionalUploadedVideoUrls(maximumVideos),
                WorkflowRequestValues.optional("audio_urls", WorkflowRequestValues.uploadedAudioUrls),
                integerRange(DURATION_FIELD, WorkflowRequestValues.duration, 4, maximumDuration),
                choice(RESOLUTION_FIELD, WorkflowRequestValues.resolution, if (supports4k) setOf("480p", "720p", "1080p", "4k") else setOf("480p", "720p", "1080p")),
                choice(ASPECT_RATIO_FIELD, WorkflowRequestValues.aspectRatio, setOf("16:9", "4:3", "1:1", "3:4", "9:16", "21:9")),
                WorkflowRequestValues.optional("generate_audio", WorkflowRequestValues.modelBoolean("generate_audio")),
            ) + if (supports4k) emptyList() else listOf(choice("output_format", WorkflowRequestValues.modelString("output_format"), setOf("mp4", "mov"))),
            requiresImageOrVideoReference = true,
        )

    private fun seedanceTextSchema(workflowId: com.higgsfield.mobile.core.model.WorkflowId, maximumDuration: Int, supports4k: Boolean) =
        WorkflowRequestSchema(
            workflowId = workflowId,
            fields = listOf(
                WorkflowRequestValues.requiredComposedPrompt(),
                integerRange(DURATION_FIELD, WorkflowRequestValues.duration, 4, maximumDuration),
                choice(RESOLUTION_FIELD, WorkflowRequestValues.resolution, if (supports4k) setOf("480p", "720p", "1080p", "4k") else setOf("480p", "720p")),
                choice(ASPECT_RATIO_FIELD, WorkflowRequestValues.aspectRatio, setOf("16:9", "4:3", "1:1", "3:4", "9:16", "21:9")),
                WorkflowRequestValues.optional("generate_audio", WorkflowRequestValues.modelBoolean("generate_audio")),
            ) + if (supports4k) emptyList() else listOf(choice("output_format", WorkflowRequestValues.modelString("output_format"), setOf("mp4", "mov"))),
        )

    private fun choice(name: String, value: WorkflowRequestValue, allowed: Set<String>) = WorkflowRequestField(
        name, value, { json: JsonElement? ->
            if (json != null && json.jsonPrimitive.content !in allowed) listOf(ErrorMapper.protocol("Unsupported $name value.")) else emptyList()
        },
    )

    private fun integerRange(name: String, value: WorkflowRequestValue, minimum: Int, maximum: Int) = WorkflowRequestField(
        name, value, { json: JsonElement? ->
            if (json != null && json.jsonPrimitive.intOrNull?.let { it in minimum..maximum } != true)
                listOf(ErrorMapper.protocol("$name must be between $minimum and $maximum.")) else emptyList()
        },
    )

    private fun motionControlSchema(workflowId: com.higgsfield.mobile.core.model.WorkflowId) =
        WorkflowRequestSchema(
            workflowId = workflowId,
            fields = listOf(
                WorkflowRequestValues.optional(PROMPT_FIELD, WorkflowRequestValues.composedPrompt),
                WorkflowRequestValues.requiredUploadedUrl(IMAGE_URL_FIELD, com.higgsfield.mobile.core.model.MediaKind.IMAGE, ErrorMapper.referenceImageRequired()),
                WorkflowRequestValues.requiredUploadedUrl(VIDEO_URL_FIELD, com.higgsfield.mobile.core.model.MediaKind.VIDEO, ErrorMapper.referenceVideoRequired()),
            ),
        )

    private fun imageReferenceVideoSchema(workflowId: com.higgsfield.mobile.core.model.WorkflowId) =
        WorkflowRequestSchema(
            workflowId = workflowId,
            fields = listOf(
                WorkflowRequestValues.requiredComposedPrompt(),
                WorkflowRequestValues.optionalUploadedImageUrls(),
            ),
        )

    private const val QUALITY_FIELD = "quality"
    private const val PROMPT_FIELD = "prompt"
    private const val HIGH_QUALITY = "high"
    private const val RESOLUTION_FIELD = "resolution"
    private const val ASPECT_RATIO_FIELD = "aspect_ratio"
    private const val ENHANCE_PROMPT_FIELD = "enhance_prompt"
    private const val SEED_FIELD = "seed"
    private const val DURATION_FIELD = "duration"
    private const val NEGATIVE_PROMPT_FIELD = "negative_prompt"
    private const val IMAGE_URL_FIELD = "image_url"
    private const val VIDEO_URL_FIELD = "video_url"
    private const val MAXIMUM_QWEN_EDIT_IMAGES = 3
    private const val MAXIMUM_MARKETING_IMAGES = 16
}
