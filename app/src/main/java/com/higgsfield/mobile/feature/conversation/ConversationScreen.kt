package com.higgsfield.mobile.feature.conversation

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.higgsfield.mobile.core.model.CreativeBrief
import com.higgsfield.mobile.core.model.MediaKind
import com.higgsfield.mobile.core.model.WorkflowDescriptor

@Composable
fun ConversationRoute(
    mediaKind: MediaKind,
    onBack: () -> Unit,
    viewModel: ConversationViewModel = hiltViewModel(),
) {
    LaunchedEffect(mediaKind) { viewModel.initialize(mediaKind) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    ConversationScreen(
        state = state,
        onBack = onBack,
        onPromptChange = viewModel::updatePrompt,
        onToggleModelMenu = viewModel::toggleModelMenu,
        onSelectWorkflow = viewModel::selectWorkflow,
        onShowInfo = viewModel::showInfo,
        onShowBrief = viewModel::showBrief,
        onShowOptions = viewModel::showOptions,
        onUpdateBrief = viewModel::updateBrief,
        onAttachSource = viewModel::attachSource,
        onDetachSource = viewModel::detachSource,
        onUseOutput = viewModel::useOutput,
        onAddDemoResult = viewModel::addDemoResult,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConversationScreen(
    state: ConversationUiState,
    onBack: () -> Unit,
    onPromptChange: (String) -> Unit,
    onToggleModelMenu: () -> Unit,
    onSelectWorkflow: (WorkflowDescriptor) -> Unit,
    onShowInfo: (Boolean) -> Unit,
    onShowBrief: (Boolean) -> Unit,
    onShowOptions: (Boolean) -> Unit,
    onUpdateBrief: (CreativeBrief) -> Unit,
    onAttachSource: (String) -> Unit,
    onDetachSource: () -> Unit,
    onUseOutput: (TimelineItem) -> Unit,
    onAddDemoResult: () -> Unit,
) {
    val picker = rememberLauncherForActivityResult(PickVisualMedia()) { uri: Uri? ->
        uri?.let { onAttachSource(it.lastPathSegment ?: "Selected image") }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(if (state.mediaKind == MediaKind.IMAGE) "Image studio" else "Video studio")
                        Text("Draft saved locally", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") }
                },
                actions = {
                    Icon(Icons.Rounded.CloudDone, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Online", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 12.dp))
                },
            )
        },
    ) { padding ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(padding)) {
            if (maxWidth >= 840.dp) {
                Row(Modifier.fillMaxSize()) {
                    ConversationRail(state, Modifier.width(280.dp).fillMaxHeight())
                    HorizontalDivider(Modifier.fillMaxHeight().width(1.dp))
                    Workspace(state, onPromptChange, onToggleModelMenu, onSelectWorkflow, onShowInfo, onShowBrief, onShowOptions, {
                        picker.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
                    }, onDetachSource, onUseOutput, onAddDemoResult, Modifier.weight(1f))
                }
            } else {
                Workspace(state, onPromptChange, onToggleModelMenu, onSelectWorkflow, onShowInfo, onShowBrief, onShowOptions, {
                    picker.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
                }, onDetachSource, onUseOutput, onAddDemoResult, Modifier.fillMaxSize())
            }
        }
    }

    if (state.infoOpen) ModelInfoDialog(state.selectedWorkflow, { onShowInfo(false) })
    if (state.briefOpen) BriefDialog(state.brief, { onShowBrief(false) }, onUpdateBrief)
    if (state.optionsOpen) OptionsDialog(state.mediaKind, { onShowOptions(false) })
}

@Composable
private fun ConversationRail(state: ConversationUiState, modifier: Modifier = Modifier) {
    Surface(modifier, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)) {
        Column(Modifier.padding(20.dp)) {
            Text("Conversations", style = MaterialTheme.typography.titleLarge)
            Text("Today", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 28.dp, bottom = 8.dp))
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Column(Modifier.padding(16.dp)) {
                    Text(if (state.mediaKind == MediaKind.IMAGE) "Image exploration" else "Video exploration", fontWeight = FontWeight.SemiBold)
                    Text("Current local draft", style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(Modifier.weight(1f))
            Text("Versions", style = MaterialTheme.typography.titleMedium)
            Text("${state.timeline.size} local demo version(s)", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun Workspace(
    state: ConversationUiState,
    onPromptChange: (String) -> Unit,
    onToggleModelMenu: () -> Unit,
    onSelectWorkflow: (WorkflowDescriptor) -> Unit,
    onShowInfo: (Boolean) -> Unit,
    onShowBrief: (Boolean) -> Unit,
    onShowOptions: (Boolean) -> Unit,
    onPickImage: () -> Unit,
    onDetachSource: () -> Unit,
    onUseOutput: (TimelineItem) -> Unit,
    onAddDemoResult: () -> Unit,
    modifier: Modifier,
) {
    Column(modifier) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box {
                AssistChip(onClick = onToggleModelMenu, label = { Text(state.selectedWorkflow?.displayName ?: "Choose model") })
                DropdownMenu(expanded = state.modelMenuOpen, onDismissRequest = onToggleModelMenu) {
                    state.workflows.forEach { workflow ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(workflow.displayName)
                                    Text("Adapter verification pending", style = MaterialTheme.typography.labelSmall)
                                }
                            },
                            onClick = { onSelectWorkflow(workflow) },
                        )
                    }
                }
            }
            FilledTonalIconButton(onClick = { onShowInfo(true) }) { Icon(Icons.Rounded.Info, "Model details and cost") }
            AssistChip(onClick = { onShowBrief(true) }, label = { Text("Creative brief") })
            Spacer(Modifier.weight(1f))
            Text("Estimate unavailable", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        HorizontalDivider()
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (state.timeline.isEmpty()) {
                item {
                    EmptyConversation(state.mediaKind)
                }
            }
            items(state.timeline, key = { it.id }) { item ->
                TimelineCard(item, item.id == state.activeSourceId, onUseOutput)
            }
        }
        Column(
            Modifier.fillMaxWidth().imePadding().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            state.message?.let { message ->
                Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(12.dp)) {
                    Text(message, Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
                }
            }
            if (state.activeSourceId != null) ActiveSourceCard(state.activeSourceLabel.orEmpty(), onDetachSource)
            OutlinedTextField(
                value = state.prompt,
                onValueChange = onPromptChange,
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 5,
                placeholder = { Text(if (state.activeSourceId != null) "Describe what to change…" else "Describe what to create…") },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPickImage) { Icon(Icons.Rounded.AddPhotoAlternate, "Attach a reference image") }
                IconButton(onClick = { onShowOptions(true) }) { Icon(Icons.Rounded.Tune, "Advanced options") }
                Spacer(Modifier.weight(1f))
                Button(onClick = onAddDemoResult, enabled = state.prompt.isNotBlank()) {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null)
                    Text("Preview locally", Modifier.padding(start = 8.dp))
                }
            }
            if (!state.credentialsConfigured) {
                Text("API disabled: add local credentials. Preview locally never calls the API.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun EmptyConversation(kind: MediaKind) {
    Column(Modifier.fillMaxWidth().padding(vertical = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Rounded.AutoAwesome, null, Modifier.size(44.dp), tint = MaterialTheme.colorScheme.primary)
        Text("Start with a clear idea", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 16.dp))
        Text(
            if (kind == MediaKind.IMAGE) "Create an image, then make the next edit from its visible active source."
            else "Create a video from text or attach a compatible reference image.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun TimelineCard(item: TimelineItem, active: Boolean, onUseOutput: (TimelineItem) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.align(Alignment.End)) {
            Text(item.prompt, Modifier.padding(14.dp))
        }
        Card(onClick = { onUseOutput(item) }, modifier = Modifier.fillMaxWidth()) {
            Box(
                Modifier.fillMaxWidth().height(180.dp).background(
                    Brush.linearGradient(listOf(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.primaryContainer))
                ),
                contentAlignment = Alignment.Center,
            ) {
                Text(item.outputLabel.orEmpty(), style = MaterialTheme.typography.titleLarge)
            }
            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(item.modelName, fontWeight = FontWeight.SemiBold)
                    Text(item.stateLabel, style = MaterialTheme.typography.bodySmall)
                }
                if (active) Text("Editing source", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                else Text("Use this", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun ActiveSourceCard(label: String, onDetach: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(58.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp)))
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text("Editing this image", fontWeight = FontWeight.Bold)
                Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onDetach) { Icon(Icons.Rounded.Close, "Detach active image and start fresh") }
        }
    }
}

@Composable
private fun ModelInfoDialog(workflow: WorkflowDescriptor?, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
        title = { Text(workflow?.displayName ?: "Model") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Capabilities: ${workflow?.capabilities?.joinToString { it.name.lowercase().replace('_', ' ') } ?: "Unknown"}")
                Text("Cost depends on the exact options. A live estimate will appear before real generation.")
                Text("This adapter is not enabled until its endpoint schema is verified.", color = MaterialTheme.colorScheme.error)
            }
        },
    )
}

@Composable
private fun BriefDialog(initial: CreativeBrief, onDismiss: () -> Unit, onSave: (CreativeBrief) -> Unit) {
    var subject by remember(initial) { mutableStateOf(initial.subject) }
    var style by remember(initial) { mutableStateOf(initial.style) }
    var exclusions by remember(initial) { mutableStateOf(initial.exclusions) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onSave(initial.copy(subject = subject, style = style, exclusions = exclusions)) }) { Text("Save brief") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text("Pinned creative brief") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Applied explicitly to each request; chat history is not silently sent.")
                OutlinedTextField(subject, { subject = it }, label = { Text("Subject") })
                OutlinedTextField(style, { style = it }, label = { Text("Style and mood") })
                OutlinedTextField(exclusions, { exclusions = it }, label = { Text("Exclusions") })
            }
        },
    )
}

@Composable
private fun OptionsDialog(kind: MediaKind, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
        title = { Text("Advanced options") },
        text = { Text(if (kind == MediaKind.IMAGE) "Aspect ratio, resolution, seed, and negative prompt will appear here when supported by the selected model." else "Aspect ratio, resolution, duration, camera, and audio options will appear here when supported.") },
    )
}
