package com.promptstudio.app.di

import android.content.Context
import androidx.room.Room
import com.promptstudio.app.core.connectivity.AndroidConnectivityStatusProvider
import com.promptstudio.app.core.connectivity.ConnectivityStatusProvider
import com.promptstudio.app.core.database.ConversationDao
import com.promptstudio.app.core.database.GenerationDao
import com.promptstudio.app.core.database.PromptStudioDatabase
import com.promptstudio.app.core.database.MediaDao
import com.promptstudio.app.core.database.MIGRATION_1_2
import com.promptstudio.app.core.data.GenerationRepository
import com.promptstudio.app.core.data.RoomGenerationRepository
import dagger.Module
import dagger.Binds
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PersistenceModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PromptStudioDatabase =
        Room.databaseBuilder(context, PromptStudioDatabase::class.java, DATABASE_NAME)
            .addMigrations(MIGRATION_1_2)
            .build()

    @Provides fun provideConversationDao(database: PromptStudioDatabase): ConversationDao = database.conversationDao()
    @Provides fun provideGenerationDao(database: PromptStudioDatabase): GenerationDao = database.generationDao()
    @Provides fun provideMediaDao(database: PromptStudioDatabase): MediaDao = database.mediaDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class PersistenceBindings {
    @Binds
    @Singleton
    abstract fun bindConversationPersistence(
        implementation: com.promptstudio.app.core.database.RoomConversationPersistence,
    ): com.promptstudio.app.core.database.ConversationPersistence

    @Binds
    @Singleton
    abstract fun bindConnectivityStatusProvider(
        implementation: AndroidConnectivityStatusProvider,
    ): ConnectivityStatusProvider

    @Binds
    @Singleton
    abstract fun bindGenerationRepository(
        implementation: RoomGenerationRepository,
    ): GenerationRepository
}

private const val DATABASE_NAME = "prompt_studio.db"
