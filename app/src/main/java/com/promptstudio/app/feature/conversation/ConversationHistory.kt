package com.promptstudio.app.feature.conversation

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.promptstudio.app.R
import com.promptstudio.app.core.model.MediaKind

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun ConversationHistorySheet(
    state: ConversationUiState,
    onSelectConversation: (String, MediaKind) -> Unit,
    onCreateConversation: (MediaKind) -> Unit,
    onRemoveConversation: () -> Unit,
    onDismiss: () -> Unit,
    onEvent: (ConversationUiEvent) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val maxSheetHeight = LocalConfiguration.current.screenHeightDp.dp * 0.85f
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        ConversationHistoryContent(
            state, onSelectConversation, onCreateConversation, onRemoveConversation, onEvent,
            Modifier.fillMaxWidth().heightIn(max = maxSheetHeight).padding(20.dp),
            scrollEnabled = sheetState.currentValue == SheetValue.Expanded,
        )
    }
}

@Composable
internal fun ConversationRail(
    state: ConversationUiState,
    onSelectConversation: (String, MediaKind) -> Unit,
    onCreateConversation: (MediaKind) -> Unit,
    onRemoveConversation: () -> Unit,
    onEvent: (ConversationUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)) {
        ConversationHistoryContent(state, onSelectConversation, onCreateConversation, onRemoveConversation, onEvent, Modifier.padding(20.dp))
    }
}

@Composable
private fun ConversationHistoryContent(
    state: ConversationUiState,
    onSelectConversation: (String, MediaKind) -> Unit,
    onCreateConversation: (MediaKind) -> Unit,
    onRemoveConversation: () -> Unit,
    onEvent: (ConversationUiEvent) -> Unit,
    modifier: Modifier = Modifier,
    scrollEnabled: Boolean = true,
) {
    var renameOpen by remember { mutableStateOf(false) }
    var removeOpen by remember { mutableStateOf(false) }
    var renameValue by remember(state.conversationTitle) { mutableStateOf(state.conversationTitle) }
    Column(modifier.verticalScroll(rememberScrollState(), enabled = scrollEnabled)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.conversations), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            TextButton(onClick = { onCreateConversation(state.mediaKind) }) { Text(stringResource(R.string.new_chat)) }
        }
        MediaKind.entries.forEach { kind ->
            val conversations = state.conversations.filter { it.mediaKind == kind }
            if (conversations.isEmpty()) return@forEach
            Text(if (kind == MediaKind.IMAGE) stringResource(R.string.image) else stringResource(R.string.video), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 12.dp))
            conversations.forEach { conversation ->
                Card(
                    onClick = { onSelectConversation(conversation.id, conversation.mediaKind) },
                    colors = CardDefaults.cardColors(containerColor = if (conversation.id == state.conversationId) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(conversation.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            DateUtils.getRelativeTimeSpanString(conversation.updatedAtEpochMillis, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        if (state.conversationTitle.isNotBlank()) TextButton(onClick = { renameOpen = true }) { Text(stringResource(R.string.rename_chat)) }
        if (state.conversationTitle.isNotBlank()) {
            Text(stringResource(R.string.chat_removal), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 20.dp))
            Text(stringResource(R.string.remove_chat_description), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = { removeOpen = true }) { Text(stringResource(R.string.remove_chat), color = MaterialTheme.colorScheme.error) }
        }
    }
    if (renameOpen) {
        AlertDialog(
            onDismissRequest = { renameOpen = false },
            title = { Text(stringResource(R.string.rename_chat)) },
            text = { OutlinedTextField(renameValue, { renameValue = it }, Modifier.fillMaxWidth()) },
            dismissButton = { TextButton(onClick = { renameOpen = false }) { Text(stringResource(R.string.cancel)) } },
            confirmButton = { TextButton(onClick = { onEvent(ConversationUiEvent.RenameConversation(renameValue)); renameOpen = false }) { Text(stringResource(R.string.done)) } },
        )
    }
    if (removeOpen) {
        AlertDialog(
            onDismissRequest = { removeOpen = false },
            title = { Text(stringResource(R.string.remove_chat_confirm_title)) },
            text = { Text(stringResource(R.string.remove_chat_description)) },
            dismissButton = { TextButton(onClick = { removeOpen = false }) { Text(stringResource(R.string.cancel)) } },
            confirmButton = { TextButton(onClick = { removeOpen = false; onRemoveConversation() }) { Text(stringResource(R.string.remove_chat_confirm), color = MaterialTheme.colorScheme.error) } },
        )
    }
}
