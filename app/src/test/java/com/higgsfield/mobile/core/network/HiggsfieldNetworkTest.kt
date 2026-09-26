package com.higgsfield.mobile.core.network

import kotlin.random.Random
import com.higgsfield.mobile.core.model.CreativeBrief
import com.higgsfield.mobile.core.model.GenerationDraft
import com.higgsfield.mobile.core.model.WorkflowCatalog
import com.higgsfield.mobile.core.model.WorkflowRegistry
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HiggsfieldNetworkTest {
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `upload URL request uses documented path body and authorization`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""
            {"public_url":"https://cdn.example.test/input.jpg","upload_url":"https://storage.example.test/upload","content_type":"image/jpeg","upload_headers":{"Content-Type":"image/jpeg"}}
        """))
        val service = HiggsfieldNetwork.createService(
            credentials = ApiCredentials("key-id", "secret-value"),
            baseUrl = server.url("/").toString(),
            authorizedHosts = setOf(server.hostName),
        )

        val response = service.generateUploadUrl(UploadUrlRequest("image/jpeg"))

        assertEquals("https://cdn.example.test/input.jpg", response.publicUrl)
        server.takeRequest().assertUploadUrlRequest()
    }

    @Test
    fun `soul v2 standard request uses documented route body and authorization`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""
            {"status":"queued","request_id":"request-1","status_url":"https://api.higgsfield.ai/requests/request-1/status","cancel_url":"https://api.higgsfield.ai/requests/request-1/cancel"}
        """))
        val service = HiggsfieldNetwork.createService(
            credentials = ApiCredentials("key-id", "secret-value"),
            baseUrl = server.url("/").toString(),
            authorizedHosts = setOf(server.hostName),
        )

        val adapter = SchemaWorkflowAdapter(
            descriptor = WorkflowRegistry.find(WorkflowCatalog.SOUL_V2.id)!!,
            schema = WorkflowRequestSchemas.soulV2Standard,
        )
        val response = service.submitWorkflow(
            endpointPath = adapter.descriptor.endpointPath!!,
            request = adapter.toRequest(
                GenerationDraft(
                    instruction = "A ceramic bird",
                    creativeBrief = CreativeBrief(),
                    workflowId = WorkflowCatalog.SOUL_V2.id,
                ),
            ),
        )

        assertEquals("queued", response.status)
        assertEquals("request-1", response.requestId)
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/higgsfield-ai/soul/v2/standard", request.path)
        assertEquals("Key key-id:secret-value", request.getHeader("Authorization"))
        assertEquals("{\"prompt\":\"A ceramic bird\"}", request.body.readUtf8())
    }

    @Test
    fun `missing credentials never add authorization`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""
            {"public_url":"a","upload_url":"b","content_type":"image/jpeg","upload_headers":{}}
        """))
        val service = HiggsfieldNetwork.createService(
            credentials = ApiCredentials("", ""),
            baseUrl = server.url("/").toString(),
            authorizedHosts = setOf(server.hostName),
        )

        service.generateUploadUrl(UploadUrlRequest("image/jpeg"))

        assertEquals(null, server.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun `status request uses the returned URL and authorization`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""
            {"status":"completed","request_id":"request-1","images":[{"url":"https://cdn.example.test/image.jpg"}]}
        """))
        val service = HiggsfieldNetwork.createService(
            credentials = ApiCredentials("key-id", "secret-value"),
            baseUrl = server.url("/").toString(),
            authorizedHosts = setOf(server.hostName),
        )

        val status = service.getRequestStatus(server.url("/requests/request-1/status").toString())

        assertEquals("completed", status.status)
        assertEquals("https://cdn.example.test/image.jpg", status.images.single().url)
        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/requests/request-1/status", request.path)
        assertEquals("Key key-id:secret-value", request.getHeader("Authorization"))
    }

    @Test
    fun `cancellation uses the supplied API URL and authorization`() = runTest {
        server.enqueue(MockResponse().setResponseCode(202))
        val service = HiggsfieldNetwork.createService(
            credentials = ApiCredentials("key-id", "secret-value"),
            baseUrl = server.url("/").toString(),
            authorizedHosts = setOf(server.hostName),
        )

        service.cancelRequest(server.url("/requests/request-1/cancel").toString())

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/requests/request-1/cancel", request.path)
        assertEquals("Key key-id:secret-value", request.getHeader("Authorization"))
    }

    @Test
    fun `only HTTPS Higgsfield URLs can be polled`() {
        assertTrue(HiggsfieldUrlValidator.isApiUrl("https://api.higgsfield.ai/requests/request-1/status"))
        assertTrue(HiggsfieldUrlValidator.isApiUrl("https://platform.higgsfield.ai/requests/request-1/status"))
        assertFalse(HiggsfieldUrlValidator.isApiUrl("http://api.higgsfield.ai/requests/request-1/status"))
        assertFalse(HiggsfieldUrlValidator.isApiUrl("https://storage.example.test/requests/request-1/status"))
    }

    @Test
    fun `presigned uploads remove any authorization header`() {
        server.enqueue(MockResponse().setResponseCode(200))
        val client = PresignedUploadClient(PresignedUploadClient.unauthenticatedClient())

        client.upload(
            uploadUrl = server.url("/upload").toString(),
            headers = mapOf("Authorization" to "Key should-not-leak", "x-amz-tagging" to "retention=temporary"),
            contentType = "image/jpeg",
            body = PresignedUploadClient.body(byteArrayOf(1, 2), "image/jpeg"),
        )

        val request = server.takeRequest()
        assertEquals("PUT", request.method)
        assertEquals(null, request.getHeader("Authorization"))
        assertEquals("retention=temporary", request.getHeader("x-amz-tagging"))
    }

    @Test
    fun `polling delays start at two seconds grow and cap at ten seconds plus jitter`() {
        val policy = PollingPolicy(Random(1))

        val first = policy.nextDelayMillis(null)
        val second = policy.nextDelayMillis(first)
        val capped = policy.nextDelayMillis(PollingPolicy.MAX_DELAY_MILLIS)

        assertTrue(first in 2_000L..2_500L)
        assertTrue(second > first)
        assertTrue(capped in 10_000L..10_500L)
        assertFalse(capped < PollingPolicy.MAX_DELAY_MILLIS)
    }

    private fun RecordedRequest.assertUploadUrlRequest() {
        assertEquals("POST", method)
        assertEquals("/files/generate-upload-url", path)
        assertEquals("Key key-id:secret-value", getHeader("Authorization"))
        assertEquals("application/json; charset=utf-8", getHeader("Content-Type"))
        assertEquals("{\"content_type\":\"image/jpeg\"}", body.readUtf8())
    }
}
