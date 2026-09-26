package com.higgsfield.mobile.feature.conversation

import android.animation.ValueAnimator
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.text.format.DateUtils
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.higgsfield.mobile.R
import com.higgsfield.mobile.core.model.CreativeBrief
import com.higgsfield.mobile.core.model.GenerationOutput
import com.higgsfield.mobile.core.model.GenerationOptions
import com.higgsfield.mobile.core.model.MediaKind
import com.higgsfield.mobile.core.model.MediaRole
import com.higgsfield.mobile.core.model.WorkflowDescriptor
import com.higgsfield.mobile.ui.theme.HiggsfieldTheme

@Composable
fun ConversationRoute(
    mediaKind: MediaKind,
    conversationId: String?,
    onBack: () -> Unit,
    onSelectMediaKind: (MediaKind) -> Unit,
    onSelectConversation: (String, MediaKind) -> Unit,
    viewModel: ConversationViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val shareChooserTitle = stringResource(R.string.share)
    LaunchedEffect(mediaKind, conversationId) { viewModel.initialize(mediaKind, conversationId) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    var pendingRole by remember { mutableStateOf<MediaRole?>(null) }
    var pendingDownload by remember { mutableStateOf<TimelineItem?>(null) }
    val outputDownload = rememberLauncherForActivityResult(CreateDocument("*/*")) { uri: Uri? ->
        pendingDownload?.let { item -> if (uri != null) viewModel.downloadOutput(item, uri.toString()) }
        pendingDownload = null
    }
    val imagePicker = rememberLauncherForActivityResult(PickVisualMedia()) { uri: Uri? ->
        val role = pendingRole
        if (uri != null && role != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            viewModel.attachMedia(role, MediaKind.IMAGE, uri.toString(), uri.lastPathSegment ?: uri.toString())
        }
        pendingRole = null
    }
    val videoPicker = rememberLauncherForActivityResult(OpenDocument()) { uri: Uri? ->
        val role = pendingRole
        if (uri != null && role != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            viewModel.attachMedia(role, MediaKind.VIDEO, uri.toString(), uri.lastPathSegment ?: uri.toString())
        }
        pendingRole = null
    }
    ConversationScreen(
        state = state,
        onBack = onBack,
        onSelectMediaKind = { kind -> viewModel.openMostRecentConversation(kind, onSelectConversation) },
        onSelectConversation = { id, kind -> viewModel.openConversation(id, kind, onSelectConversation) },
        onCreateConversation = { kind -> viewModel.createConversation(kind) { onSelectConversation(it, kind) } },
        onRenameConversation = viewModel::renameConversation,
        onRemoveConversation = { viewModel.removeCurrentConversation(onSelectConversation) },
        onPromptChange = viewModel::updatePrompt,
        onToggleModelMenu = viewModel::toggleModelMenu,
        onSelectWorkflow = viewModel::selectWorkflow,
        onShowInfo = viewModel::showInfo,
        onShowBrief = viewModel::showBrief,
        onShowOptions = viewModel::showOptions,
        onShowHistory = viewModel::showHistory,
        onUpdateBrief = viewModel::updateBrief,
        onUpdateOptions = viewModel::updateOptions,
        onPickMedia = { role, kind ->
            pendingRole = role
            when (kind) {
                MediaKind.IMAGE -> imagePicker.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
                MediaKind.VIDEO -> videoPicker.launch(arrayOf(VIDEO_MIME_TYPE))
                MediaKind.AUDIO -> Unit
            }
        },
        onRemoveMedia = viewModel::removeMedia,
        onDetachSource = viewModel::detachSource,
        onUseOutput = viewModel::useOutput,
        onGenerate = viewModel::submitGeneration,
        onRetryGeneration = viewModel::retryGeneration,
        onCancelGeneration = viewModel::cancelGeneration,
        onDownloadOutput = { item ->
            pendingDownload = item
            outputDownload.launch("higgsfield-${item.id}.${item.output?.downloadExtension() ?: "bin"}")
        },
        onCopyPrompt = { item -> clipboard.setText(AnnotatedString(item.prompt)) },
        onSharePrompt = { item ->
            context.startActivity(
                Intent.createChooser(
                    Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, item.prompt)
                    },
                    shareChooserTitle,
                ),
            )
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConversationScreen(
    state: ConversationUiState,
    onBack: () -> Unit,
    onSelectMediaKind: (MediaKind) -> Unit,
    onSelectConversation: (String, MediaKind) -> Unit,
    onCreateConversation: (MediaKind) -> Unit,
    onRenameConversation: (String) -> Unit,
    onRemoveConversation: () -> Unit,
    onPromptChange: (String) -> Unit,
    onToggleModelMenu: () -> Unit,
    onSelectWorkflow: (WorkflowDescriptor) -> Unit,
    onShowInfo: (Boolean) -> Unit,
    onShowHistory: (Boolean) -> Unit,
    onShowBrief: (Boolean) -> Unit,
    onShowOptions: (Boolean) -> Unit,
    onUpdateBrief: (CreativeBrief) -> Unit,
    onUpdateOptions: (GenerationOptions) -> Unit,
    onPickMedia: (MediaRole, MediaKind) -> Unit,
    onRemoveMedia: (MediaRole) -> Unit,
    onDetachSource: () -> Unit,
    onUseOutput: (TimelineItem) -> Unit,
    onGenerate: () -> Unit,
    onRetryGeneration: (TimelineItem) -> Unit,
    onCancelGeneration: (TimelineItem) -> Unit,
    onDownloadOutput: (TimelineItem) -> Unit,
    onCopyPrompt: (TimelineItem) -> Unit,
    onSharePrompt: (TimelineItem) -> Unit,
) {
    var pendingConversationSwitch by remember { mutableStateOf<Pair<String, MediaKind>?>(null) }
    var pendingRemoval by remember { mutableStateOf(false) }
    val requestConversationSwitch: (String, MediaKind) -> Unit = { id, kind ->
        if (id != state.conversationId) {
            pendingConversationSwitch = id to kind
        }
    }
    val requestConversationRemoval: () -> Unit = {
        if (!pendingRemoval) pendingRemoval = true
    }
    LaunchedEffect(pendingRemoval) {
        if (pendingRemoval) {
            // Let the confirmation dialog (and, on compact screens, the history sheet) leave
            // composition before showing the workspace transition.
            withFrameNanos { }
            pendingRemoval = false
            onRemoveConversation()
        }
    }
    LaunchedEffect(pendingConversationSwitch) {
        pendingConversationSwitch?.let { (id, kind) ->
            // A sheet or discard dialog must be gone before the transition overlay starts.
            withFrameNanos { }
            pendingConversationSwitch = null
            onSelectConversation(id, kind)
        }
    }
    Scaffold(
        topBar = {
            HeaderControls(
                state = state,
                onSelectMediaKind = onSelectMediaKind,
                onToggleModelMenu = onToggleModelMenu,
                onSelectWorkflow = onSelectWorkflow,
                onShowInfo = { onShowInfo(true) },
                onCreateConversation = { onCreateConversation(state.mediaKind) },
                onShowHistory = { onShowHistory(true) },
            )
        },
    ) { padding ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)) {
            if (maxWidth >= 840.dp) {
                Row(Modifier.fillMaxSize()) {
                    ConversationRail(state, requestConversationSwitch, onCreateConversation, onRenameConversation, requestConversationRemoval, Modifier.width(280.dp).fillMaxHeight())
                    HorizontalDivider(Modifier.fillMaxHeight().width(1.dp))
                    Workspace(state, onPromptChange, onShowBrief, onShowOptions, onPickMedia, onRemoveMedia, onDetachSource, onUseOutput, onGenerate, onRetryGeneration, onCancelGeneration, onDownloadOutput, onCopyPrompt, onSharePrompt, Modifier.weight(1f))
                }
            } else {
                Workspace(state, onPromptChange, onShowBrief, onShowOptions, onPickMedia, onRemoveMedia, onDetachSource, onUseOutput, onGenerate, onRetryGeneration, onCancelGeneration, onDownloadOutput, onCopyPrompt, onSharePrompt, Modifier.fillMaxSize())
            }
            if (state.isTransitioning) WorkspaceTransitionOverlay()
        }
    }

    if (state.infoOpen) ModelInfoDialog(state.selectedWorkflow, { onShowInfo(false) })
    if (state.optionsOpen) OptionsDialog(state.selectedWorkflow, state.options, { onShowOptions(false) }, onUpdateOptions)
    if (state.historyOpen) {
        ConversationHistorySheet(
            state = state,
            onSelectConversation = { id, kind ->
                onShowHistory(false)
                requestConversationSwitch(id, kind)
            },
            onCreateConversation = onCreateConversation,
            onRenameConversation = onRenameConversation,
            onRemoveConversation = {
                onShowHistory(false)
                requestConversationRemoval()
            },
            onDismiss = { onShowHistory(false) },
        )
    }
    if (state.briefOpen) CreativeBriefSheet(state.brief, { onShowBrief(false) }, onUpdateBrief)
}

@Composable
private fun WorkspaceTransitionOverlay() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.56f),
    ) {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Text(
                stringResource(R.string.preparing_workspace),
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(top = 16.dp),
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ConversationHistorySheet(
    state: ConversationUiState,
    onSelectConversation: (String, MediaKind) -> Unit,
    onCreateConversation: (MediaKind) -> Unit,
    onRenameConversation: (String) -> Unit,
    onRemoveConversation: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        ConversationHistoryContent(state, onSelectConversation, onCreateConversation, onRenameConversation, onRemoveConversation, Modifier.fillMaxWidth().padding(20.dp))
    }
}

@Composable
private fun HeaderControls(
    state: ConversationUiState,
    onSelectMediaKind: (MediaKind) -> Unit,
    onToggleModelMenu: () -> Unit,
    onSelectWorkflow: (WorkflowDescriptor) -> Unit,
    onShowInfo: () -> Unit,
    onCreateConversation: () -> Unit,
    onShowHistory: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 4.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onShowHistory) {
                    Icon(Icons.Rounded.Menu, stringResource(R.string.conversations), tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.weight(1f))
                Icon(
                    if (state.isOnline) Icons.Rounded.CloudDone else Icons.Rounded.CloudOff,
                    contentDescription = stringResource(if (state.isOnline) R.string.online else R.string.offline),
                    tint = if (state.isOnline) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp),
                )
                IconButton(onClick = onCreateConversation) {
                    Icon(Icons.Rounded.Add, stringResource(R.string.new_chat), tint = MaterialTheme.colorScheme.primary)
                }
            }
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                TextButton(onClick = { onSelectMediaKind(MediaKind.IMAGE) }) {
                    Text(stringResource(R.string.image), color = if (state.mediaKind == MediaKind.IMAGE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = { onSelectMediaKind(MediaKind.VIDEO) }) {
                    Text(stringResource(R.string.video), color = if (state.mediaKind == MediaKind.VIDEO) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Box(Modifier.weight(1f)) {
                    AssistChip(
                        onClick = onToggleModelMenu,
                        label = { Text(state.selectedWorkflow?.displayName ?: stringResource(R.string.choose_model), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    DropdownMenu(expanded = state.modelMenuOpen, onDismissRequest = onToggleModelMenu) {
                        state.workflows.forEach { workflow ->
                            DropdownMenuItem(
                                text = { Text(workflow.displayName) },
                                onClick = { onSelectWorkflow(workflow) },
                            )
                        }
                    }
                }
                IconButton(onClick = onShowInfo) {
                    Icon(Icons.Rounded.Info, stringResource(R.string.model_details_and_cost), tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun ConversationRail(
    state: ConversationUiState,
    onSelectConversation: (String, MediaKind) -> Unit,
    onCreateConversation: (MediaKind) -> Unit,
    onRenameConversation: (String) -> Unit,
    onRemoveConversation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)) {
        ConversationHistoryContent(state, onSelectConversation, onCreateConversation, onRenameConversation, onRemoveConversation, Modifier.padding(20.dp))
    }
}

@Composable
private fun ConversationHistoryContent(
    state: ConversationUiState,
    onSelectConversation: (String, MediaKind) -> Unit,
    onCreateConversation: (MediaKind) -> Unit,
    onRenameConversation: (String) -> Unit,
    onRemoveConversation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var renameOpen by remember { mutableStateOf(false) }
    var removeOpen by remember { mutableStateOf(false) }
    var renameValue by remember(state.conversationTitle) { mutableStateOf(state.conversationTitle) }
    Column(modifier) {
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
            confirmButton = { TextButton(onClick = { onRenameConversation(renameValue); renameOpen = false }) { Text(stringResource(R.string.done)) } },
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

@Composable
private fun Workspace(
    state: ConversationUiState,
    onPromptChange: (String) -> Unit,
    onShowBrief: (Boolean) -> Unit,
    onShowOptions: (Boolean) -> Unit,
    onPickMedia: (MediaRole, MediaKind) -> Unit,
    onRemoveMedia: (MediaRole) -> Unit,
    onDetachSource: () -> Unit,
    onUseOutput: (TimelineItem) -> Unit,
    onGenerate: () -> Unit,
    onRetryGeneration: (TimelineItem) -> Unit,
    onCancelGeneration: (TimelineItem) -> Unit,
    onDownloadOutput: (TimelineItem) -> Unit,
    onCopyPrompt: (TimelineItem) -> Unit,
    onSharePrompt: (TimelineItem) -> Unit,
    modifier: Modifier,
) {
    val motionEnabled = remember { Build.VERSION.SDK_INT < Build.VERSION_CODES.O || ValueAnimator.areAnimatorsEnabled() }
    Column(modifier) {
        WorkspaceTimeline(state, onUseOutput, onRetryGeneration, onCancelGeneration, onDownloadOutput, onCopyPrompt, onSharePrompt, Modifier.weight(1f))
        ComposerDock(state, motionEnabled, onPromptChange, onShowBrief, onShowOptions, onPickMedia, onRemoveMedia, onDetachSource, onGenerate)
    }
}

@Composable
private fun WorkspaceTimeline(
    state: ConversationUiState,
    onUseOutput: (TimelineItem) -> Unit,
    onRetryGeneration: (TimelineItem) -> Unit,
    onCancelGeneration: (TimelineItem) -> Unit,
    onDownloadOutput: (TimelineItem) -> Unit,
    onCopyPrompt: (TimelineItem) -> Unit,
    onSharePrompt: (TimelineItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (state.timeline.isEmpty()) item { EmptyConversation(state.mediaKind) }
        items(state.timeline, key = { it.id }, contentType = { it.lifecycle?.javaClass?.simpleName }) { item ->
            TimelineCard(item, item.id == state.activeSourceId, onUseOutput, onRetryGeneration, onCancelGeneration, onDownloadOutput, onCopyPrompt, onSharePrompt)
        }
    }
}

@Composable
private fun EmptyConversation(kind: MediaKind) {
    Column(Modifier.fillMaxWidth().padding(vertical = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Rounded.AutoAwesome, null, Modifier.size(44.dp), tint = MaterialTheme.colorScheme.primary)
        Text(stringResource(R.string.start_with_clear_idea), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 16.dp))
        Text(
            stringResource(if (kind == MediaKind.IMAGE) R.string.empty_image_conversation else R.string.empty_video_conversation),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

private fun GenerationOutput.downloadExtension(): String = when (kind) {
    MediaKind.IMAGE -> remoteUrl.substringBefore('?').substringAfterLast('.', "jpg").takeIf { it.length in 2..5 } ?: "jpg"
    MediaKind.VIDEO -> "mp4"
    MediaKind.AUDIO -> "mp3"
}

private const val VIDEO_MIME_TYPE = "video/*"

@Preview(showBackground = true, backgroundColor = 0xFF0D1316)
@Composable
private fun ConversationWorkspacePreview() {
    HiggsfieldTheme {
        Workspace(
            state = previewConversationState(),
            onPromptChange = {},
            onShowBrief = {},
            onShowOptions = {},
            onPickMedia = { _, _ -> },
            onRemoveMedia = {},
            onDetachSource = {},
            onUseOutput = {},
            onGenerate = {},
            onRetryGeneration = {},
            onCancelGeneration = {},
            onDownloadOutput = {},
            onCopyPrompt = {},
            onSharePrompt = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}
