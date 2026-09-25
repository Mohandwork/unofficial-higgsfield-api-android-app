package com.higgsfield.mobile.feature.conversation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.higgsfield.mobile.BuildConfig
import com.higgsfield.mobile.core.connectivity.AlwaysOnlineConnectivityStatusProvider
import com.higgsfield.mobile.core.connectivity.ConnectivityStatusProvider
import com.higgsfield.mobile.core.database.ConversationPersistence
import com.higgsfield.mobile.core.database.PersistedConversationSnapshot
import com.higgsfield.mobile.core.database.PersistedGenerationStatus
import com.higgsfield.mobile.core.model.CreativeBrief
import com.higgsfield.mobile.core.model.MediaKind
import com.higgsfield.mobile.core.model.WorkflowCapability
import com.higgsfield.mobile.core.model.WorkflowDescriptor
import com.higgsfield.mobile.core.model.WorkflowRegistry
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TimelineItem(
    val id: String,
    val prompt: String,
    val modelName: String,
    val stateLabel: String,
    val outputLabel: String? = null,
    val sourceOutputId: String? = null,
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
class ConversationViewModel @Inject constructor(
    private val persistence: ConversationPersistence,
    private val connectivity: ConnectivityStatusProvider = AlwaysOnlineConnectivityStatusProvider,
) : ViewModel() {
    private val mutableState = MutableStateFlow(ConversationUiState())
    val state: StateFlow<ConversationUiState> = mutableState.asStateFlow()
    private var conversationId: String? = null
    private var observation: Job? = null
    private var connectivityObservation: Job? = null

    fun initialize(kind: MediaKind) {
        if (mutableState.value.workflows.isNotEmpty() && mutableState.value.mediaKind == kind) return
        val workflows = WorkflowRegistry.forKind(kind)
        mutableState.value = ConversationUiState(
            mediaKind = kind,
            workflows = workflows,
            selectedWorkflow = workflows.firstOrNull(),
        )
        val id = kind.name.lowercase() + "-default"
        conversationId = id
        observation?.cancel()
        connectivityObservation?.cancel()
        connectivityObservation = viewModelScope.launch {
            connectivity.isOnline.collect { online ->
                mutableState.update { it.copy(isOnline = online) }
            }
        }
        observation = viewModelScope.launch {
            persistence.ensureConversation(id, kind, workflows.firstOrNull()?.id)
            persistence.observe(id).collect(::restoreSnapshot)
        }
    }

    fun updatePrompt(value: String) = mutableState.update { it.copy(prompt = value, message = null) }
    fun toggleModelMenu() = mutableState.update { it.copy(modelMenuOpen = !it.modelMenuOpen) }
    fun showInfo(show: Boolean) = mutableState.update { it.copy(infoOpen = show) }
    fun showBrief(show: Boolean) = mutableState.update { it.copy(briefOpen = show) }
    fun showOptions(show: Boolean) = mutableState.update { it.copy(optionsOpen = show) }

    fun selectWorkflow(workflow: WorkflowDescriptor) {
        mutableState.update { current ->
            val sourceIncompatible = current.activeSourceId != null && current.mediaKind == MediaKind.IMAGE &&
                WorkflowCapability.IMAGE_TO_IMAGE !in workflow.capabilities
            current.copy(
                selectedWorkflow = workflow,
                modelMenuOpen = false,
                message = if (sourceIncompatible) {
                    ConversationCopy.MODEL_CANNOT_EDIT_ACTIVE_IMAGE
                } else null,
            )
        }
        conversationId?.let { id ->
            viewModelScope.launch { persistence.saveSelectedWorkflow(id, workflow.id) }
        }
    }

    fun updateBrief(brief: CreativeBrief) {
        mutableState.update { it.copy(brief = brief, briefOpen = false) }
        conversationId?.let { id -> viewModelScope.launch { persistence.saveBrief(id, brief) } }
    }

    fun attachSource(label: String) = mutableState.update {
        it.copy(activeSourceId = "local-source", activeSourceLabel = label, message = ConversationCopy.REFERENCE_IMAGE_ATTACHED)
    }

    fun detachSource() {
        mutableState.update {
            it.copy(activeSourceId = null, activeSourceLabel = null, message = ConversationCopy.FRESH_GENERATION_STARTED)
        }
        conversationId?.let { id -> viewModelScope.launch { persistence.selectActiveSource(id, null) } }
    }

    fun useOutput(item: TimelineItem) {
        mutableState.update {
            it.copy(
                activeSourceId = item.id,
                activeSourceLabel = item.outputLabel,
                message = ConversationCopy.OLDER_OUTPUT_SELECTED,
            )
        }
        conversationId?.let { id ->
            viewModelScope.launch { persistence.selectActiveSource(id, item.sourceOutputId) }
        }
    }

    fun addDemoResult() {
        var created: TimelineItem? = null
        mutableState.update { current ->
            if (current.prompt.isBlank()) return@update current.copy(message = ConversationCopy.WRITE_INSTRUCTION_FIRST)
            val workflow = current.selectedWorkflow
                ?: return@update current.copy(message = ConversationCopy.CHOOSE_MODEL_FIRST)
            val incompatible = current.activeSourceId != null && current.mediaKind == MediaKind.IMAGE &&
                WorkflowCapability.IMAGE_TO_IMAGE !in workflow.capabilities
            if (incompatible) {
                return@update current.copy(message = ConversationCopy.CHOOSE_COMPATIBLE_EDITOR)
            }
            val id = "demo-${current.timeline.size + 1}"
            val label = if (current.mediaKind == MediaKind.IMAGE) {
                "Demo image ${current.timeline.size + 1}"
            } else {
                "Demo video ${current.timeline.size + 1}"
            }
            val item = TimelineItem(
                id = id,
                prompt = current.prompt.trim(),
                modelName = workflow.displayName,
                stateLabel = "Completed · local demo",
                outputLabel = label,
                sourceOutputId = id + "-output",
                parentId = current.activeSourceId,
            )
            created = item
            current.copy(
                prompt = "",
                timeline = current.timeline + item,
                activeSourceId = if (current.mediaKind == MediaKind.IMAGE) id else current.activeSourceId,
                activeSourceLabel = if (current.mediaKind == MediaKind.IMAGE) label else current.activeSourceLabel,
                message = ConversationCopy.DEMO_ONLY,
            )
        }
        val item = created ?: return
        val current = mutableState.value
        val id = conversationId ?: return
        val workflowId = current.selectedWorkflow?.id ?: return
        viewModelScope.launch {
            persistence.saveCompletedDemo(
                conversationId = id,
                generationId = item.id,
                parentGenerationId = item.parentId,
                workflowId = workflowId,
                instruction = item.prompt,
                outputKind = current.mediaKind,
            )
        }
    }

    private fun restoreSnapshot(snapshot: PersistedConversationSnapshot?) {
        if (snapshot == null) return
        mutableState.update { current ->
            val timeline = snapshot.timeline.map { item ->
                TimelineItem(
                    id = item.id,
                    prompt = item.prompt,
                    modelName = WorkflowRegistry.find(item.workflowId)?.displayName ?: item.workflowId.value,
                    stateLabel = when (item.status) {
                        PersistedGenerationStatus.COMPLETED -> "Completed · restored"
                        else -> item.status.name.lowercase().replace('_', ' ')
                    },
                    outputLabel = item.outputId?.let {
                        if (item.outputKind == MediaKind.IMAGE) "Restored image" else "Restored video"
                    },
                    sourceOutputId = item.outputId,
                    parentId = item.parentId,
                )
            }
            val activeGeneration = timeline.firstOrNull {
                it.sourceOutputId == snapshot.activeSourceOutputId
            }
            current.copy(
                brief = snapshot.brief,
                selectedWorkflow = snapshot.selectedWorkflowId?.let(WorkflowRegistry::find)
                    ?: current.selectedWorkflow,
                timeline = timeline,
                activeSourceId = activeGeneration?.id,
                activeSourceLabel = activeGeneration?.outputLabel,
                message = if (snapshot.requiresSourceSelection) {
                    ConversationCopy.SELECT_OUTPUT_BEFORE_CONTINUING
                } else current.message,
            )
        }
    }
}
