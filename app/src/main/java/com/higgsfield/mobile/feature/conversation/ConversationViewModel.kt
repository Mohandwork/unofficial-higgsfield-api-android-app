package com.higgsfield.mobile.feature.conversation

import androidx.lifecycle.ViewModel
import com.higgsfield.mobile.BuildConfig
import com.higgsfield.mobile.core.model.CreativeBrief
import com.higgsfield.mobile.core.model.MediaKind
import com.higgsfield.mobile.core.model.WorkflowCapability
import com.higgsfield.mobile.core.model.WorkflowDescriptor
import com.higgsfield.mobile.core.model.WorkflowId
import com.higgsfield.mobile.core.model.WorkflowRegistry
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class TimelineItem(
    val id: String,
    val prompt: String,
    val modelName: String,
    val stateLabel: String,
    val outputLabel: String? = null,
    val parentId: String? = null,
)

data class ConversationUiState(
    val mediaKind: MediaKind = MediaKind.IMAGE,
    val prompt: String = "",
    val brief: CreativeBrief = CreativeBrief(),
    val workflows: List<WorkflowDescriptor> = emptyList(),
    val selectedWorkflow: WorkflowDescriptor? = null,
    val timeline: List<TimelineItem> = emptyList(),
    val activeSourceId: String? = null,
    val activeSourceLabel: String? = null,
    val modelMenuOpen: Boolean = false,
    val infoOpen: Boolean = false,
    val briefOpen: Boolean = false,
    val optionsOpen: Boolean = false,
    val message: String? = null,
    val isOnline: Boolean = true,
    val credentialsConfigured: Boolean = BuildConfig.HF_KEY_ID.isNotBlank() && BuildConfig.HF_KEY_SECRET.isNotBlank(),
)

@HiltViewModel
class ConversationViewModel @Inject constructor() : ViewModel() {
    private val mutableState = MutableStateFlow(ConversationUiState())
    val state: StateFlow<ConversationUiState> = mutableState.asStateFlow()

    fun initialize(kind: MediaKind) {
        if (mutableState.value.workflows.isNotEmpty() && mutableState.value.mediaKind == kind) return
        val workflows = WorkflowRegistry.forKind(kind)
        mutableState.value = ConversationUiState(mediaKind = kind, workflows = workflows, selectedWorkflow = workflows.firstOrNull())
    }

    fun updatePrompt(value: String) = mutableState.update { it.copy(prompt = value, message = null) }
    fun toggleModelMenu() = mutableState.update { it.copy(modelMenuOpen = !it.modelMenuOpen) }
    fun showInfo(show: Boolean) = mutableState.update { it.copy(infoOpen = show) }
    fun showBrief(show: Boolean) = mutableState.update { it.copy(briefOpen = show) }
    fun showOptions(show: Boolean) = mutableState.update { it.copy(optionsOpen = show) }

    fun selectWorkflow(workflow: WorkflowDescriptor) = mutableState.update { current ->
        val sourceIncompatible = current.activeSourceId != null && current.mediaKind == MediaKind.IMAGE &&
            WorkflowCapability.IMAGE_TO_IMAGE !in workflow.capabilities
        current.copy(
            selectedWorkflow = workflow,
            modelMenuOpen = false,
            message = if (sourceIncompatible) "This model cannot edit the active image. Detach it or choose a compatible edit model." else null,
        )
    }

    fun updateBrief(brief: CreativeBrief) = mutableState.update { it.copy(brief = brief, briefOpen = false) }
    fun attachSource(label: String) = mutableState.update {
        it.copy(activeSourceId = "local-source", activeSourceLabel = label, message = "Reference image attached locally.")
    }
    fun detachSource() = mutableState.update { it.copy(activeSourceId = null, activeSourceLabel = null, message = "Fresh generation started.") }

    fun useOutput(item: TimelineItem) = mutableState.update {
        it.copy(activeSourceId = item.id, activeSourceLabel = item.outputLabel, message = "Older output selected. Your next request starts a new branch.")
    }

    fun addDemoResult() = mutableState.update { current ->
        if (current.prompt.isBlank()) return@update current.copy(message = "Write an instruction first.")
        val workflow = current.selectedWorkflow ?: return@update current.copy(message = "Choose a model first.")
        val incompatible = current.activeSourceId != null && current.mediaKind == MediaKind.IMAGE &&
            WorkflowCapability.IMAGE_TO_IMAGE !in workflow.capabilities
        if (incompatible) return@update current.copy(message = "Choose Qwen Image 3 Edit or another compatible image editor.")
        val id = "demo-${current.timeline.size + 1}"
        val label = if (current.mediaKind == MediaKind.IMAGE) "Demo image ${current.timeline.size + 1}" else "Demo video ${current.timeline.size + 1}"
        val item = TimelineItem(id, current.prompt.trim(), workflow.displayName, "Completed · local demo", label, current.activeSourceId)
        current.copy(
            prompt = "",
            timeline = current.timeline + item,
            activeSourceId = if (current.mediaKind == MediaKind.IMAGE) id else current.activeSourceId,
            activeSourceLabel = if (current.mediaKind == MediaKind.IMAGE) label else current.activeSourceLabel,
            message = "Demo only — no API request was made.",
        )
    }
}
