package com.higgsfield.mobile.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `generations` ADD COLUMN `sourceOutputId` TEXT")
        db.execSQL("""CREATE TABLE IF NOT EXISTS `conversation_drafts` (`conversationId` TEXT NOT NULL, `prompt` TEXT NOT NULL, `aspectRatio` TEXT NOT NULL, `resolution` TEXT, `durationSeconds` INTEGER, `seed` INTEGER, `negativePrompt` TEXT, `attachmentsJson` TEXT NOT NULL, PRIMARY KEY(`conversationId`), FOREIGN KEY(`conversationId`) REFERENCES `conversations`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )""")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_conversation_drafts_conversationId` ON `conversation_drafts` (`conversationId`)")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `conversation_drafts` ADD COLUMN `modelOptionsJson` TEXT NOT NULL DEFAULT '{}'")
    }
}
