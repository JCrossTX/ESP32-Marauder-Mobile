package com.marauder.mobile.protocol

import java.io.ByteArrayOutputStream

class SerialDemux(
    private val onLine: (String) -> Unit,
    private val onFrame: (CaptureFrame) -> Unit,
    private val onError: (String) -> Unit = {},
) {
    private enum class State { TEXT, CAPTURE }

    private var state = State.TEXT
    private val line = ByteArrayOutputStream(256)
    private val payload = ByteArrayOutputStream(4096)

    private var beginMatch = 0
    private var closeMatch = 0
    private var seq = 0L

    fun reset() {
        state = State.TEXT
        line.reset()
        payload.reset()
        beginMatch = 0
        closeMatch = 0

    }

    fun feed(data: ByteArray, len: Int) {
        var i = 0
        while (i < len) {
            val b = data[i].toInt() and 0xFF
            when (state) {
                State.TEXT -> textByte(b)
                State.CAPTURE -> captureByte(b)
            }
            i++
        }
    }

    private fun textByte(b: Int) {
        if (b == BEGIN[beginMatch]) {
            beginMatch++
            if (beginMatch == BEGIN.size) {

                emitLine()
                beginMatch = 0
                payload.reset()
                closeMatch = 0
                state = State.CAPTURE
            }
            return
        }

        if (beginMatch > 0) {
            val restart = (b == BEGIN[0])
            for (k in 0 until beginMatch) consumeTextByte(BEGIN[k])
            beginMatch = if (restart) 1 else 0
            if (!restart) consumeTextByte(b)
            return
        }
        consumeTextByte(b)
    }

    private fun consumeTextByte(b: Int) {
        when (b) {
            '\n'.code -> emitLine()
            '\r'.code -> {   }
            else -> line.write(b)
        }
    }

    private fun captureByte(b: Int) {
        if (b == CLOSE[closeMatch]) {
            closeMatch++
            if (closeMatch == CLOSE.size) {

                val bytes = payload.toByteArray()
                payload.reset()
                closeMatch = 0
                state = State.TEXT
                onFrame(CaptureFrame(seq++, CaptureFrame.TYPE_PCAP, bytes))
            }
            return
        }
        if (closeMatch > 0) {
            val restart = (b == CLOSE[0])
            for (k in 0 until closeMatch) payload.write(CLOSE[k])
            closeMatch = if (restart) 1 else 0
            if (!restart) payload.write(b)
            return
        }
        payload.write(b)
    }

    private fun emitLine() {
        if (line.size() == 0) return
        val text = line.toString("UTF-8")
        line.reset()
        if (text.isNotEmpty()) onLine(text)
    }

    private companion object {
        val BEGIN = "[BUF/BEGIN]".toByteArray(Charsets.US_ASCII).map { it.toInt() and 0xFF }.toIntArray()
        val CLOSE = "[BUF/CLOSE]".toByteArray(Charsets.US_ASCII).map { it.toInt() and 0xFF }.toIntArray()
    }
}
