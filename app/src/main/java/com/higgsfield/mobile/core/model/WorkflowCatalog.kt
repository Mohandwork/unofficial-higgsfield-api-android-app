package com.higgsfield.mobile.core.model

data class WorkflowKey(val id: WorkflowId, val displayName: String)

/** The single catalog of local workflow identities and user-visible model names. */
object WorkflowCatalog {
    const val DOCUMENTATION_URL = "https://docs.higgsfield.ai/docs/models"

    val SOUL = WorkflowKey(WorkflowId("soul"), "SOUL")
    val SOUL_V2 = WorkflowKey(WorkflowId("soul-v2"), "SOUL V2")
    val SOUL_CINEMA = WorkflowKey(WorkflowId("soul-cinema"), "SOUL Cinema")
    val MARKETING_STUDIO_2_ALPHA = WorkflowKey(WorkflowId("marketing-studio-2-alpha"), "Marketing Studio 2.0 Alpha")
    val MARKETING_STUDIO_2_5_FLARE = WorkflowKey(WorkflowId("marketing-studio-2-5-flare"), "Marketing Studio 2.5 Flare")
    val MARKETING_STUDIO_2_5_SUNBURST = WorkflowKey(WorkflowId("marketing-studio-2-5-sunburst"), "Marketing Studio 2.5 Sunburst")
    val QWEN_IMAGE_3 = WorkflowKey(WorkflowId("qwen-image-3"), "Qwen Image 3")
    val QWEN_IMAGE_3_EDIT = WorkflowKey(WorkflowId("qwen-image-3-edit"), "Qwen Image 3 Edit")
    val SEEDANCE_2 = WorkflowKey(WorkflowId("seedance-2"), "Seedance 2.0 Text to Video")
    val SEEDANCE_2_5 = WorkflowKey(WorkflowId("seedance-2-5"), "Seedance 2.5 Text to Video")
    val SEEDANCE_2_REFERENCE = WorkflowKey(WorkflowId("seedance-2-reference"), "Seedance 2.0 Reference to Video")
    val SEEDANCE_2_5_REFERENCE = WorkflowKey(WorkflowId("seedance-2-5-reference"), "Seedance 2.5 Reference to Video")
    val HAPPY_HORSE_1 = WorkflowKey(WorkflowId("happy-horse-1"), "Happy Horse 1.0 Text to Video")
    val Z_IMAGE_TURBO = WorkflowKey(WorkflowId("z-image-turbo"), "Z-Image Turbo")
    val KLING_2_5_TURBO = WorkflowKey(WorkflowId("kling-2-5-turbo"), "Kling 2.5 Turbo")
    val KLING_2_6 = WorkflowKey(WorkflowId("kling-2-6"), "Kling 2.6")
    val KLING_2_6_MOTION = WorkflowKey(WorkflowId("kling-2-6-motion"), "Kling 2.6 Motion Control")
    val KLING_3 = WorkflowKey(WorkflowId("kling-3"), "Kling 3.0")
    val KLING_3_MOTION = WorkflowKey(WorkflowId("kling-3-motion"), "Kling 3.0 Motion Control")
    val KLING_O3 = WorkflowKey(WorkflowId("kling-o3"), "Kling O3")
    val KLING_OMNI = WorkflowKey(WorkflowId("kling-omni"), "Kling Omni")
    val CINEMA_STUDIO_4 = WorkflowKey(WorkflowId("cinema-studio-4"), "Cinema Studio 4.0")
    val WAN_2_6 = WorkflowKey(WorkflowId("wan-2-6"), "Wan 2.6 Text to Video")
    val WAN_2_7 = WorkflowKey(WorkflowId("wan-2-7"), "Wan 2.7 Text to Video")
    val WAN_3 = WorkflowKey(WorkflowId("wan-3"), "Wan 3.0 Text to Video")
    val WAN_3_PRIME = WorkflowKey(WorkflowId("wan-3-prime"), "Wan 3.0 Prime Text to Video")
}
