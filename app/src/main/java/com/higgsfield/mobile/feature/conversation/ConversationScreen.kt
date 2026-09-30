package com.higgsfield.mobile.feature.conversation

import android.animation.ValueAnimator
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.activity.result.contract.ActivityResultContracts.OpenMultipleDocuments
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.higgsfield.mobile.R
import com.higgsfield.mobile.core.model.GenerationOutput
import com.higgsfield.mobile.core.model.MediaKind
import com.higgsfield.mobile.core.model.MediaRole
import com.higgsfield.mobile.core.model.WorkflowCapability
import com.higgsfield.mobile.ui.theme.HiggsfieldTheme
import kotlinx.coroutines.flow.collect

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
    LaunchedEffect(mediaKind, conversationId) { viewModel.initialize(mediaKind, conversationId) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    var pendingRole by remember { mutableStateOf<MediaRole?>(null) }
    var pendingDownload by remember { mutableStateOf<TimelineItem?>(null) }
    val outputDownload = rememberLauncherForActivityResult(CreateDocument("*/*")) { uri: Uri? ->
        pendingDownload?.let { item ->
            if (uri != null) {
                runCatching {
                    context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                viewModel.onEvent(ConversationUiEvent.DownloadDestinationSelected(item, uri.toString()))
            }
        }
        pendingDownload = null
    }
    val imagePicker = rememberLauncherForActivityResult(OpenMultipleDocuments()) { uris: List<Uri> ->
        val role = pendingRole
        if (role != null) {
            uris.forEach { uri ->
                runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                viewModel.onEvent(ConversationUiEvent.MediaPicked(role, MediaKind.IMAGE, uri.toString(), uri.lastPathSegment ?: uri.toString()))
            }
        }
        pendingRole = null
    }
    val videoPicker = rememberLauncherForActivityResult(OpenMultipleDocuments()) { uris: List<Uri> ->
        val role = pendingRole
        if (role != null) {
            uris.forEach { uri ->
                runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                viewModel.onEvent(ConversationUiEvent.MediaPicked(role, MediaKind.VIDEO, uri.toString(), uri.lastPathSegment ?: uri.toString()))
            }
        }
        pendingRole = null
    }
    val audioPicker = rememberLauncherForActivityResult(OpenMultipleDocuments()) { uris: List<Uri> ->
        val role = pendingRole
        if (role != null) {
            uris.forEach { uri ->
                runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                viewModel.onEvent(ConversationUiEvent.MediaPicked(role, MediaKind.AUDIO, uri.toString(), uri.lastPathSegment ?: uri.toString()))
            }
        }
        pendingRole = null
    }
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ConversationUiEffect.NavigateToConversation -> onSelectConversation(effect.id, effect.kind)
                is ConversationUiEffect.LaunchMediaPicker -> {
                    pendingRole = effect.role
                    when (effect.kind) {
                        MediaKind.IMAGE -> imagePicker.launch(arrayOf("image/*"))
                        MediaKind.VIDEO -> videoPicker.launch(arrayOf(VIDEO_MIME_TYPE))
                        MediaKind.AUDIO -> audioPicker.launch(arrayOf("audio/*"))
                    }
                }
                is ConversationUiEffect.LaunchDownload -> {
                    pendingDownload = effect.item
                    outputDownload.launch("higgsfield-${effect.item.id}.${effect.item.output?.downloadExtension() ?: "bin"}")
                }
                is ConversationUiEffect.CopyText -> clipboard.setText(AnnotatedString(effect.value))
            }
        }
    }
    ConversationScreen(state = state, onEvent = viewModel::onEvent)
}

@Composable
private fun ConversationScreen(
    state: ConversationUiState,
    onEvent: (ConversationUiEvent) -> Unit,
) {
    var pendingConversationSwitch by remember { mutableStateOf<Pair<String, MediaKind>?>(null) }
    var pendingConversationCreation by remember { mutableStateOf<MediaKind?>(null) }
    var pendingRemoval by remember { mutableStateOf(false) }
    var modelMenuOpen by rememberSaveable { mutableStateOf(false) }
    var infoOpen by rememberSaveable { mutableStateOf(false) }
    var historyOpen by rememberSaveable { mutableStateOf(false) }
    var briefOpen by rememberSaveable { mutableStateOf(false) }
    var optionsOpen by rememberSaveable { mutableStateOf(false) }
    val dispatch: (ConversationUiEvent) -> Unit = { event ->
        when (event) {
            ConversationUiEvent.ToggleModelMenu -> modelMenuOpen = !modelMenuOpen
            is ConversationUiEvent.ShowInfo -> infoOpen = event.show
            is ConversationUiEvent.ShowHistory -> historyOpen = event.show
            is ConversationUiEvent.ShowBrief -> briefOpen = event.show
            is ConversationUiEvent.ShowOptions -> optionsOpen = event.show
            is ConversationUiEvent.SelectWorkflow -> {
                modelMenuOpen = false
                onEvent(event)
            }
            else -> onEvent(event)
        }
    }
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
            onEvent(ConversationUiEvent.RemoveConversation)
        }
    }
    LaunchedEffect(pendingConversationSwitch) {
        pendingConversationSwitch?.let { (id, kind) ->
            // A sheet or discard dialog must be gone before the transition overlay starts.
            withFrameNanos { }
            pendingConversationSwitch = null
            onEvent(ConversationUiEvent.OpenConversation(id, kind))
        }
    }
    LaunchedEffect(pendingConversationCreation) {
        pendingConversationCreation?.let { kind ->
            withFrameNanos { }
            pendingConversationCreation = null
            onEvent(ConversationUiEvent.CreateConversation(kind))
        }
    }
    Scaffold(
        topBar = {
            ConversationHeader(state, dispatch, modelMenuOpen) { pendingConversationCreation = state.mediaKind }
        },
    ) { padding ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)) {
            if (maxWidth >= 840.dp) {
                Row(Modifier.fillMaxSize()) {
                    ConversationRail(state, requestConversationSwitch, { pendingConversationCreation = it }, requestConversationRemoval, dispatch, Modifier.width(280.dp).fillMaxHeight())
                    HorizontalDivider(Modifier.fillMaxHeight().width(1.dp))
                    Workspace(state, dispatch, Modifier.weight(1f))
                }
            } else {
                Workspace(state, dispatch, Modifier.fillMaxSize())
            }
            if (state.isTransitioning) WorkspaceTransitionOverlay()
        }
    }

    if (infoOpen) ModelInfoDialog(state.selectedWorkflow, { dispatch(ConversationUiEvent.ShowInfo(false)) })
    if (optionsOpen) OptionsDialog(state.selectedWorkflow, state.options, { dispatch(ConversationUiEvent.ShowOptions(false)) }, { dispatch(ConversationUiEvent.UpdateOptions(it)) })
    if (historyOpen) {
        ConversationHistorySheet(
            state = state,
            onSelectConversation = { id, kind ->
                dispatch(ConversationUiEvent.ShowHistory(false))
                requestConversationSwitch(id, kind)
            },
            onCreateConversation = { kind ->
                dispatch(ConversationUiEvent.ShowHistory(false))
                pendingConversationCreation = kind
            },
            onRemoveConversation = {
                dispatch(ConversationUiEvent.ShowHistory(false))
                requestConversationRemoval()
            },
            onDismiss = { dispatch(ConversationUiEvent.ShowHistory(false)) },
            onEvent = dispatch,
        )
    }
    if (briefOpen) CreativeBriefSheet(state.brief, { dispatch(ConversationUiEvent.ShowBrief(false)) }, { dispatch(ConversationUiEvent.UpdateBrief(it)) })
    if (state.isSubmitting && !state.isTransitioning) SubmissionOverlay()
}

@Composable
private fun SubmissionOverlay() {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
    ) {
        Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface) {
            Column(
                Modifier.fillMaxWidth().padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                CircularProgressIndicator()
                Text(stringResource(R.string.submitting_request), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.waiting_for_request_acceptance),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
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
private fun Workspace(
    state: ConversationUiState,
    onEvent: (ConversationUiEvent) -> Unit,
    modifier: Modifier,
) {
    val motionEnabled = remember { Build.VERSION.SDK_INT < Build.VERSION_CODES.O || ValueAnimator.areAnimatorsEnabled() }
    Column(modifier) {
        WorkspaceTimeline(state, onEvent, Modifier.weight(1f))
        ComposerDock(state, motionEnabled, onEvent)
    }
}

@Composable
private fun WorkspaceTimeline(
    state: ConversationUiState,
    onEvent: (ConversationUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val latestId = state.timeline.lastOrNull()?.id
    LaunchedEffect(state.conversationId, latestId) {
        if (latestId != null) listState.animateScrollToItem(state.timeline.lastIndex)
    }
    LazyColumn(modifier.fillMaxWidth(), state = listState, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (state.timeline.isEmpty()) item { EmptyConversation(state.mediaKind) }
        items(state.timeline, key = { it.id }, contentType = { it.lifecycle?.javaClass?.simpleName }) { item ->
            TimelineCard(
                item, item.id == state.activeSourceId,
                WorkflowCapability.IMAGE_TO_IMAGE in state.selectedWorkflow?.capabilities.orEmpty(),
                onEvent,
            )
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
            onEvent = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}
