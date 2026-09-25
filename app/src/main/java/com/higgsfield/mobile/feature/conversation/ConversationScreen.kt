package com.higgsfield.mobile.feature.conversation

import android.net.Uri
import android.animation.ValueAnimator
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.GetContent
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.foundation.background
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
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
import androidx.compose.material.icons.rounded.CloudOff
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.higgsfield.mobile.R
import com.higgsfield.mobile.core.model.CreativeBrief
import com.higgsfield.mobile.core.model.GenerationOutput
import com.higgsfield.mobile.core.model.GenerationOptions
import com.higgsfield.mobile.core.model.GenerationStatus
import com.higgsfield.mobile.core.model.MediaKind
import com.higgsfield.mobile.core.model.MediaRole
import com.higgsfield.mobile.core.model.WorkflowDescriptor
import com.higgsfield.mobile.core.model.WorkflowOption
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage

@Composable
fun ConversationRoute(
    mediaKind: MediaKind,
    onBack: () -> Unit,
    viewModel: ConversationViewModel = hiltViewModel(),
) {
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
    onUpdateOptions: (GenerationOptions) -> Unit,
    onPickMedia: (MediaRole, MediaKind) -> Unit,
    onRemoveMedia: (MediaRole) -> Unit,
    onDetachSource: () -> Unit,
    onUseOutput: (TimelineItem) -> Unit,
    onGenerate: () -> Unit,
    onRetryGeneration: (TimelineItem) -> Unit,
    onCancelGeneration: (TimelineItem) -> Unit,
    onDownloadOutput: (TimelineItem) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(if (state.mediaKind == MediaKind.IMAGE) R.string.image_studio else R.string.video_studio),
                        modifier = Modifier.semantics { heading() },
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back)) }
                },
                actions = {
                    Icon(
                        if (state.isOnline) Icons.Rounded.CloudDone else Icons.Rounded.CloudOff,
                        contentDescription = stringResource(if (state.isOnline) R.string.online else R.string.offline),
                        tint = if (state.isOnline) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
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
                    Workspace(state, onPromptChange, onToggleModelMenu, onSelectWorkflow, onShowInfo, onShowBrief, onShowOptions, onPickMedia, onRemoveMedia, onDetachSource, onUseOutput, onGenerate, onRetryGeneration, onCancelGeneration, onDownloadOutput, Modifier.weight(1f))
                }
            } else {
                Workspace(state, onPromptChange, onToggleModelMenu, onSelectWorkflow, onShowInfo, onShowBrief, onShowOptions, onPickMedia, onRemoveMedia, onDetachSource, onUseOutput, onGenerate, onRetryGeneration, onCancelGeneration, onDownloadOutput, Modifier.fillMaxSize())
            }
        }
    }

    if (state.infoOpen) ModelInfoDialog(state.selectedWorkflow, { onShowInfo(false) })
    if (state.briefOpen) BriefDialog(state.brief, { onShowBrief(false) }, onUpdateBrief)
    if (state.optionsOpen) OptionsDialog(state.selectedWorkflow, state.options, { onShowOptions(false) }, onUpdateOptions)
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
    onToggleModelMenu: () -> Unit,
    onSelectWorkflow: (WorkflowDescriptor) -> Unit,
    onShowInfo: (Boolean) -> Unit,
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
    modifier: Modifier,
) {
    var attachmentMenuOpen by remember { mutableStateOf(false) }
    val motionEnabled = remember { Build.VERSION.SDK_INT < Build.VERSION_CODES.O || ValueAnimator.areAnimatorsEnabled() }
    Column(modifier) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box {
                AssistChip(onClick = onToggleModelMenu, label = { Text(state.selectedWorkflow?.displayName ?: stringResource(R.string.choose_model)) })
                DropdownMenu(expanded = state.modelMenuOpen, onDismissRequest = onToggleModelMenu) {
                    state.workflows.forEach { workflow ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(workflow.displayName)
                                    Text(
                                        stringResource(
                                            if (workflow.isSubmissionEnabled) R.string.adapter_verified
                                            else R.string.adapter_verification_pending,
                                        ),
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                }
                            },
                            onClick = { onSelectWorkflow(workflow) },
                        )
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            AssistChip(
                onClick = { onShowInfo(true) },
                label = { Text(state.selectedWorkflow?.staticEstimate?.fromPrice?.let { stringResource(R.string.estimate_from, it) } ?: stringResource(R.string.pricing_unavailable)) },
            )
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
            items(state.timeline, key = { it.id }, contentType = { it.lifecycle?.javaClass?.simpleName }) { item ->
                TimelineCard(item, item.id == state.activeSourceId, onUseOutput, onRetryGeneration, onCancelGeneration, onDownloadOutput)
            }
        }
        Column(
            (if (motionEnabled) Modifier.animateContentSize() else Modifier)
                .fillMaxWidth().imePadding().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            state.message?.let { message ->
                Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(12.dp)) {
                    Text(message.resolve(), Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
                }
            }
            if (state.activeSourceId != null) ActiveSourceCard(state.activeSourceLabel?.resolve().orEmpty(), onDetachSource)
            if (motionEnabled) {
                AnimatedVisibility(state.attachments.isNotEmpty()) { AttachmentList(state.attachments, onRemoveMedia) }
            } else if (state.attachments.isNotEmpty()) AttachmentList(state.attachments, onRemoveMedia)
            OutlinedTextField(
                value = state.prompt,
                onValueChange = onPromptChange,
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 5,
                placeholder = { Text(stringResource(if (state.activeSourceId != null) R.string.describe_change else R.string.describe_creation)) },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (state.attachmentSlots.isNotEmpty()) Box {
                    IconButton(onClick = { attachmentMenuOpen = true }) {
                        Icon(Icons.Rounded.AddPhotoAlternate, stringResource(R.string.attach_media))
                    }
                    DropdownMenu(expanded = attachmentMenuOpen, onDismissRequest = { attachmentMenuOpen = false }) {
                        state.attachmentSlots.forEach { slot ->
                            DropdownMenuItem(
                                text = { Text(attachmentRoleText(slot.role)) },
                                onClick = {
                                    attachmentMenuOpen = false
                                    onPickMedia(slot.role, slot.kind)
                                },
                            )
                        }
                    }
                }
                if (state.selectedWorkflow?.supportedOptions?.isNotEmpty() == true) IconButton(onClick = { onShowOptions(true) }) { Icon(Icons.Rounded.Tune, stringResource(R.string.advanced_options_description)) }
                IconButton(onClick = { onShowBrief(true) }) { Icon(Icons.Rounded.Info, stringResource(R.string.creative_brief)) }
                Spacer(Modifier.weight(1f))
                Button(onClick = onGenerate, enabled = state.prompt.isNotBlank() && state.isOnline && !state.isSubmitting) {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null)
                    Text(stringResource(if (state.isSubmitting) R.string.generating else R.string.generate), Modifier.padding(start = 8.dp))
                }
            }
            if (!state.credentialsConfigured) {
                Text(stringResource(R.string.api_credentials_not_configured), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun DraftAttachmentCard(attachment: DraftMediaAttachment, onRemove: (MediaRole) -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(12.dp)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(attachmentRoleText(attachment.role), style = MaterialTheme.typography.labelLarge)
                Text(attachment.label, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = { onRemove(attachment.role) }) {
                Icon(Icons.Rounded.Close, stringResource(R.string.remove_attachment))
            }
        }
    }
}

@Composable
private fun AttachmentList(attachments: List<DraftMediaAttachment>, onRemove: (MediaRole) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        attachments.forEach { attachment -> DraftAttachmentCard(attachment, onRemove) }
    }
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

@Composable
private fun TimelineCard(
    item: TimelineItem,
    active: Boolean,
    onUseOutput: (TimelineItem) -> Unit,
    onRetry: (TimelineItem) -> Unit,
    onCancel: (TimelineItem) -> Unit,
    onDownload: (TimelineItem) -> Unit,
) {
    val hasOutput = item.lifecycle is GenerationStatus.Completed && item.outputLabel != null
    val isFailure = item.lifecycle is GenerationStatus.Failed ||
        item.lifecycle is GenerationStatus.Nsfw || item.lifecycle is GenerationStatus.UnknownSubmissionOutcome
    val statusDescription = stringResource(R.string.generation_status_content_description, item.modelName, item.stateLabel.resolve())
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.align(Alignment.End)) {
            Text(item.prompt, Modifier.padding(14.dp))
        }
        Card(
            modifier = Modifier.fillMaxWidth().semantics {
                contentDescription = statusDescription
            },
        ) {
            Box(
                Modifier.fillMaxWidth().height(if (hasOutput) 180.dp else 104.dp).background(
                    Brush.linearGradient(
                        if (isFailure) listOf(MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.surfaceVariant)
                        else listOf(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.primaryContainer),
                    )
                ),
                contentAlignment = Alignment.Center,
            ) {
                if (hasOutput) {
                    item.output?.let { GenerationOutputPreview(it) } ?: Text(
                        item.outputLabel?.resolve().orEmpty(),
                        style = MaterialTheme.typography.titleLarge,
                    )
                } else {
                    Text(item.stateLabel.resolve(), style = MaterialTheme.typography.titleLarge)
                }
            }
            if (item.lifecycle is GenerationStatus.InProgress) {
                val progress = (item.lifecycle as GenerationStatus.InProgress).progress
                if (progress == null) LinearProgressIndicator(Modifier.fillMaxWidth())
                else LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
            }
            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(item.modelName, fontWeight = FontWeight.SemiBold)
                    Text(item.stateLabel.resolve(), style = MaterialTheme.typography.bodySmall)
                    if (hasOutput && item.output?.localUri == null) {
                        Text(stringResource(R.string.output_temporary), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    item.errorText?.let { Text(it.resolve(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
                }
                when {
                    hasOutput && active -> Text(stringResource(R.string.editing_source), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                    hasOutput -> Row {
                        TextButton(onClick = { onDownload(item) }) { Text(stringResource(R.string.download)) }
                        TextButton(onClick = { onUseOutput(item) }) { Text(stringResource(R.string.use_this)) }
                    }
                    item.canRetry -> TextButton(onClick = { onRetry(item) }) { Text(stringResource(R.string.retry)) }
                    item.canCancel -> TextButton(onClick = { onCancel(item) }) { Text(stringResource(R.string.cancel_generation)) }
                }
            }
        }
    }
}

@Composable
private fun GenerationOutputPreview(output: GenerationOutput) {
    when (output.kind) {
        MediaKind.IMAGE -> AsyncImage(
            model = output.localUri ?: output.remoteUrl,
            contentDescription = stringResource(R.string.generated_image),
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        MediaKind.VIDEO, MediaKind.AUDIO -> MediaOutputPlayer(output)
    }
}

@Composable
private fun MediaOutputPlayer(output: GenerationOutput) {
    val context = LocalContext.current
    val player = remember(output.id, output.localUri, output.remoteUrl) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(output.localUri ?: output.remoteUrl))
            prepare()
        }
    }
    DisposableEffect(player) { onDispose(player::release) }
    AndroidView(
        factory = { PlayerView(it).apply { this.player = player; useController = true } },
        update = { it.player = player },
        modifier = Modifier.fillMaxSize(),
    )
}

private const val OUTPUT_DOCUMENT_MIME_TYPE = "application/octet-stream"

@Composable
private fun ActiveSourceCard(label: String, onDetach: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(58.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp)))
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(stringResource(R.string.editing_this_image), fontWeight = FontWeight.Bold)
                Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onDetach) { Icon(Icons.Rounded.Close, stringResource(R.string.detach_active_image)) }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ModelInfoDialog(workflow: WorkflowDescriptor?, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(workflow?.displayName ?: stringResource(R.string.model), style = MaterialTheme.typography.titleLarge)
            val capabilities = workflow?.capabilities?.joinToString { it.name.lowercase().replace('_', ' ') } ?: stringResource(R.string.unknown)
            Text(stringResource(R.string.capabilities, capabilities))
            val estimate = workflow?.staticEstimate
            Text(estimate?.fromPrice?.let { stringResource(R.string.estimate_from, it) } ?: stringResource(R.string.pricing_unavailable), style = MaterialTheme.typography.titleMedium)
            Text(estimate?.creditGuidance ?: stringResource(R.string.estimate_credits_unavailable))
            Text(estimate?.expectedLatency ?: stringResource(R.string.estimate_latency_unavailable))
            estimate?.let {
                Text(stringResource(R.string.estimate_verified, it.verifiedOn), style = MaterialTheme.typography.labelSmall)
                Text(stringResource(R.string.estimate_source, it.sourceUrl), style = MaterialTheme.typography.labelSmall)
            }
            if (workflow?.isSubmissionEnabled == false) Text(stringResource(R.string.adapter_not_enabled), color = MaterialTheme.colorScheme.error)
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text(stringResource(R.string.done)) }
        }
    }
}

@Composable
private fun BriefDialog(initial: CreativeBrief, onDismiss: () -> Unit, onSave: (CreativeBrief) -> Unit) {
    var subject by remember(initial) { mutableStateOf(initial.subject) }
    var style by remember(initial) { mutableStateOf(initial.style) }
    var exclusions by remember(initial) { mutableStateOf(initial.exclusions) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onSave(initial.copy(subject = subject, style = style, exclusions = exclusions)) }) { Text(stringResource(R.string.save_brief)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
        title = { Text(stringResource(R.string.pinned_creative_brief)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.brief_explanation))
                OutlinedTextField(subject, { subject = it }, label = { Text(stringResource(R.string.subject)) })
                OutlinedTextField(style, { style = it }, label = { Text(stringResource(R.string.style_and_mood)) })
                OutlinedTextField(exclusions, { exclusions = it }, label = { Text(stringResource(R.string.exclusions)) })
            }
        },
    )
}

@Composable
private fun OptionsDialog(
    workflow: WorkflowDescriptor?,
    initial: GenerationOptions,
    onDismiss: () -> Unit,
    onUpdateOptions: (GenerationOptions) -> Unit,
) {
    var aspectRatio by remember(initial) { mutableStateOf(initial.aspectRatio) }
    var resolution by remember(initial) { mutableStateOf(initial.resolution.orEmpty()) }
    var seed by remember(initial) { mutableStateOf(initial.seed?.toString().orEmpty()) }
    var negativePrompt by remember(initial) { mutableStateOf(initial.negativePrompt.orEmpty()) }
    val supported = workflow?.supportedOptions.orEmpty()
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = {
            onUpdateOptions(GenerationOptions(
                aspectRatio = aspectRatio,
                resolution = resolution.ifBlank { null },
                seed = seed.toLongOrNull(),
                negativePrompt = negativePrompt.ifBlank { null },
            ))
            onDismiss()
        }) { Text(stringResource(R.string.done)) } },
        title = { Text(stringResource(R.string.advanced_options_description)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (WorkflowOption.ASPECT_RATIO in supported) OutlinedTextField(aspectRatio, { aspectRatio = it }, label = { Text(stringResource(R.string.aspect_ratio)) })
                if (WorkflowOption.RESOLUTION in supported) OutlinedTextField(resolution, { resolution = it }, label = { Text(stringResource(R.string.resolution)) })
                if (WorkflowOption.SEED in supported) OutlinedTextField(seed, { seed = it }, label = { Text(stringResource(R.string.seed)) })
                if (WorkflowOption.NEGATIVE_PROMPT in supported) OutlinedTextField(negativePrompt, { negativePrompt = it }, label = { Text(stringResource(R.string.negative_prompt)) })
                if (supported.isEmpty()) Text(stringResource(R.string.no_model_settings))
            }
        },
    )
}

private const val VIDEO_MIME_TYPE = "video/*"
