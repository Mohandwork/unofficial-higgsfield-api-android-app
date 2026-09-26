package com.higgsfield.mobile.feature.conversation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.higgsfield.mobile.R
import com.higgsfield.mobile.core.connectivity.AlwaysOnlineConnectivityStatusProvider
import com.higgsfield.mobile.core.connectivity.ConnectivityStatusProvider
import com.higgsfield.mobile.core.database.ConversationPersistence
import com.higgsfield.mobile.core.database.PersistedConversationSnapshot
import com.higgsfield.mobile.core.database.PersistedComposerDraft
import com.higgsfield.mobile.core.database.PersistedDraftAttachment
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ConversationViewModel @Inject constructor(
    private val persistence: ConversationPersistence,
    private val connectivity: ConnectivityStatusProvider = AlwaysOnlineConnectivityStatusProvider,
    private val generationRepository: GenerationRepository? = null,
    private val statusPoller: RequestStatusPoller? = null,
    private val conversationLifecycle: ConversationLifecycleUseCase = ConversationLifecycleUseCase(persistence),
    private val submitGenerationUseCase: SubmitGenerationUseCase? = generationRepository?.let { SubmitGenerationUseCase(it, persistence) },
    private val composerUseCase: ConversationComposerUseCase = ConversationComposerUseCase(),
) : ViewModel() {
    private val mutableState = MutableStateFlow(ConversationUiState())
    val state: StateFlow<ConversationUiState> = mutableState.asStateFlow()
    private val effectChannel = Channel<ConversationUiEffect>(Channel.BUFFERED)
    val effects = effectChannel.receiveAsFlow()
    private var conversationId: String? = null
    private var observation: Job? = null
    private var generationObservation: Job? = null
    private var historyObservation: Job? = null
    private var connectivityObservation: Job? = null
    private var detachAwaitingDatabaseConfirmation = false
    private var persistedActiveSourceOutputId: String? = null
    private var draftTouched = false
    private var snapshotLoaded = false
    private var recordsLoaded = false
    private val draftWrites = Channel<Triple<String, MediaKind, PersistedComposerDraft>>(Channel.UNLIMITED)
    private val statusPollingJobs = mutableMapOf<String, Job>()

    init {
        viewModelScope.launch {
            for ((id, kind, draft) in draftWrites) {
                try {
                    persistence.ensureConversation(id, kind, WorkflowRegistry.forKind(kind).firstOrNull()?.id)
                    persistence.saveDraft(id, draft)
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Exception) {
                    if (mutableState.value.conversationId == id) {
                        mutableState.update { it.copy(message = ConversationText.Resource(R.string.error_unknown)) }
                    }
                }
            }
        }
    }

    fun initialize(kind: MediaKind, requestedConversationId: String? = null) {
        val id = requestedConversationId ?: kind.name.lowercase() + "-default"
        if (conversationId == id && mutableState.value.workflows.isNotEmpty()) return
        val workflows = WorkflowRegistry.forKind(kind)
        mutableState.value = ConversationUiState(
            mediaKind = kind,
            conversationId = id,
            workflows = workflows,
            selectedWorkflow = workflows.firstOrNull(),
            attachmentSlots = composerUseCase.slotsFor(workflows.firstOrNull()),
            isTransitioning = true,
        )
        conversationId = id
        draftTouched = false
        snapshotLoaded = false
        recordsLoaded = generationRepository == null
        persistedActiveSourceOutputId = null
        observation?.cancel()
        generationObservation?.cancel()
        historyObservation?.cancel()
        connectivityObservation?.cancel()
        statusPollingJobs.values.forEach(Job::cancel)
        statusPollingJobs.clear()
        connectivityObservation = viewModelScope.launch {
            connectivity.isOnline.collect { online ->
                mutableState.update { it.copy(isOnline = online) }
            }
        }
        observation = viewModelScope.launch {
            try {
                persistence.ensureConversation(id, kind, workflows.firstOrNull()?.id)
                persistence.observe(id).collect(::restoreSnapshot)
            } catch (canceled: CancellationException) {
                throw canceled
            } catch (_: Exception) {
                if (conversationId == id) mutableState.update {
                    it.copy(isTransitioning = false, message = ConversationText.Resource(R.string.error_unknown))
                }
            }
        }
        historyObservation = viewModelScope.launch {
            persistence.observeConversations().collect { conversations ->
                mutableState.update { it.copy(conversations = conversations) }
            }
        }
        generationObservation = generationRepository?.let { repository ->
            viewModelScope.launch {
                try {
                    repository.observeConversation(id).collect { records ->
                        if (conversationId == id) restoreGenerationRecords(records)
                    }
                } catch (canceled: CancellationException) {
                    throw canceled
                } catch (_: Exception) {
                    if (conversationId == id) mutableState.update {
                        it.copy(isTransitioning = false, message = ConversationText.Resource(R.string.error_unknown))
                    }
                }
            }
        }
    }

    fun updatePrompt(value: String) {
        draftTouched = true
        mutableState.update { it.copy(prompt = value, message = null) }
        persistDraft()
    }

    fun onEvent(event: ConversationUiEvent) {
        when (event) {
            is ConversationUiEvent.SelectMediaKind -> openMostRecentConversation(event.kind)
            is ConversationUiEvent.OpenConversation -> openConversation(event.id, event.kind)
            is ConversationUiEvent.CreateConversation -> createConversation(event.kind)
            ConversationUiEvent.RemoveConversation -> removeCurrentConversation()
            is ConversationUiEvent.RenameConversation -> renameConversation(event.title)
            is ConversationUiEvent.ChangePrompt -> updatePrompt(event.value)
            ConversationUiEvent.ToggleModelMenu -> toggleModelMenu()
            is ConversationUiEvent.SelectWorkflow -> selectWorkflow(event.workflow)
            is ConversationUiEvent.ShowInfo -> showInfo(event.show)
            is ConversationUiEvent.ShowHistory -> showHistory(event.show)
            is ConversationUiEvent.ShowBrief -> showBrief(event.show)
            is ConversationUiEvent.ShowOptions -> showOptions(event.show)
            is ConversationUiEvent.UpdateBrief -> updateBrief(event.brief)
            is ConversationUiEvent.UpdateOptions -> updateOptions(event.options)
            is ConversationUiEvent.PickMedia -> viewModelScope.launch {
                effectChannel.send(ConversationUiEffect.LaunchMediaPicker(event.role, event.kind))
            }
            is ConversationUiEvent.MediaPicked -> attachMedia(event.role, event.kind, event.uri, event.label)
            is ConversationUiEvent.RemoveMedia -> removeMedia(event.role)
            ConversationUiEvent.DetachSource -> detachSource()
            is ConversationUiEvent.EditImage -> useOutput(event.item)
            is ConversationUiEvent.ReuseParameters -> reuseParameters(event.item)
            ConversationUiEvent.Generate -> submitGeneration()
            is ConversationUiEvent.Retry -> retryGeneration(event.item)
            is ConversationUiEvent.Cancel -> cancelGeneration(event.item)
            is ConversationUiEvent.Download -> viewModelScope.launch {
                effectChannel.send(ConversationUiEffect.LaunchDownload(event.item))
            }
            is ConversationUiEvent.DownloadDestinationSelected -> downloadOutput(event.item, event.uri)
            is ConversationUiEvent.CopyPrompt -> viewModelScope.launch {
                effectChannel.send(ConversationUiEffect.CopyText(event.item.prompt))
            }
        }
    }
    fun toggleModelMenu() = mutableState.update { it.copy(modelMenuOpen = !it.modelMenuOpen) }
    fun showInfo(show: Boolean) = mutableState.update { it.copy(infoOpen = show) }
    fun showBrief(show: Boolean) = mutableState.update { it.copy(briefOpen = show) }
    fun showOptions(show: Boolean) = mutableState.update { it.copy(optionsOpen = show) }
    fun showHistory(show: Boolean) = mutableState.update { it.copy(historyOpen = show) }

    fun renameConversation(title: String) {
        val id = conversationId ?: return
        viewModelScope.launch {
            try {
                persistence.renameConversation(id, title)
            } catch (canceled: CancellationException) {
                throw canceled
            } catch (_: Exception) {
                mutableState.update { it.copy(message = ConversationText.Resource(R.string.error_unknown)) }
            }
        }
    }

    fun createConversation(kind: MediaKind) = navigate(kind) { conversationLifecycle.create(kind) }

    fun openMostRecentConversation(kind: MediaKind) = navigate(kind) { conversationLifecycle.mostRecent(kind) }

    fun openConversation(id: String, kind: MediaKind) = navigate(kind) { id }

    fun removeCurrentConversation() {
        val id = conversationId ?: return
        val kind = mutableState.value.mediaKind
        navigate(kind) { conversationLifecycle.removeAndOpenNext(id, kind) }
    }

    private fun navigate(kind: MediaKind, destination: suspend () -> String) {
        mutableState.update { it.copy(isTransitioning = true) }
        viewModelScope.launch {
            try {
                effectChannel.send(ConversationUiEffect.NavigateToConversation(destination(), kind))
            } catch (canceled: CancellationException) {
                throw canceled
            } catch (_: Exception) {
                mutableState.update { it.copy(isTransitioning = false, message = ConversationText.Resource(R.string.error_unknown)) }
            }
        }
    }

    fun selectWorkflow(workflow: WorkflowDescriptor) {
        draftTouched = true
        mutableState.update { current ->
            val selection = composerUseCase.selectWorkflow(current, workflow)
            current.copy(
                selectedWorkflow = workflow,
                attachmentSlots = selection.slots,
                attachments = selection.attachments,
                options = selection.options,
                modelMenuOpen = false,
                message = if (selection.activeImageIncompatible) {
                    ConversationText.Resource(R.string.message_model_cannot_edit_active_image)
                } else null,
            )
        }
        conversationId?.let { id ->
            viewModelScope.launch { persistence.saveSelectedWorkflow(id, workflow.id) }
        }
        persistDraft()
    }

    fun attachMedia(role: MediaRole, kind: MediaKind, uri: String, label: String) {
        draftTouched = true
        mutableState.update { current ->
            val slot = current.attachmentSlots.firstOrNull { it.role == role && it.kind == kind }
                ?: return@update current
            current.copy(
                attachments = current.attachments.filterNot { it.role == role } +
                    DraftMediaAttachment(role, slot.kind, uri, label),
            )
        }
        persistDraft()
    }

    fun removeMedia(role: MediaRole) {
        draftTouched = true
        mutableState.update { current ->
            current.copy(attachments = current.attachments.filterNot { it.role == role })
        }
        persistDraft()
    }

    fun updateOptions(options: GenerationOptions) {
        draftTouched = true
        mutableState.update { current ->
            current.copy(options = composerUseCase.retainOptionsFor(options, current.selectedWorkflow))
        }
        persistDraft()
    }

    fun currentDraft(): GenerationDraft? = composerUseCase.draftFor(mutableState.value, persistedActiveSourceOutputId)

    fun updateBrief(brief: CreativeBrief) {
        mutableState.update { it.copy(brief = brief, briefOpen = false) }
        conversationId?.let { id -> viewModelScope.launch { persistence.saveBrief(id, brief) } }
    }

    fun submitGeneration() {
        val id = conversationId ?: return
        val current = mutableState.value
        if (current.activeSourceId != null && current.timeline.none {
                it.id == current.activeSourceId && it.sourceOutputId != null
            }) {
            mutableState.update { it.copy(message = ConversationText.Resource(R.string.error_active_image_unavailable)) }
            return
        }
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
        val useCase = submitGenerationUseCase
            ?: return mutableState.update { it.copy(message = ConversationText.Resource(R.string.error_unknown)) }
        if (!mutableState.value.isOnline || mutableState.value.isSubmitting) return
        mutableState.update { it.copy(isSubmitting = true, message = null) }
        viewModelScope.launch {
            val result = try {
                useCase(conversationId, draft)
            } catch (canceled: CancellationException) {
                throw canceled
            } catch (error: Exception) {
                Result.failure(error)
            }
            mutableState.update { current ->
                current.copy(
                    prompt = if (result.isSuccess && clearPromptOnSuccess && current.conversationId == conversationId && current.prompt == draft.instruction) "" else current.prompt,
                    isSubmitting = false,
                    message = result.exceptionOrNull()?.toConversationText(),
                )
            }
            if (result.isSuccess && clearPromptOnSuccess && mutableState.value.conversationId == conversationId) persistDraft()
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
        val id = conversationId ?: return
        detachAwaitingDatabaseConfirmation = true
        persistedActiveSourceOutputId = null
        mutableState.update {
            it.copy(activeSourceId = null, activeSourceLabel = null, message = ConversationText.Resource(R.string.message_fresh_generation_started))
        }
        viewModelScope.launch {
            runCatching { persistence.selectActiveSource(id, null) }
                .onFailure {
                    detachAwaitingDatabaseConfirmation = false
                    mutableState.update { state -> state.copy(message = ConversationText.Resource(R.string.error_unknown)) }
                }
        }
    }

    fun useOutput(item: TimelineItem) {
        if (item.output?.kind != MediaKind.IMAGE || item.sourceOutputId == null || mutableState.value.mediaKind != MediaKind.IMAGE) return
        if (!composerUseCase.canEditOutput(mutableState.value, item)) {
            mutableState.update { it.copy(message = ConversationText.Resource(R.string.message_choose_compatible_editor)) }
            return
        }
        persistedActiveSourceOutputId = item.sourceOutputId
        draftTouched = true
        mutableState.update { current ->
            current.copy(
                activeSourceId = item.id,
                activeSourceLabel = item.outputLabel,
                message = ConversationText.Resource(R.string.message_older_output_selected),
            )
        }
        conversationId?.let { id ->
            viewModelScope.launch { persistence.selectActiveSource(id, item.sourceOutputId) }
        }
        persistDraft()
    }

    fun reuseParameters(item: TimelineItem) {
        val id = conversationId ?: return
        val reusable = composerUseCase.reusable(item, mutableState.value.mediaKind) ?: return
        val draft = reusable.draft
        val workflow = reusable.workflow
        draftTouched = true
        detachAwaitingDatabaseConfirmation = true
        persistedActiveSourceOutputId = null
        mutableState.update { current ->
            current.copy(
                selectedWorkflow = workflow,
                prompt = draft.instruction,
                brief = draft.creativeBrief,
                options = draft.options,
                attachmentSlots = reusable.slots,
                attachments = emptyList(),
                activeSourceId = null,
                activeSourceLabel = null,
                message = ConversationText.Resource(R.string.message_parameters_reused),
            )
        }
        viewModelScope.launch {
            try {
                persistence.saveSelectedWorkflow(id, workflow.id)
                persistence.saveBrief(id, draft.creativeBrief)
                persistence.selectActiveSource(id, null)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                detachAwaitingDatabaseConfirmation = false
                mutableState.update { it.copy(message = ConversationText.Resource(R.string.error_unknown)) }
            }
        }
        persistDraft()
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
        persistDraft()
        viewModelScope.launch {
            persistence.saveCompletedDemo(
                conversationId = id,
                generationId = item.id,
                parentGenerationId = item.parentId,
                workflowId = workflowId,
                instruction = item.prompt,
                outputKind = current.mediaKind,
            )
            persistence.deriveTitleFromFirstPrompt(id, item.prompt)
        }
    }

    private fun restoreSnapshot(snapshot: PersistedConversationSnapshot?) {
        if (snapshot == null) return
        if (snapshot.id != conversationId) return
        persistedActiveSourceOutputId = if (detachAwaitingDatabaseConfirmation) null else snapshot.activeSourceOutputId
        mutableState.update { current ->
            val restoredDraft = if (draftTouched) null else snapshot.draft
            val restoredWorkflow = snapshot.selectedWorkflowId?.let(WorkflowRegistry::find) ?: current.selectedWorkflow
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
            val activeGeneration = snapshot.activeSourceOutputId?.let { activeOutputId ->
                timeline.firstOrNull { it.sourceOutputId == activeOutputId }
            }
            if (detachAwaitingDatabaseConfirmation && snapshot.activeSourceOutputId == null) {
                detachAwaitingDatabaseConfirmation = false
            }
            current.copy(
                conversationId = snapshot.id,
                conversationTitle = snapshot.title,
                brief = snapshot.brief,
                selectedWorkflow = restoredWorkflow,
                timeline = timeline,
                activeSourceId = if (detachAwaitingDatabaseConfirmation) null else activeGeneration?.id,
                activeSourceLabel = if (detachAwaitingDatabaseConfirmation) null else activeGeneration?.outputLabel,
                prompt = restoredDraft?.prompt ?: current.prompt,
                options = restoredDraft?.options?.let { composerUseCase.retainOptionsFor(it, restoredWorkflow) } ?: current.options,
                attachments = restoredDraft?.attachments?.map { DraftMediaAttachment(it.role, it.kind, it.uri, it.label, it.remoteUrl) } ?: current.attachments,
                attachmentSlots = composerUseCase.slotsFor(restoredWorkflow),
                message = if (snapshot.requiresSourceSelection) {
                    ConversationText.Resource(R.string.message_select_output_before_continuing)
                } else current.message,
            )
        }
        snapshotLoaded = true
        finishWorkspaceTransitionIfReady()
    }

    private fun restoreGenerationRecords(records: List<GenerationRecord>) {
        records.forEach(::startStatusPollingIfNeeded)
        mutableState.update { current ->
            val timeline = records.map(GenerationRecord::toTimelineItem)
            val activeImage = timeline.firstOrNull {
                it.sourceOutputId == persistedActiveSourceOutputId && it.output?.kind == MediaKind.IMAGE
            }
            current.copy(
                timeline = timeline,
                activeSourceId = activeImage?.id,
                activeSourceLabel = activeImage?.outputLabel,
            )
        }
        recordsLoaded = true
        finishWorkspaceTransitionIfReady()
    }

    private fun finishWorkspaceTransitionIfReady() {
        if (snapshotLoaded && recordsLoaded) mutableState.update { it.copy(isTransitioning = false) }
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

    private fun persistDraft() {
        val id = conversationId ?: return
        val current = mutableState.value
        draftWrites.trySend(Triple(id, current.mediaKind, PersistedComposerDraft(
            prompt = current.prompt,
            options = current.options,
            attachments = current.attachments.map { PersistedDraftAttachment(it.role, it.kind, it.uri, it.label, it.remoteUrl) },
        )))
    }

    private companion object {
        const val LOCAL_SOURCE_ID = "local-source"
        const val DEMO_GENERATION_PREFIX = "demo-"
        const val OUTPUT_SUFFIX = "-output"
    }
}

private fun Throwable.toConversationText(): ConversationText = when (this) {
    is GenerationSubmissionException -> appError.userMessage?.let(ConversationText::Dynamic)
        ?: ConversationText.Resource(appError.messageResId)
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
        errorText = when (status) {
            is GenerationStatus.Failed -> status.userMessage.takeIf(String::isNotBlank)?.let(ConversationText::Dynamic)
            is GenerationStatus.UnknownSubmissionOutcome -> status.userMessage.takeIf(String::isNotBlank)?.let(ConversationText::Dynamic)
            else -> ErrorMapper.messageResIdFor(errorCode)?.let(ConversationText::Resource)
        },
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
