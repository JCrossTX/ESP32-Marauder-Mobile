package com.marauder.mobile

import com.marauder.mobile.protocol.CaptureFrame
import com.marauder.mobile.protocol.SerialDemux
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayOutputStream

class SerialDemuxTest {

    private val BEGIN = "[BUF/BEGIN]".toByteArray(Charsets.US_ASCII)
    private val CLOSE = "[BUF/CLOSE]".toByteArray(Charsets.US_ASCII)

    private fun block(payload: ByteArray): ByteArray =
        ByteArrayOutputStream().apply { write(BEGIN); write(payload); write(CLOSE) }.toByteArray()

    private fun run(stream: ByteArray, chunk: Int): Pair<List<String>, List<CaptureFrame>> {
        val lines = ArrayList<String>()
        val frames = ArrayList<CaptureFrame>()
        val d = SerialDemux(onLine = { lines.add(it) }, onFrame = { frames.add(it) })
        var i = 0
        while (i < stream.size) {
            val n = minOf(chunk, stream.size - i)
            d.feed(stream.copyOfRange(i, i + n), n)
            i += n
        }
        return lines to frames
    }

    @Test fun separatesLinesAndFrames() {

        val payload = ByteArray(300) { (it and 0xFF).toByte() }
        val stream = ByteArrayOutputStream().apply {
            write("Starting Raw sniff. Stop with stopscan\n".toByteArray(Charsets.UTF_8))
            write(block(payload))
            write("plain console line\n".toByteArray(Charsets.UTF_8))
        }.toByteArray()

        val (lines, frames) = run(stream, chunk = stream.size)

        assertEquals(listOf("Starting Raw sniff. Stop with stopscan", "plain console line"), lines)
        assertEquals(1, frames.size)
        assertArrayEquals(payload, frames[0].payload)
    }

    @Test fun reassemblesAcrossOneBytePerChunk() {
        val payload = ByteArray(1024) { ((it * 31) and 0xFF).toByte() }
        val stream = ByteArrayOutputStream().apply {
            write("line1\n".toByteArray(Charsets.UTF_8))
            write(block(payload))
            write(block("hello".toByteArray()))
        }.toByteArray()

        val (lines, frames) = run(stream, chunk = 1)

        assertEquals(listOf("line1"), lines)
        assertEquals(2, frames.size)
        assertArrayEquals(payload, frames[0].payload)
        assertArrayEquals("hello".toByteArray(), frames[1].payload)
        assertEquals(0L, frames[0].seq)
        assertEquals(1L, frames[1].seq)
    }

    @Test fun payloadContainingPartialMarkersSurvives() {

        val payload = "[BUF/ noise [BUF/CLOS more [ data".toByteArray(Charsets.US_ASCII)
        val (lines, frames) = run(block(payload), chunk = 3)
        assertEquals(emptyList<String>(), lines)
        assertEquals(1, frames.size)
        assertArrayEquals(payload, frames[0].payload)
    }

    @Test fun bracketTextThatIsNotAMarkerStaysText() {

        val stream = "config [BUF/BEG] not a marker\nnext\n".toByteArray(Charsets.UTF_8)
        val (lines, frames) = run(stream, chunk = 5)
        assertEquals(listOf("config [BUF/BEG] not a marker", "next"), lines)
        assertEquals(0, frames.size)
    }

    @Test fun blockWithNoLeadingNewlineStillSplits() {

        val stream = ByteArrayOutputStream().apply {
            write("tail".toByteArray(Charsets.UTF_8))
            write(block(byteArrayOf(1, 2, 3)))
            write("\n".toByteArray(Charsets.UTF_8))
        }.toByteArray()
        val (lines, frames) = run(stream, chunk = stream.size)
        assertEquals(listOf("tail"), lines)
        assertEquals(1, frames.size)
        assertArrayEquals(byteArrayOf(1, 2, 3), frames[0].payload)
    }
}
