package com.promptstudio.app.core.error

import com.promptstudio.app.R
import com.promptstudio.app.core.network.UploadTooLargeException
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ErrorMapperTest {
    @Test
    fun `network failures map to a retryable resource-backed error`() {
        val error = ErrorMapper.from(IOException())

        assertEquals("network", error.code)
        assertEquals(R.string.error_network_unavailable, error.messageResId)
        assertTrue(error.retryable)
    }

    @Test
    fun `protocol failures keep only a safe diagnostic message`() {
        val error = ErrorMapper.protocol("Completed request returned no media outputs")

        assertEquals("remote_protocol", error.code)
        assertEquals(R.string.error_remote_protocol, error.messageResId)
        assertFalse(error.retryable)
        assertEquals("Completed request returned no media outputs", error.diagnosticMessage)
    }

    @Test
    fun `upload size rejection shows the maximum when storage supplies it`() {
        val error = ErrorMapper.from(UploadTooLargeException("<MaxSizeAllowed>20971520</MaxSizeAllowed>"))

        assertEquals("attachment_too_large", error.code)
        assertEquals("This file is too large. The maximum upload size is 20.0 MiB. Choose a smaller file.", error.userMessage)
        assertFalse(error.retryable)
    }

    @Test
    fun `upload size rejection without a maximum uses an honest fallback`() {
        val error = ErrorMapper.from(UploadTooLargeException("<Code>EntityTooLarge</Code>"))

        assertEquals(R.string.error_attachment_too_large, error.messageResId)
        assertEquals(null, error.userMessage)
        assertFalse(error.retryable)
    }
}
