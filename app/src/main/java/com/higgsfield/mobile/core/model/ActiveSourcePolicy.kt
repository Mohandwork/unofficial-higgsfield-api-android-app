package com.higgsfield.mobile.core.model

sealed interface ActiveSourceDecision {
    data object KeepCurrent : ActiveSourceDecision
    data class Replace(val outputId: String) : ActiveSourceDecision
    data object RequireSelection : ActiveSourceDecision
}

object ActiveSourcePolicy {
    fun after(status: GenerationStatus): ActiveSourceDecision {
        if (status !is GenerationStatus.Completed) return ActiveSourceDecision.KeepCurrent
        val images = status.outputs.filter { it.kind == MediaKind.IMAGE }
        return when (images.size) {
            0 -> ActiveSourceDecision.KeepCurrent
            1 -> ActiveSourceDecision.Replace(images.single().id)
            else -> ActiveSourceDecision.RequireSelection
        }
    }
}
