package com.higgsfield.mobile.feature.conversation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.higgsfield.mobile.R
import com.higgsfield.mobile.core.model.GenerationOptions
import com.higgsfield.mobile.core.model.WorkflowDescriptor
import com.higgsfield.mobile.core.model.WorkflowOption
import com.higgsfield.mobile.ui.theme.HiggsfieldTheme

@Preview(showBackground = true, backgroundColor = 0xFF0D1316)
@Composable
private fun ModelInfoSheetPreview() {
    HiggsfieldTheme { ModelInfoDialog(previewWorkflow, {}) }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D1316)
@Composable
private fun GenerationSpecsSheetPreview() {
    HiggsfieldTheme { OptionsDialog(previewWorkflow, previewConversationState().options, {}, {}) }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun ModelInfoDialog(workflow: WorkflowDescriptor?, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(workflow?.displayName ?: stringResource(R.string.model), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.capabilities, workflow?.capabilities?.joinToString { it.name.lowercase().replace('_', ' ') } ?: stringResource(R.string.unknown)))
            val estimate = workflow?.staticEstimate
            Text(estimate?.fromPrice?.let { stringResource(R.string.estimate_from, it) } ?: stringResource(R.string.pricing_unavailable), style = MaterialTheme.typography.titleMedium)
            Text(estimate?.creditGuidance ?: stringResource(R.string.estimate_credits_unavailable))
            Text(estimate?.expectedLatency ?: stringResource(R.string.estimate_latency_unavailable))
            estimate?.let { Text(stringResource(R.string.estimate_verified, it.verifiedOn), style = MaterialTheme.typography.labelSmall); Text(stringResource(R.string.estimate_source, it.sourceUrl), style = MaterialTheme.typography.labelSmall) }
            if (workflow?.isSubmissionEnabled == false) Text(stringResource(R.string.adapter_not_enabled), color = MaterialTheme.colorScheme.error)
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text(stringResource(R.string.done)) }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun OptionsDialog(workflow: WorkflowDescriptor?, initial: GenerationOptions, onDismiss: () -> Unit, onUpdateOptions: (GenerationOptions) -> Unit) {
    var aspectRatio by remember(initial) { mutableStateOf(initial.aspectRatio) }
    var resolution by remember(initial) { mutableStateOf(initial.resolution.orEmpty()) }
    var seed by remember(initial) { mutableStateOf(initial.seed?.toString().orEmpty()) }
    var negativePrompt by remember(initial) { mutableStateOf(initial.negativePrompt.orEmpty()) }
    val supported = workflow?.supportedOptions.orEmpty()
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.generation_specs), style = MaterialTheme.typography.titleLarge)
            Text(workflow?.displayName ?: stringResource(R.string.model), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            if (WorkflowOption.ASPECT_RATIO in supported) OutlinedTextField(aspectRatio, { aspectRatio = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.aspect_ratio)) })
            if (WorkflowOption.RESOLUTION in supported) OutlinedTextField(resolution, { resolution = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.resolution)) })
            if (WorkflowOption.SEED in supported) OutlinedTextField(seed, { seed = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.seed)) })
            if (WorkflowOption.NEGATIVE_PROMPT in supported) OutlinedTextField(negativePrompt, { negativePrompt = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.negative_prompt)) })
            if (supported.isEmpty()) Text(stringResource(R.string.no_model_settings))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { TextButton(onClick = { onUpdateOptions(GenerationOptions(aspectRatio = aspectRatio, resolution = resolution.ifBlank { null }, seed = seed.toLongOrNull(), negativePrompt = negativePrompt.ifBlank { null })); onDismiss() }) { Text(stringResource(R.string.done)) } }
        }
    }
}
