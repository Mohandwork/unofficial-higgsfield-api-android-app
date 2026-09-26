package com.higgsfield.mobile.core.database

import com.higgsfield.mobile.core.model.GenerationOptions
import com.higgsfield.mobile.core.model.MediaKind
import com.higgsfield.mobile.core.model.MediaRole
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class PersistedDraftAttachment(
    val role: MediaRole,
    val kind: MediaKind,
    val uri: String,
    val label: String,
)

data class PersistedComposerDraft(
    val prompt: String = "",
    val options: GenerationOptions = GenerationOptions(),
    val attachments: List<PersistedDraftAttachment> = emptyList(),
)

internal fun PersistedComposerDraft.toEntity(conversationId: String) = ConversationDraftEntity(
    conversationId = conversationId,
    prompt = prompt,
    aspectRatio = options.aspectRatio,
    resolution = options.resolution,
    durationSeconds = options.durationSeconds,
    seed = options.seed,
    negativePrompt = options.negativePrompt,
    attachmentsJson = JsonArray(attachments.map { attachment ->
        buildJsonObject {
            put("role", JsonPrimitive(attachment.role.name))
            put("kind", JsonPrimitive(attachment.kind.name))
            put("uri", JsonPrimitive(attachment.uri))
            put("label", JsonPrimitive(attachment.label))
        }
    }).toString(),
)

internal fun ConversationDraftEntity.toDraft() = PersistedComposerDraft(
    prompt = prompt,
    options = GenerationOptions(aspectRatio, resolution, durationSeconds, seed, negativePrompt),
    attachments = runCatching {
        Json.parseToJsonElement(attachmentsJson).jsonArray.map { item ->
            val fields = item.jsonObject
            PersistedDraftAttachment(
                role = MediaRole.valueOf(fields.getValue("role").jsonPrimitive.content),
                kind = MediaKind.valueOf(fields.getValue("kind").jsonPrimitive.content),
                uri = fields.getValue("uri").jsonPrimitive.content,
                label = fields.getValue("label").jsonPrimitive.content,
            )
        }
    }.getOrDefault(emptyList()),
)
