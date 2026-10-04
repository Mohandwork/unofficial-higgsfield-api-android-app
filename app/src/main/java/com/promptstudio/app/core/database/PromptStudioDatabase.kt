package com.promptstudio.app.core.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ConversationEntity::class, ConversationDraftEntity::class, GenerationEntity::class, AttachmentEntity::class, OutputEntity::class],
    version = 3,
    exportSchema = true,
)
abstract class PromptStudioDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
    abstract fun generationDao(): GenerationDao
    abstract fun mediaDao(): MediaDao
}
