package com.higgsfield.mobile.feature.conversation

/** Non-Compose state messages, centralized until the UI state migrates to resource-backed UiText. */
object ConversationCopy {
    const val MODEL_CANNOT_EDIT_ACTIVE_IMAGE = "This model cannot edit the active image. Detach it or choose a compatible edit model."
    const val REFERENCE_IMAGE_ATTACHED = "Reference image attached locally."
    const val FRESH_GENERATION_STARTED = "Fresh generation started."
    const val OLDER_OUTPUT_SELECTED = "Older output selected. Your next request starts a new branch."
    const val WRITE_INSTRUCTION_FIRST = "Write an instruction first."
    const val CHOOSE_MODEL_FIRST = "Choose a model first."
    const val CHOOSE_COMPATIBLE_EDITOR = "Choose Qwen Image 3 Edit or another compatible image editor."
    const val DEMO_ONLY = "Demo only — no API request was made."
    const val SELECT_OUTPUT_BEFORE_CONTINUING = "Choose one of the latest images before continuing."
}
