package com.promptstudio.app.feature.conversation

import com.promptstudio.app.core.data.GenerationRepository
import com.promptstudio.app.core.database.ConversationPersistence
import com.promptstudio.app.core.model.GenerationDraft
import com.promptstudio.app.core.model.GenerationRecord
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

/** A single deliberate POST followed by a best-effort local title update; never retries the POST. */
class SubmitGenerationUseCase @Inject constructor(
    private val repository: GenerationRepository,
    private val persistence: ConversationPersistence,
) {
    suspend operator fun invoke(conversationId: String, draft: GenerationDraft): Result<GenerationRecord> {
        val result = repository.submit(conversationId, draft)
        if (result.isSuccess) {
            try {
                persistence.deriveTitleFromFirstPrompt(conversationId, draft.instruction)
            } catch (canceled: CancellationException) {
                throw canceled
            } catch (_: Exception) {
                // A title is optional metadata; a saved accepted request must remain a success.
            }
        }
        return result
    }
}
