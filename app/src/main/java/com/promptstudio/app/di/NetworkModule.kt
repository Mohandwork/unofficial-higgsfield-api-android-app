package com.promptstudio.app.di

import com.promptstudio.app.BuildConfig
import com.promptstudio.app.core.network.ApiCredentials
import com.promptstudio.app.core.network.ProviderNetwork
import com.promptstudio.app.core.network.ProviderService
import com.promptstudio.app.core.network.LocalAttachmentSource
import com.promptstudio.app.core.network.ContentResolverAttachmentSource
import com.promptstudio.app.core.network.AttachmentBinaryUploader
import com.promptstudio.app.core.network.PresignedUploadClient
import com.promptstudio.app.core.network.PresignedAttachmentBinaryUploader
import com.promptstudio.app.core.network.GenerationRequestSynchronizer
import com.promptstudio.app.core.network.RequestStatusSynchronizer
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideApiCredentials() = ApiCredentials(
        keyId = BuildConfig.HF_KEY_ID,
        secret = BuildConfig.HF_KEY_SECRET,
    )

    @Provides
    @Singleton
    fun provideProviderService(credentials: ApiCredentials): ProviderService =
        ProviderNetwork.createService(credentials)

    @Provides
    @Singleton
    fun providePresignedUploadClient(): PresignedUploadClient =
        PresignedUploadClient(PresignedUploadClient.unauthenticatedClient())
}

@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkBindings {
    @Binds
    @Singleton
    abstract fun bindLocalAttachmentSource(
        implementation: ContentResolverAttachmentSource,
    ): LocalAttachmentSource

    @Binds
    @Singleton
    abstract fun bindAttachmentBinaryUploader(
        implementation: PresignedAttachmentBinaryUploader,
    ): AttachmentBinaryUploader

    @Binds
    @Singleton
    abstract fun bindGenerationRequestSynchronizer(
        implementation: RequestStatusSynchronizer,
    ): GenerationRequestSynchronizer
}
