package com.higgsfield.mobile.di

import com.higgsfield.mobile.BuildConfig
import com.higgsfield.mobile.core.network.ApiCredentials
import com.higgsfield.mobile.core.network.HiggsfieldNetwork
import com.higgsfield.mobile.core.network.HiggsfieldService
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
}
