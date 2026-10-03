package com.promptstudio.app.core.network

import com.promptstudio.app.BuildConfig
import java.io.IOException
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

/** Performs a PUT to a presigned storage URL without exposing API credentials. */
class PresignedUploadClient(
    private val client: OkHttpClient,
) {
    @Throws(IOException::class)
    fun upload(
        uploadUrl: String,
        headers: Map<String, String>,
        contentType: String,
        body: RequestBody,
    ) {
        require(contentType == body.contentType()?.toString()) {
            CONTENT_TYPE_MISMATCH_MESSAGE
        }
        val request = Request.Builder()
            .url(uploadUrl)
            .put(body)
            .apply { headers.forEach { (name, value) -> header(name, value) } }
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("$UPLOAD_FAILED_PREFIX${response.code}")
        }
    }

    companion object {
        fun unauthenticatedClient(): OkHttpClient {
            val builder = OkHttpClient.Builder()
                .addInterceptor(RemoveAuthorizationHeaderInterceptor)
            if (BuildConfig.DEBUG) builder.addInterceptor(SafeNetworkLoggingInterceptor())
            return builder.build()
        }

        fun body(bytes: ByteArray, contentType: String): RequestBody =
            bytes.toRequestBody(contentType.toMediaType())
    }
}

private object RemoveAuthorizationHeaderInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain) = chain.proceed(
        chain.request().newBuilder().removeHeader(AUTHORIZATION_HEADER).build()
    )
}

private const val AUTHORIZATION_HEADER = "Authorization"
private const val CONTENT_TYPE_MISMATCH_MESSAGE = "The upload body content type must match the presigned upload content type"
private const val UPLOAD_FAILED_PREFIX = "Presigned upload failed with HTTP "
