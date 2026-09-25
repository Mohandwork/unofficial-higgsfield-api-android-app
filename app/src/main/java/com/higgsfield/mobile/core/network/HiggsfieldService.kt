package com.higgsfield.mobile.core.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Url

@Serializable
data class UploadUrlRequest(
    @SerialName(CONTENT_TYPE_FIELD) val contentType: String,
)

@Serializable
data class UploadUrlResponse(
    @SerialName(PUBLIC_URL_FIELD) val publicUrl: String,
    @SerialName(UPLOAD_URL_FIELD) val uploadUrl: String,
    @SerialName(CONTENT_TYPE_FIELD) val contentType: String,
    @SerialName(UPLOAD_HEADERS_FIELD) val uploadHeaders: Map<String, String>,
)

@Serializable
data class RemoteMediaOutput(val url: String)

@Serializable
data class RemoteRequestStatus(
    val status: String,
    @SerialName(REQUEST_ID_FIELD) val requestId: String,
    @SerialName(STATUS_URL_FIELD) val statusUrl: String? = null,
    @SerialName(CANCEL_URL_FIELD) val cancelUrl: String? = null,
    val error: String? = null,
    val images: List<RemoteMediaOutput> = emptyList(),
    val video: RemoteMediaOutput? = null,
    val audio: RemoteMediaOutput? = null,
    val audios: List<RemoteMediaOutput> = emptyList(),
)

/** API operations whose schemas are shared and documented independently of any model adapter. */
interface HiggsfieldService {
    @POST(UPLOAD_URL_ROUTE)
    suspend fun generateUploadUrl(@Body request: UploadUrlRequest): UploadUrlResponse

    @POST
    suspend fun submitWorkflow(@Url endpointPath: String, @Body request: JsonObject): RemoteRequestStatus

    @GET
    suspend fun getRequestStatus(@Url statusUrl: String): RemoteRequestStatus

    @POST
    suspend fun cancelRequest(@Url cancelUrl: String)
}

private const val CONTENT_TYPE_FIELD = "content_type"
private const val PUBLIC_URL_FIELD = "public_url"
private const val UPLOAD_URL_FIELD = "upload_url"
private const val UPLOAD_HEADERS_FIELD = "upload_headers"
private const val REQUEST_ID_FIELD = "request_id"
private const val STATUS_URL_FIELD = "status_url"
private const val CANCEL_URL_FIELD = "cancel_url"
private const val UPLOAD_URL_ROUTE = "files/generate-upload-url"
