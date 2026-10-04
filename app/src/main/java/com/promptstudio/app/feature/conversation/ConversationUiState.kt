package com.promptstudio.app.feature.conversation

import com.promptstudio.app.BuildConfig
import com.promptstudio.app.core.database.ConversationSummary
import com.promptstudio.app.core.model.CreativeBrief
import com.promptstudio.app.core.model.GenerationOptions
import com.promptstudio.app.core.model.GenerationOutput
import com.promptstudio.app.core.model.GenerationRecord
import com.promptstudio.app.core.model.GenerationStatus
import com.promptstudio.app.core.model.MediaKind
import com.promptstudio.app.core.model.MediaRequirement
import com.promptstudio.app.core.model.MediaRole
import com.promptstudio.app.core.model.WorkflowDescriptor

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

/** Durable workspace state grouped by ownership. One-time platform actions are effects. */
data class ConversationUiState(
    val chat: ChatUiState = ChatUiState(),
    val composer: ComposerUiState = ComposerUiState(),
    val generation: GenerationUiState = GenerationUiState(),
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
}
