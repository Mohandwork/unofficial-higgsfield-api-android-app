package com.higgsfield.mobile.feature.conversation

import com.higgsfield.mobile.core.database.ConversationPersistence
import com.higgsfield.mobile.core.database.PersistedComposerDraft
import com.higgsfield.mobile.core.model.CreativeBrief
import com.higgsfield.mobile.core.model.MediaKind
import com.higgsfield.mobile.core.model.WorkflowId
import javax.inject.Inject

/** Chooses a valid destination after conversation creation, switching, or removal. */
class ConversationLifecycleUseCase @Inject constructor(
    private val persistence: ConversationPersistence,
) {
    suspend fun createFromDraft(kind: MediaKind, workflowId: WorkflowId?, brief: CreativeBrief, draft: PersistedComposerDraft): String =
        persistence.createConversationFromDraft(kind, workflowId, brief, draft)

    suspend fun mostRecent(kind: MediaKind): String? = persistence.mostRecentConversation(kind)

    suspend fun removeAndOpenNext(id: String, kind: MediaKind): String? {
        persistence.deleteConversation(id)
        return mostRecent(kind)
    }
}
