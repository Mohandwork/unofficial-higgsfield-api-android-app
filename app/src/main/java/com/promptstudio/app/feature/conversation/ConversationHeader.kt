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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.promptstudio.app.R
import com.promptstudio.app.core.model.MediaKind
import com.promptstudio.app.core.model.WorkflowFamily

@Composable
internal fun ConversationHeader(
    state: ConversationUiState,
    onEvent: (ConversationUiEvent) -> Unit,
    modelMenuOpen: Boolean,
    onCreateConversation: () -> Unit,
) {
    val workflowsByFamily = state.workflows.groupBy { it.family }
    var openFamily by remember(state.mediaKind, modelMenuOpen) { mutableStateOf<WorkflowFamily?>(null) }
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
                        trailingIcon = { Icon(Icons.Rounded.ArrowDropDown, stringResource(R.string.choose_model)) },
                        modifier = Modifier.fillMaxWidth().testTag("model_picker"),
                    )
                    DropdownMenu(
                        expanded = modelMenuOpen,
                        onDismissRequest = { onEvent(ConversationUiEvent.ToggleModelMenu) },
                        modifier = Modifier.widthIn(min = 220.dp, max = 320.dp),
                    ) {
                        if (openFamily == null) {
                            workflowsByFamily.forEach { (family, workflows) ->
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.model_family_count, modelFamilyLabel(family), workflows.size)) },
                                    trailingIcon = { Icon(Icons.Rounded.ChevronRight, null) },
                                    onClick = { openFamily = family },
                                    modifier = Modifier.testTag("model_family_${family.name}"),
                                )
                            }
                        } else {
                            val family = requireNotNull(openFamily)
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.all_model_families)) },
                                leadingIcon = { Icon(Icons.AutoMirrored.Rounded.ArrowBack, null) },
                                onClick = { openFamily = null },
                                modifier = Modifier.testTag("model_family_back"),
                            )
                            Text(
                                modelFamilyLabel(family),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            )
                            workflowsByFamily[family].orEmpty().forEach { workflow ->
                                DropdownMenuItem(
                                    text = { Text(workflow.displayName, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                                    trailingIcon = {
                                        if (workflow.id == state.selectedWorkflow?.id) Icon(Icons.Rounded.Check, null)
                                    },
                                    onClick = { onEvent(ConversationUiEvent.SelectWorkflow(workflow)) },
                                    modifier = Modifier.testTag("model_${workflow.id.value}"),
                                )
                            }
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

@Composable
private fun modelFamilyLabel(family: WorkflowFamily): String = stringResource(when (family) {
    WorkflowFamily.SOUL -> R.string.model_family_soul
    WorkflowFamily.MARKETING_STUDIO -> R.string.model_family_marketing_studio
    WorkflowFamily.QWEN -> R.string.model_family_qwen
    WorkflowFamily.SEEDANCE -> R.string.model_family_seedance
    WorkflowFamily.KLING -> R.string.model_family_kling
    WorkflowFamily.CINEMA_STUDIO -> R.string.model_family_cinema_studio
    WorkflowFamily.WAN -> R.string.model_family_wan
    WorkflowFamily.HAPPY_HORSE -> R.string.model_family_happy_horse
    WorkflowFamily.Z_IMAGE -> R.string.model_family_z_image
    WorkflowFamily.GROK -> R.string.model_family_grok
    WorkflowFamily.IDEOGRAM -> R.string.model_family_ideogram
    WorkflowFamily.RECRAFT -> R.string.model_family_recraft
    WorkflowFamily.GENJUTSU -> R.string.model_family_genjutsu
    WorkflowFamily.MINIMAX -> R.string.model_family_minimax
    WorkflowFamily.PIXVERSE -> R.string.model_family_pixverse
})
