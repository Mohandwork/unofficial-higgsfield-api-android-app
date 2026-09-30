package com.higgsfield.mobile.core.network

import com.higgsfield.mobile.core.error.ErrorMapper
import com.higgsfield.mobile.core.model.DirectModelRoutes
import com.higgsfield.mobile.core.model.MediaKind
import com.higgsfield.mobile.core.model.MediaRole
import com.higgsfield.mobile.core.model.OptionConstraint
import com.higgsfield.mobile.core.model.WorkflowDescriptor
import com.higgsfield.mobile.core.model.WorkflowOption
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

/** Request fields for direct operations; workflow guides are intentionally absent. */
object DirectModelRequestSchemas {
    val all = DirectModelRoutes.all.map(::schemaFor)

    fun schemaFor(model: WorkflowDescriptor): WorkflowRequestSchema =
        WorkflowRequestSchema(model.id, buildList {
            if (!model.promptRequired) add(WorkflowRequestValues.optional("prompt", WorkflowRequestValues.composedPrompt))
            else add(WorkflowRequestValues.requiredComposedPrompt())
            when (model) {
                DirectModelRoutes.grok -> add(WorkflowRequestValues.optionalUploadedImageUrls())
                DirectModelRoutes.ideogram -> add(WorkflowRequestValues.optional("image_url", WorkflowRequestValues.uploadedUrl(MediaRole.SOURCE, MediaKind.IMAGE)))
                DirectModelRoutes.soulV2Edit -> {
                    add(WorkflowRequestValues.requiredUploadedUrl("image_url", MediaRole.SOURCE, MediaKind.IMAGE, ErrorMapper.referenceImageRequired()))
                    add(WorkflowRequestValues.constantBoolean("enhance_prompt", true))
                }
                DirectModelRoutes.genjutsuMotion, DirectModelRoutes.genjutsuSwap -> {
                    add(WorkflowRequestValues.requiredUploadedUrl("video_url", MediaRole.SOURCE, MediaKind.VIDEO, ErrorMapper.referenceVideoRequired()))
                    add(WorkflowRequestValues.requiredUploadedImageUrls(8))
                }
                DirectModelRoutes.miniImage, DirectModelRoutes.pixImage,
                DirectModelRoutes.wan3Image, DirectModelRoutes.wan3PrimeImage,
                DirectModelRoutes.seedance2Image, DirectModelRoutes.seedance2_5Image,
                DirectModelRoutes.wan2_7Image -> {
                    add(WorkflowRequestValues.requiredUploadedUrl("image_url", MediaRole.START_FRAME, MediaKind.IMAGE, ErrorMapper.referenceImageRequired()))
                    add(WorkflowRequestValues.optional("end_image_url", WorkflowRequestValues.uploadedUrl(MediaRole.END_FRAME, MediaKind.IMAGE)))
                    if (model == DirectModelRoutes.wan2_7Image)
                        add(WorkflowRequestValues.optional("audio_url", WorkflowRequestValues.uploadedUrl(MediaRole.AUDIO, MediaKind.AUDIO)))
                }
                DirectModelRoutes.kling2_5Image, DirectModelRoutes.kling2_5StandardImage,
                DirectModelRoutes.kling2_6Image, DirectModelRoutes.kling3TurboImage ->
                    add(WorkflowRequestValues.requiredUploadedUrl("image_url", MediaRole.START_FRAME, MediaKind.IMAGE, ErrorMapper.referenceImageRequired()))
                DirectModelRoutes.happyImage, DirectModelRoutes.wan2_6Image ->
                    add(WorkflowRequestValues.requiredUploadedUrl("image_url", MediaRole.START_FRAME, MediaKind.IMAGE, ErrorMapper.referenceImageRequired()))
                DirectModelRoutes.kling3StandardImage, DirectModelRoutes.kling3ProImage, DirectModelRoutes.kling3_4kImage -> {
                    add(WorkflowRequestValues.requiredUploadedUrl("image_url", MediaRole.START_FRAME, MediaKind.IMAGE, ErrorMapper.referenceImageRequired()))
                    add(WorkflowRequestValues.optional("last_image_url", WorkflowRequestValues.uploadedUrl(MediaRole.END_FRAME, MediaKind.IMAGE)))
                }
                DirectModelRoutes.kling2_6MotionStandard, DirectModelRoutes.kling3MotionStandard -> {
                    add(WorkflowRequestValues.requiredUploadedUrl("image_url", MediaRole.SOURCE, MediaKind.IMAGE, ErrorMapper.referenceImageRequired()))
                    add(WorkflowRequestValues.requiredUploadedUrl("video_url", MediaRole.MOTION_REFERENCE, MediaKind.VIDEO, ErrorMapper.referenceVideoRequired()))
                }
                DirectModelRoutes.klingO3Frames, DirectModelRoutes.klingOmniFrames -> {
                    add(WorkflowRequestValues.requiredUploadedUrl("first_frame_url", MediaRole.START_FRAME, MediaKind.IMAGE, ErrorMapper.referenceImageRequired()))
                    add(WorkflowRequestValues.optional("last_frame_url", WorkflowRequestValues.uploadedUrl(MediaRole.END_FRAME, MediaKind.IMAGE)))
                }
                DirectModelRoutes.klingO3Edit, DirectModelRoutes.klingOmniEdit,
                DirectModelRoutes.klingO3VideoReference, DirectModelRoutes.klingOmniVideoReference -> {
                    val role = if (model in setOf(DirectModelRoutes.klingO3Edit, DirectModelRoutes.klingOmniEdit)) MediaRole.SOURCE else MediaRole.VIDEO_REFERENCE
                    add(WorkflowRequestField("video_urls", WorkflowRequestValues.uploadedUrls(role, MediaKind.VIDEO),
                        { value -> if (value == null) listOf(ErrorMapper.referenceVideoRequired()) else emptyList() }))
                    add(WorkflowRequestValues.optional("image_urls", WorkflowRequestValues.uploadedUrls(MediaRole.REFERENCE, MediaKind.IMAGE)))
                }
                DirectModelRoutes.miniReference, DirectModelRoutes.wan3Reference,
                DirectModelRoutes.wan3PrimeReference -> {
                    add(WorkflowRequestValues.optionalUploadedImageUrls())
                    add(WorkflowRequestValues.optional("video_urls", WorkflowRequestValues.uploadedVideoUrls))
                    add(WorkflowRequestValues.optional("audio_urls", WorkflowRequestValues.uploadedAudioUrls))
                }
                DirectModelRoutes.happyReference -> add(WorkflowRequestField("image_urls", WorkflowRequestValues.uploadedImageUrls,
                    { value -> if (value == null) listOf(ErrorMapper.referenceImageRequired()) else emptyList() }))
                DirectModelRoutes.wan2_6Reference -> add(WorkflowRequestField("video_urls", WorkflowRequestValues.uploadedVideoUrls,
                    { value -> if (value == null) listOf(ErrorMapper.referenceVideoRequired()) else emptyList() }))
                DirectModelRoutes.seedance2_5Edit, DirectModelRoutes.seedance2_5Extend -> {
                    add(WorkflowRequestValues.requiredUploadedUrl("video_url", MediaRole.SOURCE, MediaKind.VIDEO, ErrorMapper.referenceVideoRequired()))
                    add(WorkflowRequestValues.optional("image_urls", WorkflowRequestValues.uploadedUrls(MediaRole.REFERENCE, MediaKind.IMAGE)))
                    add(WorkflowRequestValues.optional("video_urls", WorkflowRequestValues.uploadedUrls(MediaRole.VIDEO_REFERENCE, MediaKind.VIDEO)))
                    add(WorkflowRequestValues.optional("audio_urls", WorkflowRequestValues.uploadedUrls(MediaRole.AUDIO, MediaKind.AUDIO)))
                }
            }
            model.supportedOptions.forEach { option ->
                val name = option.apiName ?: return@forEach
                val value = when (option) {
                    WorkflowOption.ASPECT_RATIO -> WorkflowRequestValues.aspectRatio
                    WorkflowOption.RESOLUTION -> WorkflowRequestValues.resolution
                    WorkflowOption.DURATION -> WorkflowRequestValues.duration
                    WorkflowOption.SEED -> WorkflowRequestValues.seed
                    WorkflowOption.NEGATIVE_PROMPT -> WorkflowRequestValues.negativePrompt
                    WorkflowOption.GENERATE_AUDIO, WorkflowOption.AIGC_WATERMARK,
                    WorkflowOption.ENABLE_THINKING, WorkflowOption.PROMPT_EXTEND -> WorkflowRequestValues.modelBoolean(name)
                    WorkflowOption.IMAGE_WEIGHT -> WorkflowRequestValues.modelInteger(name)
                    WorkflowOption.CFG_SCALE -> WorkflowRequestValues.modelDecimal(name)
                    else -> WorkflowRequestValues.modelString(name)
                }
                add(constrained(name, value, model.optionConstraints[option]))
            }
        })

    private fun constrained(name: String, value: WorkflowRequestValue, constraint: OptionConstraint?) =
        WorkflowRequestField(name, value, { json: JsonElement? ->
            if (json == null || constraint == null) emptyList()
            else if (constraint.choices.isNotEmpty() && json.jsonPrimitive.content !in constraint.choices)
                listOf(ErrorMapper.protocol("Unsupported $name value."))
            else if ((constraint.minimum != null || constraint.maximum != null) &&
                json.jsonPrimitive.intOrNull?.let { it >= (constraint.minimum ?: Int.MIN_VALUE) && it <= (constraint.maximum ?: Int.MAX_VALUE) } != true)
                listOf(ErrorMapper.protocol("$name is outside the supported range."))
            else emptyList()
        })

    private val WorkflowOption.apiName: String? get() = when (this) {
        WorkflowOption.ASPECT_RATIO -> "aspect_ratio"
        WorkflowOption.RESOLUTION -> "resolution"
        WorkflowOption.DURATION -> "duration"
        WorkflowOption.SEED -> "seed"
        WorkflowOption.NEGATIVE_PROMPT -> "negative_prompt"
        WorkflowOption.GENERATE_AUDIO -> "generate_audio"
        WorkflowOption.AIGC_WATERMARK -> "aigc_watermark"
        WorkflowOption.ENABLE_THINKING -> "enable_thinking"
        WorkflowOption.PROMPT_EXTEND -> "prompt_extend"
        WorkflowOption.RENDERING_SPEED -> "rendering_speed"
        WorkflowOption.IMAGE_WEIGHT -> "image_weight"
        WorkflowOption.OUTPUT_FORMAT -> "output_format"
        WorkflowOption.QUALITY -> "quality"
        WorkflowOption.SOUND -> "sound"
        WorkflowOption.MODE -> "mode"
        WorkflowOption.CFG_SCALE -> "cfg_scale"
        WorkflowOption.KEEP_ORIGINAL_SOUND -> "keep_original_sound"
        WorkflowOption.CHARACTER_ORIENTATION -> "character_orientation"
    }
}
