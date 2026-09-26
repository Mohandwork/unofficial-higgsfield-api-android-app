package com.higgsfield.mobile.core.model

object PromptComposer {
    fun compose(brief: CreativeBrief, instruction: String, supportsNegativePrompt: Boolean = false): String = buildList {
        addField(SUBJECT_LABEL, brief.subject)
        addField(STYLE_LABEL, brief.style)
        addField(MOOD_LABEL, brief.mood)
        addField(CAMERA_LABEL, brief.cameraDirection)
        addField(REQUIREMENTS_LABEL, brief.requirements)
        addField(OUTPUT_GOAL_LABEL, brief.outputGoal)
        if (!supportsNegativePrompt) addField(AVOID_LABEL, brief.exclusions)
        if (instruction.isNotBlank()) add(instruction.trim())
    }.joinToString(separator = "\n")

    fun composeNegativePrompt(brief: CreativeBrief, oneOffNegativePrompt: String?): String? =
        listOf(brief.exclusions, oneOffNegativePrompt.orEmpty()).filter(String::isNotBlank)
            .joinToString("\n").takeIf(String::isNotBlank)

    private fun MutableList<String>.addField(label: String, value: String) {
        if (value.isNotBlank()) add("$label: ${value.trim()}")
    }

    private const val SUBJECT_LABEL = "Subject"
    private const val STYLE_LABEL = "Style"
    private const val MOOD_LABEL = "Mood"
    private const val CAMERA_LABEL = "Camera"
    private const val REQUIREMENTS_LABEL = "Requirements"
    private const val OUTPUT_GOAL_LABEL = "Output goal"
    private const val AVOID_LABEL = "Avoid"
}
