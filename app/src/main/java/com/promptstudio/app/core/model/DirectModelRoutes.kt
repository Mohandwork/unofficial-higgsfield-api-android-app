package com.promptstudio.app.core.model

/** Direct API operations. Guided workflow presentations are documented separately. */
object DirectModelRoutes {
    private const val VERIFIED = "2026-09-30"
    private const val DOCS = "https://open.higgsfield.ai/models/"
    private val frame = listOf(
        MediaRequirement(MediaRole.START_FRAME, MediaKind.IMAGE, 1, 1),
        MediaRequirement(MediaRole.END_FRAME, MediaKind.IMAGE, 0, 1),
    )
    private val imageVideoOptions = setOf(WorkflowOption.ASPECT_RATIO, WorkflowOption.RESOLUTION, WorkflowOption.DURATION,
        WorkflowOption.SEED, WorkflowOption.GENERATE_AUDIO, WorkflowOption.ENABLE_THINKING)
    private val wanChoices = mapOf(
        WorkflowOption.ASPECT_RATIO to OptionConstraint(listOf("adaptive", "16:9", "4:3", "1:1", "3:4", "9:16")),
        WorkflowOption.RESOLUTION to OptionConstraint(listOf("480p", "720p", "1080p")),
        WorkflowOption.DURATION to OptionConstraint(minimum = 2, maximum = 30),
        WorkflowOption.SEED to OptionConstraint(minimum = 0, maximum = Int.MAX_VALUE),
    )
    private val miniOptions = setOf(WorkflowOption.DURATION, WorkflowOption.RESOLUTION, WorkflowOption.ASPECT_RATIO, WorkflowOption.AIGC_WATERMARK)
    private val miniChoices = mapOf(WorkflowOption.DURATION to OptionConstraint(minimum = 5, maximum = 15),
        WorkflowOption.RESOLUTION to OptionConstraint(listOf("2K")),
        WorkflowOption.ASPECT_RATIO to OptionConstraint(listOf("auto", "adaptive", "21:9", "16:9", "4:3", "1:1", "3:4", "9:16")))
    private val pixTextOptions = setOf(WorkflowOption.SEED, WorkflowOption.DURATION, WorkflowOption.RESOLUTION,
        WorkflowOption.ASPECT_RATIO, WorkflowOption.GENERATE_AUDIO, WorkflowOption.NEGATIVE_PROMPT)
    private val pixImageOptions = pixTextOptions - WorkflowOption.ASPECT_RATIO
    private val pixTextChoices = mapOf(WorkflowOption.SEED to OptionConstraint(minimum = 0, maximum = Int.MAX_VALUE),
        WorkflowOption.DURATION to OptionConstraint(minimum = 1, maximum = 15),
        WorkflowOption.RESOLUTION to OptionConstraint(listOf("360p", "540p", "720p", "1080p")),
        WorkflowOption.ASPECT_RATIO to OptionConstraint(listOf("16:9", "4:3", "1:1", "3:4", "9:16")))
    private val pixImageChoices = pixTextChoices - WorkflowOption.ASPECT_RATIO
    private val cfgChoices = OptionConstraint((0..10).map { "${it / 10}.${it % 10}" })
    private val klingImageOptions = setOf(WorkflowOption.DURATION, WorkflowOption.SOUND, WorkflowOption.CFG_SCALE)
    private val klingImageChoices = mapOf(WorkflowOption.DURATION to OptionConstraint(minimum = 3, maximum = 15),
        WorkflowOption.SOUND to OptionConstraint(listOf("on", "off")), WorkflowOption.CFG_SCALE to cfgChoices)

    val grok = route("grok-image-2", "Grok Imagine 2.0", WorkflowFamily.GROK, MediaKind.IMAGE,
        "xai/grok-imagine-image-2.0", setOf(WorkflowCapability.TEXT_TO_IMAGE, WorkflowCapability.IMAGE_TO_IMAGE, WorkflowCapability.REFERENCE_IMAGE),
        listOf(MediaRequirement(MediaRole.REFERENCE, MediaKind.IMAGE, 0, null)),
        setOf(WorkflowOption.QUALITY, WorkflowOption.RESOLUTION, WorkflowOption.ASPECT_RATIO),
        mapOf(WorkflowOption.QUALITY to OptionConstraint(listOf("low", "medium")),
            WorkflowOption.RESOLUTION to OptionConstraint(listOf("1k", "2k")),
            WorkflowOption.ASPECT_RATIO to OptionConstraint(listOf("auto", "1:1", "1:2", "2:1", "3:2", "2:3", "4:3", "3:4", "16:9", "9:16"))))

    val soulV2Edit = route("soul-v2-image-edit", "SOUL V2 Image to Image", WorkflowFamily.SOUL, MediaKind.IMAGE,
        "higgsfield-ai/soul/v2/image-to-image", setOf(WorkflowCapability.IMAGE_TO_IMAGE),
        listOf(MediaRequirement(MediaRole.SOURCE, MediaKind.IMAGE, 1, 1)),
        setOf(WorkflowOption.SEED, WorkflowOption.RESOLUTION, WorkflowOption.ASPECT_RATIO),
        mapOf(WorkflowOption.SEED to OptionConstraint(minimum = 1, maximum = 1000000),
            WorkflowOption.RESOLUTION to OptionConstraint(listOf("720p", "1080p")),
            WorkflowOption.ASPECT_RATIO to OptionConstraint(listOf("9:16", "16:9", "4:3", "3:4", "1:1", "2:3", "3:2"))))

    val ideogram = route("ideogram-4", "Ideogram 4.0", WorkflowFamily.IDEOGRAM, MediaKind.IMAGE,
        "ideogram/v4.0", setOf(WorkflowCapability.TEXT_TO_IMAGE, WorkflowCapability.IMAGE_TO_IMAGE),
        listOf(MediaRequirement(MediaRole.SOURCE, MediaKind.IMAGE, 0, 1)),
        setOf(WorkflowOption.ASPECT_RATIO, WorkflowOption.IMAGE_WEIGHT, WorkflowOption.RENDERING_SPEED),
        mapOf(WorkflowOption.ASPECT_RATIO to OptionConstraint(listOf("1:1", "1:2", "2:1", "2:3", "3:2", "4:5", "5:4", "9:16", "16:9", "5:8", "8:5", "3:4", "4:3", "9:22", "22:9", "9:23", "23:9", "3:8", "8:3", "5:12", "12:5", "1:3", "3:1")),
            WorkflowOption.IMAGE_WEIGHT to OptionConstraint(minimum = 1, maximum = 100),
            WorkflowOption.RENDERING_SPEED to OptionConstraint(listOf("TURBO", "DEFAULT", "QUALITY"))))

    val recraft = route("recraft-4-1", "Recraft V4.1 Text to Image", WorkflowFamily.RECRAFT, MediaKind.IMAGE,
        "recraft/v4.1/text-to-image", setOf(WorkflowCapability.TEXT_TO_IMAGE),
        options = setOf(WorkflowOption.RESOLUTION, WorkflowOption.ASPECT_RATIO, WorkflowOption.OUTPUT_FORMAT),
        constraints = mapOf(WorkflowOption.RESOLUTION to OptionConstraint(listOf("1k")),
            WorkflowOption.ASPECT_RATIO to OptionConstraint(listOf("1:1", "2:1", "1:2", "3:2", "2:3", "4:3", "3:4", "5:4", "4:5", "6:10", "14:10", "10:14", "16:9", "9:16")),
            WorkflowOption.OUTPUT_FORMAT to OptionConstraint(listOf("jpg", "png", "webp"))))
    val recraftPro = recraft.copy(id = WorkflowId("recraft-4-1-pro"), displayName = "Recraft V4.1 Pro Text to Image",
        endpointPath = "recraft/v4.1/pro/text-to-image", documentationUrl = "${DOCS}recraft/v4.1/pro/text-to-image/playground",
        optionConstraints = recraft.optionConstraints + (WorkflowOption.RESOLUTION to OptionConstraint(listOf("2k"))))
    val recraftUtility = recraft.copy(id = WorkflowId("recraft-4-1-utility"), displayName = "Recraft V4.1 Utility Text to Image",
        endpointPath = "recraft/v4.1/utility/text-to-image", documentationUrl = "${DOCS}recraft/v4.1/utility/text-to-image/api-reference")
    val recraftUtilityPro = recraft.copy(id = WorkflowId("recraft-4-1-utility-pro"), displayName = "Recraft V4.1 Utility Pro Text to Image",
        endpointPath = "recraft/v4.1/utility/pro/text-to-image", documentationUrl = "${DOCS}recraft/v4.1/utility/pro/text-to-image/playground",
        optionConstraints = recraft.optionConstraints + (WorkflowOption.RESOLUTION to OptionConstraint(listOf("2k"))))

    val genjutsuMotion = route("genjutsu-motion-transfer", "Genjutsu Motion Transfer", WorkflowFamily.GENJUTSU, MediaKind.VIDEO,
        "higgsfield/genjutsu/motion-transfer/v1.0", setOf(WorkflowCapability.REFERENCE_IMAGE),
        listOf(MediaRequirement(MediaRole.SOURCE, MediaKind.VIDEO, 1, 1), MediaRequirement(MediaRole.REFERENCE, MediaKind.IMAGE, 1, 8)),
        setOf(WorkflowOption.RESOLUTION), mapOf(WorkflowOption.RESOLUTION to OptionConstraint(listOf("480p", "720p", "1080p"))), promptRequired = false)
    val genjutsuSwap = route("genjutsu-object-swap", "Genjutsu Object Swap", WorkflowFamily.GENJUTSU, MediaKind.VIDEO,
        "higgsfield/genjutsu/object-swap/v1.0", setOf(WorkflowCapability.REFERENCE_IMAGE),
        listOf(MediaRequirement(MediaRole.SOURCE, MediaKind.VIDEO, 1, 1), MediaRequirement(MediaRole.REFERENCE, MediaKind.IMAGE, 1, 8)),
        setOf(WorkflowOption.RESOLUTION), mapOf(WorkflowOption.RESOLUTION to OptionConstraint(listOf("480p", "720p", "1080p"))), promptRequired = false)

    val seedance2Image = route("seedance-2-image", "Seedance 2.0 Image to Video", WorkflowFamily.SEEDANCE, MediaKind.VIDEO,
        "bytedance/seedance-2.0/image-to-video", setOf(WorkflowCapability.IMAGE_TO_VIDEO), frame,
        setOf(WorkflowOption.DURATION, WorkflowOption.RESOLUTION, WorkflowOption.GENERATE_AUDIO),
        mapOf(WorkflowOption.DURATION to OptionConstraint(minimum = 4, maximum = 15),
            WorkflowOption.RESOLUTION to OptionConstraint(listOf("480p", "720p", "1080p", "4k"))), promptRequired = false)
    val seedance2_5Image = route("seedance-2-5-image", "Seedance 2.5 Image to Video", WorkflowFamily.SEEDANCE, MediaKind.VIDEO,
        "bytedance/seedance-2.5/image-to-video", setOf(WorkflowCapability.IMAGE_TO_VIDEO), frame,
        setOf(WorkflowOption.DURATION, WorkflowOption.RESOLUTION, WorkflowOption.GENERATE_AUDIO, WorkflowOption.OUTPUT_FORMAT),
        mapOf(WorkflowOption.DURATION to OptionConstraint(minimum = 4, maximum = 30),
            WorkflowOption.RESOLUTION to OptionConstraint(listOf("480p", "720p", "1080p")),
            WorkflowOption.OUTPUT_FORMAT to OptionConstraint(listOf("mp4", "mov"))), promptRequired = false)
    val seedance2_5Edit = seedance2_5VideoOperation("seedance-2-5-video-edit", "Seedance 2.5 Video Edit", "bytedance/seedance-2.5/video-edit", false)
    val seedance2_5Extend = seedance2_5VideoOperation("seedance-2-5-video-extend", "Seedance 2.5 Video Extend", "bytedance/seedance-2.5/video-extend", true)

    val happyReference = route("happy-horse-reference", "Happy Horse 1.0 Reference to Video", WorkflowFamily.HAPPY_HORSE, MediaKind.VIDEO,
        "alibaba/happy-horse/reference-to-video", setOf(WorkflowCapability.REFERENCE_IMAGE),
        listOf(MediaRequirement(MediaRole.REFERENCE, MediaKind.IMAGE, 1, null)),
        setOf(WorkflowOption.DURATION, WorkflowOption.RESOLUTION, WorkflowOption.SEED),
        mapOf(WorkflowOption.DURATION to OptionConstraint(minimum = 2, maximum = 15),
            WorkflowOption.RESOLUTION to OptionConstraint(listOf("720p", "1080p")),
            WorkflowOption.SEED to OptionConstraint(minimum = 1, maximum = 2147483646)))
    val happyImage = route("happy-horse-image", "Happy Horse 1.0 Image to Video", WorkflowFamily.HAPPY_HORSE, MediaKind.VIDEO,
        "alibaba/happy-horse/image-to-video", setOf(WorkflowCapability.IMAGE_TO_VIDEO), listOf(frame.first()),
        setOf(WorkflowOption.DURATION, WorkflowOption.RESOLUTION),
        mapOf(WorkflowOption.DURATION to OptionConstraint(minimum = 2, maximum = 15),
            WorkflowOption.RESOLUTION to OptionConstraint(listOf("720p", "1080p"))), promptRequired = false)

    val kling2_5Image = route("kling-2-5-turbo-pro-image", "Kling 2.5 Turbo Pro Image to Video", WorkflowFamily.KLING, MediaKind.VIDEO,
        "kling-video/v2.5-turbo/pro/image-to-video", setOf(WorkflowCapability.IMAGE_TO_VIDEO, WorkflowCapability.NEGATIVE_PROMPT),
        listOf(frame.first()), setOf(WorkflowOption.DURATION, WorkflowOption.CFG_SCALE, WorkflowOption.NEGATIVE_PROMPT),
        mapOf(WorkflowOption.DURATION to OptionConstraint(listOf("5", "10")), WorkflowOption.CFG_SCALE to cfgChoices))
    val kling2_5StandardImage = kling2_5Image.copy(id = WorkflowId("kling-2-5-turbo-standard-image"),
        displayName = "Kling 2.5 Turbo Standard Image to Video", endpointPath = "kling-video/v2.5-turbo/standard/image-to-video",
        documentationUrl = "${DOCS}kling-video/v2.5-turbo/standard/image-to-video/playground")
    val kling2_6Image = route("kling-2-6-pro-image", "Kling 2.6 Pro Image to Video", WorkflowFamily.KLING, MediaKind.VIDEO,
        "kling-video/v2.6/pro/image-to-video", setOf(WorkflowCapability.IMAGE_TO_VIDEO), listOf(frame.first()),
        klingImageOptions + WorkflowOption.ASPECT_RATIO,
        klingImageChoices + (WorkflowOption.DURATION to OptionConstraint(listOf("5", "10"))) +
            (WorkflowOption.ASPECT_RATIO to OptionConstraint(listOf("16:9", "9:16", "1:1"))))
    val kling3StandardImage = kling3Image("kling-3-std-image", "Kling 3.0 Standard Image to Video", "kling-video/v3.0/std/image-to-video")
    val kling3ProImage = kling3Image("kling-3-pro-image", "Kling 3.0 Pro Image to Video", "kling-video/v3.0/pro/image-to-video")
    val kling3_4kImage = kling3Image("kling-3-4k-image", "Kling 3.0 4K Image to Video", "kling-video/v3.0/4k/image-to-video")
    val kling3StandardText = kling3Text("kling-3-std-text", "Kling 3.0 Standard Text to Video", "kling-video/v3.0/std/text-to-video")
    val kling3_4kText = kling3Text("kling-3-4k-text", "Kling 3.0 4K Text to Video", "kling-video/v3.0/4k/text-to-video")
    val kling3TurboText = route("kling-3-turbo-text", "Kling 3.0 Turbo Text to Video", WorkflowFamily.KLING, MediaKind.VIDEO,
        "kling-video/v3.0-turbo/text-to-video", setOf(WorkflowCapability.TEXT_TO_VIDEO),
        options = setOf(WorkflowOption.DURATION, WorkflowOption.RESOLUTION, WorkflowOption.ASPECT_RATIO),
        constraints = mapOf(WorkflowOption.DURATION to OptionConstraint(minimum = 3, maximum = 15),
            WorkflowOption.RESOLUTION to OptionConstraint(listOf("720p", "1080p")),
            WorkflowOption.ASPECT_RATIO to OptionConstraint(listOf("16:9", "9:16", "1:1"))))
    val kling3TurboImage = route("kling-3-turbo-image", "Kling 3.0 Turbo Image to Video", WorkflowFamily.KLING, MediaKind.VIDEO,
        "kling-video/v3.0-turbo/image-to-video", setOf(WorkflowCapability.IMAGE_TO_VIDEO), listOf(frame.first()),
        setOf(WorkflowOption.DURATION, WorkflowOption.RESOLUTION),
        mapOf(WorkflowOption.DURATION to OptionConstraint(minimum = 3, maximum = 15),
            WorkflowOption.RESOLUTION to OptionConstraint(listOf("720p", "1080p"))))
    val kling2_6MotionStandard = klingMotionStandard("kling-2-6-motion-std", "Kling 2.6 Motion Control Standard", "kling-video/motion-control/std")
    val kling3MotionStandard = klingMotionStandard("kling-3-motion-std", "Kling 3.0 Motion Control Standard", "kling-video/v3/motion-control/std")
    val klingO3Frames = klingFrames("kling-o3-frames", "Kling O3 First and Last Frame", "kling-video/o3/first-last-frame", true)
    val klingOmniFrames = klingFrames("kling-omni-frames", "Kling Omni First and Last Frame", "kling-video/omni/first-last-frame", false)
    val klingO3Edit = klingVideoArray("kling-o3-edit", "Kling O3 Video Edit", "kling-video/o3/video-edit", true, false)
    val klingOmniEdit = klingVideoArray("kling-omni-edit", "Kling Omni Video Edit", "kling-video/omni/video-edit", false, false)
    val klingO3VideoReference = klingVideoArray("kling-o3-video-reference", "Kling O3 Video Reference", "kling-video/o3/video-reference", true, true)
    val klingOmniVideoReference = klingVideoArray("kling-omni-video-reference", "Kling Omni Video Reference", "kling-video/omni/video-reference", false, true)

    val miniText = route("minimax-h3-text", "MiniMax H3 Text to Video", WorkflowFamily.MINIMAX, MediaKind.VIDEO,
        "minimax/h3/text-to-video", setOf(WorkflowCapability.TEXT_TO_VIDEO), options = miniOptions, constraints = miniChoices)
    val miniImage = route("minimax-h3-image", "MiniMax H3 Image to Video", WorkflowFamily.MINIMAX, MediaKind.VIDEO,
        "minimax/h3/image-to-video", setOf(WorkflowCapability.IMAGE_TO_VIDEO), frame, miniOptions, miniChoices)
    val miniReference = route("minimax-h3-reference", "MiniMax H3 Reference to Video", WorkflowFamily.MINIMAX, MediaKind.VIDEO,
        "minimax/h3/reference-to-video", setOf(WorkflowCapability.REFERENCE_IMAGE),
        listOf(MediaRequirement(MediaRole.REFERENCE, MediaKind.IMAGE, 0, null),
            MediaRequirement(MediaRole.VIDEO_REFERENCE, MediaKind.VIDEO, 0, null),
            MediaRequirement(MediaRole.AUDIO, MediaKind.AUDIO, 0, null)), miniOptions, miniChoices)

    val pixText = route("pixverse-v6-text", "PixVerse V6 Text to Video", WorkflowFamily.PIXVERSE, MediaKind.VIDEO,
        "pixverse/v6/text-to-video", setOf(WorkflowCapability.TEXT_TO_VIDEO, WorkflowCapability.NEGATIVE_PROMPT),
        options = pixTextOptions, constraints = pixTextChoices)
    val pixImage = route("pixverse-v6-image", "PixVerse V6 Image to Video", WorkflowFamily.PIXVERSE, MediaKind.VIDEO,
        "pixverse/v6/image-to-video", setOf(WorkflowCapability.IMAGE_TO_VIDEO, WorkflowCapability.NEGATIVE_PROMPT),
        frame, pixImageOptions, pixImageChoices)

    val wan3Image = wanImage("wan-3-image", "Wan 3.0 Image to Video", "alibaba/wan-3.0/image-to-video")
    val wan3Reference = wanReference("wan-3-reference", "Wan 3.0 Reference to Video", "alibaba/wan-3.0/reference-to-video", null)
    val wan3PrimeImage = wanImage("wan-3-prime-image", "Wan 3.0 Prime Image to Video", "alibaba/wan-3.0-prime/image-to-video", "Prime")
    val wan3PrimeReference = wanReference("wan-3-prime-reference", "Wan 3.0 Prime Reference to Video", "alibaba/wan-3.0-prime/reference-to-video", "Prime")
    val wan2_6Reference = route("wan-2-6-reference", "Wan 2.6 Reference to Video", WorkflowFamily.WAN, MediaKind.VIDEO,
        "wan/v2.6/reference-to-video", setOf(WorkflowCapability.REFERENCE_IMAGE),
        listOf(MediaRequirement(MediaRole.VIDEO_REFERENCE, MediaKind.VIDEO, 1, null)),
        setOf(WorkflowOption.DURATION, WorkflowOption.RESOLUTION, WorkflowOption.ASPECT_RATIO),
        mapOf(WorkflowOption.DURATION to OptionConstraint(listOf("5", "10")),
            WorkflowOption.RESOLUTION to OptionConstraint(listOf("720p", "1080p")),
            WorkflowOption.ASPECT_RATIO to OptionConstraint(listOf("16:9", "9:16", "1:1", "4:3", "3:4"))))
    val wan2_6Image = route("wan-2-6-image", "Wan 2.6 Image to Video", WorkflowFamily.WAN, MediaKind.VIDEO,
        "wan/v2.6/image-to-video", setOf(WorkflowCapability.IMAGE_TO_VIDEO, WorkflowCapability.NEGATIVE_PROMPT), listOf(frame.first()),
        setOf(WorkflowOption.DURATION, WorkflowOption.RESOLUTION, WorkflowOption.PROMPT_EXTEND, WorkflowOption.NEGATIVE_PROMPT),
        mapOf(WorkflowOption.DURATION to OptionConstraint(listOf("5", "10", "15")),
            WorkflowOption.RESOLUTION to OptionConstraint(listOf("480p", "720p", "1080p"))))
    val wan2_7Image = route("wan-2-7-image", "Wan 2.7 Image to Video", WorkflowFamily.WAN, MediaKind.VIDEO,
        "wan/v2.7/image-to-video", setOf(WorkflowCapability.IMAGE_TO_VIDEO, WorkflowCapability.NEGATIVE_PROMPT),
        frame + MediaRequirement(MediaRole.AUDIO, MediaKind.AUDIO, 0, 1),
        setOf(WorkflowOption.SEED, WorkflowOption.DURATION, WorkflowOption.RESOLUTION, WorkflowOption.PROMPT_EXTEND, WorkflowOption.NEGATIVE_PROMPT),
        mapOf(WorkflowOption.SEED to OptionConstraint(minimum = 1, maximum = 2147483646),
            WorkflowOption.DURATION to OptionConstraint(minimum = 2, maximum = 15),
            WorkflowOption.RESOLUTION to OptionConstraint(listOf("720p", "1080p"))), promptRequired = false)

    val all = listOf(soulV2Edit, grok, ideogram, recraft, recraftPro, recraftUtility, recraftUtilityPro,
        genjutsuMotion, genjutsuSwap, seedance2Image, seedance2_5Image, seedance2_5Edit, seedance2_5Extend, happyImage, happyReference,
        kling2_5Image, kling2_5StandardImage, kling2_6Image, kling3StandardText, kling3StandardImage,
        kling3ProImage, kling3_4kText, kling3_4kImage, kling3TurboText, kling3TurboImage,
        kling2_6MotionStandard, kling3MotionStandard, klingO3Frames, klingOmniFrames,
        klingO3Edit, klingOmniEdit, klingO3VideoReference, klingOmniVideoReference,
        miniText, miniImage, miniReference,
        pixText, pixImage, wan2_6Image, wan2_6Reference, wan2_7Image, wan3Image, wan3Reference, wan3PrimeImage, wan3PrimeReference)


    private fun wanImage(id: String, name: String, endpoint: String, tier: String? = null) =
        route(id, name, WorkflowFamily.WAN, MediaKind.VIDEO, endpoint, setOf(WorkflowCapability.IMAGE_TO_VIDEO),
            frame, imageVideoOptions, wanChoices, tier)

    private fun kling3Image(id: String, name: String, endpoint: String) =
        route(id, name, WorkflowFamily.KLING, MediaKind.VIDEO, endpoint, setOf(WorkflowCapability.IMAGE_TO_VIDEO),
            frame, klingImageOptions, klingImageChoices, promptRequired = false)

    private fun kling3Text(id: String, name: String, endpoint: String) =
        route(id, name, WorkflowFamily.KLING, MediaKind.VIDEO, endpoint, setOf(WorkflowCapability.TEXT_TO_VIDEO),
            options = klingImageOptions + WorkflowOption.ASPECT_RATIO,
            constraints = klingImageChoices + (WorkflowOption.ASPECT_RATIO to OptionConstraint(listOf("16:9", "9:16", "1:1"))))

    private fun klingMotionStandard(id: String, name: String, endpoint: String) =
        route(id, name, WorkflowFamily.KLING, MediaKind.VIDEO, endpoint, setOf(WorkflowCapability.IMAGE_TO_VIDEO),
            listOf(MediaRequirement(MediaRole.SOURCE, MediaKind.IMAGE, 1, 1),
                MediaRequirement(MediaRole.MOTION_REFERENCE, MediaKind.VIDEO, 1, 1)),
            setOf(WorkflowOption.KEEP_ORIGINAL_SOUND, WorkflowOption.CHARACTER_ORIENTATION),
            mapOf(WorkflowOption.KEEP_ORIGINAL_SOUND to OptionConstraint(listOf("yes", "no")),
                WorkflowOption.CHARACTER_ORIENTATION to OptionConstraint(listOf("video", "image"))), promptRequired = false)

    private fun klingFrames(id: String, name: String, endpoint: String, isO3: Boolean) =
        route(id, name, WorkflowFamily.KLING, MediaKind.VIDEO, endpoint, setOf(WorkflowCapability.IMAGE_TO_VIDEO),
            listOf(MediaRequirement(MediaRole.START_FRAME, MediaKind.IMAGE, 1, 1),
                MediaRequirement(MediaRole.END_FRAME, MediaKind.IMAGE, 0, 1)),
            setOf(WorkflowOption.MODE, WorkflowOption.DURATION, WorkflowOption.ASPECT_RATIO) +
                if (isO3) setOf(WorkflowOption.SOUND) else emptySet(),
            mapOf(WorkflowOption.MODE to OptionConstraint(if (isO3) listOf("std", "pro", "4k") else listOf("std", "pro")),
                WorkflowOption.DURATION to OptionConstraint(minimum = if (isO3) 3 else 5, maximum = if (isO3) 15 else 10),
                WorkflowOption.ASPECT_RATIO to OptionConstraint(listOf("16:9", "9:16", "1:1")),
                WorkflowOption.SOUND to OptionConstraint(listOf("on", "off"))), promptRequired = !isO3)

    private fun klingVideoArray(id: String, name: String, endpoint: String, isO3: Boolean, isReference: Boolean) =
        route(id, name, WorkflowFamily.KLING, MediaKind.VIDEO, endpoint, setOf(WorkflowCapability.REFERENCE_IMAGE),
            listOf(MediaRequirement(if (isReference) MediaRole.VIDEO_REFERENCE else MediaRole.SOURCE, MediaKind.VIDEO, 1, 1),
                MediaRequirement(MediaRole.REFERENCE, MediaKind.IMAGE, 0, null)),
            setOf(WorkflowOption.MODE) + if (isReference) setOf(WorkflowOption.DURATION, WorkflowOption.ASPECT_RATIO) else emptySet(),
            mapOf(WorkflowOption.MODE to OptionConstraint(if (isO3 && !isReference) listOf("std", "pro", "4k") else listOf("std", "pro")),
                WorkflowOption.DURATION to OptionConstraint(minimum = 3, maximum = 10),
                WorkflowOption.ASPECT_RATIO to OptionConstraint(listOf("16:9", "9:16", "1:1"))))

    private fun seedance2_5VideoOperation(id: String, name: String, endpoint: String, hasDuration: Boolean) =
        route(id, name, WorkflowFamily.SEEDANCE, MediaKind.VIDEO, endpoint, setOf(WorkflowCapability.REFERENCE_IMAGE),
            listOf(MediaRequirement(MediaRole.SOURCE, MediaKind.VIDEO, 1, 1),
                MediaRequirement(MediaRole.REFERENCE, MediaKind.IMAGE, 0, null),
                MediaRequirement(MediaRole.VIDEO_REFERENCE, MediaKind.VIDEO, 0, null),
                MediaRequirement(MediaRole.AUDIO, MediaKind.AUDIO, 0, null)),
            setOf(WorkflowOption.RESOLUTION, WorkflowOption.OUTPUT_FORMAT, WorkflowOption.GENERATE_AUDIO) +
                if (hasDuration) setOf(WorkflowOption.DURATION) else emptySet(),
            mapOf(WorkflowOption.RESOLUTION to OptionConstraint(listOf("480p", "720p", "1080p")),
                WorkflowOption.OUTPUT_FORMAT to OptionConstraint(listOf("mp4", "mov"))) +
                if (hasDuration) mapOf(WorkflowOption.DURATION to OptionConstraint(minimum = 4, maximum = 30)) else emptyMap())

    private fun wanReference(id: String, name: String, endpoint: String, tier: String?) =
        route(id, name, WorkflowFamily.WAN, MediaKind.VIDEO, endpoint, setOf(WorkflowCapability.REFERENCE_IMAGE),
            listOf(MediaRequirement(MediaRole.REFERENCE, MediaKind.IMAGE, 0, if (tier == "Prime") 10 else null),
                MediaRequirement(MediaRole.VIDEO_REFERENCE, MediaKind.VIDEO, 0, if (tier == "Prime") 10 else 5),
                MediaRequirement(MediaRole.AUDIO, MediaKind.AUDIO, 0, 5)), imageVideoOptions, wanChoices, tier,
            maximumCombinedReferences = if (tier == "Prime") 10 else null)

    private fun route(
        id: String, name: String, family: WorkflowFamily, kind: MediaKind, endpoint: String,
        capabilities: Set<WorkflowCapability>, requirements: List<MediaRequirement> = emptyList(),
        options: Set<WorkflowOption> = emptySet(), constraints: Map<WorkflowOption, OptionConstraint> = emptyMap(),
        tier: String? = null, maximumCombinedReferences: Int? = null, promptRequired: Boolean = true,
    ) = WorkflowDescriptor(
        id = WorkflowId(id), displayName = name, family = family, mediaKind = kind, tier = tier,
        capabilities = capabilities, mediaRequirements = requirements, endpointPath = endpoint,
        supportedOptions = options, optionConstraints = constraints,
        maximumCombinedReferences = maximumCombinedReferences,
        promptRequired = promptRequired,
        staticEstimate = StaticEstimateMetadata(fromPrice = ModelStartingRates.priceFor(WorkflowId(id)), sourceLabel = "Check the current Higgsfield API pricing", sourceUrl = "$DOCS$endpoint/api-reference", verifiedOn = VERIFIED),
        documentationUrl = "$DOCS$endpoint/api-reference", schemaVerifiedOn = VERIFIED, isSubmissionEnabled = true,
    )
}
