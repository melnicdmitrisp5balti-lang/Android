package com.parentalcontrol.app.utils

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.ByteArrayInputStream

class MjpegFrameReaderTest {

    @Test
    fun `readJpegFrame returns bytes between jpeg markers`() {
        val jpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0x11, 0x22, 0xFF.toByte(), 0xD9.toByte())
        val payload = byteArrayOf(0x01, 0x02, 0x03) + jpeg + byteArrayOf(0x55)
        val frame = MjpegFrameReader.readJpegFrame(ByteArrayInputStream(payload))
        assertArrayEquals(jpeg, frame)
    }

    @Test
    fun `readJpegFrame returns null when markers are missing`() {
        val frame = MjpegFrameReader.readJpegFrame(ByteArrayInputStream(byteArrayOf(0x10, 0x20, 0x30)))
        assertNull(frame)
    }
}
