package com.parentalcontrol.app.utils

import java.io.ByteArrayOutputStream
import java.io.InputStream

object MjpegFrameReader {

    fun readJpegFrame(input: InputStream): ByteArray? {
        val output = ByteArrayOutputStream()
        var prev = -1
        var started = false

        while (true) {
            val current = input.read()
            if (current == -1) return null
            if (!started) {
                if (prev == 0xFF && current == 0xD8) {
                    started = true
                    output.write(0xFF)
                    output.write(0xD8)
                }
            } else {
                output.write(current)
                if (prev == 0xFF && current == 0xD9) {
                    return output.toByteArray()
                }
            }
            prev = current
        }
    }
}
