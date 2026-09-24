package com.higgsfield.mobile.core.database

import androidx.room.withTransaction
import com.higgsfield.mobile.core.model.CreativeBrief
import com.higgsfield.mobile.core.model.MediaKind
import com.higgsfield.mobile.core.model.WorkflowId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class PersistedTimelineItem(
    val id: String,
    val prompt: String,
    val workflowId: WorkflowId,
    val status: PersistedGenerationStatus,
    val outputId: String?,
    val outputKind: MediaKind?,
    val parentId: String?,
)

data class PersistedConversationSnapshot(
    val brief: CreativeBrief,
    val selectedWorkflowId: WorkflowId?,
    val activeSourceOutputId: String?,
    val requiresSourceSelection: Boolean,
    val timeline: List<PersistedTimelineItem>,
)

interface ConversationPersistence {
    fun observe(conversationId: String): Flow<PersistedConversationSnapshot?>
    suspend fun ensureConversation(conversationId: String, kind: MediaKind, initialWorkflowId: WorkflowId?)
    suspend fun saveBrief(conversationId: String, brief: CreativeBrief)
    suspend fun saveSelectedWorkflow(conversationId: String, workflowId: WorkflowId)
    suspend fun saveCompletedDemo(
        conversationId: String,
        generationId: String,
        parentGenerationId: String?,
        workflowId: WorkflowId,
        instruction: String,
        outputKind: MediaKind,
    )
    suspend fun selectActiveSource(conversationId: String, outputId: String?)
}

@Singleton
class RoomConversationPersistence @Inject constructor(
    private val database: HiggsfieldDatabase,
    private val conversationDao: ConversationDao,
    private val generationDao: GenerationDao,
    private val localStore: LocalGenerationStore,
) : ConversationPersistence {
    override fun observe(conversationId: String): Flow<PersistedConversationSnapshot?> =
        combine(
            conversationDao.observe(conversationId),
            generationDao.observeWithOutputs(conversationId),
        ) { conversation, generations ->
            conversation?.let { entity ->
                PersistedConversationSnapshot(
                    brief = entity.toBrief(),
                    selectedWorkflowId = entity.selectedWorkflowId?.let(::WorkflowId),
                    activeSourceOutputId = entity.activeSourceOutputId,
                    requiresSourceSelection = entity.requiresSourceSelection,
                    timeline = generations.map { generation ->
                        val firstOutput = generation.outputs.firstOrNull()
                        PersistedTimelineItem(
                            id = generation.generation.id,
                            prompt = generation.generation.instruction,
                            workflowId = WorkflowId(generation.generation.workflowId),
                            status = generation.generation.status,
                            outputId = firstOutput?.id,
                            outputKind = firstOutput?.mediaKind?.let(MediaKind::valueOf),
                            parentId = generation.generation.parentGenerationId,
                        )
                    },
                )
            }
        }

    override suspend fun ensureConversation(
        conversationId: String,
        kind: MediaKind,
        initialWorkflowId: WorkflowId?,
    ) {
        database.withTransaction {
            if (conversationDao.get(conversationId) == null) {
                val now = System.currentTimeMillis()
                conversationDao.upsert(
                    ConversationEntity(
                        id = conversationId,
                        mediaKind = kind.name,
                        title = if (kind == MediaKind.IMAGE) "Image exploration" else "Video exploration",
                        selectedWorkflowId = initialWorkflowId?.value,
                        createdAtEpochMillis = now,
                        updatedAtEpochMillis = now,
                    )
                )
            }
        }
    }

    override suspend fun saveBrief(conversationId: String, brief: CreativeBrief) {
        check(conversationDao.updateBrief(
            conversationId = conversationId,
            subject = brief.subject,
            style = brief.style,
            mood = brief.mood,
            camera = brief.cameraDirection,
            requirements = brief.requirements,
            exclusions = brief.exclusions,
            outputGoal = brief.outputGoal,
            now = System.currentTimeMillis(),
        ) == 1) { "Cannot save a brief for a missing conversation" }
    }

    override suspend fun saveSelectedWorkflow(conversationId: String, workflowId: WorkflowId) {
        check(conversationDao.setSelectedWorkflow(conversationId, workflowId.value, System.currentTimeMillis()) == 1) {
            "Cannot save a workflow for a missing conversation"
        }
    }

    override suspend fun saveCompletedDemo(
        conversationId: String,
        generationId: String,
        parentGenerationId: String?,
        workflowId: WorkflowId,
        instruction: String,
        outputKind: MediaKind,
    ) {
        val now = System.currentTimeMillis()
        val parent = parentGenerationId?.let { generationDao.get(it) }
        require(parentGenerationId == null || parent?.conversationId == conversationId) {
            "A branch parent must exist in the same conversation"
        }
        val branchRoot = parent?.branchRootId ?: generationId
        localStore.insertDraft(
            GenerationEntity(
                id = generationId,
                conversationId = conversationId,
                parentGenerationId = parentGenerationId,
                branchRootId = branchRoot,
                workflowId = workflowId.value,
                instruction = instruction,
                composedPrompt = instruction,
                optionsSnapshotJson = "{}",
                createdAtEpochMillis = now,
                updatedAtEpochMillis = now,
            ),
            attachments = emptyList(),
        )
        val output = OutputEntity(
            id = "$generationId-output",
            generationId = generationId,
            mediaKind = outputKind.name,
            remoteUrl = "demo://$generationId",
            createdAtEpochMillis = now,
        )
        localStore.applyCompleted(conversationId, generationId, listOf(output), now)
    }

    override suspend fun selectActiveSource(conversationId: String, outputId: String?) {
        localStore.selectActiveSource(conversationId, outputId, System.currentTimeMillis())
    }
}

private fun ConversationEntity.toBrief() = CreativeBrief(
    subject = briefSubject,
    style = briefStyle,
    mood = briefMood,
    cameraDirection = briefCameraDirection,
    requirements = briefRequirements,
    exclusions = briefExclusions,
    outputGoal = briefOutputGoal,
)
