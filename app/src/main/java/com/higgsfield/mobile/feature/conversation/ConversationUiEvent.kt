package com.higgsfield.mobile.feature.conversation

import com.higgsfield.mobile.core.model.CreativeBrief
import com.higgsfield.mobile.core.model.GenerationOptions
import com.higgsfield.mobile.core.model.MediaKind
import com.higgsfield.mobile.core.model.MediaRole
import com.higgsfield.mobile.core.model.WorkflowDescriptor

/** User intent; the route handles only platform results returned by a launched effect. */
sealed interface ConversationUiEvent {
    data class SelectMediaKind(val kind: MediaKind) : ConversationUiEvent
    data class OpenConversation(val id: String, val kind: MediaKind) : ConversationUiEvent
    data class CreateConversation(val kind: MediaKind) : ConversationUiEvent
    data object RemoveConversation : ConversationUiEvent
    data class RenameConversation(val title: String) : ConversationUiEvent
    data class ChangePrompt(val value: String) : ConversationUiEvent
    data object ToggleModelMenu : ConversationUiEvent
    data class SelectWorkflow(val workflow: WorkflowDescriptor) : ConversationUiEvent
    data class ShowInfo(val show: Boolean) : ConversationUiEvent
    data class ShowHistory(val show: Boolean) : ConversationUiEvent
    data class ShowBrief(val show: Boolean) : ConversationUiEvent
    data class ShowOptions(val show: Boolean) : ConversationUiEvent
    data class UpdateBrief(val brief: CreativeBrief) : ConversationUiEvent
    data class UpdateOptions(val options: GenerationOptions) : ConversationUiEvent
    data class PickMedia(val role: MediaRole, val kind: MediaKind) : ConversationUiEvent
    data class MediaPicked(val role: MediaRole, val kind: MediaKind, val uri: String, val label: String) : ConversationUiEvent
    data class RemoveMedia(val role: MediaRole) : ConversationUiEvent
    data object DetachSource : ConversationUiEvent
    data class EditImage(val item: TimelineItem) : ConversationUiEvent
    data class ReuseParameters(val item: TimelineItem) : ConversationUiEvent
    data object Generate : ConversationUiEvent
    data class Retry(val item: TimelineItem) : ConversationUiEvent
    data class Cancel(val item: TimelineItem) : ConversationUiEvent
    data class Download(val item: TimelineItem) : ConversationUiEvent
    data class DownloadDestinationSelected(val item: TimelineItem, val uri: String) : ConversationUiEvent
    data class CopyPrompt(val item: TimelineItem) : ConversationUiEvent
}
