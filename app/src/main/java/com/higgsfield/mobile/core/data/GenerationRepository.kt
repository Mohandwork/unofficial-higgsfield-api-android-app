package com.higgsfield.mobile.core.data

import com.higgsfield.mobile.core.model.EstimateState
import com.higgsfield.mobile.core.model.GenerationDraft
import com.higgsfield.mobile.core.model.GenerationRecord
import kotlinx.coroutines.flow.Flow

interface GenerationRepository {
    fun observeConversation(conversationId: String): Flow<List<GenerationRecord>>
    suspend fun estimate(draft: GenerationDraft): EstimateState
    suspend fun submit(conversationId: String, draft: GenerationDraft): Result<GenerationRecord>
    suspend fun cancel(generationId: String): Result<Unit>
    suspend fun resumeAcceptedRequests()
}
