package com.higgsfield.mobile.core.model

object PromptComposer {
    fun compose(brief: CreativeBrief, instruction: String): String = buildList {
        addField(SUBJECT_LABEL, brief.subject)
        addField(STYLE_LABEL, brief.style)
        addField(MOOD_LABEL, brief.mood)
        addField(CAMERA_LABEL, brief.cameraDirection)
        addField(REQUIREMENTS_LABEL, brief.requirements)
        addField(OUTPUT_GOAL_LABEL, brief.outputGoal)
        if (instruction.isNotBlank()) add(instruction.trim())
    }.joinToString(separator = "\n")

    private fun MutableList<String>.addField(label: String, value: String) {
        if (value.isNotBlank()) add("$label: ${value.trim()}")
    }

    private const val SUBJECT_LABEL = "Subject"
    private const val STYLE_LABEL = "Style"
    private const val MOOD_LABEL = "Mood"
    private const val CAMERA_LABEL = "Camera"
    private const val REQUIREMENTS_LABEL = "Requirements"
    private const val OUTPUT_GOAL_LABEL = "Output goal"
}
