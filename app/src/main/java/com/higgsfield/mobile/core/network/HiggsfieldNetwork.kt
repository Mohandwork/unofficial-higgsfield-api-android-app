package com.higgsfield.mobile.core.network

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import com.higgsfield.mobile.BuildConfig

object HiggsfieldNetwork {
    const val API_HOST = "api.higgsfield.ai"
    const val PLATFORM_HOST = "platform.higgsfield.ai"
    const val BASE_URL = "https://$API_HOST/"

    fun createService(
        credentials: ApiCredentials,
        baseUrl: String = BASE_URL,
        authorizedHosts: Set<String> = setOf(API_HOST, PLATFORM_HOST),
    ): HiggsfieldService {
        val clientBuilder = OkHttpClient.Builder()
            .addInterceptor(HiggsfieldAuthorizationInterceptor(credentials, authorizedHosts))
        if (BuildConfig.DEBUG) clientBuilder.addInterceptor(DebugApiLoggingInterceptor().interceptor())
        val client = clientBuilder.build()
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory(JSON_MEDIA_TYPE.toMediaType()))
            .build()
            .create(HiggsfieldService::class.java)
    }

    private val json = Json { ignoreUnknownKeys = true }
    private const val JSON_MEDIA_TYPE = "application/json"
}
