package com.marauder.mobile.data

import com.marauder.mobile.protocol.DeviceMessage
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class PhoneLocation(
    val lat: Double,
    val lon: Double,
    val altMeters: Double,
    val accuracyMeters: Double,
    val timeMillis: Long,
)

object WardriveAssembler {

    fun header(appRelease: String): String =
        "WigleWifi-1.4,appRelease=$appRelease,model=ESP32 Marauder,release=$appRelease," +
            "device=Marauder Mobile,display=phone,board=ESP32 Marauder,brand=JustCallMeKoko\n" +
            "MAC,SSID,AuthMode,FirstSeen,Channel,RSSI,CurrentLatitude,CurrentLongitude," +
            "AltitudeMeters,AccuracyMeters,Type\n"

    fun line(ap: DeviceMessage.Ap, loc: PhoneLocation?): String {
        val lat = loc?.lat ?: 0.0
        val lon = loc?.lon ?: 0.0
        val alt = loc?.altMeters ?: 0.0
        val acc = loc?.accuracyMeters ?: 0.0
        val seen = firstSeen(loc?.timeMillis ?: System.currentTimeMillis())
        val ssid = ap.essid.replace(",", " ")
        return "${ap.bssid},$ssid,${authMode(ap.sec)},$seen,${ap.channel},${ap.rssi}," +
            "$lat,$lon,$alt,$acc,WIFI\n"
    }

    fun bleLine(mac: String, rssi: Int, loc: PhoneLocation?): String {
        val lat = loc?.lat ?: 0.0
        val lon = loc?.lon ?: 0.0
        val alt = loc?.altMeters ?: 0.0
        val acc = loc?.accuracyMeters ?: 0.0
        val seen = firstSeen(loc?.timeMillis ?: System.currentTimeMillis())
        return "$mac,,[BLE],$seen,0,$rssi,$lat,$lon,$alt,$acc,BLE\n"
    }

    private val fmt: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault())

    private fun firstSeen(millis: Long): String = fmt.format(Instant.ofEpochMilli(millis))

    private fun authMode(sec: Int): String = when (sec) {
        0 -> "[OPEN]"
        1 -> "[WEP]"
        2 -> "[WPA-PSK]"
        3 -> "[WPA2-PSK]"
        4 -> "[WPA-WPA2-PSK]"
        5 -> "[WPA2-EAP]"
        6 -> "[WPA3-PSK]"
        7 -> "[WPA2-WPA3-PSK]"
        8 -> "[WAPI-PSK]"
        else -> "[UNKNOWN]"
    }
}
