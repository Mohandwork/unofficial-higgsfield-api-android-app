package com.higgsfield.mobile.feature.conversation

import com.higgsfield.mobile.core.model.CreativeBrief
import com.higgsfield.mobile.core.model.GenerationOptions
import com.higgsfield.mobile.core.model.MediaKind
import com.higgsfield.mobile.core.model.MediaRole
import com.higgsfield.mobile.core.model.WorkflowDescriptor

/** User intent; the route handles only platform results returned by a launched effect. */
sealed interface ConversationUiEvent {
    sealed interface Chat : ConversationUiEvent
    sealed interface Composer : ConversationUiEvent
    sealed interface Generation : ConversationUiEvent
    sealed interface Panel : ConversationUiEvent

    data class SelectMediaKind(val kind: MediaKind) : Chat
    data class OpenConversation(val id: String, val kind: MediaKind) : Chat
    data class CreateConversation(val kind: MediaKind) : Chat
    data object RemoveConversation : Chat
    data class RenameConversation(val title: String) : Chat
    data class ShowHistory(val show: Boolean) : Panel

    data class ChangePrompt(val value: String) : Composer
    data class SelectWorkflow(val workflow: WorkflowDescriptor) : Composer
    data class UpdateBrief(val brief: CreativeBrief) : Composer
    data class UpdateOptions(val options: GenerationOptions) : Composer
    data class PickMedia(val role: MediaRole, val kind: MediaKind) : Composer
    data class MediaPicked(val role: MediaRole, val kind: MediaKind, val uri: String, val label: String) : Composer
    data class RemoveMedia(val role: MediaRole, val uri: String) : Composer
    data object DetachSource : Composer
    data class EditImage(val item: TimelineItem) : Composer
    data class ReuseParameters(val item: TimelineItem) : Composer

    data object Generate : Generation
    data class Retry(val item: TimelineItem) : Generation
    data class Cancel(val item: TimelineItem) : Generation
    data class Download(val item: TimelineItem) : Generation
    data class DownloadDestinationSelected(val item: TimelineItem, val uri: String) : Generation
    data class CopyPrompt(val item: TimelineItem) : Generation

    data object ToggleModelMenu : Panel
    data class ShowInfo(val show: Boolean) : Panel
    data class ShowBrief(val show: Boolean) : Panel
    data class ShowOptions(val show: Boolean) : Panel
}
