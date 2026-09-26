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

/** Durable, renderable workspace state. One-time navigation and platform actions are effects. */
data class ConversationUiState(
    val mediaKind: MediaKind = MediaKind.IMAGE,
    val conversationId: String = "",
    val conversationTitle: String = "",
    val conversations: List<ConversationSummary> = emptyList(),
    val prompt: String = "",
    val brief: CreativeBrief = CreativeBrief(),
    val workflows: List<WorkflowDescriptor> = emptyList(),
    val selectedWorkflow: WorkflowDescriptor? = null,
    val timeline: List<TimelineItem> = emptyList(),
    val activeSourceId: String? = null,
    val activeSourceLabel: ConversationText? = null,
    val attachmentSlots: List<MediaRequirement> = emptyList(),
    val attachments: List<DraftMediaAttachment> = emptyList(),
    val options: GenerationOptions = GenerationOptions(),
    val modelMenuOpen: Boolean = false,
    val infoOpen: Boolean = false,
    val briefOpen: Boolean = false,
    val optionsOpen: Boolean = false,
    val historyOpen: Boolean = false,
    val message: ConversationText? = null,
    val isSubmitting: Boolean = false,
    val isTransitioning: Boolean = false,
    val isOnline: Boolean = true,
    val credentialsConfigured: Boolean = BuildConfig.HF_KEY_ID.isNotBlank() && BuildConfig.HF_KEY_SECRET.isNotBlank(),
)
