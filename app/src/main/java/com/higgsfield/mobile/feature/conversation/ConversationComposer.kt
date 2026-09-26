package com.higgsfield.mobile.feature.conversation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import coil3.compose.AsyncImage
import com.higgsfield.mobile.R
import com.higgsfield.mobile.core.model.MediaKind
import com.higgsfield.mobile.core.model.MediaRole
import com.higgsfield.mobile.core.model.WorkflowDescriptor
import com.higgsfield.mobile.ui.theme.HiggsfieldTheme

@Preview(showBackground = true, backgroundColor = 0xFF0D1316)
@Composable
private fun ComposerDockPreview() {
    HiggsfieldTheme {
        ComposerDock(previewConversationState(), true, {}, {}, { _, _ -> }, {}, {}, {})
    }
}

@Composable
internal fun ComposerDock(
    state: ConversationUiState,
    motionEnabled: Boolean,
    onPromptChange: (String) -> Unit,
    onShowOptions: (Boolean) -> Unit,
    onPickMedia: (MediaRole, MediaKind) -> Unit,
    onRemoveMedia: (MediaRole) -> Unit,
    onDetachSource: () -> Unit,
    onGenerate: () -> Unit,
) {
    Column(
        (if (motionEnabled) Modifier.animateContentSize() else Modifier).fillMaxWidth().imePadding().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ComposerMessage(state.message)
        if (state.activeSourceId != null) ActiveSourceCard(state.activeSourceLabel?.resolve().orEmpty(), onDetachSource)
        ComposerAttachments(state.attachments, motionEnabled, onRemoveMedia)
        PromptInput(state, onPromptChange, onGenerate)
        ComposerActions(state, onShowOptions, onPickMedia)
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
private fun ComposerAttachments(attachments: List<DraftMediaAttachment>, motionEnabled: Boolean, onRemoveMedia: (MediaRole) -> Unit) {
    if (motionEnabled) AnimatedVisibility(attachments.isNotEmpty()) { AttachmentList(attachments, onRemoveMedia) }
    else if (attachments.isNotEmpty()) AttachmentList(attachments, onRemoveMedia)
}

@Composable
private fun PromptInput(state: ConversationUiState, onPromptChange: (String) -> Unit, onGenerate: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = state.prompt,
            onValueChange = onPromptChange,
            modifier = Modifier.weight(1f),
            minLines = 1,
            maxLines = 5,
            placeholder = { Text(stringResource(if (state.activeSourceId != null) R.string.describe_change else R.string.describe_creation)) },
        )
        FilledTonalIconButton(onClick = onGenerate, enabled = state.prompt.isNotBlank() && state.isOnline && !state.isSubmitting, modifier = Modifier.size(52.dp)) {
            Icon(if (state.isSubmitting) Icons.Rounded.AutoAwesome else Icons.Rounded.ArrowUpward, stringResource(if (state.isSubmitting) R.string.generating else R.string.generate))
        }
    }
}

@Composable
private fun ComposerActions(state: ConversationUiState, onShowOptions: (Boolean) -> Unit, onPickMedia: (MediaRole, MediaKind) -> Unit) {
    var attachmentMenuOpen by remember { mutableStateOf(false) }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (state.attachmentSlots.isNotEmpty()) Box {
            AssistChip(onClick = { attachmentMenuOpen = true }, label = { Text(stringResource(R.string.add_reference)) }, leadingIcon = { Icon(Icons.Rounded.AddPhotoAlternate, null) })
            DropdownMenu(expanded = attachmentMenuOpen, onDismissRequest = { attachmentMenuOpen = false }) {
                state.attachmentSlots.forEach { slot ->
                    DropdownMenuItem(text = { Text(attachmentRoleText(slot.role)) }, onClick = {
                        attachmentMenuOpen = false
                        onPickMedia(slot.role, slot.kind)
                    })
                }
            }
        }
        if (state.selectedWorkflow?.supportedOptions?.isNotEmpty() == true) AssistChip(onClick = { onShowOptions(true) }, label = { Text(stringResource(R.string.presets)) }, leadingIcon = { Icon(Icons.Rounded.Tune, null) })
        Spacer(Modifier.weight(1f))
        EstimateIndicator(state.selectedWorkflow)
    }
}

@Composable
private fun EstimateIndicator(workflow: WorkflowDescriptor?) {
    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(20.dp)) {
        Text(workflow?.staticEstimate?.fromPrice?.let { stringResource(R.string.estimate_from, it) } ?: stringResource(R.string.pricing_unavailable), Modifier.padding(horizontal = 12.dp, vertical = 8.dp), style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun DraftAttachmentCard(attachment: DraftMediaAttachment, onRemove: (MediaRole) -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(model = attachment.uri, contentDescription = attachmentRoleText(attachment.role), modifier = Modifier.size(56.dp).background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp)), contentScale = ContentScale.Crop)
            Column(Modifier.weight(1f)) {
                Text(attachmentRoleText(attachment.role), style = MaterialTheme.typography.labelLarge)
                Text(attachment.label, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = { onRemove(attachment.role) }) { Icon(Icons.Rounded.Close, stringResource(R.string.remove_attachment)) }
        }
    }
}

@Composable
private fun AttachmentList(attachments: List<DraftMediaAttachment>, onRemove: (MediaRole) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { attachments.forEach { DraftAttachmentCard(it, onRemove) } }
}

@Composable
private fun attachmentRoleText(role: MediaRole): String = stringResource(
    when (role) {
        MediaRole.SOURCE -> R.string.media_role_source_image
        MediaRole.MOTION_REFERENCE -> R.string.media_role_motion_video
        MediaRole.REFERENCE -> R.string.media_role_reference_image
        MediaRole.START_FRAME -> R.string.media_role_start_frame
        MediaRole.END_FRAME -> R.string.media_role_end_frame
        MediaRole.AUDIO -> R.string.media_role_audio
    },
)

@Composable
private fun ActiveSourceCard(label: String, onDetach: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.tertiaryContainer, shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(58.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp)))
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(stringResource(R.string.editing_this_image), style = MaterialTheme.typography.labelLarge)
                Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onDetach) { Icon(Icons.Rounded.Close, stringResource(R.string.detach_active_image)) }
        }
    }
}
