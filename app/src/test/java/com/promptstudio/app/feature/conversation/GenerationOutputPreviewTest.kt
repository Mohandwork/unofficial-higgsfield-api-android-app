package com.promptstudio.app.feature.conversation

import com.promptstudio.app.core.model.GenerationOutput
import com.promptstudio.app.core.model.MediaKind
import org.junit.Assert.assertEquals
import org.junit.Test

class GenerationOutputPreviewTest {
    private val output = GenerationOutput(
        id = "video-1",
        remoteUrl = "https://media.example.test/video.mp4",
        kind = MediaKind.VIDEO,
        localUri = "content://downloads/video-1",
    )

    @Test
    fun `missing persisted read permission uses remote media`() {
        assertEquals(output.remoteUrl, output.previewUri(emptySet()))
    }

    @Test
    fun `persisted read permission permits local media`() {
        assertEquals(output.localUri, output.previewUri(setOf("content://downloads/video-1")))
    }
}
