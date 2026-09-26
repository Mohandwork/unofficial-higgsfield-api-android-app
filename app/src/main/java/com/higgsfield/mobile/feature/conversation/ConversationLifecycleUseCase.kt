package com.higgsfield.mobile.feature.conversation

import com.higgsfield.mobile.core.database.ConversationPersistence
import com.higgsfield.mobile.core.model.MediaKind
import com.higgsfield.mobile.core.model.WorkflowRegistry
import javax.inject.Inject

/** Chooses a valid destination after conversation creation, switching, or removal. */
class ConversationLifecycleUseCase @Inject constructor(
    private val persistence: ConversationPersistence,
) {
    suspend fun create(kind: MediaKind): String =
        persistence.createConversation(kind, WorkflowRegistry.forKind(kind).firstOrNull()?.id)

    suspend fun mostRecent(kind: MediaKind): String =
        persistence.mostRecentConversation(kind, WorkflowRegistry.forKind(kind).firstOrNull()?.id)

    suspend fun removeAndOpenNext(id: String, kind: MediaKind): String {
        persistence.deleteConversation(id)
        return mostRecent(kind)
    }
}
