package com.promptstudio.app.feature.conversation

import com.promptstudio.app.core.model.MediaKind
import com.promptstudio.app.core.model.MediaRole

/** One-time work performed by the Android route, never stored in UiState. */
sealed interface ConversationUiEffect {
    data class NavigateToConversation(val id: String, val kind: MediaKind) : ConversationUiEffect
    data class LaunchMediaPicker(val role: MediaRole, val kind: MediaKind) : ConversationUiEffect
    data class LaunchDownload(val item: TimelineItem) : ConversationUiEffect
    data class CopyText(val value: String) : ConversationUiEffect
}
