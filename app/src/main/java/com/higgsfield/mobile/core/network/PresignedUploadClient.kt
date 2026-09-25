package com.higgsfield.mobile.core.network

import java.io.IOException
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

/** Performs a PUT to a presigned storage URL without exposing Higgsfield credentials. */
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
            "The upload body content type must match the presigned upload content type"
        }
        val request = Request.Builder()
            .url(uploadUrl)
            .put(body)
            .apply { headers.forEach { (name, value) -> header(name, value) } }
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Presigned upload failed with HTTP ${response.code}")
        }
    }

    companion object {
        fun unauthenticatedClient(): OkHttpClient = OkHttpClient.Builder()
            .addInterceptor(RemoveAuthorizationHeaderInterceptor)
            .build()

        fun body(bytes: ByteArray, contentType: String): RequestBody =
            bytes.toRequestBody(contentType.toMediaType())
    }
}

private object RemoveAuthorizationHeaderInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain) = chain.proceed(
        chain.request().newBuilder().removeHeader("Authorization").build()
    )
}
