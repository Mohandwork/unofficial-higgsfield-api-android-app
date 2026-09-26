package com.higgsfield.mobile.di

import android.content.Context
import androidx.room.Room
import com.higgsfield.mobile.core.connectivity.AndroidConnectivityStatusProvider
import com.higgsfield.mobile.core.connectivity.ConnectivityStatusProvider
import com.higgsfield.mobile.core.database.ConversationDao
import com.higgsfield.mobile.core.database.GenerationDao
import com.higgsfield.mobile.core.database.HiggsfieldDatabase
import com.higgsfield.mobile.core.database.MediaDao
import com.higgsfield.mobile.core.database.MIGRATION_1_2
import com.higgsfield.mobile.core.data.GenerationRepository
import com.higgsfield.mobile.core.data.RoomGenerationRepository
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
    fun provideDatabase(@ApplicationContext context: Context): HiggsfieldDatabase =
        Room.databaseBuilder(context, HiggsfieldDatabase::class.java, DATABASE_NAME)
            .addMigrations(MIGRATION_1_2)
            .build()

    @Provides fun provideConversationDao(database: HiggsfieldDatabase): ConversationDao = database.conversationDao()
    @Provides fun provideGenerationDao(database: HiggsfieldDatabase): GenerationDao = database.generationDao()
    @Provides fun provideMediaDao(database: HiggsfieldDatabase): MediaDao = database.mediaDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class PersistenceBindings {
    @Binds
    @Singleton
    abstract fun bindConversationPersistence(
        implementation: com.higgsfield.mobile.core.database.RoomConversationPersistence,
    ): com.higgsfield.mobile.core.database.ConversationPersistence

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

private const val DATABASE_NAME = "higgsfield.db"
