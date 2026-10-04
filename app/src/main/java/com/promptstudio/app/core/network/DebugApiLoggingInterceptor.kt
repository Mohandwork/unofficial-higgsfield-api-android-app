package com.promptstudio.app.core.network

import android.util.Log
import okhttp3.logging.HttpLoggingInterceptor

/**
 * Verbose API diagnostics for debug builds. OkHttp supplies headers, URL/query, and JSON bodies;
 * this logger removes credentials and private/signed media URLs before they reach logcat.
 */
class DebugApiLoggingInterceptor {
    private val delegate = HttpLoggingInterceptor { message ->
        safeDebug(redact(message))
    }.apply {
        level = HttpLoggingInterceptor.Level.BODY
        redactHeader(HEADER_AUTHORIZATION)
        redactHeader(HEADER_API_KEY)
        redactHeader(HEADER_COOKIE)
        redactHeader(HEADER_SET_COOKIE)
        redactHeader(HEADER_KEY_ID)
        redactHeader(HEADER_KEY_SECRET)
    }

    fun interceptor(): HttpLoggingInterceptor = delegate

    private fun redact(message: String): String = message
        .replace(SENSITIVE_QUERY_PATTERN, "$1=<redacted>")
        .replace(PRIVATE_URL_FIELD_PATTERN, "$1<redacted>$3")

    private fun safeDebug(message: String) {
        runCatching { Log.d(LOG_TAG, message) }
    }

    private companion object {
        const val LOG_TAG = "PromptStudioHttp"
        const val HEADER_AUTHORIZATION = "Authorization"
        const val HEADER_API_KEY = "Api-Key"
        const val HEADER_COOKIE = "Cookie"
        const val HEADER_SET_COOKIE = "Set-Cookie"
        const val HEADER_KEY_ID = "X-HF-Key-Id"
        const val HEADER_KEY_SECRET = "X-HF-Key-Secret"
        val SENSITIVE_QUERY_PATTERN = Regex(
            "(?i)([?&](?:sig|signature|token|secret|key|expires|x-amz-[^=]+)=)[^&\\s]+",
        )
        val PRIVATE_URL_FIELD_PATTERN = Regex(
            "(?i)([\\\"](?:upload_url|public_url|image_url|video_url|audio_url)[\\\"]\\s*:\\s*[\\\"])([^\\\"]+)([\\\"])",
        )
    }
}
