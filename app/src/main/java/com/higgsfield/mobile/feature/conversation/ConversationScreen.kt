package com.higgsfield.mobile.feature.conversation

import android.animation.ValueAnimator
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.activity.result.contract.ActivityResultContracts.GetContent
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.higgsfield.mobile.core.model.GenerationOptions
import com.higgsfield.mobile.core.model.MediaKind
import com.higgsfield.mobile.core.model.MediaRole
import com.higgsfield.mobile.core.model.WorkflowDescriptor
import com.higgsfield.mobile.ui.theme.HiggsfieldTheme

@Composable
fun ConversationRoute(
    mediaKind: MediaKind,
    onBack: () -> Unit,
    onSelectMediaKind: (MediaKind) -> Unit,
    viewModel: ConversationViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val shareChooserTitle = stringResource(R.string.share)
    LaunchedEffect(mediaKind) { viewModel.initialize(mediaKind) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    var pendingRole by remember { mutableStateOf<MediaRole?>(null) }
    var pendingDownload by remember { mutableStateOf<TimelineItem?>(null) }
    val outputDownload = rememberLauncherForActivityResult(CreateDocument(OUTPUT_DOCUMENT_MIME_TYPE)) { uri: Uri? ->
        pendingDownload?.let { item -> if (uri != null) viewModel.downloadOutput(item, uri.toString()) }
        pendingDownload = null
    }
    val imagePicker = rememberLauncherForActivityResult(PickVisualMedia()) { uri: Uri? ->
        val role = pendingRole
        if (uri != null && role != null) {
            viewModel.attachMedia(role, MediaKind.IMAGE, uri.toString(), uri.lastPathSegment ?: uri.toString())
        }
        pendingRole = null
    }
    val videoPicker = rememberLauncherForActivityResult(GetContent()) { uri: Uri? ->
        val role = pendingRole
        if (uri != null && role != null) {
            viewModel.attachMedia(role, MediaKind.VIDEO, uri.toString(), uri.lastPathSegment ?: uri.toString())
        }
        pendingRole = null
    }
    ConversationScreen(
        state = state,
        onBack = onBack,
        onSelectMediaKind = onSelectMediaKind,
        onPromptChange = viewModel::updatePrompt,
        onToggleModelMenu = viewModel::toggleModelMenu,
        onSelectWorkflow = viewModel::selectWorkflow,
        onShowInfo = viewModel::showInfo,
        onShowBrief = viewModel::showBrief,
        onShowOptions = viewModel::showOptions,
        onUpdateBrief = viewModel::updateBrief,
        onUpdateOptions = viewModel::updateOptions,
        onPickMedia = { role, kind ->
            pendingRole = role
            when (kind) {
                MediaKind.IMAGE -> imagePicker.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
                MediaKind.VIDEO -> videoPicker.launch(VIDEO_MIME_TYPE)
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
            outputDownload.launch("higgsfield-${item.id}")
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
    onPromptChange: (String) -> Unit,
    onToggleModelMenu: () -> Unit,
    onSelectWorkflow: (WorkflowDescriptor) -> Unit,
    onShowInfo: (Boolean) -> Unit,
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
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    HeaderControls(
                        state = state,
                        onSelectMediaKind = onSelectMediaKind,
                        onToggleModelMenu = onToggleModelMenu,
                        onSelectWorkflow = onSelectWorkflow,
                        onShowInfo = { onShowInfo(true) },
                    )
                },
            )
        },
    ) { padding ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)) {
            if (maxWidth >= 840.dp) {
                Row(Modifier.fillMaxSize()) {
                    ConversationRail(state, Modifier.width(280.dp).fillMaxHeight())
                    HorizontalDivider(Modifier.fillMaxHeight().width(1.dp))
                    Workspace(state, onPromptChange, onShowOptions, onPickMedia, onRemoveMedia, onDetachSource, onUseOutput, onGenerate, onRetryGeneration, onCancelGeneration, onDownloadOutput, onCopyPrompt, onSharePrompt, Modifier.weight(1f))
                }
            } else {
                Workspace(state, onPromptChange, onShowOptions, onPickMedia, onRemoveMedia, onDetachSource, onUseOutput, onGenerate, onRetryGeneration, onCancelGeneration, onDownloadOutput, onCopyPrompt, onSharePrompt, Modifier.fillMaxSize())
            }
        }
    }

    if (state.infoOpen) ModelInfoDialog(state.selectedWorkflow, { onShowInfo(false) })
    if (state.optionsOpen) OptionsDialog(state.selectedWorkflow, state.options, { onShowOptions(false) }, onUpdateOptions)
}

@Composable
private fun HeaderControls(
    state: ConversationUiState,
    onSelectMediaKind: (MediaKind) -> Unit,
    onToggleModelMenu: () -> Unit,
    onSelectWorkflow: (WorkflowDescriptor) -> Unit,
    onShowInfo: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
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
        Icon(
            if (state.isOnline) Icons.Rounded.CloudDone else Icons.Rounded.CloudOff,
            contentDescription = stringResource(if (state.isOnline) R.string.online else R.string.offline),
            tint = if (state.isOnline) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun ConversationRail(state: ConversationUiState, modifier: Modifier = Modifier) {
    Surface(modifier, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)) {
        Column(Modifier.padding(20.dp)) {
            Text(stringResource(R.string.conversations), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.today), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 28.dp, bottom = 8.dp))
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Column(Modifier.padding(16.dp)) {
                    Text(stringResource(if (state.mediaKind == MediaKind.IMAGE) R.string.image_studio else R.string.video_studio), fontWeight = FontWeight.SemiBold)
                    Text(stringResource(R.string.current_local_draft), style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(Modifier.weight(1f))
            Text(stringResource(R.string.versions), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.local_demo_versions, state.timeline.size), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun Workspace(
    state: ConversationUiState,
    onPromptChange: (String) -> Unit,
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
        ComposerDock(state, motionEnabled, onPromptChange, onShowOptions, onPickMedia, onRemoveMedia, onDetachSource, onGenerate)
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

private const val OUTPUT_DOCUMENT_MIME_TYPE = "application/octet-stream"

private const val VIDEO_MIME_TYPE = "video/*"

@Preview(showBackground = true, backgroundColor = 0xFF0D1316)
@Composable
private fun ConversationWorkspacePreview() {
    HiggsfieldTheme {
        Workspace(
            state = previewConversationState(),
            onPromptChange = {},
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
