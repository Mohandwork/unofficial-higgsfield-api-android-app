package com.promptstudio.app.core.network

import okhttp3.Interceptor
import okhttp3.Response

/** Adds credentials only to configured API hosts; storage uploads use a separate client. */
class ProviderAuthorizationInterceptor(
    private val credentials: ApiCredentials,
    private val authorizedHosts: Set<String> = setOf(ProviderNetwork.API_HOST, ProviderNetwork.PLATFORM_HOST),
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val authorization = credentials.authorizationValue()
        if (authorization == null || request.url.host !in authorizedHosts) return chain.proceed(request)

        return chain.proceed(
            request.newBuilder()
                .header(AUTHORIZATION_HEADER, authorization)
                .build()
        )
    }

    private companion object {
        const val AUTHORIZATION_HEADER = "Authorization"
    }
}
