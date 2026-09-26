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
import kotlinx.coroutines.delay
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

/** Debounces full-draft writes while retaining the latest pending draft for every conversation. */
internal class ComposerDraftWriter(private val persistence: ConversationPersistence) {
    private val wakeups = Channel<Unit>(Channel.CONFLATED)
    private val pending = mutableMapOf<String, Pair<MediaKind, PersistedComposerDraft>>()
    private val guard = Any()

    fun start(scope: CoroutineScope, onFailure: (String) -> Unit) {
        scope.launch {
            for (ignored in wakeups) {
                delay(DRAFT_WRITE_DEBOUNCE_MILLIS)
                drain().forEach { (id, value) ->
                    persist(id, value.first, value.second, onFailure)
                }
            }
        }
    }

    fun enqueue(id: String, kind: MediaKind, draft: PersistedComposerDraft) {
        synchronized(guard) { pending[id] = kind to draft }
        wakeups.trySend(Unit)
    }

    suspend fun flush(id: String, onFailure: (String) -> Unit) {
        val value = synchronized(guard) { pending.remove(id) } ?: return
        persist(id, value.first, value.second, onFailure)
    }

    private fun drain(): Map<String, Pair<MediaKind, PersistedComposerDraft>> = synchronized(guard) {
        pending.toMap().also { pending.clear() }
    }

    private suspend fun persist(
        id: String,
        kind: MediaKind,
        draft: PersistedComposerDraft,
        onFailure: (String) -> Unit,
    ) {
        try {
            persistence.ensureConversation(id, kind, WorkflowRegistry.forKind(kind).firstOrNull()?.id)
            persistence.saveDraft(id, draft)
        } catch (canceled: CancellationException) {
            throw canceled
        } catch (_: Exception) {
            onFailure(id)
        }
    }

    private companion object {
        const val DRAFT_WRITE_DEBOUNCE_MILLIS = 250L
    }
}
