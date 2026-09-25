package com.higgsfield.mobile.core.error

import com.higgsfield.mobile.R
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
}
