package com.higgsfield.mobile.core.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Url

@Serializable
data class UploadUrlRequest(
    @SerialName("content_type") val contentType: String,
)

@Serializable
data class UploadUrlResponse(
    @SerialName("public_url") val publicUrl: String,
    @SerialName("upload_url") val uploadUrl: String,
    @SerialName("content_type") val contentType: String,
    @SerialName("upload_headers") val uploadHeaders: Map<String, String>,
)

@Serializable
data class RemoteMediaOutput(val url: String)

@Serializable
data class RemoteRequestStatus(
    val status: String,
    @SerialName("request_id") val requestId: String,
    @SerialName("status_url") val statusUrl: String? = null,
    @SerialName("cancel_url") val cancelUrl: String? = null,
    val error: String? = null,
    val images: List<RemoteMediaOutput> = emptyList(),
    val video: RemoteMediaOutput? = null,
    val audio: RemoteMediaOutput? = null,
    val audios: List<RemoteMediaOutput> = emptyList(),
)

/** API operations whose schemas are shared and documented independently of any model adapter. */
interface HiggsfieldService {
    @POST("files/generate-upload-url")
    suspend fun generateUploadUrl(@Body request: UploadUrlRequest): UploadUrlResponse

    @GET
    suspend fun getRequestStatus(@Url statusUrl: String): RemoteRequestStatus

    @POST
    suspend fun cancelRequest(@Url cancelUrl: String)
}
