package com.promptstudio.app.feature.conversation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Switch
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.promptstudio.app.R
import com.promptstudio.app.core.model.GenerationOptions
import com.promptstudio.app.core.model.CreativeBrief
import com.promptstudio.app.core.model.WorkflowDescriptor
import com.promptstudio.app.core.model.WorkflowOption
import com.promptstudio.app.ui.theme.PromptStudioTheme

@Preview(showBackground = true, backgroundColor = 0xFF0D1316)
@Composable
private fun ModelInfoSheetPreview() {
    PromptStudioTheme { ModelInfoDialog(previewWorkflow, {}) }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D1316)
@Composable
private fun GenerationSpecsSheetPreview() {
    PromptStudioTheme { OptionsDialog(previewWorkflow, previewConversationState().options, {}, {}) }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun ModelInfoDialog(workflow: WorkflowDescriptor?, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val maxSheetHeight = LocalConfiguration.current.screenHeightDp.dp * 0.85f
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier.fillMaxWidth().heightIn(max = maxSheetHeight)
                .verticalScroll(rememberScrollState(), enabled = sheetState.currentValue == androidx.compose.material3.SheetValue.Expanded)
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.model_details), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(workflow?.displayName ?: stringResource(R.string.model), style = MaterialTheme.typography.headlineSmall)
            val estimate = workflow?.staticEstimate
            DetailRow(stringResource(R.string.starting_price), estimate?.fromPrice ?: stringResource(R.string.pricing_unavailable))
            estimate?.maximumResolution?.let { DetailRow(stringResource(R.string.max_resolution), it) }
            estimate?.supportedDurations?.let { DetailRow(stringResource(R.string.supported_duration), it) }
            DetailRow(stringResource(R.string.capabilities), workflow?.capabilities?.joinToString { it.name.lowercase().replace('_', ' ') } ?: stringResource(R.string.unknown))
            Text(stringResource(R.string.model_price_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            estimate?.let {
                Text(it.sourceLabel ?: stringResource(R.string.model_source_user_provided), style = MaterialTheme.typography.labelSmall)
                Text(stringResource(R.string.estimate_verified, it.verifiedOn), style = MaterialTheme.typography.labelSmall)
            }
            if (workflow?.isSubmissionEnabled == false) Text(stringResource(R.string.adapter_not_enabled), color = MaterialTheme.colorScheme.error)
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text(stringResource(R.string.done)) }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    if (value.length > 24) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyMedium)
        }
    } else {
        Row(Modifier.fillMaxWidth()) {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            Text(value, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun OptionsDialog(workflow: WorkflowDescriptor?, initial: GenerationOptions, onDismiss: () -> Unit, onUpdateOptions: (GenerationOptions) -> Unit) {
    var aspectRatio by remember(initial) { mutableStateOf(initial.aspectRatio) }
    var resolution by remember(initial) { mutableStateOf(initial.resolution.orEmpty()) }
    var duration by remember(initial) { mutableStateOf(initial.durationSeconds?.toString().orEmpty()) }
    var seed by remember(initial) { mutableStateOf(initial.seed?.toString().orEmpty()) }
    var negativePrompt by remember(initial) { mutableStateOf(initial.negativePrompt.orEmpty()) }
    var modelOptions by remember(initial) { mutableStateOf(initial.modelOptions) }
    val supported = workflow?.supportedOptions.orEmpty()
    val constraints = workflow?.optionConstraints.orEmpty()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.fillMaxWidth().fillMaxHeight(0.85f).imePadding()
                .verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.generation_specs), style = MaterialTheme.typography.titleLarge)
            Text(workflow?.displayName ?: stringResource(R.string.model), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            if (WorkflowOption.ASPECT_RATIO in supported) ModelChoiceField(stringResource(R.string.aspect_ratio), aspectRatio, constraints[WorkflowOption.ASPECT_RATIO]?.choices.orEmpty(), { aspectRatio = it })
            if (WorkflowOption.RESOLUTION in supported) ModelChoiceField(stringResource(R.string.resolution), resolution, constraints[WorkflowOption.RESOLUTION]?.choices.orEmpty(), { resolution = it })
            if (WorkflowOption.DURATION in supported) {
                val durationConstraint = constraints[WorkflowOption.DURATION]
                if (durationConstraint?.choices?.isNotEmpty() == true)
                    ModelChoiceField(stringResource(R.string.duration_seconds), duration, durationConstraint.choices, { duration = it })
                else OutlinedTextField(duration, { duration = it.filter(Char::isDigit) }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.duration_seconds)) }, supportingText = { durationConstraint?.let { Text("${it.minimum ?: 0}–${it.maximum ?: "?"} seconds") } })
            }
            if (WorkflowOption.SEED in supported) OutlinedTextField(seed, { seed = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.seed)) })
            if (WorkflowOption.NEGATIVE_PROMPT in supported) OutlinedTextField(negativePrompt, { negativePrompt = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.negative_prompt)) })
            supported.filter { it in setOf(WorkflowOption.GENERATE_AUDIO, WorkflowOption.AIGC_WATERMARK, WorkflowOption.ENABLE_THINKING, WorkflowOption.PROMPT_EXTEND) }.forEach { option ->
                val key = option.name.lowercase()
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(option.name.lowercase().replace('_', ' ').replaceFirstChar(Char::uppercase), Modifier.weight(1f))
                    Switch(checked = modelOptions[key]?.toBooleanStrictOrNull() ?: (option == WorkflowOption.GENERATE_AUDIO),
                        onCheckedChange = { modelOptions = modelOptions + (key to it.toString()) })
                }
            }
            supported.filter { it in setOf(WorkflowOption.QUALITY, WorkflowOption.RENDERING_SPEED, WorkflowOption.OUTPUT_FORMAT, WorkflowOption.SOUND, WorkflowOption.MODE, WorkflowOption.CFG_SCALE, WorkflowOption.KEEP_ORIGINAL_SOUND, WorkflowOption.CHARACTER_ORIENTATION) }.forEach { option ->
                val key = option.name.lowercase()
                ModelChoiceField(option.name.lowercase().replace('_', ' ').replaceFirstChar(Char::uppercase), modelOptions[key].orEmpty(), constraints[option]?.choices.orEmpty()) {
                    modelOptions = modelOptions + (key to it)
                }
            }
            if (WorkflowOption.IMAGE_WEIGHT in supported) OutlinedTextField(modelOptions["image_weight"].orEmpty(), { modelOptions = modelOptions + ("image_weight" to it.filter(Char::isDigit)) }, Modifier.fillMaxWidth(), label = { Text("Image weight") }, supportingText = { Text("1–100") })
            if (supported.isEmpty()) Text(stringResource(R.string.no_model_settings))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { TextButton(onClick = { onUpdateOptions(GenerationOptions(aspectRatio = aspectRatio, resolution = resolution.ifBlank { null }, durationSeconds = duration.toIntOrNull(), seed = seed.toLongOrNull(), negativePrompt = negativePrompt.ifBlank { null }, modelOptions = modelOptions)); onDismiss() }) { Text(stringResource(R.string.done)) } }
        }
    }
}

@Composable
private fun ModelChoiceField(label: String, value: String, choices: List<String>, onChange: (String) -> Unit) {
    if (choices.isEmpty()) {
        OutlinedTextField(value, onChange, Modifier.fillMaxWidth(), label = { Text(label) })
        return
    }
    var expanded by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth()) {
        OutlinedTextField(value.ifBlank { "Default" }, {}, Modifier.fillMaxWidth(), readOnly = true,
            label = { Text(label) }, trailingIcon = { TextButton(onClick = { expanded = true }) { Text("Choose") } })
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("Default") }, onClick = { onChange(""); expanded = false })
            choices.forEach { choice -> DropdownMenuItem(text = { Text(choice) }, onClick = { onChange(choice); expanded = false }) }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun CreativeBriefSheet(initial: CreativeBrief, onDismiss: () -> Unit, onSave: (CreativeBrief) -> Unit) {
    var subject by remember(initial) { mutableStateOf(initial.subject) }
    var style by remember(initial) { mutableStateOf(initial.style) }
    var mood by remember(initial) { mutableStateOf(initial.mood) }
    var camera by remember(initial) { mutableStateOf(initial.cameraDirection) }
    var requirements by remember(initial) { mutableStateOf(initial.requirements) }
    var exclusions by remember(initial) { mutableStateOf(initial.exclusions) }
    var outputGoal by remember(initial) { mutableStateOf(initial.outputGoal) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.fillMaxWidth().fillMaxHeight(0.85f).imePadding()
                .verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(stringResource(R.string.prompt_settings), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.brief_detail_explanation), style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(subject, { subject = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text(stringResource(R.string.subject)) })
            OutlinedTextField(outputGoal, { outputGoal = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text(stringResource(R.string.output_goal)) })
            OutlinedTextField(style, { style = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text(stringResource(R.string.style_and_mood)) })
            OutlinedTextField(mood, { mood = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text(stringResource(R.string.style_and_mood)) })
            OutlinedTextField(camera, { camera = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text(stringResource(R.string.camera_direction)) })
            OutlinedTextField(requirements, { requirements = it }, Modifier.fillMaxWidth(), minLines = 2, maxLines = 4, label = { Text(stringResource(R.string.requirements)) })
            OutlinedTextField(exclusions, { exclusions = it }, Modifier.fillMaxWidth(), minLines = 2, maxLines = 4, label = { Text(stringResource(R.string.exclusions)) })
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = { onSave(CreativeBrief()); onDismiss() }) { Text(stringResource(R.string.clear_brief)) }
                TextButton(onClick = { onSave(CreativeBrief(subject, style, mood, camera, requirements, exclusions, outputGoal)); onDismiss() }) { Text(stringResource(R.string.save_brief)) }
            }
        }
    }
}
