package com.higgsfield.mobile.feature.conversation

import com.higgsfield.mobile.core.database.ConversationPersistence
import com.higgsfield.mobile.core.model.CreativeBrief
import com.higgsfield.mobile.core.model.WorkflowDescriptor
import javax.inject.Inject

/** Keeps the three writes that define a source/composer transition consistent at the feature boundary. */
class SourceSelectionUseCase @Inject constructor(
    private val persistence: ConversationPersistence,
) {
    suspend fun selectOutput(conversationId: String, outputId: String) {
        persistence.selectActiveSource(conversationId, outputId)
    }

    suspend fun detach(conversationId: String) {
        persistence.selectActiveSource(conversationId, null)
    }

    suspend fun reuseParameters(
        conversationId: String,
        workflow: WorkflowDescriptor,
        brief: CreativeBrief,
    ) {
        persistence.saveSelectedWorkflow(conversationId, workflow.id)
        persistence.saveBrief(conversationId, brief)
        persistence.selectActiveSource(conversationId, null)
    }
}
