package com.marauder.mobile.protocol

sealed interface DeviceMessage {

    data class Info(
        val fw: String,
        val proto: Int,
        val board: String,
        val caps: List<String>,
    ) : DeviceMessage {
        fun has(cap: String) = caps.any { it.equals(cap, ignoreCase = true) }
    }

    data class Status(
        val modeLabel: String,
        val running: Boolean,
        val free: Long,
        val aps: Int,
        val stations: Int,
        val ssids: Int,
        val ips: Int,
        val probes: Int,
        val airtags: Int,
    ) : DeviceMessage

    data class Ap(
        val index: Int,
        val channel: Int,
        val rssi: Int,
        val selected: Boolean,
        val packets: Long,
        val sec: Int,
        val wps: Boolean,
        val stations: Int,
        val bssid: String,
        val essid: String,
    ) : DeviceMessage

    data class Sta(
        val ap: Int,
        val index: Int,
        val selected: Boolean,
        val packets: Long,
        val mac: String,
    ) : DeviceMessage

    data class SsidRow(
        val index: Int,
        val channel: Int,
        val selected: Boolean,
        val essid: String,
    ) : DeviceMessage

    data class Ip(val index: Int, val ip: String) : DeviceMessage

    data class FoxTargetItem(val index: Int, val label: String) : DeviceMessage

    data class Probe(
        val index: Int,
        val requests: Long,
        val selected: Boolean,
        val essid: String,
    ) : DeviceMessage

    data class Airtag(
        val index: Int,
        val rssi: Int,
        val selected: Boolean,
        val mac: String,
    ) : DeviceMessage

    data class Ble(
        val index: Int,
        val rssi: Int,
        val name: String,
    ) : DeviceMessage

    data class Cred(val user: String, val pass: String) : DeviceMessage
}
