package com.marauder.mobile.protocol

class CaptureFrame(val seq: Long, val type: Int, val payload: ByteArray) {

    companion object {
        const val TYPE_PCAP = 0
        const val TYPE_LOG = 1
        const val TYPE_GPX = 2

        fun extension(type: Int): String = when (type) {
            TYPE_PCAP -> "pcap"
            TYPE_GPX -> "gpx"
            else -> "log"
        }
    }
}
