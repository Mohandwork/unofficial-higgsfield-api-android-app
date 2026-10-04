package com.promptstudio.app.feature.conversation

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.promptstudio.app.R
import com.promptstudio.app.core.database.ConversationSummary
import com.promptstudio.app.core.model.MediaKind

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun ConversationHistorySheet(
    state: ConversationUiState,
    onSelectConversation: (String, MediaKind) -> Unit,
    onCreateConversation: (MediaKind) -> Unit,
    onRemoveConversation: (String, MediaKind) -> Unit,
    onDismiss: () -> Unit,
    onEvent: (ConversationUiEvent) -> Unit,
) {
    // TODO use: val maxSheetHeight = LocalWindowInfo.current.containerSize
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
    onRemoveConversation: (String, MediaKind) -> Unit,
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
    onRemoveConversation: (String, MediaKind) -> Unit,
    onEvent: (ConversationUiEvent) -> Unit,
    modifier: Modifier = Modifier,
    scrollEnabled: Boolean = true,
) {
    var renameTarget by remember { mutableStateOf<ConversationSummary?>(null) }
    var removeTarget by remember { mutableStateOf<ConversationSummary?>(null) }
    var renameValue by remember { mutableStateOf("") }
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
                key(conversation.id) {
                    val swipeState = rememberSwipeToDismissBoxState()
                    LaunchedEffect(swipeState.currentValue) {
                        if (swipeState.currentValue == SwipeToDismissBoxValue.EndToStart) {
                            removeTarget = conversation
                            swipeState.reset()
                        }
                    }
                    SwipeToDismissBox(
                        state = swipeState,
                        enableDismissFromStartToEnd = false,
                        enableDismissFromEndToStart = true,
                        backgroundContent = {
                            Box(Modifier.fillMaxSize().padding(top = 8.dp), contentAlignment = Alignment.CenterEnd) {
                                Icon(Icons.Rounded.Delete, stringResource(R.string.remove_chat), tint = MaterialTheme.colorScheme.error, modifier = Modifier.padding(end = 20.dp))
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp).testTag("chat_row_${conversation.id}"),
                    ) {
                        Card(
                            onClick = { onSelectConversation(conversation.id, conversation.mediaKind) },
                            colors = CardDefaults.cardColors(containerColor = if (conversation.id == state.conversationId) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface),
                        ) {
                            Column(Modifier.padding(start = 12.dp, top = 4.dp, bottom = 10.dp, end = 4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(conversation.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                    IconButton(onClick = { renameValue = conversation.title; renameTarget = conversation }, modifier = Modifier.testTag("rename_chat_${conversation.id}")) {
                                        Icon(Icons.Rounded.Edit, stringResource(R.string.rename_chat))
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        DateUtils.getRelativeTimeSpanString(conversation.updatedAtEpochMillis, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(stringResource(R.string.swipe_left_to_remove_chat), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    renameTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text(stringResource(R.string.rename_chat)) },
            text = { OutlinedTextField(renameValue, { renameValue = it }, Modifier.fillMaxWidth().testTag("rename_chat_input")) },
            dismissButton = { TextButton(onClick = { renameTarget = null }) { Text(stringResource(R.string.cancel)) } },
            confirmButton = { TextButton(onClick = { onEvent(ConversationUiEvent.RenameConversation(target.id, renameValue)); renameTarget = null }, enabled = renameValue.isNotBlank()) { Text(stringResource(R.string.done)) } },
        )
    }
    removeTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { removeTarget = null },
            title = { Text(stringResource(R.string.remove_chat_confirm_title)) },
            text = { Text(stringResource(R.string.remove_chat_description)) },
            dismissButton = { TextButton(onClick = { removeTarget = null }) { Text(stringResource(R.string.cancel)) } },
            confirmButton = { TextButton(onClick = { removeTarget = null; onRemoveConversation(target.id, target.mediaKind) }) { Text(stringResource(R.string.remove_chat_confirm), color = MaterialTheme.colorScheme.error) } },
        )
    }
}
