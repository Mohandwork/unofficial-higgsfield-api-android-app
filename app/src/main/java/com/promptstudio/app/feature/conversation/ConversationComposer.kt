package com.promptstudio.app.feature.conversation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import coil3.compose.AsyncImage
import com.promptstudio.app.R
import com.promptstudio.app.core.model.MediaKind
import com.promptstudio.app.core.model.GenerationOutput
import com.promptstudio.app.core.model.MediaRole
import com.promptstudio.app.core.model.WorkflowDescriptor
import com.promptstudio.app.core.model.WorkflowCapability
import com.promptstudio.app.ui.theme.PromptStudioTheme

@Preview(showBackground = true, backgroundColor = 0xFF0D1316)
@Composable
private fun ComposerDockPreview() {
    PromptStudioTheme {
        ComposerDock(previewConversationState(), true, {})
    }
}

@Composable
internal fun ComposerDock(
    state: ConversationUiState,
    motionEnabled: Boolean,
    onEvent: (ConversationUiEvent) -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().imePadding().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ComposerMessage(state.message)
        if (state.activeSourceId != null) ActiveSourceCard(
            state.activeSourceLabel?.resolve().orEmpty(),
            state.timeline.firstOrNull { it.id == state.activeSourceId }?.output,
            { onEvent(ConversationUiEvent.DetachSource) },
        )
        if (state.activeSourceId != null && WorkflowCapability.IMAGE_TO_IMAGE !in state.selectedWorkflow?.capabilities.orEmpty()) {
            Text(
                stringResource(R.string.message_choose_compatible_editor),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        ComposerAttachments(state.attachments, state.attachmentSlots, motionEnabled) { role, uri -> onEvent(ConversationUiEvent.RemoveMedia(role, uri)) }
        PromptInput(state, { onEvent(ConversationUiEvent.ChangePrompt(it)) }, { onEvent(ConversationUiEvent.Generate) })
        ComposerActions(state, { onEvent(ConversationUiEvent.ShowBrief(it)) }, { onEvent(ConversationUiEvent.ShowOptions(it)) }) { role, kind ->
            onEvent(ConversationUiEvent.PickMedia(role, kind))
        }
        if (!state.credentialsConfigured) Text(stringResource(R.string.api_credentials_not_configured), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ComposerMessage(message: ConversationText?) {
    message?.let {
        Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(12.dp)) {
            Text(it.resolve(), Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ComposerAttachments(attachments: List<DraftMediaAttachment>, slots: List<MediaRequirement>, motionEnabled: Boolean, onRemoveMedia: (MediaRole, String) -> Unit) {
    val hasEndFrame = slots.any { it.role == MediaRole.END_FRAME }
    if (motionEnabled) AnimatedVisibility(attachments.isNotEmpty()) { AttachmentList(attachments, hasEndFrame, onRemoveMedia) }
    else if (attachments.isNotEmpty()) AttachmentList(attachments, hasEndFrame, onRemoveMedia)
}

@Composable
private fun PromptInput(state: ConversationUiState, onPromptChange: (String) -> Unit, onGenerate: () -> Unit) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val hasStartFrame = state.attachmentSlots.any { it.role == MediaRole.START_FRAME }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = state.prompt,
            onValueChange = onPromptChange,
            modifier = Modifier.weight(1f).testTag("conversation_prompt"),
            minLines = 2,
            maxLines = 5,
            placeholder = { Text(stringResource(when {
                state.activeSourceId != null -> R.string.describe_change
                hasStartFrame && state.selectedWorkflow?.promptRequired == false -> R.string.describe_video_motion_optional
                hasStartFrame -> R.string.describe_video_motion
                else -> R.string.describe_creation
            })) },
        )
        val sourceCompatible = state.activeSourceId == null || WorkflowCapability.IMAGE_TO_IMAGE in state.selectedWorkflow?.capabilities.orEmpty()
        val referencesAreEnough = state.selectedWorkflow?.id in setOf(
            WorkflowCatalog.SEEDANCE_2_REFERENCE.id,
            WorkflowCatalog.SEEDANCE_2_5_REFERENCE.id,
        ) && state.attachments.any { it.kind == MediaKind.IMAGE || it.kind == MediaKind.VIDEO } || state.selectedWorkflow?.let { workflow -> !workflow.promptRequired &&
            workflow.mediaRequirements.any { it.minimumCount > 0 } &&
            workflow.mediaRequirements.filter { it.minimumCount > 0 }.all { slot ->
                state.attachments.count { it.role == slot.role && it.kind == slot.kind } >= slot.minimumCount
            } } == true
        FilledTonalIconButton(onClick = {
            focusManager.clearFocus()
            keyboardController?.hide()
            onGenerate()
        }, enabled = (state.prompt.isNotBlank() || referencesAreEnough) && state.isOnline && !state.isSubmitting && sourceCompatible, modifier = Modifier.size(52.dp)) {
            Icon(if (state.isSubmitting) Icons.Rounded.AutoAwesome else Icons.Rounded.ArrowUpward, stringResource(if (state.isSubmitting) R.string.generating else R.string.generate))
        }
    }
}

@Composable
private fun ComposerActions(state: ConversationUiState, onShowBrief: (Boolean) -> Unit, onShowOptions: (Boolean) -> Unit, onPickMedia: (MediaRole, MediaKind) -> Unit) {
    val hasEndFrame = state.attachmentSlots.any { it.role == MediaRole.END_FRAME }
    Column {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            state.attachmentSlots.forEach { slot ->
                val selectedCount = state.attachments.count { it.role == slot.role && it.kind == slot.kind }
                val slotLabel = when {
                    slot.role == MediaRole.START_FRAME && !hasEndFrame -> stringResource(R.string.media_role_input_image)
                    slot.role == MediaRole.END_FRAME && slot.minimumCount == 0 -> stringResource(R.string.media_role_optional_end_frame)
                    else -> attachmentRoleText(slot.role, slot.kind)
                }
                AssistChip(
                    onClick = { onPickMedia(slot.role, slot.kind) },
                    enabled = slot.maximumCount == null || slot.maximumCount == 1 || selectedCount < slot.maximumCount,
                    label = {
                        Text(
                            if (slot.maximumCount == null) stringResource(R.string.attachment_count_unspecified, slotLabel, selectedCount)
                            else stringResource(R.string.attachment_count_limited, slotLabel, selectedCount, slot.maximumCount),
                            maxLines = 2,
                        )
                    },
                    leadingIcon = { Icon(when (slot.kind) {
                        MediaKind.IMAGE -> Icons.Rounded.AddPhotoAlternate
                        MediaKind.VIDEO -> Icons.Rounded.PlayArrow
                        MediaKind.AUDIO -> Icons.Rounded.MusicNote
                    }, null) },
                    modifier = Modifier.testTag("media_slot_${slot.role}_${slot.kind}"),
                )
            }
            if (state.selectedWorkflow?.supportedOptions?.isNotEmpty() == true) {
                AssistChip(onClick = { onShowOptions(true) }, label = { Text(stringResource(R.string.presets), maxLines = 1) }, leadingIcon = { Icon(Icons.Rounded.Tune, null) })
            }
            AssistChip(
                onClick = { onShowBrief(true) },
                label = {
                    Text(
                        stringResource(if (state.brief == com.higgsfield.mobile.core.model.CreativeBrief()) R.string.brief_empty_summary else R.string.brief_active_summary),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
            )
        }
        state.selectedWorkflow?.maximumCombinedReferences?.let { maximum ->
            Text(stringResource(R.string.reference_combined_limit, maximum), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DraftAttachmentCard(attachment: DraftMediaAttachment, hasEndFrame: Boolean, onRemove: (MediaRole, String) -> Unit) {
    val roleLabel = if (attachment.role == MediaRole.START_FRAME && !hasEndFrame) stringResource(R.string.media_role_input_image)
        else attachmentRoleText(attachment.role, attachment.kind)
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(model = attachment.uri, contentDescription = roleLabel, modifier = Modifier.size(56.dp).background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp)), contentScale = ContentScale.Crop)
            Column(Modifier.weight(1f)) {
                Text(roleLabel, style = MaterialTheme.typography.labelLarge)
                Text(attachment.label, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = { onRemove(attachment.role, attachment.uri) }) { Icon(Icons.Rounded.Close, stringResource(R.string.remove_attachment)) }
        }
    }
}

@Composable
private fun AttachmentList(attachments: List<DraftMediaAttachment>, hasEndFrame: Boolean, onRemove: (MediaRole, String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { attachments.forEach { DraftAttachmentCard(it, hasEndFrame, onRemove) } }
}

@Composable
internal fun attachmentRoleText(role: MediaRole, kind: MediaKind = MediaKind.IMAGE): String = stringResource(
    when (role) {
        MediaRole.SOURCE -> if (kind == MediaKind.VIDEO) R.string.media_role_source_video else R.string.media_role_source_image
        MediaRole.MOTION_REFERENCE -> R.string.media_role_motion_video
        MediaRole.REFERENCE -> R.string.media_role_reference_image
        MediaRole.VIDEO_REFERENCE -> R.string.media_role_reference_video
        MediaRole.START_FRAME -> R.string.media_role_start_frame
        MediaRole.END_FRAME -> R.string.media_role_end_frame
        MediaRole.AUDIO -> R.string.media_role_audio
    },
)

@Composable
private fun ActiveSourceCard(label: String, output: GenerationOutput?, onDetach: () -> Unit) {
    val context = LocalContext.current
    val previewUri = remember(output?.localUri, output?.remoteUrl) {
        output?.previewUri(context.contentResolver.persistedUriPermissions
            .filter { it.isReadPermission }
            .map { it.uri.toString() }
            .toSet())
    }
    Surface(color = MaterialTheme.colorScheme.tertiaryContainer, shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (previewUri != null) AsyncImage(
                model = previewUri,
                contentDescription = stringResource(R.string.generated_image),
                modifier = Modifier.size(58.dp).background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp)),
                contentScale = ContentScale.Crop,
            ) else Box(Modifier.size(58.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp)))
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(stringResource(R.string.editing_this_image), style = MaterialTheme.typography.labelLarge)
                Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onDetach) { Icon(Icons.Rounded.Close, stringResource(R.string.detach_active_image)) }
        }
    }
}
