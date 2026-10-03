package com.higgsfield.mobile.feature.conversation

import com.higgsfield.mobile.core.model.MediaKind
import com.higgsfield.mobile.core.model.MediaRole

/** One-time work performed by the Android route, never stored in UiState. */
sealed interface ConversationUiEffect {
    data class NavigateToConversation(val id: String?, val kind: MediaKind) : ConversationUiEffect
    data class LaunchMediaPicker(val role: MediaRole, val kind: MediaKind) : ConversationUiEffect
    data class LaunchDownload(val item: TimelineItem) : ConversationUiEffect
    data class CopyText(val value: String) : ConversationUiEffect
}
