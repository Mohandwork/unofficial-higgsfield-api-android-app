package com.higgsfield.mobile.feature.conversation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Share
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.tooling.preview.Preview
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage
import com.higgsfield.mobile.R
import com.higgsfield.mobile.core.model.GenerationOutput
import com.higgsfield.mobile.core.model.GenerationStatus
import com.higgsfield.mobile.core.model.MediaKind
import com.higgsfield.mobile.ui.theme.HiggsfieldTheme

@Preview(showBackground = true, backgroundColor = 0xFF0D1316)
@Composable
private fun GenerationCardPreview() {
    HiggsfieldTheme {
        TimelineCard(previewGenerationItem(), false, {}, {}, {}, {}, {}, {})
    }
}

@Composable
internal fun TimelineCard(
    item: TimelineItem,
    active: Boolean,
    onUseOutput: (TimelineItem) -> Unit,
    onRetry: (TimelineItem) -> Unit,
    onCancel: (TimelineItem) -> Unit,
    onDownload: (TimelineItem) -> Unit,
    onCopyPrompt: (TimelineItem) -> Unit,
    onSharePrompt: (TimelineItem) -> Unit,
) {
    val hasOutput = item.lifecycle is GenerationStatus.Completed && item.outputLabel != null
    val isFailure = item.lifecycle is GenerationStatus.Failed || item.lifecycle is GenerationStatus.Nsfw || item.lifecycle is GenerationStatus.UnknownSubmissionOutcome
    val statusDescription = stringResource(R.string.generation_status_content_description, item.modelName, item.stateLabel.resolve())
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PromptBubble(item.prompt, Modifier.align(Alignment.End))
        Card(modifier = Modifier.fillMaxWidth().semantics { contentDescription = statusDescription }) {
            GenerationMediaSurface(item, hasOutput, isFailure)
            GenerationProgress(item.lifecycle)
            GenerationMetadata(item, hasOutput, active, onRetry, onCancel)
            if (hasOutput) GenerationActions(item, onDownload, onSharePrompt, onCopyPrompt, onUseOutput)
        }
    }
}

@Composable
private fun PromptBubble(prompt: String, modifier: Modifier = Modifier) {
    Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.secondaryContainer, modifier = modifier) { Text(prompt, Modifier.padding(14.dp)) }
}

@Composable
private fun GenerationMediaSurface(item: TimelineItem, hasOutput: Boolean, isFailure: Boolean) {
    Box(
        Modifier.fillMaxWidth().height(if (hasOutput) 220.dp else 156.dp).background(Brush.linearGradient(if (isFailure) listOf(MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.surfaceVariant) else listOf(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.primaryContainer))),
        contentAlignment = Alignment.Center,
    ) {
        if (hasOutput) item.output?.let { GenerationOutputPreview(it) } else GenerationPlaceholder(
            item.stateLabel.resolve(),
            item.lifecycle is GenerationStatus.Queued || item.lifecycle is GenerationStatus.InProgress,
        )
        if (hasOutput && item.output == null) Text(item.outputLabel?.resolve().orEmpty(), style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun GenerationPlaceholder(label: String, isLoading: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (isLoading) CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
        else Icon(Icons.Rounded.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
        Text(label, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun GenerationProgress(lifecycle: GenerationStatus?) {
    val inProgress = lifecycle as? GenerationStatus.InProgress ?: return
    if (inProgress.progress == null) LinearProgressIndicator(Modifier.fillMaxWidth()) else LinearProgressIndicator(progress = { inProgress.progress }, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun GenerationMetadata(item: TimelineItem, hasOutput: Boolean, active: Boolean, onRetry: (TimelineItem) -> Unit, onCancel: (TimelineItem) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(item.modelName, fontWeight = FontWeight.SemiBold)
            Text(item.stateLabel.resolve(), style = MaterialTheme.typography.bodySmall)
            if (hasOutput && item.output?.localUri == null) Text(stringResource(R.string.output_temporary), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            item.errorText?.let { Text(it.resolve(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
        }
        when {
            hasOutput && active -> Text(stringResource(R.string.editing_source), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
            item.canRetry -> TextButton(onClick = { onRetry(item) }) { Text(stringResource(R.string.retry)) }
            item.canCancel -> TextButton(onClick = { onCancel(item) }) { Text(stringResource(R.string.cancel_generation)) }
        }
    }
}

@Composable
private fun GenerationActions(item: TimelineItem, onDownload: (TimelineItem) -> Unit, onSharePrompt: (TimelineItem) -> Unit, onCopyPrompt: (TimelineItem) -> Unit, onUseOutput: (TimelineItem) -> Unit) {
    HorizontalDivider()
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = { onDownload(item) }) { Text(stringResource(R.string.download)) }
        TextButton(onClick = { onSharePrompt(item) }, enabled = item.prompt.isNotBlank()) { Icon(Icons.Rounded.Share, null, modifier = Modifier.size(16.dp)); Text(stringResource(R.string.share), Modifier.padding(start = 4.dp)) }
        TextButton(onClick = { onCopyPrompt(item) }, enabled = item.prompt.isNotBlank()) { Icon(Icons.Rounded.ContentCopy, null, modifier = Modifier.size(16.dp)); Text(stringResource(R.string.copy_prompt), Modifier.padding(start = 4.dp)) }
        TextButton(onClick = { onUseOutput(item) }) { Text(stringResource(R.string.reuse_parameters)) }
    }
}

@Composable
private fun GenerationOutputPreview(output: GenerationOutput) {
    when (output.kind) {
        MediaKind.IMAGE -> AsyncImage(model = output.localUri ?: output.remoteUrl, contentDescription = stringResource(R.string.generated_image), modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        MediaKind.VIDEO, MediaKind.AUDIO -> MediaOutputPlayer(output)
    }
}

@Composable
private fun MediaOutputPlayer(output: GenerationOutput) {
    val context = LocalContext.current
    val exoPlayer = remember(output.id, output.localUri, output.remoteUrl) { ExoPlayer.Builder(context).build().apply { setMediaItem(MediaItem.fromUri(output.localUri ?: output.remoteUrl)); prepare() } }
    DisposableEffect(exoPlayer) { onDispose(exoPlayer::release) }
    AndroidView(factory = { PlayerView(it).apply { player = exoPlayer; useController = true } }, update = { it.player = exoPlayer }, modifier = Modifier.fillMaxSize())
}
