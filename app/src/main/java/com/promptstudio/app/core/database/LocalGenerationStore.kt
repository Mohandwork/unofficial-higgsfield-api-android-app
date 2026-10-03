package com.promptstudio.app.core.database

import androidx.room.withTransaction
import com.promptstudio.app.core.model.ActiveSourceDecision
import com.promptstudio.app.core.model.ActiveSourcePolicy
import com.promptstudio.app.core.model.GenerationStatus
import com.promptstudio.app.core.model.GenerationOutput
import com.promptstudio.app.core.model.MediaKind
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalGenerationStore @Inject constructor(
    private val database: PromptStudioDatabase,
    private val conversationDao: ConversationDao,
    private val generationDao: GenerationDao,
    private val mediaDao: MediaDao,
) {
    suspend fun insertDraft(generation: GenerationEntity, attachments: List<AttachmentEntity>) {
        database.withTransaction {
            check(generation.status == PersistedGenerationStatus.DRAFT)
            generationDao.insert(generation)
            if (attachments.isNotEmpty()) mediaDao.upsertAttachments(attachments)
        }
    }

    suspend fun markAccepted(
        generationId: String,
        requestId: String,
        statusUrl: String,
        cancellationUrl: String?,
        correlationId: String?,
        now: Long,
    ) = database.withTransaction {
        val changed = generationDao.markAccepted(
            generationId = generationId,
            status = PersistedGenerationStatus.QUEUED,
            requestId = requestId,
            statusUrl = statusUrl,
            cancellationUrl = cancellationUrl,
            correlationId = correlationId,
            now = now,
        )
        check(changed == 1) { "Accepted request was not persisted because its generation is missing" }
    }

    suspend fun applyCompleted(
        conversationId: String,
        generationId: String,
        outputs: List<OutputEntity>,
        now: Long,
    ) = database.withTransaction {
        require(outputs.isNotEmpty()) { "A completed generation must contain at least one output" }
        require(outputs.all { it.generationId == generationId }) { "Every output must belong to the completed generation" }
        val changed = generationDao.updateStatus(generationId, PersistedGenerationStatus.COMPLETED, now = now)
        check(changed == 1) { "Completed request was not persisted because its generation is missing" }
        mediaDao.upsertOutputs(outputs)
        val domainStatus = GenerationStatus.Completed(outputs.map {
            GenerationOutput(it.id, it.remoteUrl, MediaKind.valueOf(it.mediaKind), it.localUri)
        })
        when (val decision = ActiveSourcePolicy.after(domainStatus)) {
            ActiveSourceDecision.KeepCurrent -> Unit
            is ActiveSourceDecision.Replace -> check(
                conversationDao.setActiveSource(conversationId, decision.outputId, false, now) == 1
            ) { "Completed output cannot be attached to a missing conversation" }
            ActiveSourceDecision.RequireSelection -> {
                val current = conversationDao.get(conversationId)
                    ?: error("Completed outputs belong to a missing conversation")
                check(conversationDao.setActiveSource(conversationId, current.activeSourceOutputId, true, now) == 1)
            }
        }
    }

    suspend fun selectActiveSource(conversationId: String, outputId: String?, now: Long) {
        database.withTransaction {
            if (outputId != null) {
                val output = mediaDao.getOutput(outputId) ?: error("Selected output does not exist")
                val generation = generationDao.get(output.generationId) ?: error("Selected output has no generation")
                require(generation.conversationId == conversationId) { "Selected output belongs to another conversation" }
            }
            check(conversationDao.setActiveSource(conversationId, outputId, false, now) == 1)
        }
    }

    suspend fun acceptedPending(): List<GenerationEntity> = generationDao.getAcceptedPending()
}
