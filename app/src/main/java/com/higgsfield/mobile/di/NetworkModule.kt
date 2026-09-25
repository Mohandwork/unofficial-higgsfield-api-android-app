package com.higgsfield.mobile.di

import com.higgsfield.mobile.BuildConfig
import com.higgsfield.mobile.core.network.ApiCredentials
import com.higgsfield.mobile.core.network.HiggsfieldNetwork
import com.higgsfield.mobile.core.network.HiggsfieldService
import com.higgsfield.mobile.core.network.LocalAttachmentSource
import com.higgsfield.mobile.core.network.ContentResolverAttachmentSource
import com.higgsfield.mobile.core.network.AttachmentBinaryUploader
import com.higgsfield.mobile.core.network.PresignedUploadClient
import com.higgsfield.mobile.core.network.PresignedAttachmentBinaryUploader
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
    fun provideHiggsfieldService(credentials: ApiCredentials): HiggsfieldService =
        HiggsfieldNetwork.createService(credentials)

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
}
