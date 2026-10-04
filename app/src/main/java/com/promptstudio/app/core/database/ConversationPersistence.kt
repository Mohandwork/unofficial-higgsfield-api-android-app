package com.promptstudio.app.core.database

import androidx.room.withTransaction
import com.promptstudio.app.core.model.CreativeBrief
import com.promptstudio.app.core.model.MediaKind
import com.promptstudio.app.core.model.WorkflowId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

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
    val id: String,
    val title: String,
    val mediaKind: MediaKind,
    val brief: CreativeBrief,
    val selectedWorkflowId: WorkflowId?,
    val activeSourceOutputId: String?,
    val requiresSourceSelection: Boolean,
    val timeline: List<PersistedTimelineItem>,
    val draft: PersistedComposerDraft = PersistedComposerDraft(),
)

data class ConversationSummary(
    val id: String,
    val mediaKind: MediaKind,
    val title: String,
    val updatedAtEpochMillis: Long,
)

interface ConversationPersistence {
    fun observe(conversationId: String): Flow<PersistedConversationSnapshot?>
    fun observeConversations(): Flow<List<ConversationSummary>>
    suspend fun ensureConversation(conversationId: String, kind: MediaKind, initialWorkflowId: WorkflowId?)
    suspend fun mostRecentConversation(kind: MediaKind): String?
    suspend fun createConversationFromDraft(kind: MediaKind, workflowId: WorkflowId?, brief: CreativeBrief, draft: PersistedComposerDraft): String
    suspend fun renameConversation(conversationId: String, title: String)
    suspend fun deriveTitleFromFirstPrompt(conversationId: String, prompt: String)
    suspend fun deleteConversation(conversationId: String)
    suspend fun saveBrief(conversationId: String, brief: CreativeBrief)
    suspend fun saveDraft(conversationId: String, draft: PersistedComposerDraft)
    suspend fun saveSelectedWorkflow(conversationId: String, workflowId: WorkflowId)
    suspend fun reuseParameters(conversationId: String, workflowId: WorkflowId, brief: CreativeBrief, draft: PersistedComposerDraft)
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
    private val database: PromptStudioDatabase,
    private val conversationDao: ConversationDao,
    private val generationDao: GenerationDao,
    private val localStore: LocalGenerationStore,
) : ConversationPersistence {
    override fun observe(conversationId: String): Flow<PersistedConversationSnapshot?> =
        combine(
            conversationDao.observe(conversationId),
            generationDao.observeWithOutputs(conversationId),
            conversationDao.observeDraft(conversationId),
        ) { conversation, generations, draft ->
            conversation?.let { entity ->
                PersistedConversationSnapshot(
                    id = entity.id,
                    title = entity.title,
                    mediaKind = MediaKind.valueOf(entity.mediaKind),
                    brief = entity.toBrief(),
                    selectedWorkflowId = entity.selectedWorkflowId?.let(::WorkflowId),
                    activeSourceOutputId = entity.activeSourceOutputId,
                    requiresSourceSelection = entity.requiresSourceSelection,
                    draft = draft?.toDraft() ?: PersistedComposerDraft(),
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

    override fun observeConversations(): Flow<List<ConversationSummary>> =
        conversationDao.observeAll().map { entities ->
            entities.map { ConversationSummary(it.id, MediaKind.valueOf(it.mediaKind), it.title, it.updatedAtEpochMillis) }
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
                        title = if (kind == MediaKind.IMAGE) IMAGE_EXPLORATION_TITLE else VIDEO_EXPLORATION_TITLE,
                        selectedWorkflowId = initialWorkflowId?.value,
                        createdAtEpochMillis = now,
                        updatedAtEpochMillis = now,
                    )
                )
            }
        }
    }

    override suspend fun mostRecentConversation(kind: MediaKind): String? =
        conversationDao.mostRecentForKind(kind.name)?.id

    override suspend fun createConversationFromDraft(
        kind: MediaKind,
        workflowId: WorkflowId?,
        brief: CreativeBrief,
        draft: PersistedComposerDraft,
    ): String = database.withTransaction {
        val id = "${kind.name.lowercase()}-${java.util.UUID.randomUUID()}"
        val now = System.currentTimeMillis()
        conversationDao.upsert(ConversationEntity(
            id = id,
            mediaKind = kind.name,
            title = if (kind == MediaKind.IMAGE) "New image chat" else "New video chat",
            selectedWorkflowId = workflowId?.value,
            briefSubject = brief.subject,
            briefStyle = brief.style,
            briefMood = brief.mood,
            briefCameraDirection = brief.cameraDirection,
            briefRequirements = brief.requirements,
            briefExclusions = brief.exclusions,
            briefOutputGoal = brief.outputGoal,
            createdAtEpochMillis = now,
            updatedAtEpochMillis = now,
        ))
        conversationDao.upsertDraft(draft.toEntity(id))
        id
    }

    override suspend fun renameConversation(conversationId: String, title: String) {
        val trimmed = title.trim()
        require(trimmed.isNotEmpty()) { "A conversation title cannot be empty" }
        check(conversationDao.rename(conversationId, trimmed, System.currentTimeMillis()) == 1) {
            "Cannot rename a missing conversation"
        }
    }

    override suspend fun deriveTitleFromFirstPrompt(conversationId: String, prompt: String) {
        val conversation = conversationDao.get(conversationId) ?: return
        if (conversation.title !in AUTO_TITLE_PLACEHOLDERS) return
        val title = prompt.trim().replace(Regex("\\s+"), " ").take(TITLE_MAX_LENGTH)
        if (title.isNotBlank()) renameConversation(conversationId, title)
    }

    override suspend fun deleteConversation(conversationId: String) {
        check(conversationDao.delete(conversationId) == 1) { "Cannot remove a missing conversation" }
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

    override suspend fun saveDraft(conversationId: String, draft: PersistedComposerDraft) {
        if (conversationDao.get(conversationId) != null) {
            conversationDao.upsertDraft(draft.toEntity(conversationId))
        }
    }

    override suspend fun saveSelectedWorkflow(conversationId: String, workflowId: WorkflowId) {
        check(conversationDao.setSelectedWorkflow(conversationId, workflowId.value, System.currentTimeMillis()) == 1) {
            MISSING_CONVERSATION_WORKFLOW_MESSAGE
        }
    }

    override suspend fun reuseParameters(
        conversationId: String,
        workflowId: WorkflowId,
        brief: CreativeBrief,
        draft: PersistedComposerDraft,
    ) {
        database.withTransaction {
            check(conversationDao.reuseParameters(
                conversationId = conversationId,
                workflowId = workflowId.value,
                subject = brief.subject,
                style = brief.style,
                mood = brief.mood,
                camera = brief.cameraDirection,
                requirements = brief.requirements,
                exclusions = brief.exclusions,
                outputGoal = brief.outputGoal,
                now = System.currentTimeMillis(),
            ) == 1) { "Cannot reuse parameters for a missing conversation" }
            conversationDao.upsertDraft(draft.toEntity(conversationId))
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
            CROSS_CONVERSATION_PARENT_MESSAGE
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
                optionsSnapshotJson = EMPTY_OPTIONS_SNAPSHOT,
                createdAtEpochMillis = now,
                updatedAtEpochMillis = now,
            ),
            attachments = emptyList(),
        )
        val output = OutputEntity(
            id = "$generationId$DEMO_OUTPUT_SUFFIX",
            generationId = generationId,
            mediaKind = outputKind.name,
            remoteUrl = "$DEMO_URL_PREFIX$generationId",
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

private const val IMAGE_EXPLORATION_TITLE = "Image exploration"
private const val VIDEO_EXPLORATION_TITLE = "Video exploration"
private const val MISSING_CONVERSATION_WORKFLOW_MESSAGE = "Cannot save a workflow for a missing conversation"
private const val CROSS_CONVERSATION_PARENT_MESSAGE = "A branch parent must exist in the same conversation"
private const val EMPTY_OPTIONS_SNAPSHOT = "{}"
private const val DEMO_OUTPUT_SUFFIX = "-output"
private const val DEMO_URL_PREFIX = "demo://"
private val AUTO_TITLE_PLACEHOLDERS = setOf("Image exploration", "Video exploration", "New image chat", "New video chat")
private const val TITLE_MAX_LENGTH = 48
