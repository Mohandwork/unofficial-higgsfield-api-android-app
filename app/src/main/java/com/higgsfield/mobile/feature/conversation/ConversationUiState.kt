package com.higgsfield.mobile.feature.conversation

import com.higgsfield.mobile.BuildConfig
import com.higgsfield.mobile.core.database.ConversationSummary
import com.higgsfield.mobile.core.model.CreativeBrief
import com.higgsfield.mobile.core.model.GenerationOptions
import com.higgsfield.mobile.core.model.GenerationOutput
import com.higgsfield.mobile.core.model.GenerationRecord
import com.higgsfield.mobile.core.model.GenerationStatus
import com.higgsfield.mobile.core.model.MediaKind
import com.higgsfield.mobile.core.model.MediaRequirement
import com.higgsfield.mobile.core.model.MediaRole
import com.higgsfield.mobile.core.model.WorkflowDescriptor

data class TimelineItem(
    val id: String,
    val prompt: String,
    val modelName: String,
    val stateLabel: ConversationText,
    val outputLabel: ConversationText? = null,
    val output: GenerationOutput? = null,
    val sourceOutputId: String? = null,
    val parentId: String? = null,
    val lifecycle: GenerationStatus? = null,
    val record: GenerationRecord? = null,
    val errorText: ConversationText? = null,
    val canRetry: Boolean = false,
    val canCancel: Boolean = false,
)

data class DraftMediaAttachment(
    val role: MediaRole,
    val kind: MediaKind,
    val uri: String,
    val label: String,
    val remoteUrl: String? = null,
)

data class ChatUiState(
    val mediaKind: MediaKind = MediaKind.IMAGE,
    val id: String = "",
    val title: String = "",
    val conversations: List<ConversationSummary> = emptyList(),
    val isTransitioning: Boolean = false,
)

data class ComposerUiState(
    val prompt: String = "",
    val brief: CreativeBrief = CreativeBrief(),
    val workflows: List<WorkflowDescriptor> = emptyList(),
    val selectedWorkflow: WorkflowDescriptor? = null,
    val activeSourceId: String? = null,
    val activeSourceLabel: ConversationText? = null,
    val attachmentSlots: List<MediaRequirement> = emptyList(),
    val attachments: List<DraftMediaAttachment> = emptyList(),
    val options: GenerationOptions = GenerationOptions(),
)

data class GenerationUiState(
    val timeline: List<TimelineItem> = emptyList(),
    val isSubmitting: Boolean = false,
)

data class ConversationPanelsState(
    val modelMenuOpen: Boolean = false,
    val infoOpen: Boolean = false,
    val briefOpen: Boolean = false,
    val optionsOpen: Boolean = false,
    val historyOpen: Boolean = false,
)

/** Durable workspace state grouped by ownership. One-time platform actions are effects. */
data class ConversationUiState(
    val chat: ChatUiState = ChatUiState(),
    val composer: ComposerUiState = ComposerUiState(),
    val generation: GenerationUiState = GenerationUiState(),
    val panels: ConversationPanelsState = ConversationPanelsState(),
    val message: ConversationText? = null,
    val isOnline: Boolean = true,
    val credentialsConfigured: Boolean = BuildConfig.HF_KEY_ID.isNotBlank() && BuildConfig.HF_KEY_SECRET.isNotBlank(),
) {
    // Read-only aliases keep rendering call sites concise; state writes use section ownership.
    val mediaKind get() = chat.mediaKind
    val conversationId get() = chat.id
    val conversationTitle get() = chat.title
    val conversations get() = chat.conversations
    val isTransitioning get() = chat.isTransitioning
    val prompt get() = composer.prompt
    val brief get() = composer.brief
    val workflows get() = composer.workflows
    val selectedWorkflow get() = composer.selectedWorkflow
    val activeSourceId get() = composer.activeSourceId
    val activeSourceLabel get() = composer.activeSourceLabel
    val attachmentSlots get() = composer.attachmentSlots
    val attachments get() = composer.attachments
    val options get() = composer.options
    val timeline get() = generation.timeline
    val isSubmitting get() = generation.isSubmitting
    val modelMenuOpen get() = panels.modelMenuOpen
    val infoOpen get() = panels.infoOpen
    val briefOpen get() = panels.briefOpen
    val optionsOpen get() = panels.optionsOpen
    val historyOpen get() = panels.historyOpen
}
