package com.promptstudio.app.feature.conversation

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage
import com.promptstudio.app.R
import com.promptstudio.app.core.model.GenerationAttachment
import com.promptstudio.app.core.model.GenerationOutput
import com.promptstudio.app.core.model.GenerationStatus
import com.promptstudio.app.core.model.MediaKind
import com.promptstudio.app.ui.theme.PromptStudioTheme

@Preview(showBackground = true, backgroundColor = 0xFF0D1316)
@Composable
private fun GenerationCardPreview() {
    PromptStudioTheme {
        TimelineCard(previewGenerationItem(), false, true, {})
    }
}

@Composable
internal fun TimelineCard(
    item: TimelineItem,
    active: Boolean,
    canEditImage: Boolean,
    onEvent: (ConversationUiEvent) -> Unit,
) {
    val hasOutput = item.lifecycle is GenerationStatus.Completed && item.outputLabel != null
    val isFailure =
        item.lifecycle is GenerationStatus.Failed || item.lifecycle is GenerationStatus.Nsfw || item.lifecycle is GenerationStatus.UnknownSubmissionOutcome
    val statusDescription = stringResource(
        R.string.generation_status_content_description,
        item.modelName,
        item.stateLabel.resolve()
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PromptBubble(item.prompt, Modifier.align(Alignment.End))
        val references = item.record?.draft?.attachments.orEmpty()
        if (references.isNotEmpty()) SubmittedReferences(references)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = statusDescription }) {
            GenerationMediaSurface(item, hasOutput, isFailure)
            GenerationProgress(item.lifecycle)
            GenerationMetadata(item, hasOutput, active, { onEvent(ConversationUiEvent.Retry(it)) }, { onEvent(ConversationUiEvent.Cancel(it)) })
            if (hasOutput) GenerationActions(
                item,
                { onEvent(ConversationUiEvent.Download(it)) },
                { onEvent(ConversationUiEvent.CopyPrompt(it)) },
                { onEvent(ConversationUiEvent.EditImage(it)) },
                { onEvent(ConversationUiEvent.ReuseParameters(it)) },
                canEditImage,
            )
        }
    }
}

@Composable
private fun SubmittedReferences(references: List<GenerationAttachment>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            stringResource(R.string.submitted_references),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            references.forEach { reference ->
                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                    Row(Modifier.padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (reference.kind == MediaKind.IMAGE) AsyncImage(
                            model = reference.uri.takeIf(String::isNotBlank) ?: reference.remoteUrl,
                            contentDescription = attachmentRoleText(reference.role),
                            modifier = Modifier.size(48.dp),
                            contentScale = ContentScale.Crop,
                        ) else Icon(
                            Icons.Rounded.AutoAwesome,
                            contentDescription = attachmentRoleText(reference.role),
                            modifier = Modifier.size(48.dp),
                        )
                        Text(
                            attachmentRoleText(reference.role),
                            modifier = Modifier.padding(horizontal = 8.dp),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PromptBubble(prompt: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = modifier
    ) {
        SelectionContainer { Text(prompt, Modifier.padding(14.dp)) }
    }
}

@Composable
private fun GenerationMediaSurface(item: TimelineItem, hasOutput: Boolean, isFailure: Boolean) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(if (hasOutput) 220.dp else 156.dp)
            .background(
                Brush.linearGradient(
                    if (isFailure) listOf(
                        MaterialTheme.colorScheme.errorContainer,
                        MaterialTheme.colorScheme.surfaceVariant
                    ) else listOf(
                        MaterialTheme.colorScheme.secondaryContainer,
                        MaterialTheme.colorScheme.primaryContainer
                    )
                )
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (hasOutput) item.output?.let { GenerationOutputPreview(it) } else GenerationPlaceholder(
            item.stateLabel.resolve(),
            item.lifecycle is GenerationStatus.Queued || item.lifecycle is GenerationStatus.InProgress,
        )
        if (hasOutput && item.output == null) Text(
            item.outputLabel?.resolve().orEmpty(),
            style = MaterialTheme.typography.titleLarge
        )
    }
}

@Composable
private fun GenerationPlaceholder(label: String, isLoading: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (isLoading) CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(32.dp)
        )
        else Icon(
            Icons.Rounded.AutoAwesome,
            null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(32.dp)
        )
        Text(label, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun GenerationProgress(lifecycle: GenerationStatus?) {
    val inProgress = lifecycle as? GenerationStatus.InProgress ?: return
    if (inProgress.progress == null) LinearProgressIndicator(Modifier.fillMaxWidth()) else LinearProgressIndicator(
        progress = { inProgress.progress },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun GenerationMetadata(
    item: TimelineItem,
    hasOutput: Boolean,
    active: Boolean,
    onRetry: (TimelineItem) -> Unit,
    onCancel: (TimelineItem) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            SelectionContainer {
                Column {
                    Text(item.modelName, fontWeight = FontWeight.SemiBold)
                    Text(item.stateLabel.resolve(), style = MaterialTheme.typography.bodySmall)
                    if (hasOutput && item.output?.localUri == null) Text(
                        stringResource(R.string.output_temporary),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    item.errorText?.let {
                        Text(
                            it.resolve(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
        when {
            hasOutput && active -> Text(
                stringResource(R.string.editing_source),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge
            )

            item.canRetry -> TextButton(onClick = { onRetry(item) }) { Text(stringResource(R.string.retry)) }
            item.canCancel -> TextButton(onClick = { onCancel(item) }) { Text(stringResource(R.string.cancel_generation)) }
        }
    }
}

@Composable
private fun GenerationActions(
    item: TimelineItem,
    onDownload: (TimelineItem) -> Unit,
    onCopyPrompt: (TimelineItem) -> Unit,
    onUseOutput: (TimelineItem) -> Unit,
    onReuseParameters: (TimelineItem) -> Unit,
    canEditImage: Boolean,
) {
    HorizontalDivider()
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = { onDownload(item) }) { Text(stringResource(R.string.download), maxLines = 1) }
        TextButton(onClick = { onCopyPrompt(item) }, enabled = item.prompt.isNotBlank()) {
            Icon(
                Icons.Rounded.ContentCopy,
                null,
                modifier = Modifier.size(16.dp)
            ); Text(stringResource(R.string.copy_prompt), Modifier.padding(start = 4.dp), maxLines = 1)
        }
    }
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(
                horizontal = 4.dp,
                vertical = 4.dp
            ),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (item.output?.kind == MediaKind.IMAGE && canEditImage) {
            TextButton(onClick = { onUseOutput(item) }) { Text(stringResource(R.string.edit_image), maxLines = 1) }
        }
        if (item.record != null) {
            TextButton(onClick = { onReuseParameters(item) }) { Text(stringResource(R.string.reuse_parameters), maxLines = 1) }
        }
    }
}

@Composable
private fun GenerationOutputPreview(output: GenerationOutput) {
    val context = LocalContext.current
    val previewUri = remember(output.localUri, output.remoteUrl) {
        output.previewUri(context.contentResolver.persistedUriPermissions
            .filter { it.isReadPermission }
            .map { it.uri.toString() }
            .toSet())
    }
    when (output.kind) {
        MediaKind.IMAGE -> ImageOutputPreview(previewUri)

        MediaKind.VIDEO, MediaKind.AUDIO -> MediaOutputPlayer(output, previewUri)
    }
}

private enum class MediaLoadState { Loading, Ready, Failed }

@Composable
private fun ImageOutputPreview(previewUri: String) {
    var attempt by remember(previewUri) { mutableIntStateOf(0) }
    var loadState by remember(previewUri, attempt) { mutableStateOf(MediaLoadState.Loading) }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        key(attempt) {
            AsyncImage(
                model = previewUri,
                contentDescription = stringResource(R.string.generated_image),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                onLoading = { loadState = MediaLoadState.Loading },
                onSuccess = { loadState = MediaLoadState.Ready },
                onError = { loadState = MediaLoadState.Failed },
            )
        }
        MediaLoadOverlay(loadState) { attempt++ }
    }
}

@Composable
private fun MediaOutputPlayer(output: GenerationOutput, previewUri: String) {
    val context = LocalContext.current
    var attempt by remember(output.id, previewUri) { mutableIntStateOf(0) }
    var loadState by remember(output.id, previewUri, attempt) { mutableStateOf(MediaLoadState.Loading) }
    val exoPlayer = remember(output.id, previewUri, attempt) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(previewUri))
            prepare()
        }
    }
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                loadState = if (playbackState == Player.STATE_READY) MediaLoadState.Ready else MediaLoadState.Loading
            }
            override fun onPlayerError(error: PlaybackException) { loadState = MediaLoadState.Failed }
        }
        exoPlayer.addListener(listener)
        loadState = when {
            exoPlayer.playerError != null -> MediaLoadState.Failed
            exoPlayer.playbackState == Player.STATE_READY -> MediaLoadState.Ready
            else -> MediaLoadState.Loading
        }
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AndroidView(
            factory = { PlayerView(it).apply { player = exoPlayer; useController = true } },
            update = { it.player = exoPlayer },
            modifier = Modifier.fillMaxSize(),
        )
        MediaLoadOverlay(loadState) { attempt++ }
    }
}

@Composable
private fun MediaLoadOverlay(state: MediaLoadState, onRetry: () -> Unit) {
    when (state) {
        MediaLoadState.Loading -> CircularProgressIndicator(modifier = Modifier.size(32.dp))
        MediaLoadState.Ready -> Unit
        MediaLoadState.Failed -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.media_unavailable), color = MaterialTheme.colorScheme.onSurface)
            TextButton(onClick = onRetry) { Text(stringResource(R.string.retry)) }
        }
    }
}

internal fun GenerationOutput.previewUri(persistedReadUris: Set<String>): String =
    localUri?.takeIf(persistedReadUris::contains) ?: remoteUrl
