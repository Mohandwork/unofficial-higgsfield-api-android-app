package com.promptstudio.app.core.network

import android.util.Log
import java.io.IOException
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Debug-only network diagnostics that never print headers, query parameters, bodies, or URLs
 * containing signed media credentials. The method, host/path, status, and elapsed time are enough
 * to verify the API boundary while keeping prompts, tokens, and presigned URLs out of logcat.
 */
class SafeNetworkLoggingInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val target = "${request.url.host}${request.url.encodedPath}"
        val startedAt = System.nanoTime()
        safeDebug("HTTP → ${request.method} $target")
        return try {
            val response = chain.proceed(request)
            val elapsedMillis = (System.nanoTime() - startedAt) / NANOS_PER_MILLISECOND
            safeDebug("HTTP ← ${response.code} ${request.method} $target (${elapsedMillis}ms)")
            response
        } catch (error: IOException) {
            val elapsedMillis = (System.nanoTime() - startedAt) / NANOS_PER_MILLISECOND
            safeError("HTTP ✕ ${request.method} $target (${elapsedMillis}ms) ${error::class.java.simpleName}")
            throw error
        }
    }

    private fun safeDebug(message: String) {
        runCatching { Log.d(LOG_TAG, message) }
    }

    private fun safeError(message: String) {
        runCatching { Log.e(LOG_TAG, message) }
    }

    private companion object {
        const val LOG_TAG = "PromptStudioHttp"
        const val NANOS_PER_MILLISECOND = 1_000_000L
    }
}
