package com.promptstudio.app.core.network

import com.promptstudio.app.R
import com.promptstudio.app.core.model.GenerationAttachment
import com.promptstudio.app.core.model.MediaKind
import com.promptstudio.app.core.model.MediaRole
import java.io.ByteArrayInputStream
import java.io.InputStream
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import okhttp3.RequestBody
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SecureAttachmentUploaderTest {
    @Test
    fun `local content is streamed to an unauthenticated binary uploader and receives its public URL`() = runTest {
        val service = FakeProviderService()
        val binaryUploader = RecordingBinaryUploader()
        val uploader = SecureAttachmentUploader(
            service = service,
            binaryUploader = binaryUploader,
            localSource = FakeLocalAttachmentSource(IMAGE_CONTENT_TYPE, CONTENT_BYTES),
        )

        val result = uploader.uploadAll(listOf(localImage()))

        assertTrue(result is AttachmentUploadResult.Uploaded)
        val uploaded = (result as AttachmentUploadResult.Uploaded).attachments.single()
        assertEquals(PUBLIC_URL, uploaded.remoteUrl)
        assertEquals(IMAGE_CONTENT_TYPE, service.requestedContentTypes.single())
        assertEquals(CONTENT_BYTES.toList(), binaryUploader.bytes.toList())
        assertEquals(UPLOAD_URL, binaryUploader.uploadUrl)
    }

    @Test
    fun `attachment kind mismatch returns a central resource backed error without requesting upload`() = runTest {
        val service = FakeProviderService()
        val uploader = SecureAttachmentUploader(
            service = service,
            binaryUploader = RecordingBinaryUploader(),
            localSource = FakeLocalAttachmentSource(VIDEO_CONTENT_TYPE, CONTENT_BYTES),
        )

        val result = uploader.uploadAll(listOf(localImage()))

        assertTrue(result is AttachmentUploadResult.Failed)
        val failure = result as AttachmentUploadResult.Failed
        assertEquals("attachment_type_unsupported", failure.error.code)
        assertEquals(R.string.error_attachment_type_unsupported, failure.error.messageResId)
        assertTrue(service.requestedContentTypes.isEmpty())
    }

    @Test
    fun `existing HTTPS public URL is retained without reading or uploading local content`() = runTest {
        val service = FakeProviderService()
        val binaryUploader = RecordingBinaryUploader()
        val uploader = SecureAttachmentUploader(
            service = service,
            binaryUploader = binaryUploader,
            localSource = FakeLocalAttachmentSource(null, CONTENT_BYTES),
        )

        val result = uploader.uploadAll(listOf(localImage().copy(remoteUrl = PUBLIC_URL)))

        assertEquals(PUBLIC_URL, (result as AttachmentUploadResult.Uploaded).attachments.single().remoteUrl)
        assertTrue(service.requestedContentTypes.isEmpty())
        assertTrue(binaryUploader.bytes.isEmpty())
    }

    private fun localImage() = GenerationAttachment(
        id = ATTACHMENT_ID,
        uri = LOCAL_URI,
        kind = MediaKind.IMAGE,
        role = MediaRole.SOURCE,
    )
}

private class FakeLocalAttachmentSource(
    private val mimeType: String?,
    private val bytes: ByteArray,
) : LocalAttachmentSource {
    override fun contentType(uri: String): String? = mimeType
    override fun contentLength(uri: String): Long = bytes.size.toLong()
    override fun open(uri: String): InputStream = ByteArrayInputStream(bytes)
}

private class RecordingBinaryUploader : AttachmentBinaryUploader {
    var uploadUrl: String? = null
    var bytes: ByteArray = byteArrayOf()

    override fun upload(
        uploadUrl: String,
        headers: Map<String, String>,
        contentType: String,
        body: RequestBody,
    ) {
        this.uploadUrl = uploadUrl
        val buffer = Buffer()
        body.writeTo(buffer)
        bytes = buffer.readByteArray()
    }
}

private class FakeProviderService : ProviderService {
    val requestedContentTypes = mutableListOf<String>()

    override suspend fun generateUploadUrl(request: UploadUrlRequest): UploadUrlResponse {
        requestedContentTypes += request.contentType
        return UploadUrlResponse(
            publicUrl = PUBLIC_URL,
            uploadUrl = UPLOAD_URL,
            contentType = request.contentType,
            uploadHeaders = mapOf(UPLOAD_HEADER_NAME to UPLOAD_HEADER_VALUE),
        )
    }

    override suspend fun submitWorkflow(endpointPath: String, request: JsonObject): RemoteRequestStatus =
        error(UNEXPECTED_CALL_MESSAGE)

    override suspend fun getRequestStatus(statusUrl: String): RemoteRequestStatus = error(UNEXPECTED_CALL_MESSAGE)

    override suspend fun cancelRequest(cancelUrl: String) = error(UNEXPECTED_CALL_MESSAGE)
}

private val CONTENT_BYTES = byteArrayOf(1, 2, 3, 4)
private const val ATTACHMENT_ID = "attachment-1"
private const val LOCAL_URI = "content://draft/source"
private const val IMAGE_CONTENT_TYPE = "image/jpeg"
private const val VIDEO_CONTENT_TYPE = "video/mp4"
private const val PUBLIC_URL = "https://cdn.example.test/source.jpg"
private const val UPLOAD_URL = "https://storage.example.test/upload"
private const val UPLOAD_HEADER_NAME = "x-upload-token"
private const val UPLOAD_HEADER_VALUE = "token"
private const val UNEXPECTED_CALL_MESSAGE = "Unexpected service call"
