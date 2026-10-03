package com.higgsfield.mobile.feature.conversation

import com.higgsfield.mobile.core.database.ConversationPersistence
import com.higgsfield.mobile.core.database.PersistedComposerDraft
import com.higgsfield.mobile.core.model.CreativeBrief
import com.higgsfield.mobile.core.model.WorkflowDescriptor
import javax.inject.Inject
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Serializes source and model changes; parameter reuse persists its full selection in one transaction. */
class SourceSelectionUseCase @Inject constructor(
    private val persistence: ConversationPersistence,
) {
    private val mutationMutex = Mutex()

    suspend fun selectOutput(conversationId: String, outputId: String) {
        mutationMutex.withLock { persistence.selectActiveSource(conversationId, outputId) }
    }

    suspend fun detach(conversationId: String) {
        mutationMutex.withLock { persistence.selectActiveSource(conversationId, null) }
    }

    suspend fun selectWorkflow(conversationId: String, workflow: WorkflowDescriptor) {
        mutationMutex.withLock { persistence.saveSelectedWorkflow(conversationId, workflow.id) }
    }

    suspend fun reuseParameters(
        conversationId: String,
        workflow: WorkflowDescriptor,
        brief: CreativeBrief,
        draft: PersistedComposerDraft,
    ) {
        mutationMutex.withLock { persistence.reuseParameters(conversationId, workflow.id, brief, draft) }
    }
}
