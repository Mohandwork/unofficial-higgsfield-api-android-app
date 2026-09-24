package com.higgsfield.mobile.core.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ConversationEntity::class, GenerationEntity::class, AttachmentEntity::class, OutputEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class HiggsfieldDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
    abstract fun generationDao(): GenerationDao
    abstract fun mediaDao(): MediaDao
}
