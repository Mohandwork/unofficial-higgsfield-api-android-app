package com.higgsfield.mobile.core.model

/** Manually maintained starting-price labels shown in Model Details. These are display-only. */
object ModelStartingRates {
    private val byId = mapOf(
        "soul" to "\$0.0938/image",
        "soul-v2" to "\$0.0032/image",
        "soul-cinema" to "Pricing varies",
        "marketing-studio-2-alpha" to "\$0.0107/image",
        "marketing-studio-2-5-flare" to "\$0.0107/image",
        "marketing-studio-2-5-sunburst" to "\$0.0107/image",
        "qwen-image-3" to "\$0.04/image",
        "qwen-image-3-edit" to "\$0.04/image",
        "seedance-2" to "\$0.0985/s",
        "seedance-2-5" to "\$0.144/s",
        "kling-2-5-turbo" to "\$0.0231/s",
        "kling-2-6" to "\$0.0385/s",
        "kling-2-6-motion" to "\$0.0385/s",
        "kling-3" to "\$0.0462/s",
        "kling-3-motion" to "\$0.0462/s",
        "kling-o3" to "\$0.0462/s",
        "kling-omni" to "\$0.0462/s",
        "cinema-studio-4" to "\$0.2057/s",
        "wan-2-6" to "\$0.05/s",
        "wan-2-7" to "\$0.05/s",
        "wan-3" to "\$0.025/s",
        "wan-3-prime" to "\$0.0476/s",
    )

    fun priceFor(id: WorkflowId): String = byId[id.value] ?: "Pricing unavailable"
}
