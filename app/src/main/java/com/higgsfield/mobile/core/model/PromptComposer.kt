package com.higgsfield.mobile.core.model

object PromptComposer {
    fun compose(brief: CreativeBrief, instruction: String): String = buildList {
        addField("Subject", brief.subject)
        addField("Style", brief.style)
        addField("Mood", brief.mood)
        addField("Camera", brief.cameraDirection)
        addField("Requirements", brief.requirements)
        addField("Output goal", brief.outputGoal)
        if (instruction.isNotBlank()) add(instruction.trim())
    }.joinToString(separator = "\n")

    private fun MutableList<String>.addField(label: String, value: String) {
        if (value.isNotBlank()) add("$label: ${value.trim()}")
    }
}
