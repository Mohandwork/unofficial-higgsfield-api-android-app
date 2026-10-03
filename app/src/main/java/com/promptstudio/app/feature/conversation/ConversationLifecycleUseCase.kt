package com.promptstudio.app.feature.conversation

import com.promptstudio.app.core.database.ConversationPersistence
import com.promptstudio.app.core.model.MediaKind
import com.promptstudio.app.core.model.WorkflowRegistry
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
