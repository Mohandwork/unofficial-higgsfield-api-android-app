package com.higgsfield.mobile.core.model

/** Manually maintained starting-price labels shown in Model Details. These are display-only. */
object ModelStartingRates {
    private const val UNVERIFIED = "Pricing unavailable"

    // Edit one rate per main model. Route variants inherit it unless a verified route override is added.
    private val byModelId = mapOf(
        "soul" to "\$0.0057/image",
        "soul-v2" to "\$0.1875/image",
        "soul-cinema" to "\$0.1875/image",
        "marketing-studio-2-alpha" to "\$0.6136/image",
        "marketing-studio-2-5-flare" to "\$0.6136/image",
        "marketing-studio-2-5-sunburst" to "\$0.6136/image",
        "qwen-image-3" to "\$0.075/image",
        "qwen-image-3-edit" to "\$0.075/image",
        "z-image-turbo" to "\$0.015/image",
        "grok-image-2" to "\$0.08/image",
        "ideogram-4" to "\$0.03/image",
        "recraft-4-1" to "\$0.035 - 0.21/image",
        "seedance-2" to "\$0.1196/s",
        "seedance-2-5" to "\$1.0/s",
        "happy-horse-1" to "\$0.153/s",
        "genjutsu" to "\$1.3872/s",
        "kling-2-5-turbo" to "\$0.0595/s",
        "kling-2-6" to "\$0.0595/s",
        "kling-2-6-motion" to "\$0.0952/s",
        "kling-3" to "\$0.357/s",
        "kling-3-motion" to "\$0.1428/s",
        "kling-o3" to "\$0.1071/s",
        "kling-omni" to "\$0.1071/s",
        "cinema-studio-4" to "\$0.4623/s",
        "wan-2-6" to "\$0.1275/s",
        "wan-2-7" to "\$0.1275/s",
        "wan-3" to "\$0.17/s",
        "wan-3-prime" to "\$0.238/s",
        "minimax-h3" to "\$0.1105/s",
        "pixverse-v6" to "\$0.0978/s",
    )

    // Use this only when a route has a confirmed price that differs from its main model.
    private val byRouteId: Map<String, String> = emptyMap()

    private val modelIdsBySpecificity = byModelId.keys.sortedByDescending { it.length }

    fun priceFor(id: WorkflowId): String {
        byRouteId[id.value]?.let { return it }
        return byModelId[modelIdFor(id)] ?: UNVERIFIED
    }

    internal fun modelIdFor(id: WorkflowId): String? =
        when (id.value) {
            "happy-horse-image", "happy-horse-reference" -> "happy-horse-1"
            else -> modelIdsBySpecificity.firstOrNull { id.value == it || id.value.startsWith("$it-") }
        }
}
