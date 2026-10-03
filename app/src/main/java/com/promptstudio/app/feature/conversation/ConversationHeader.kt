package com.promptstudio.app.feature.conversation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.promptstudio.app.R
import com.promptstudio.app.core.model.MediaKind

@Composable
internal fun ConversationHeader(
    state: ConversationUiState,
    onEvent: (ConversationUiEvent) -> Unit,
    modelMenuOpen: Boolean,
    onCreateConversation: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 4.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onEvent(ConversationUiEvent.ShowHistory(true)) }) {
                    Icon(Icons.Rounded.Menu, stringResource(R.string.conversations), tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.weight(1f))
                Icon(
                    if (state.isOnline) Icons.Rounded.CloudDone else Icons.Rounded.CloudOff,
                    contentDescription = stringResource(if (state.isOnline) R.string.online else R.string.offline),
                    tint = if (state.isOnline) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp),
                )
                IconButton(onClick = onCreateConversation, modifier = Modifier.testTag("new_conversation")) {
                    Icon(Icons.Rounded.Add, stringResource(R.string.new_chat), tint = MaterialTheme.colorScheme.primary)
                }
            }
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                TextButton(onClick = { onEvent(ConversationUiEvent.SelectMediaKind(MediaKind.IMAGE)) }) {
                    Text(stringResource(R.string.image), color = if (state.mediaKind == MediaKind.IMAGE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = { onEvent(ConversationUiEvent.SelectMediaKind(MediaKind.VIDEO)) }) {
                    Text(stringResource(R.string.video), color = if (state.mediaKind == MediaKind.VIDEO) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Box(Modifier.weight(1f)) {
                    AssistChip(
                        onClick = { onEvent(ConversationUiEvent.ToggleModelMenu) },
                        label = { Text(state.selectedWorkflow?.displayName ?: stringResource(R.string.choose_model), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    DropdownMenu(expanded = modelMenuOpen, onDismissRequest = { onEvent(ConversationUiEvent.ToggleModelMenu) }) {
                        state.workflows.forEach { workflow ->
                            DropdownMenuItem(text = { Text(workflow.displayName) }, onClick = { onEvent(ConversationUiEvent.SelectWorkflow(workflow)) })
                        }
                    }
                }
                IconButton(onClick = { onEvent(ConversationUiEvent.ShowInfo(true)) }) {
                    Icon(Icons.Rounded.Info, stringResource(R.string.model_details_and_cost), tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
