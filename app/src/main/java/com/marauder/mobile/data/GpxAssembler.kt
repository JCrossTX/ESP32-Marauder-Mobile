package com.marauder.mobile.data

import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

object GpxAssembler {

    const val TRACK_HEADER =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<gpx version=\"1.1\" creator=\"Marauder Mobile\" xmlns=\"http://www.topografix.com/GPX/1/1\">\n" +
            "  <trk><name>Marauder Track</name><trkseg>\n"
    const val TRACK_FOOTER = "  </trkseg></trk>\n</gpx>\n"

    const val POI_HEADER =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<gpx version=\"1.1\" creator=\"Marauder Mobile\" xmlns=\"http://www.topografix.com/GPX/1/1\">\n"
    const val POI_FOOTER = "</gpx>\n"

    fun trackPoint(loc: PhoneLocation): String =
        "    <trkpt lat=\"${loc.lat}\" lon=\"${loc.lon}\">" +
            "<ele>${"%.2f".format(loc.altMeters)}</ele><time>${iso(loc.timeMillis)}</time></trkpt>\n"

    fun waypoint(loc: PhoneLocation, name: String): String =
        "  <wpt lat=\"${loc.lat}\" lon=\"${loc.lon}\">" +
            "<ele>${"%.2f".format(loc.altMeters)}</ele><time>${iso(loc.timeMillis)}</time>" +
            "<name>${escape(name)}</name></wpt>\n"

    private val fmt: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC)

    private fun iso(millis: Long): String = fmt.format(Instant.ofEpochMilli(millis))

    private fun escape(s: String): String =
        s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
}
