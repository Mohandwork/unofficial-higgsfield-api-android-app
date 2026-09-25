package com.higgsfield.mobile.core.network

import okhttp3.Interceptor
import okhttp3.Response

/** Adds credentials only to Higgsfield API hosts; storage uploads use a separate client. */
class HiggsfieldAuthorizationInterceptor(
    private val credentials: ApiCredentials,
    private val authorizedHosts: Set<String> = setOf(HiggsfieldNetwork.API_HOST),
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
