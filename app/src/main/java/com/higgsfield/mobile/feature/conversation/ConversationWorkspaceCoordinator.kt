package com.higgsfield.mobile.feature.conversation

import com.higgsfield.mobile.core.connectivity.ConnectivityStatusProvider
import com.higgsfield.mobile.core.database.ConversationPersistence
import com.higgsfield.mobile.core.database.ConversationSummary
import com.higgsfield.mobile.core.database.PersistedComposerDraft
import com.higgsfield.mobile.core.database.PersistedConversationSnapshot
import com.higgsfield.mobile.core.data.GenerationRepository
import com.higgsfield.mobile.core.model.GenerationRecord
import com.higgsfield.mobile.core.model.GenerationStatus
import com.higgsfield.mobile.core.model.MediaKind
import com.higgsfield.mobile.core.model.WorkflowRegistry
import com.higgsfield.mobile.core.network.RequestStatusPoller
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

/** Owns workspace subscriptions and request polling across chat switches. */
internal class ConversationWorkspaceCoordinator(
    private val persistence: ConversationPersistence,
    private val connectivity: ConnectivityStatusProvider,
    private val generationRepository: GenerationRepository?,
    private val statusPoller: RequestStatusPoller?,
) {
    private val observations = mutableListOf<Job>()
    private val pollingJobs = mutableMapOf<String, Job>()

    fun observe(
        scope: CoroutineScope,
        id: String,
        kind: MediaKind,
        onSnapshot: (PersistedConversationSnapshot?) -> Unit,
        onRecords: (List<GenerationRecord>) -> Unit,
        onHistory: (List<ConversationSummary>) -> Unit,
        onConnectivity: (Boolean) -> Unit,
        onFailure: () -> Unit,
    ) {
        stop()
        observations += scope.launch { connectivity.isOnline.collect(onConnectivity) }
        observations += scope.launch {
            try {
                persistence.ensureConversation(id, kind, WorkflowRegistry.forKind(kind).firstOrNull()?.id)
                persistence.observe(id).collect(onSnapshot)
            } catch (canceled: CancellationException) {
                throw canceled
            } catch (_: Exception) {
                onFailure()
            }
        }
        observations += scope.launch {
            try {
                persistence.observeConversations().collect(onHistory)
            } catch (canceled: CancellationException) {
                throw canceled
            } catch (_: Exception) {
                onFailure()
            }
        }
        generationRepository?.let { repository ->
            observations += scope.launch {
                try {
                    repository.observeConversation(id).collect { records ->
                        records.forEach { pollIfNeeded(scope, it) }
                        onRecords(records)
                    }
                } catch (canceled: CancellationException) {
                    throw canceled
                } catch (_: Exception) {
                    onFailure()
                }
            }
        }
    }

    fun pollIfNeeded(scope: CoroutineScope, record: GenerationRecord) {
        if (statusPoller == null) return
        if (record.status !is GenerationStatus.Queued && record.status !is GenerationStatus.InProgress) return
        if (pollingJobs[record.id]?.isActive == true) return
        pollingJobs[record.id] = scope.launch {
            try {
                statusPoller.pollUntilTerminal(record.id)
            } finally {
                pollingJobs.remove(record.id)
            }
        }
    }

    fun stop() {
        observations.forEach(Job::cancel)
        observations.clear()
        pollingJobs.values.forEach(Job::cancel)
        pollingJobs.clear()
    }
}

/** Preserves write ordering so a chat switch cannot persist an older draft last. */
internal class ComposerDraftWriter(private val persistence: ConversationPersistence) {
    private val writes = Channel<Triple<String, MediaKind, PersistedComposerDraft>>(Channel.UNLIMITED)

    fun start(scope: CoroutineScope, onFailure: (String) -> Unit) {
        scope.launch {
            for ((id, kind, draft) in writes) {
                try {
                    persistence.ensureConversation(id, kind, WorkflowRegistry.forKind(kind).firstOrNull()?.id)
                    persistence.saveDraft(id, draft)
                } catch (canceled: CancellationException) {
                    throw canceled
                } catch (_: Exception) {
                    onFailure(id)
                }
            }
        }
    }

    fun enqueue(id: String, kind: MediaKind, draft: PersistedComposerDraft) {
        writes.trySend(Triple(id, kind, draft))
    }
}
