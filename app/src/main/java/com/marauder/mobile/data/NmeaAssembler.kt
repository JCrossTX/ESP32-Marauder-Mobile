package com.marauder.mobile.data

import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.math.abs

object NmeaAssembler {

    fun gga(loc: PhoneLocation): String {
        val (lat, ns) = ddmm(loc.lat, true)
        val (lon, ew) = ddmm(loc.lon, false)
        val body = "GPGGA,${hms(loc.timeMillis)},$lat,$ns,$lon,$ew,1,08,0.9," +
            "${"%.1f".format(loc.altMeters)},M,0.0,M,,"
        return withChecksum(body)
    }

    fun rmc(loc: PhoneLocation): String {
        val (lat, ns) = ddmm(loc.lat, true)
        val (lon, ew) = ddmm(loc.lon, false)
        val body = "GPRMC,${hms(loc.timeMillis)},A,$lat,$ns,$lon,$ew,0.0,0.0,${dmy(loc.timeMillis)},,"
        return withChecksum(body)
    }

    private fun ddmm(v: Double, isLat: Boolean): Pair<String, Char> {
        val hemi = if (isLat) (if (v >= 0) 'N' else 'S') else (if (v >= 0) 'E' else 'W')
        val a = abs(v)
        val deg = a.toInt()
        val min = (a - deg) * 60.0
        val degStr = if (isLat) "%02d".format(deg) else "%03d".format(deg)
        return "$degStr${"%07.4f".format(min)}" to hemi
    }

    private fun withChecksum(body: String): String {
        var cs = 0
        for (c in body) cs = cs xor c.code
        return "\$$body*${"%02X".format(cs)}"
    }

    private val hmsFmt = DateTimeFormatter.ofPattern("HHmmss.00").withZone(ZoneOffset.UTC)
    private val dmyFmt = DateTimeFormatter.ofPattern("ddMMyy").withZone(ZoneOffset.UTC)
    private fun hms(millis: Long) = hmsFmt.format(Instant.ofEpochMilli(millis))
    private fun dmy(millis: Long) = dmyFmt.format(Instant.ofEpochMilli(millis))
}
