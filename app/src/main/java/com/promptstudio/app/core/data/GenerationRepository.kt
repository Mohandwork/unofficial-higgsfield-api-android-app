package com.promptstudio.app.core.data

import com.promptstudio.app.core.model.EstimateState
import com.promptstudio.app.core.model.GenerationDraft
import com.promptstudio.app.core.model.GenerationOutput
import com.promptstudio.app.core.model.GenerationRecord
import kotlinx.coroutines.flow.Flow

interface GenerationRepository {
    fun observeConversation(conversationId: String): Flow<List<GenerationRecord>>
    suspend fun estimate(draft: GenerationDraft): EstimateState
    suspend fun submit(conversationId: String, draft: GenerationDraft): Result<GenerationRecord>
    suspend fun cancel(generationId: String): Result<Unit>
    suspend fun downloadOutput(output: GenerationOutput, destinationUri: String): Result<Unit>
    suspend fun resumeAcceptedRequests()
}
