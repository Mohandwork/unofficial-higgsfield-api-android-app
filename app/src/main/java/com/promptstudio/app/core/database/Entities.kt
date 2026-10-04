package com.promptstudio.app.core.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,
    val mediaKind: String,
    val title: String,
    val briefSubject: String = "",
    val briefStyle: String = "",
    val briefMood: String = "",
    val briefCameraDirection: String = "",
    val briefRequirements: String = "",
    val briefExclusions: String = "",
    val briefOutputGoal: String = "",
    val selectedWorkflowId: String? = null,
    val activeSourceOutputId: String? = null,
    val requiresSourceSelection: Boolean = false,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

@Entity(
    tableName = "conversation_drafts",
    foreignKeys = [ForeignKey(
        entity = ConversationEntity::class,
        parentColumns = ["id"],
        childColumns = ["conversationId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("conversationId")],
)
data class ConversationDraftEntity(
    @PrimaryKey val conversationId: String,
    val prompt: String = "",
    val aspectRatio: String = "1:1",
    val resolution: String? = null,
    val durationSeconds: Int? = null,
    val seed: Long? = null,
    val negativePrompt: String? = null,
    val attachmentsJson: String = "[]",
    val modelOptionsJson: String = "{}",
)

enum class PersistedGenerationStatus { DRAFT, QUEUED, IN_PROGRESS, COMPLETED, FAILED, NSFW, CANCELED, UNKNOWN_SUBMISSION_OUTCOME }

@Entity(
    tableName = "generations",
    foreignKeys = [ForeignKey(
        entity = ConversationEntity::class,
        parentColumns = ["id"],
        childColumns = ["conversationId"],
        onDelete = ForeignKey.CASCADE,
    ), ForeignKey(
        entity = GenerationEntity::class,
        parentColumns = ["conversationId", "id"],
        childColumns = ["conversationId", "parentGenerationId"],
        onDelete = ForeignKey.NO_ACTION,
    ), ForeignKey(
        entity = GenerationEntity::class,
        parentColumns = ["conversationId", "id"],
        childColumns = ["conversationId", "branchRootId"],
        onDelete = ForeignKey.NO_ACTION,
    )],
    indices = [
        Index("conversationId"),
        Index(value = ["conversationId", "id"], unique = true),
        Index(value = ["conversationId", "parentGenerationId"]),
        Index(value = ["conversationId", "branchRootId"]),
        Index(value = ["requestId"], unique = true),
    ],
)
data class GenerationEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val parentGenerationId: String? = null,
    val branchRootId: String,
    val sourceOutputId: String? = null,
    val workflowId: String,
    val instruction: String,
    val composedPrompt: String,
    val negativePrompt: String? = null,
    val optionsSnapshotJson: String,
    val status: PersistedGenerationStatus = PersistedGenerationStatus.DRAFT,
    val requestId: String? = null,
    val statusUrl: String? = null,
    val cancellationUrl: String? = null,
    val correlationId: String? = null,
    val estimatedCredits: String? = null,
    val errorCode: String? = null,
    val errorMessage: String? = null,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

@Entity(
    tableName = "attachments",
    foreignKeys = [ForeignKey(
        entity = GenerationEntity::class,
        parentColumns = ["id"],
        childColumns = ["generationId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("generationId")],
)
data class AttachmentEntity(
    @PrimaryKey val id: String,
    val generationId: String,
    val role: String,
    val mediaKind: String,
    val localUri: String? = null,
    val remoteUrl: String? = null,
    val mimeType: String? = null,
)

@Entity(
    tableName = "outputs",
    foreignKeys = [ForeignKey(
        entity = GenerationEntity::class,
        parentColumns = ["id"],
        childColumns = ["generationId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("generationId")],
)
data class OutputEntity(
    @PrimaryKey val id: String,
    val generationId: String,
    val mediaKind: String,
    val remoteUrl: String,
    val localUri: String? = null,
    val createdAtEpochMillis: Long,
)
