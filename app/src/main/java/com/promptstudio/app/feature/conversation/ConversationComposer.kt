package com.promptstudio.app.feature.conversation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
        ComposerAttachments(state.attachments, motionEnabled) { onEvent(ConversationUiEvent.RemoveMedia(it)) }
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
private fun ComposerAttachments(attachments: List<DraftMediaAttachment>, motionEnabled: Boolean, onRemoveMedia: (MediaRole) -> Unit) {
    if (motionEnabled) AnimatedVisibility(attachments.isNotEmpty()) { AttachmentList(attachments, onRemoveMedia) }
    else if (attachments.isNotEmpty()) AttachmentList(attachments, onRemoveMedia)
}

@Composable
private fun PromptInput(state: ConversationUiState, onPromptChange: (String) -> Unit, onGenerate: () -> Unit) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = state.prompt,
            onValueChange = onPromptChange,
            modifier = Modifier.weight(1f).testTag("conversation_prompt"),
            minLines = 2,
            maxLines = 5,
            placeholder = { Text(stringResource(if (state.activeSourceId != null) R.string.describe_change else R.string.describe_creation)) },
        )
        val sourceCompatible = state.activeSourceId == null || WorkflowCapability.IMAGE_TO_IMAGE in state.selectedWorkflow?.capabilities.orEmpty()
        FilledTonalIconButton(onClick = {
            focusManager.clearFocus()
            keyboardController?.hide()
            onGenerate()
        }, enabled = state.prompt.isNotBlank() && state.isOnline && !state.isSubmitting && sourceCompatible, modifier = Modifier.size(52.dp)) {
            Icon(if (state.isSubmitting) Icons.Rounded.AutoAwesome else Icons.Rounded.ArrowUpward, stringResource(if (state.isSubmitting) R.string.generating else R.string.generate))
        }
    }
}

@Composable
private fun ComposerActions(state: ConversationUiState, onShowBrief: (Boolean) -> Unit, onShowOptions: (Boolean) -> Unit, onPickMedia: (MediaRole, MediaKind) -> Unit) {
    var attachmentMenuOpen by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            when (state.attachmentSlots.size) {
                0 -> Unit
                1 -> {
                    val slot = state.attachmentSlots.single()
                    AssistChip(
                        onClick = { onPickMedia(slot.role, slot.kind) },
                        label = { Text(stringResource(R.string.add_reference), maxLines = 1) },
                        leadingIcon = { Icon(Icons.Rounded.AddPhotoAlternate, null) },
                    )
                }
                else -> Box {
                    AssistChip(onClick = { attachmentMenuOpen = true }, label = { Text(stringResource(R.string.add_reference), maxLines = 1) }, leadingIcon = { Icon(Icons.Rounded.AddPhotoAlternate, null) })
                    DropdownMenu(expanded = attachmentMenuOpen, onDismissRequest = { attachmentMenuOpen = false }) {
                        state.attachmentSlots.forEach { slot ->
                            DropdownMenuItem(text = { Text(attachmentRoleText(slot.role)) }, onClick = {
                                attachmentMenuOpen = false
                                onPickMedia(slot.role, slot.kind)
                            })
                        }
                    }
                }
            }
            if (state.selectedWorkflow?.supportedOptions?.isNotEmpty() == true) {
                AssistChip(onClick = { onShowOptions(true) }, label = { Text(stringResource(R.string.presets), maxLines = 1) }, leadingIcon = { Icon(Icons.Rounded.Tune, null) })
            }
        }
        AssistChip(
            onClick = { onShowBrief(true) },
            label = {
                Text(
                    stringResource(if (state.brief == com.promptstudio.app.core.model.CreativeBrief()) R.string.brief_empty_summary else R.string.brief_active_summary),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            modifier = Modifier.fillMaxWidth(),
        )
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
internal fun attachmentRoleText(role: MediaRole): String = stringResource(
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
