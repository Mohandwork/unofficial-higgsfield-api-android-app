package com.higgsfield.mobile.core.network

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import com.higgsfield.mobile.core.error.AppError
import com.higgsfield.mobile.core.error.ErrorMapper
import com.higgsfield.mobile.core.model.GenerationAttachment
import com.higgsfield.mobile.core.model.MediaKind
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okio.BufferedSink
import okio.source
import retrofit2.HttpException

interface LocalAttachmentSource {
    fun contentType(uri: String): String?
    fun contentLength(uri: String): Long?
    fun open(uri: String): InputStream?
}

@Singleton
class ContentResolverAttachmentSource @Inject constructor(
    @ApplicationContext context: Context,
) : LocalAttachmentSource {
    private val resolver: ContentResolver = context.contentResolver

    override fun contentType(uri: String): String? = resolver.getType(uri.toContentUri())

    override fun contentLength(uri: String): Long? = resolver
        .openAssetFileDescriptor(uri.toContentUri(), READ_MODE)
        ?.use { descriptor -> descriptor.length.takeIf { it >= 0L } }

    override fun open(uri: String): InputStream? = resolver.openInputStream(uri.toContentUri())

    private fun String.toContentUri(): Uri = toUri().also { parsed ->
        require(parsed.scheme == ContentResolver.SCHEME_CONTENT) { CONTENT_URI_REQUIRED_MESSAGE }
    }
}

sealed interface AttachmentUploadResult {
    data class Uploaded(val attachments: List<GenerationAttachment>) : AttachmentUploadResult
    data class Failed(val attachmentId: String, val error: AppError) : AttachmentUploadResult
}

interface AttachmentBinaryUploader {
    fun upload(uploadUrl: String, headers: Map<String, String>, contentType: String, body: RequestBody)
}

@Singleton
class PresignedAttachmentBinaryUploader @Inject constructor(
    private val client: PresignedUploadClient,
) : AttachmentBinaryUploader {
    override fun upload(
        uploadUrl: String,
        headers: Map<String, String>,
        contentType: String,
        body: RequestBody,
    ) = client.upload(uploadUrl, headers, contentType, body)
}

@Singleton
class SecureAttachmentUploader @Inject constructor(
    private val service: HiggsfieldService,
    private val binaryUploader: AttachmentBinaryUploader,
    private val localSource: LocalAttachmentSource,
) {
    suspend fun uploadAll(attachments: List<GenerationAttachment>): AttachmentUploadResult = withContext(Dispatchers.IO) {
        val uploaded = mutableListOf<GenerationAttachment>()
        for (attachment in attachments) {
            when (val result = upload(attachment)) {
                is SingleUploadResult.Uploaded -> uploaded += result.attachment
                is SingleUploadResult.Failed -> return@withContext AttachmentUploadResult.Failed(attachment.id, result.error)
            }
        }
        AttachmentUploadResult.Uploaded(uploaded)
    }

    private suspend fun upload(attachment: GenerationAttachment): SingleUploadResult {
        attachment.remoteUrl?.takeIf(String::isNotBlank)?.let { remoteUrl ->
            return if (remoteUrl.isHttps()) {
                SingleUploadResult.Uploaded(attachment)
            } else {
                SingleUploadResult.Failed(attachment, ErrorMapper.protocol(INVALID_PUBLIC_URL_MESSAGE))
            }
        }

        val contentType = try {
            localSource.contentType(attachment.uri)
        } catch (error: Exception) {
            return SingleUploadResult.Failed(attachment, ErrorMapper.attachmentUnreadable(error.safeDiagnostic()))
        }
        if (contentType == null) {
            return SingleUploadResult.Failed(attachment, ErrorMapper.attachmentUnreadable(MISSING_CONTENT_TYPE_MESSAGE))
        }
        if (!contentType.matches(attachment.kind)) {
            return SingleUploadResult.Failed(
                attachment,
                ErrorMapper.attachmentTypeUnsupported("$CONTENT_TYPE_DIAGNOSTIC_PREFIX$contentType"),
            )
        }

        return try {
            val ticket = service.generateUploadUrl(UploadUrlRequest(contentType))
            check(ticket.uploadUrl.isHttps()) { INVALID_UPLOAD_URL_MESSAGE }
            check(ticket.publicUrl.isHttps()) { INVALID_PUBLIC_URL_MESSAGE }
            check(ticket.contentType == contentType) { CONTENT_TYPE_CHANGED_MESSAGE }
            val body = StreamingAttachmentRequestBody(
                contentType = contentType,
                contentLength = localSource.contentLength(attachment.uri),
                openSource = { localSource.open(attachment.uri) },
            )
            binaryUploader.upload(ticket.uploadUrl, ticket.uploadHeaders, ticket.contentType, body)
            SingleUploadResult.Uploaded(attachment.copy(remoteUrl = ticket.publicUrl))
        } catch (error: IOException) {
            SingleUploadResult.Failed(attachment, ErrorMapper.from(error))
        } catch (error: HttpException) {
            SingleUploadResult.Failed(attachment, ErrorMapper.from(error))
        } catch (error: IllegalArgumentException) {
            SingleUploadResult.Failed(attachment, ErrorMapper.attachmentUnreadable(error.safeDiagnostic()))
        } catch (error: IllegalStateException) {
            SingleUploadResult.Failed(attachment, ErrorMapper.protocol(error.safeDiagnostic()))
        } catch (error: SecurityException) {
            SingleUploadResult.Failed(attachment, ErrorMapper.attachmentUnreadable(error.safeDiagnostic()))
        }
    }

    private sealed interface SingleUploadResult {
        data class Uploaded(val attachment: GenerationAttachment) : SingleUploadResult
        data class Failed(val attachment: GenerationAttachment, val error: AppError) : SingleUploadResult
    }
}

private class StreamingAttachmentRequestBody(
    contentType: String,
    private val contentLength: Long?,
    private val openSource: () -> InputStream?,
) : RequestBody() {
    private val mediaType = contentType.toMediaType()

    override fun contentType() = mediaType

    override fun contentLength(): Long = contentLength ?: UNKNOWN_CONTENT_LENGTH

    override fun writeTo(sink: BufferedSink) {
        val input = openSource() ?: throw IOException(ATTACHMENT_OPEN_FAILED_MESSAGE)
        input.use { stream -> sink.writeAll(stream.source()) }
    }
}

private fun String.isHttps(): Boolean = toHttpUrlOrNull()?.isHttps == true

private fun String.matches(kind: MediaKind): Boolean = when (kind) {
    MediaKind.IMAGE -> startsWith(IMAGE_CONTENT_TYPE_PREFIX)
    MediaKind.VIDEO -> startsWith(VIDEO_CONTENT_TYPE_PREFIX)
    MediaKind.AUDIO -> startsWith(AUDIO_CONTENT_TYPE_PREFIX)
}

private fun Throwable.safeDiagnostic(): String = this::class.java.simpleName

private const val READ_MODE = "r"
private const val UNKNOWN_CONTENT_LENGTH = -1L
private const val IMAGE_CONTENT_TYPE_PREFIX = "image/"
private const val VIDEO_CONTENT_TYPE_PREFIX = "video/"
private const val AUDIO_CONTENT_TYPE_PREFIX = "audio/"
private const val CONTENT_URI_REQUIRED_MESSAGE = "Local attachments must use a content URI"
private const val MISSING_CONTENT_TYPE_MESSAGE = "The content resolver did not return a MIME type"
private const val CONTENT_TYPE_DIAGNOSTIC_PREFIX = "Attachment MIME type: "
private const val INVALID_UPLOAD_URL_MESSAGE = "The presigned upload URL must use HTTPS"
private const val INVALID_PUBLIC_URL_MESSAGE = "The uploaded attachment URL must use HTTPS"
private const val CONTENT_TYPE_CHANGED_MESSAGE = "The upload ticket returned a different content type"
private const val ATTACHMENT_OPEN_FAILED_MESSAGE = "The selected attachment could not be opened"
