package com.promptstudio.app.feature.conversation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.promptstudio.app.R
import com.promptstudio.app.core.connectivity.AlwaysOnlineConnectivityStatusProvider
import com.promptstudio.app.core.connectivity.ConnectivityStatusProvider
import com.promptstudio.app.core.data.GenerationRepository
import com.promptstudio.app.core.data.GenerationSubmissionException
import com.promptstudio.app.core.database.ConversationPersistence
import com.promptstudio.app.core.database.PersistedComposerDraft
import com.promptstudio.app.core.database.PersistedConversationSnapshot
import com.promptstudio.app.core.database.PersistedDraftAttachment
import com.promptstudio.app.core.database.PersistedGenerationStatus
import com.promptstudio.app.core.error.ErrorMapper
import com.promptstudio.app.core.model.CreativeBrief
import com.promptstudio.app.core.model.GenerationDraft
import com.promptstudio.app.core.model.GenerationOptions
import com.promptstudio.app.core.model.GenerationRecord
import com.promptstudio.app.core.model.GenerationStatus
import com.promptstudio.app.core.model.MediaKind
import com.promptstudio.app.core.model.MediaRole
import com.promptstudio.app.core.model.WorkflowCapability
import com.promptstudio.app.core.model.WorkflowDescriptor
import com.promptstudio.app.core.model.WorkflowId
import com.promptstudio.app.core.model.WorkflowRegistry
import com.promptstudio.app.core.network.RequestStatusPoller
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConversationViewModel @Inject constructor(
    private val persistence: ConversationPersistence,
    //TODO figure out whatever is wrong here
    private val connectivity: ConnectivityStatusProvider = AlwaysOnlineConnectivityStatusProvider,
    private val generationRepository: GenerationRepository? = null,
    private val statusPoller: RequestStatusPoller? = null,
    private val conversationLifecycle: ConversationLifecycleUseCase = ConversationLifecycleUseCase(persistence),
    private val submitGenerationUseCase: SubmitGenerationUseCase? = generationRepository?.let { SubmitGenerationUseCase(it, persistence) },
    private val composerUseCase: ConversationComposerUseCase = ConversationComposerUseCase(),
    private val sourceSelectionUseCase: SourceSelectionUseCase = SourceSelectionUseCase(persistence),
) : ViewModel() {
    private val mutableState = MutableStateFlow(ConversationUiState())
    val state: StateFlow<ConversationUiState> = mutableState.asStateFlow()
    private val effectChannel = Channel<ConversationUiEffect>(Channel.BUFFERED)
    val effects = effectChannel.receiveAsFlow()
    private var conversationId: String? = null
    private val workspace = ConversationWorkspaceCoordinator(persistence, connectivity, generationRepository, statusPoller)
    private val draftWriter = ComposerDraftWriter(persistence)
    private var sourceConfirmationPending = false
    private var pendingSourceOutputId: String? = null
    private var lastObservedSourceOutputId: String? = null
    private var workflowConfirmationPending = false
    private var pendingWorkflowId: WorkflowId? = null
    private var lastObservedWorkflowId: WorkflowId? = null
    private var persistedActiveSourceOutputId: String? = null
    private var draftTouched = false
    private var briefTouched = false
    private var snapshotLoaded = false
    private var recordsLoaded = false
    private var creatingConversation = false
    private var submitAfterCreation = false
    private var workspaceVersion = 0
    init {
        draftWriter.start(viewModelScope, ::onDraftWriteFailure)
    }

    fun initialize(kind: MediaKind, requestedConversationId: String? = null) {
        val id = requestedConversationId ?: (kind.name.lowercase() + "-default")
        if (conversationId == id && mutableState.value.workflows.isNotEmpty()) return
        workspaceVersion++
        conversationId?.takeIf { it != id }?.let { previousId ->
            viewModelScope.launch { draftWriter.flush(previousId, ::onDraftWriteFailure) }
        }
        val workflows = WorkflowRegistry.forKind(kind)
        mutableState.value = ConversationUiState(
            chat = ChatUiState(mediaKind = kind, id = id, isTransitioning = true),
            composer = ComposerUiState(
                workflows = workflows,
                selectedWorkflow = workflows.firstOrNull(),
                attachmentSlots = composerUseCase.slotsFor(workflows.firstOrNull()),
            ),
        )
        conversationId = id
        draftTouched = false
        briefTouched = false
        snapshotLoaded = false
        recordsLoaded = generationRepository == null
        persistedActiveSourceOutputId = null
        sourceConfirmationPending = false
        pendingSourceOutputId = null
        lastObservedSourceOutputId = null
        workflowConfirmationPending = false
        pendingWorkflowId = null
        lastObservedWorkflowId = null
        observeSavedConversation(id)
    }

    fun initializeEmpty(kind: MediaKind, force: Boolean = false) {
        if (!force && conversationId == null && mutableState.value.workflows.isNotEmpty() && mutableState.value.mediaKind == kind) return
        workspaceVersion++
        conversationId = null
        creatingConversation = false
        submitAfterCreation = false
        draftTouched = false
        briefTouched = false
        val workflows = WorkflowRegistry.forKind(kind)
        mutableState.value = ConversationUiState(
            chat = ChatUiState(mediaKind = kind),
            composer = ComposerUiState(
                workflows = workflows,
                selectedWorkflow = workflows.firstOrNull(),
                attachmentSlots = composerUseCase.slotsFor(workflows.firstOrNull()),
            ),
        )
        workspace.observeEmpty(
            viewModelScope,
            onHistory = { conversations -> mutableState.update { it.copy(chat = it.chat.copy(conversations = conversations)) } },
            onConnectivity = { online -> mutableState.update { it.copy(isOnline = online) } },
            onFailure = { mutableState.update { it.copy(message = ConversationText.Resource(R.string.error_unknown)) } },
        )
    }

    private fun observeSavedConversation(id: String) {
        workspace.observe(
            scope = viewModelScope,
            id = id,
            onSnapshot = ::restoreSnapshot,
            onRecords = { records -> if (conversationId == id) restoreGenerationRecords(records) },
            onHistory = { conversations -> mutableState.update { it.copy(chat = it.chat.copy(conversations = conversations)) } },
            onConnectivity = { online -> mutableState.update { it.copy(isOnline = online) } },
            onFailure = {
                if (conversationId == id) mutableState.update {
                    it.copy(chat = it.chat.copy(isTransitioning = false), message = ConversationText.Resource(R.string.error_unknown))
                }
            }
        )
    }

    fun updatePrompt(value: String) {
        draftTouched = true
        mutableState.update { it.copy(composer = it.composer.copy(prompt = value), message = null) }
        persistDraft()
        if (conversationId == null && value.isNotBlank()) createFromComposer()
    }

    fun onEvent(event: ConversationUiEvent) {
        when (event) {
            is ConversationUiEvent.Chat -> onChatEvent(event)
            is ConversationUiEvent.Composer -> onComposerEvent(event)
            is ConversationUiEvent.Generation -> onGenerationEvent(event)
            is ConversationUiEvent.Panel -> Unit // Panels are view-local state owned by ConversationScreen.
        }
    }

    private fun onChatEvent(event: ConversationUiEvent.Chat) {
        when (event) {
            is ConversationUiEvent.SelectMediaKind -> selectMediaKind(event.kind)
            is ConversationUiEvent.OpenConversation -> openConversation(event.id, event.kind)
            is ConversationUiEvent.CreateConversation -> createConversation(event.kind)
            is ConversationUiEvent.RemoveConversation -> removeConversation(event.id, event.kind)
            is ConversationUiEvent.RenameConversation -> renameConversation(event.id, event.title)
        }
    }

    private fun onComposerEvent(event: ConversationUiEvent.Composer) {
        when (event) {
            is ConversationUiEvent.ChangePrompt -> updatePrompt(event.value)
            is ConversationUiEvent.SelectWorkflow -> selectWorkflow(event.workflow)
            is ConversationUiEvent.UpdateBrief -> updateBrief(event.brief)
            is ConversationUiEvent.UpdateOptions -> updateOptions(event.options)
            is ConversationUiEvent.PickMedia -> viewModelScope.launch {
                effectChannel.send(ConversationUiEffect.LaunchMediaPicker(event.role, event.kind))
            }
            is ConversationUiEvent.MediaPicked -> attachMedia(event.role, event.kind, event.uri, event.label)
            is ConversationUiEvent.RemoveMedia -> removeMedia(event.role, event.uri)
            ConversationUiEvent.DetachSource -> detachSource()
            is ConversationUiEvent.EditImage -> useOutput(event.item)
            is ConversationUiEvent.ReuseParameters -> reuseParameters(event.item)
        }
    }

    private fun onGenerationEvent(event: ConversationUiEvent.Generation) {
        when (event) {
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

    fun renameConversation(id: String, title: String) {
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

    fun createConversation(kind: MediaKind) {
        if (conversationId == null && kind == mutableState.value.mediaKind) {
            initializeEmpty(kind, force = true)
        } else {
            navigate(kind) { null }
        }
    }

    fun selectMediaKind(kind: MediaKind) {
        if (kind == mutableState.value.mediaKind) return
        navigate(kind) { null }
    }

    fun openConversation(id: String, kind: MediaKind) = navigate(kind) { id }

    fun removeCurrentConversation() {
        val id = conversationId ?: return
        val kind = mutableState.value.mediaKind
        removeConversation(id, kind)
    }

    fun removeConversation(id: String, kind: MediaKind) {
        if (id == conversationId) {
            navigate(kind) { conversationLifecycle.removeAndOpenNext(id, kind) }
            return
        }
        viewModelScope.launch {
            try {
                persistence.deleteConversation(id)
            } catch (canceled: CancellationException) {
                throw canceled
            } catch (_: Exception) {
                mutableState.update { it.copy(message = ConversationText.Resource(R.string.error_unknown)) }
            }
        }
    }

    private fun navigate(kind: MediaKind, destination: suspend () -> String?) {
        mutableState.update { it.copy(chat = it.chat.copy(isTransitioning = true)) }
        viewModelScope.launch {
            try {
                val id = destination()
                if (id == conversationId && kind == mutableState.value.mediaKind) {
                    mutableState.update { it.copy(chat = it.chat.copy(isTransitioning = false)) }
                    return@launch
                }
                effectChannel.send(ConversationUiEffect.NavigateToConversation(id, kind))
            } catch (canceled: CancellationException) {
                throw canceled
            } catch (_: Exception) {
                mutableState.update { it.copy(chat = it.chat.copy(isTransitioning = false), message = ConversationText.Resource(R.string.error_unknown)) }
            }
        }
    }

    fun selectWorkflow(workflow: WorkflowDescriptor) {
        val id = conversationId
        if (id == null) {
            draftTouched = true
            mutableState.update { current ->
                val selection = composerUseCase.selectWorkflow(current, workflow)
                current.copy(composer = current.composer.copy(
                    selectedWorkflow = workflow,
                    attachmentSlots = selection.slots,
                    attachments = selection.attachments,
                    options = selection.options,
                ))
            }
            return
        }
        viewModelScope.launch {
            try {
                sourceSelectionUseCase.selectWorkflow(id, workflow)
                if (conversationId != id) return@launch
                awaitWorkflowConfirmation(workflow.id)
                draftTouched = true
                mutableState.update { current ->
                    val selection = composerUseCase.selectWorkflow(current, workflow)
                    current.copy(
                        composer = current.composer.copy(
                            selectedWorkflow = workflow,
                            attachmentSlots = selection.slots,
                            attachments = selection.attachments,
                            options = selection.options,
                        ),
                        message = if (selection.activeImageIncompatible) {
                            ConversationText.Resource(R.string.message_model_cannot_edit_active_image)
                        } else null,
                    )
                }
                persistDraft()
            } catch (canceled: CancellationException) {
                throw canceled
            } catch (_: Exception) {
                if (conversationId == id) mutableState.update {
                    it.copy(message = ConversationText.Resource(R.string.error_unknown))
                }
            }
        }
    }

    fun attachMedia(role: MediaRole, kind: MediaKind, uri: String, label: String) {
        draftTouched = true
        mutableState.update { current ->
            val slot = current.attachmentSlots.firstOrNull { it.role == role && it.kind == kind }
                ?: return@update current
            if (current.attachments.any { it.uri == uri && it.role == role }) return@update current
            val sameRole = current.attachments.filter { it.role == role && it.kind == kind }
            val combinedMaximum = current.selectedWorkflow?.maximumCombinedReferences
            if (combinedMaximum != null && role in setOf(MediaRole.REFERENCE, MediaRole.VIDEO_REFERENCE) &&
                current.attachments.count { it.role in setOf(MediaRole.REFERENCE, MediaRole.VIDEO_REFERENCE) } >= combinedMaximum) {
                return@update current.copy(message = ConversationText.Resource(R.string.message_photo_reference_limit, listOf(combinedMaximum)))
            }
            if (slot.maximumCount != null && slot.maximumCount > 1 && sameRole.size >= slot.maximumCount) return@update current.copy(
                message = ConversationText.Resource(
                    if (kind == MediaKind.VIDEO) R.string.message_video_reference_limit else R.string.message_photo_reference_limit,
                    listOf(slot.maximumCount),
                ),
            )
            current.copy(
                composer = current.composer.copy(
                    attachments = (if (slot.maximumCount == 1) current.attachments.filterNot { it.role == role && it.kind == kind } else current.attachments) +
                        DraftMediaAttachment(role, slot.kind, uri, label),
                ),
                message = null,
            )
        }
        persistDraft()
    }

    fun removeMedia(role: MediaRole, uri: String) {
        draftTouched = true
        mutableState.update { current ->
            current.copy(composer = current.composer.copy(attachments = current.attachments.filterNot { it.role == role && it.uri == uri }))
        }
        persistDraft()
    }

    fun updateOptions(options: GenerationOptions) {
        draftTouched = true
        mutableState.update { current ->
            current.copy(composer = current.composer.copy(options = composerUseCase.retainOptionsFor(options, current.selectedWorkflow)))
        }
        persistDraft()
    }

    fun currentDraft(): GenerationDraft? = composerUseCase.draftFor(mutableState.value, persistedActiveSourceOutputId)

    fun updateBrief(brief: CreativeBrief) {
        briefTouched = true
        mutableState.update { it.copy(composer = it.composer.copy(brief = brief)) }
        conversationId?.let { id ->
            viewModelScope.launch {
                try {
                    persistence.saveBrief(id, brief)
                } catch (canceled: CancellationException) {
                    throw canceled
                } catch (_: Exception) {
                    if (conversationId == id && mutableState.value.brief == brief) mutableState.update {
                        it.copy(message = ConversationText.Resource(R.string.error_unknown))
                    }
                }
            }
        }
    }

    fun submitGeneration() {
        val current = mutableState.value
        if (conversationId == null) {
            if (composerUseCase.draftFor(current, null) != null) {
                submitAfterCreation = true
                createFromComposer()
            }
            return
        }
        val id = conversationId ?: return
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
        mutableState.update { it.copy(generation = it.generation.copy(isSubmitting = true), message = null) }
        viewModelScope.launch {
            val result = try {
                useCase(conversationId, draft)
            } catch (canceled: CancellationException) {
                throw canceled
            } catch (error: Exception) {
                Result.failure(error)
            }
            mutableState.update { current ->
                if (current.conversationId != conversationId) return@update current
                current.copy(
                    composer = current.composer.copy(prompt = if (result.isSuccess && clearPromptOnSuccess && current.prompt == draft.instruction) "" else current.prompt),
                    generation = current.generation.copy(isSubmitting = false),
                    message = result.exceptionOrNull()?.toConversationText(),
                )
            }
            if (result.isSuccess && clearPromptOnSuccess && mutableState.value.conversationId == conversationId) persistDraft(flush = true)
            result.getOrNull()?.let { workspace.pollIfNeeded(viewModelScope, it) }
        }
    }
    // TODO deprecated function
    fun attachSource(label: String) = mutableState.update {
        it.copy(
            composer = it.composer.copy(activeSourceId = LOCAL_SOURCE_ID, activeSourceLabel = ConversationText.Dynamic(label)),
            message = ConversationText.Resource(R.string.message_reference_image_attached),
        )
    }

    fun detachSource() {
        val id = conversationId ?: return
        viewModelScope.launch {
            try {
                sourceSelectionUseCase.detach(id)
                if (conversationId != id) return@launch
                awaitSourceConfirmation(null)
                mutableState.update { current ->
                    current.copy(composer = current.composer.copy(activeSourceId = null, activeSourceLabel = null),
                        message = ConversationText.Resource(R.string.message_fresh_generation_started))
                }
            } catch (canceled: CancellationException) {
                throw canceled
            } catch (_: Exception) {
                if (conversationId == id) mutableState.update { it.copy(message = ConversationText.Resource(R.string.error_unknown)) }
            }
        }
    }

    fun useOutput(item: TimelineItem) {
        if (item.output?.kind != MediaKind.IMAGE || item.sourceOutputId == null || mutableState.value.mediaKind != MediaKind.IMAGE) return
        if (!composerUseCase.canEditOutput(mutableState.value, item)) {
            mutableState.update { it.copy(message = ConversationText.Resource(R.string.message_choose_compatible_editor)) }
            return
        }
        val id = conversationId ?: return
        viewModelScope.launch {
            try {
                sourceSelectionUseCase.selectOutput(id, item.sourceOutputId)
                if (conversationId != id) return@launch
                awaitSourceConfirmation(item.sourceOutputId)
                draftTouched = true
                mutableState.update { current ->
                    current.copy(
                        composer = current.composer.copy(activeSourceId = item.id, activeSourceLabel = item.outputLabel),
                        message = ConversationText.Resource(R.string.message_older_output_selected),
                    )
                }
                persistDraft(flush = true)
            } catch (canceled: CancellationException) {
                throw canceled
            } catch (_: Exception) {
                if (conversationId == id) mutableState.update { it.copy(message = ConversationText.Resource(R.string.error_unknown)) }
            }
        }
    }

    fun reuseParameters(item: TimelineItem) {
        val id = conversationId ?: return
        val reusable = composerUseCase.reusable(item, mutableState.value.mediaKind) ?: return
        val draft = reusable.draft
        val workflow = reusable.workflow
        viewModelScope.launch {
            try {
                sourceSelectionUseCase.reuseParameters(
                    id, workflow, draft.creativeBrief,
                    PersistedComposerDraft(prompt = draft.instruction, options = draft.options),
                )
                if (conversationId != id) return@launch
                awaitSourceConfirmation(null)
                awaitWorkflowConfirmation(workflow.id)
                draftTouched = true
                briefTouched = true
                mutableState.update { current ->
                    current.copy(
                        composer = current.composer.copy(
                            selectedWorkflow = workflow,
                            prompt = draft.instruction,
                            brief = draft.creativeBrief,
                            options = draft.options,
                            attachmentSlots = reusable.slots,
                            attachments = emptyList(),
                            activeSourceId = null,
                            activeSourceLabel = null,
                        ),
                        message = ConversationText.Resource(R.string.message_parameters_reused),
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                if (conversationId == id) mutableState.update { it.copy(message = ConversationText.Resource(R.string.error_unknown)) }
            }
        }
    }

    private fun awaitSourceConfirmation(outputId: String?) {
        persistedActiveSourceOutputId = outputId
        pendingSourceOutputId = outputId
        sourceConfirmationPending = !snapshotLoaded || lastObservedSourceOutputId != outputId
    }

    private fun awaitWorkflowConfirmation(workflowId: WorkflowId) {
        pendingWorkflowId = workflowId
        workflowConfirmationPending = !snapshotLoaded || lastObservedWorkflowId != workflowId
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
                composer = current.composer.copy(
                    prompt = "",
                    activeSourceId = if (current.mediaKind == MediaKind.IMAGE) id else current.activeSourceId,
                    activeSourceLabel = if (current.mediaKind == MediaKind.IMAGE) label else current.activeSourceLabel,
                ),
                generation = current.generation.copy(timeline = current.timeline + item),
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
        lastObservedSourceOutputId = snapshot.activeSourceOutputId
        lastObservedWorkflowId = snapshot.selectedWorkflowId
        if (sourceConfirmationPending && snapshot.activeSourceOutputId == pendingSourceOutputId) {
            sourceConfirmationPending = false
        }
        if (workflowConfirmationPending && snapshot.selectedWorkflowId == pendingWorkflowId) {
            workflowConfirmationPending = false
        }
        val activeSourceOutputId = if (sourceConfirmationPending) pendingSourceOutputId else snapshot.activeSourceOutputId
        persistedActiveSourceOutputId = activeSourceOutputId
        mutableState.update { current ->
            val restoredDraft = if (draftTouched) null else snapshot.draft
            val restoredWorkflowId = if (workflowConfirmationPending) pendingWorkflowId else snapshot.selectedWorkflowId
            val restoredWorkflow = restoredWorkflowId?.let(WorkflowRegistry::find) ?: current.selectedWorkflow
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
            val activeGeneration = activeSourceOutputId?.let { activeOutputId ->
                timeline.firstOrNull { it.sourceOutputId == activeOutputId }
            }
            current.copy(
                chat = current.chat.copy(id = snapshot.id, title = snapshot.title),
                composer = current.composer.copy(
                    brief = if (briefTouched) current.brief else snapshot.brief,
                    selectedWorkflow = restoredWorkflow,
                    activeSourceId = activeGeneration?.id,
                    activeSourceLabel = activeGeneration?.outputLabel,
                    prompt = restoredDraft?.prompt ?: current.prompt,
                    options = restoredDraft?.options?.let { composerUseCase.retainOptionsFor(it, restoredWorkflow) } ?: current.options,
                    attachments = restoredDraft?.attachments?.map { DraftMediaAttachment(it.role, it.kind, it.uri, it.label, it.remoteUrl) } ?: current.attachments,
                    attachmentSlots = composerUseCase.slotsFor(restoredWorkflow),
                ),
                generation = current.generation.copy(timeline = timeline),
                message = if (snapshot.requiresSourceSelection && !sourceConfirmationPending) {
                    ConversationText.Resource(R.string.message_select_output_before_continuing)
                } else current.message,
            )
        }
        snapshotLoaded = true
        finishWorkspaceTransitionIfReady()
    }

    private fun restoreGenerationRecords(records: List<GenerationRecord>) {
        mutableState.update { current ->
            val timeline = records.map(GenerationRecord::toTimelineItem)
            val activeImage = timeline.firstOrNull {
                it.sourceOutputId == persistedActiveSourceOutputId && it.output?.kind == MediaKind.IMAGE
            }
            current.copy(
                generation = current.generation.copy(timeline = timeline),
                composer = current.composer.copy(activeSourceId = activeImage?.id, activeSourceLabel = activeImage?.outputLabel),
            )
        }
        recordsLoaded = true
        finishWorkspaceTransitionIfReady()
    }

    private fun finishWorkspaceTransitionIfReady() {
        if (snapshotLoaded && recordsLoaded) mutableState.update { it.copy(chat = it.chat.copy(isTransitioning = false)) }
    }

    private fun createFromComposer() {
        if (conversationId != null || creatingConversation) return
        creatingConversation = true
        val version = workspaceVersion
        viewModelScope.launch {
            val initial = mutableState.value
            try {
                val id = conversationLifecycle.createFromDraft(
                    initial.mediaKind,
                    initial.selectedWorkflow?.id,
                    initial.brief,
                    persistedDraft(initial),
                )
                if (workspaceVersion != version) return@launch
                conversationId = id
                mutableState.update { it.copy(chat = it.chat.copy(id = id)) }
                draftTouched = true
                briefTouched = true
                snapshotLoaded = false
                recordsLoaded = generationRepository == null
                val latest = mutableState.value
                var latestSaved = true
                if (latest.composer != initial.composer) {
                    try {
                        val workflowId = latest.selectedWorkflow?.id ?: error("A model is required")
                        persistence.reuseParameters(id, workflowId, latest.brief, persistedDraft(latest))
                    } catch (canceled: CancellationException) {
                        throw canceled
                    } catch (_: Exception) {
                        latestSaved = false
                        mutableState.update { it.copy(message = ConversationText.Resource(R.string.error_unknown)) }
                    }
                }
                creatingConversation = false
                observeSavedConversation(id)
                if (submitAfterCreation && latestSaved) {
                    submitAfterCreation = false
                    submitGeneration()
                } else {
                    submitAfterCreation = false
                }
            } catch (canceled: CancellationException) {
                throw canceled
            } catch (_: Exception) {
                creatingConversation = false
                submitAfterCreation = false
                mutableState.update { it.copy(message = ConversationText.Resource(R.string.error_unknown)) }
            }
        }
    }

    private fun persistedDraft(state: ConversationUiState) = PersistedComposerDraft(
        prompt = state.prompt,
        options = state.options,
        attachments = state.attachments.map { PersistedDraftAttachment(it.role, it.kind, it.uri, it.label, it.remoteUrl) },
    )

    private fun persistDraft(flush: Boolean = false) {
        val id = conversationId ?: return
        val current = mutableState.value
        draftWriter.enqueue(id, current.mediaKind, persistedDraft(current))
        if (flush) viewModelScope.launch { draftWriter.flush(id, ::onDraftWriteFailure) }
    }

    private fun onDraftWriteFailure(id: String) {
        if (mutableState.value.conversationId == id) {
            mutableState.update { it.copy(message = ConversationText.Resource(R.string.error_unknown)) }
        }
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
