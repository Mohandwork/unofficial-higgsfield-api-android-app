package com.higgsfield.mobile.feature.conversation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.higgsfield.mobile.R
import com.higgsfield.mobile.BuildConfig
import com.higgsfield.mobile.core.connectivity.AlwaysOnlineConnectivityStatusProvider
import com.higgsfield.mobile.core.connectivity.ConnectivityStatusProvider
import com.higgsfield.mobile.core.database.ConversationPersistence
import com.higgsfield.mobile.core.database.PersistedConversationSnapshot
import com.higgsfield.mobile.core.database.PersistedGenerationStatus
import com.higgsfield.mobile.core.data.GenerationRepository
import com.higgsfield.mobile.core.data.GenerationSubmissionException
import com.higgsfield.mobile.core.error.ErrorMapper
import com.higgsfield.mobile.core.model.CreativeBrief
import com.higgsfield.mobile.core.model.GenerationAttachment
import com.higgsfield.mobile.core.model.GenerationDraft
import com.higgsfield.mobile.core.model.GenerationOutput
import com.higgsfield.mobile.core.model.GenerationOptions
import com.higgsfield.mobile.core.model.GenerationRecord
import com.higgsfield.mobile.core.model.GenerationStatus
import com.higgsfield.mobile.core.model.MediaKind
import com.higgsfield.mobile.core.model.MediaRequirement
import com.higgsfield.mobile.core.model.MediaRole
import com.higgsfield.mobile.core.model.WorkflowCapability
import com.higgsfield.mobile.core.model.WorkflowDescriptor
import com.higgsfield.mobile.core.model.WorkflowOption
import com.higgsfield.mobile.core.model.WorkflowRegistry
import com.higgsfield.mobile.core.network.RequestStatusPoller
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
    val stateLabel: ConversationText,
    val outputLabel: ConversationText? = null,
    val output: GenerationOutput? = null,
    val sourceOutputId: String? = null,
    val parentId: String? = null,
    val lifecycle: GenerationStatus? = null,
    val record: GenerationRecord? = null,
    val errorText: ConversationText? = null,
    val canRetry: Boolean = false,
    val canCancel: Boolean = false,
)

data class DraftMediaAttachment(
    val role: MediaRole,
    val kind: MediaKind,
    val uri: String,
    val label: String,
)

data class ConversationUiState(
    val mediaKind: MediaKind = MediaKind.IMAGE,
    val prompt: String = "",
    val brief: CreativeBrief = CreativeBrief(),
    val workflows: List<WorkflowDescriptor> = emptyList(),
    val selectedWorkflow: WorkflowDescriptor? = null,
    val timeline: List<TimelineItem> = emptyList(),
    val activeSourceId: String? = null,
    val activeSourceLabel: ConversationText? = null,
    val attachmentSlots: List<MediaRequirement> = emptyList(),
    val attachments: List<DraftMediaAttachment> = emptyList(),
    val options: GenerationOptions = GenerationOptions(),
    val modelMenuOpen: Boolean = false,
    val infoOpen: Boolean = false,
    val briefOpen: Boolean = false,
    val optionsOpen: Boolean = false,
    val message: ConversationText? = null,
    val isSubmitting: Boolean = false,
    val isOnline: Boolean = true,
    val credentialsConfigured: Boolean = BuildConfig.HF_KEY_ID.isNotBlank() && BuildConfig.HF_KEY_SECRET.isNotBlank(),
)

@HiltViewModel
class ConversationViewModel @Inject constructor(
    private val persistence: ConversationPersistence,
    private val connectivity: ConnectivityStatusProvider = AlwaysOnlineConnectivityStatusProvider,
    private val generationRepository: GenerationRepository? = null,
    private val statusPoller: RequestStatusPoller? = null,
) : ViewModel() {
    private val mutableState = MutableStateFlow(ConversationUiState())
    val state: StateFlow<ConversationUiState> = mutableState.asStateFlow()
    private var conversationId: String? = null
    private var observation: Job? = null
    private var generationObservation: Job? = null
    private var connectivityObservation: Job? = null
    private val statusPollingJobs = mutableMapOf<String, Job>()

    fun initialize(kind: MediaKind) {
        if (mutableState.value.workflows.isNotEmpty() && mutableState.value.mediaKind == kind) return
        val workflows = WorkflowRegistry.forKind(kind)
        mutableState.value = ConversationUiState(
            mediaKind = kind,
            workflows = workflows,
            selectedWorkflow = workflows.firstOrNull(),
            attachmentSlots = attachmentSlotsFor(workflows.firstOrNull()),
        )
        val id = kind.name.lowercase() + "-default"
        conversationId = id
        observation?.cancel()
        generationObservation?.cancel()
        connectivityObservation?.cancel()
        statusPollingJobs.values.forEach(Job::cancel)
        statusPollingJobs.clear()
        connectivityObservation = viewModelScope.launch {
            connectivity.isOnline.collect { online ->
                mutableState.update { it.copy(isOnline = online) }
            }
        }
        observation = viewModelScope.launch {
            persistence.ensureConversation(id, kind, workflows.firstOrNull()?.id)
            persistence.observe(id).collect(::restoreSnapshot)
        }
        generationObservation = generationRepository?.let { repository ->
            viewModelScope.launch {
                repository.observeConversation(id).collect(::restoreGenerationRecords)
            }
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
                attachmentSlots = attachmentSlotsFor(workflow),
                attachments = current.attachments.filter { attachment ->
                    attachmentSlotsFor(workflow).any { slot ->
                        slot.role == attachment.role && slot.kind == attachment.kind
                    }
                },
                options = current.options.retainFor(workflow),
                modelMenuOpen = false,
                message = if (sourceIncompatible) {
                    ConversationText.Resource(R.string.message_model_cannot_edit_active_image)
                } else null,
            )
        }
        conversationId?.let { id ->
            viewModelScope.launch { persistence.saveSelectedWorkflow(id, workflow.id) }
        }
    }

    fun attachMedia(role: MediaRole, kind: MediaKind, uri: String, label: String) {
        mutableState.update { current ->
            val slot = current.attachmentSlots.firstOrNull { it.role == role && it.kind == kind }
                ?: return@update current
            current.copy(
                attachments = current.attachments.filterNot { it.role == role } +
                    DraftMediaAttachment(role, slot.kind, uri, label),
            )
        }
    }

    fun removeMedia(role: MediaRole) = mutableState.update { current ->
        current.copy(attachments = current.attachments.filterNot { it.role == role })
    }

    fun updateOptions(options: GenerationOptions) = mutableState.update { current ->
        current.copy(options = options.retainFor(current.selectedWorkflow))
    }

    fun currentDraft(): GenerationDraft? {
        val current = mutableState.value
        val workflow = current.selectedWorkflow ?: return null
        return GenerationDraft(
            instruction = current.prompt,
            creativeBrief = current.brief,
            workflowId = workflow.id,
            attachments = current.attachments.map { attachment ->
                GenerationAttachment(
                    id = "$LOCAL_ATTACHMENT_ID_PREFIX${attachment.role.name.lowercase()}",
                    uri = attachment.uri,
                    kind = attachment.kind,
                    role = attachment.role,
                )
            },
            activeSourceId = current.activeSourceId,
            options = current.options,
        )
    }

    fun updateBrief(brief: CreativeBrief) {
        mutableState.update { it.copy(brief = brief, briefOpen = false) }
        conversationId?.let { id -> viewModelScope.launch { persistence.saveBrief(id, brief) } }
    }

    fun submitGeneration() {
        val id = conversationId ?: return
        val draft = currentDraft()
            ?: return mutableState.update { it.copy(message = ConversationText.Resource(R.string.message_choose_model_first)) }
        submitDraft(id, draft, clearPromptOnSuccess = true)
    }

    fun retryGeneration(item: TimelineItem) {
        val id = conversationId ?: return
        val record = item.record ?: return
        if (!item.canRetry) return
        submitDraft(id, record.draft, clearPromptOnSuccess = false)
    }

    fun cancelGeneration(item: TimelineItem) {
        val repository = generationRepository ?: return
        if (!item.canCancel) return
        viewModelScope.launch {
            val result = repository.cancel(item.id)
            mutableState.update { current ->
                current.copy(message = result.exceptionOrNull()?.toConversationText())
            }
        }
    }

    fun downloadOutput(item: TimelineItem, destinationUri: String) {
        val output = item.output ?: return
        val repository = generationRepository ?: return
        viewModelScope.launch {
            val result = repository.downloadOutput(output, destinationUri)
            mutableState.update {
                it.copy(
                    message = result.exceptionOrNull()?.toConversationText()
                        ?: ConversationText.Resource(R.string.message_download_saved),
                )
            }
        }
    }

    private fun submitDraft(conversationId: String, draft: GenerationDraft, clearPromptOnSuccess: Boolean) {
        val repository = generationRepository
            ?: return mutableState.update { it.copy(message = ConversationText.Resource(R.string.error_unknown)) }
        if (!mutableState.value.isOnline || mutableState.value.isSubmitting) return
        mutableState.update { it.copy(isSubmitting = true, message = null) }
        viewModelScope.launch {
            val result = repository.submit(conversationId, draft)
            mutableState.update { current ->
                current.copy(
                    prompt = if (result.isSuccess && clearPromptOnSuccess) "" else current.prompt,
                    isSubmitting = false,
                    message = result.exceptionOrNull()?.toConversationText(),
                )
            }
            result.getOrNull()?.let(::startStatusPollingIfNeeded)
        }
    }

    fun attachSource(label: String) = mutableState.update {
        it.copy(
            activeSourceId = LOCAL_SOURCE_ID,
            activeSourceLabel = ConversationText.Dynamic(label),
            message = ConversationText.Resource(R.string.message_reference_image_attached),
        )
    }

    fun detachSource() {
        mutableState.update {
            it.copy(activeSourceId = null, activeSourceLabel = null, message = ConversationText.Resource(R.string.message_fresh_generation_started))
        }
        conversationId?.let { id -> viewModelScope.launch { persistence.selectActiveSource(id, null) } }
    }

    fun useOutput(item: TimelineItem) {
        mutableState.update {
            it.copy(
                activeSourceId = item.id,
                activeSourceLabel = item.outputLabel,
                message = ConversationText.Resource(R.string.message_older_output_selected),
            )
        }
        conversationId?.let { id ->
            viewModelScope.launch { persistence.selectActiveSource(id, item.sourceOutputId) }
        }
    }

    fun addDemoResult() {
        var created: TimelineItem? = null
        mutableState.update { current ->
            if (current.prompt.isBlank()) return@update current.copy(message = ConversationText.Resource(R.string.message_write_instruction_first))
            val workflow = current.selectedWorkflow
                ?: return@update current.copy(message = ConversationText.Resource(R.string.message_choose_model_first))
            val incompatible = current.activeSourceId != null && current.mediaKind == MediaKind.IMAGE &&
                WorkflowCapability.IMAGE_TO_IMAGE !in workflow.capabilities
            if (incompatible) {
                return@update current.copy(message = ConversationText.Resource(R.string.message_choose_compatible_editor))
            }
            val itemNumber = current.timeline.size + 1
            val id = "$DEMO_GENERATION_PREFIX$itemNumber"
            val label = ConversationText.Resource(
                if (current.mediaKind == MediaKind.IMAGE) R.string.demo_image else R.string.demo_video,
                listOf(itemNumber),
            )
            val item = TimelineItem(
                id = id,
                prompt = current.prompt.trim(),
                modelName = workflow.displayName,
                stateLabel = ConversationText.Resource(R.string.status_completed_local_demo),
                outputLabel = label,
                sourceOutputId = "$id$OUTPUT_SUFFIX",
                parentId = current.activeSourceId,
            )
            created = item
            current.copy(
                prompt = "",
                timeline = current.timeline + item,
                activeSourceId = if (current.mediaKind == MediaKind.IMAGE) id else current.activeSourceId,
                activeSourceLabel = if (current.mediaKind == MediaKind.IMAGE) label else current.activeSourceLabel,
                message = ConversationText.Resource(R.string.message_demo_only),
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
            val timeline = if (generationRepository == null) snapshot.timeline.map { item ->
                TimelineItem(
                    id = item.id,
                    prompt = item.prompt,
                    modelName = WorkflowRegistry.find(item.workflowId)?.displayName ?: item.workflowId.value,
                    stateLabel = when (item.status) {
                        PersistedGenerationStatus.COMPLETED -> ConversationText.Resource(R.string.status_completed_restored)
                        else -> ConversationText.Resource(
                            R.string.status_label,
                            listOf(item.status.name.lowercase().replace('_', ' ')),
                        )
                    },
                    outputLabel = item.outputId?.let {
                        ConversationText.Resource(
                            if (item.outputKind == MediaKind.IMAGE) R.string.restored_image else R.string.restored_video,
                        )
                    },
                    sourceOutputId = item.outputId,
                    parentId = item.parentId,
                    lifecycle = item.status.toLifecycle(),
                )
            } else current.timeline
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
                    ConversationText.Resource(R.string.message_select_output_before_continuing)
                } else current.message,
            )
        }
    }

    private fun restoreGenerationRecords(records: List<GenerationRecord>) {
        records.forEach(::startStatusPollingIfNeeded)
        mutableState.update { current ->
            current.copy(timeline = records.map(GenerationRecord::toTimelineItem))
        }
    }

    private fun startStatusPollingIfNeeded(record: GenerationRecord) {
        if (record.status !is GenerationStatus.Queued && record.status !is GenerationStatus.InProgress) return
        if (statusPollingJobs[record.id]?.isActive == true) return
        statusPollingJobs[record.id] = viewModelScope.launch {
            try {
                statusPoller?.pollUntilTerminal(record.id)
            } finally {
                statusPollingJobs.remove(record.id)
            }
        }
    }

    private fun attachmentSlotsFor(workflow: WorkflowDescriptor?): List<MediaRequirement> {
        if (workflow == null) return emptyList()
        val referenceSlot = MediaRequirement(MediaRole.REFERENCE, MediaKind.IMAGE)
        return workflow.mediaRequirements + if (
            WorkflowCapability.REFERENCE_IMAGE in workflow.capabilities &&
                workflow.mediaRequirements.none { it.role == MediaRole.REFERENCE }
        ) {
            listOf(referenceSlot)
        } else {
            emptyList()
        }
    }

    private companion object {
        const val LOCAL_SOURCE_ID = "local-source"
        const val DEMO_GENERATION_PREFIX = "demo-"
        const val OUTPUT_SUFFIX = "-output"
        const val LOCAL_ATTACHMENT_ID_PREFIX = "local-attachment-"
    }
}

private fun Throwable.toConversationText(): ConversationText = when (this) {
    is GenerationSubmissionException -> ConversationText.Resource(appError.messageResId)
    else -> ConversationText.Resource(R.string.error_unknown)
}

private fun GenerationRecord.toTimelineItem(): TimelineItem {
    val outputs = (status as? GenerationStatus.Completed)?.outputs.orEmpty()
    val output = outputs.firstOrNull()
    return TimelineItem(
        id = id,
        prompt = draft.instruction,
        modelName = WorkflowRegistry.find(draft.workflowId)?.displayName ?: draft.workflowId.value,
        stateLabel = status.toStateText(),
        outputLabel = output?.let { ConversationText.Resource(if (it.kind == MediaKind.IMAGE) R.string.restored_image else R.string.restored_video) },
        output = output,
        sourceOutputId = output?.id,
        parentId = parentGenerationId,
        lifecycle = status,
        record = this,
        errorText = ErrorMapper.messageResIdFor(errorCode)?.let(ConversationText::Resource),
        canRetry = (status as? GenerationStatus.Failed)?.retryable == true,
        canCancel = status is GenerationStatus.Queued,
    )
}

private fun GenerationStatus.toStateText(): ConversationText = ConversationText.Resource(
    when (this) {
        GenerationStatus.Draft -> R.string.status_draft
        GenerationStatus.Queued -> R.string.status_queued
        is GenerationStatus.InProgress -> R.string.status_generating
        is GenerationStatus.Completed -> R.string.status_completed
        is GenerationStatus.Failed -> R.string.status_failed
        is GenerationStatus.UnknownSubmissionOutcome -> R.string.status_submission_unknown
        is GenerationStatus.Nsfw -> R.string.status_moderated
        GenerationStatus.Canceled -> R.string.status_canceled
    },
)

private fun GenerationOptions.retainFor(workflow: WorkflowDescriptor?): GenerationOptions {
    val supported = workflow?.supportedOptions.orEmpty()
    return copy(
        aspectRatio = if (WorkflowOption.ASPECT_RATIO in supported) aspectRatio else GenerationOptions().aspectRatio,
        resolution = resolution?.takeIf { WorkflowOption.RESOLUTION in supported },
        durationSeconds = durationSeconds?.takeIf { WorkflowOption.DURATION in supported },
        seed = seed?.takeIf { WorkflowOption.SEED in supported },
        negativePrompt = negativePrompt?.takeIf { WorkflowOption.NEGATIVE_PROMPT in supported },
    )
}

private fun PersistedGenerationStatus.toLifecycle(): GenerationStatus = when (this) {
    PersistedGenerationStatus.DRAFT -> GenerationStatus.Draft
    PersistedGenerationStatus.QUEUED -> GenerationStatus.Queued
    PersistedGenerationStatus.IN_PROGRESS -> GenerationStatus.InProgress()
    PersistedGenerationStatus.COMPLETED -> GenerationStatus.Completed(emptyList())
    PersistedGenerationStatus.FAILED -> GenerationStatus.Failed("", retryable = false)
    PersistedGenerationStatus.UNKNOWN_SUBMISSION_OUTCOME -> GenerationStatus.UnknownSubmissionOutcome("")
    PersistedGenerationStatus.NSFW -> GenerationStatus.Nsfw("")
    PersistedGenerationStatus.CANCELED -> GenerationStatus.Canceled
}
