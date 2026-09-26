package com.higgsfield.mobile.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import androidx.room.Embedded
import androidx.room.Relation
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Upsert
    suspend fun upsert(conversation: ConversationEntity)

    @Query("SELECT * FROM conversations ORDER BY updatedAtEpochMillis DESC")
    fun observeAll(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE mediaKind = :mediaKind ORDER BY updatedAtEpochMillis DESC")
    fun observeForKind(mediaKind: String): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE mediaKind = :mediaKind ORDER BY updatedAtEpochMillis DESC LIMIT 1")
    suspend fun mostRecentForKind(mediaKind: String): ConversationEntity?

    @Query("SELECT * FROM conversations WHERE id = :id")
    suspend fun get(id: String): ConversationEntity?

    @Query("SELECT * FROM conversations WHERE id = :id")
    fun observe(id: String): Flow<ConversationEntity?>

    @Query("""
        UPDATE conversations SET activeSourceOutputId = :outputId,
        requiresSourceSelection = :requiresSelection, updatedAtEpochMillis = :now
        WHERE id = :conversationId
    """)
    suspend fun setActiveSource(conversationId: String, outputId: String?, requiresSelection: Boolean, now: Long): Int

    @Query("UPDATE conversations SET selectedWorkflowId = :workflowId, updatedAtEpochMillis = :now WHERE id = :conversationId")
    suspend fun setSelectedWorkflow(conversationId: String, workflowId: String, now: Long): Int

    @Query("""
        UPDATE conversations SET briefSubject = :subject, briefStyle = :style, briefMood = :mood,
        briefCameraDirection = :camera, briefRequirements = :requirements,
        briefExclusions = :exclusions, briefOutputGoal = :outputGoal,
        updatedAtEpochMillis = :now WHERE id = :conversationId
    """)
    suspend fun updateBrief(
        conversationId: String, subject: String, style: String, mood: String,
        camera: String, requirements: String, exclusions: String, outputGoal: String, now: Long,
    ): Int

    @Query("UPDATE conversations SET title = :title, updatedAtEpochMillis = :now WHERE id = :conversationId")
    suspend fun rename(conversationId: String, title: String, now: Long): Int

    @Query("DELETE FROM conversations WHERE id = :conversationId")
    suspend fun delete(conversationId: String): Int
}

@Dao
interface GenerationDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(generation: GenerationEntity)

    @Update
    suspend fun update(generation: GenerationEntity)

    @Query("SELECT * FROM generations WHERE conversationId = :conversationId ORDER BY createdAtEpochMillis")
    fun observeForConversation(conversationId: String): Flow<List<GenerationEntity>>

    @Transaction
    @Query("SELECT * FROM generations WHERE conversationId = :conversationId ORDER BY createdAtEpochMillis")
    fun observeWithOutputs(conversationId: String): Flow<List<GenerationWithOutputs>>

    @Transaction
    @Query("SELECT * FROM generations WHERE conversationId = :conversationId ORDER BY createdAtEpochMillis")
    fun observeWithMedia(conversationId: String): Flow<List<GenerationWithMedia>>

    @Query("SELECT * FROM generations WHERE id = :id")
    suspend fun get(id: String): GenerationEntity?

    @Query("SELECT * FROM generations WHERE status IN ('QUEUED', 'IN_PROGRESS') AND requestId IS NOT NULL")
    suspend fun getAcceptedPending(): List<GenerationEntity>

    @Query("""
        UPDATE generations SET status = :status, requestId = :requestId, statusUrl = :statusUrl,
        cancellationUrl = :cancellationUrl, correlationId = :correlationId,
        updatedAtEpochMillis = :now WHERE id = :generationId
    """)
    suspend fun markAccepted(
        generationId: String,
        status: PersistedGenerationStatus,
        requestId: String,
        statusUrl: String,
        cancellationUrl: String?,
        correlationId: String?,
        now: Long,
    ): Int

    @Query("""
        UPDATE generations SET status = :status, errorCode = :errorCode,
        errorMessage = :errorMessage, updatedAtEpochMillis = :now WHERE id = :generationId
    """)
    suspend fun updateStatus(
        generationId: String,
        status: PersistedGenerationStatus,
        errorCode: String? = null,
        errorMessage: String? = null,
        now: Long,
    ): Int
}

@Dao
interface MediaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAttachments(attachments: List<AttachmentEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOutputs(outputs: List<OutputEntity>)

    @Query("SELECT * FROM attachments WHERE generationId = :generationId")
    suspend fun attachmentsFor(generationId: String): List<AttachmentEntity>

    @Query("SELECT * FROM outputs WHERE generationId = :generationId ORDER BY createdAtEpochMillis")
    suspend fun outputsFor(generationId: String): List<OutputEntity>

    @Query("SELECT * FROM outputs WHERE id = :outputId")
    suspend fun getOutput(outputId: String): OutputEntity?

    @Query("UPDATE outputs SET localUri = :localUri WHERE id = :outputId")
    suspend fun setOutputLocalUri(outputId: String, localUri: String): Int
}

data class GenerationWithOutputs(
    @Embedded val generation: GenerationEntity,
    @Relation(parentColumn = "id", entityColumn = "generationId")
    val outputs: List<OutputEntity>,
)

data class GenerationWithMedia(
    @Embedded val generation: GenerationEntity,
    @Relation(parentColumn = "id", entityColumn = "generationId")
    val attachments: List<AttachmentEntity>,
    @Relation(parentColumn = "id", entityColumn = "generationId")
    val outputs: List<OutputEntity>,
)
