package com.marauder.mobile.protocol

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.longOrNull

object UpstreamParser {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    const val MACHINE_PREFIX = "@MARAUDER:"

    private val LEADING_PROMPT = Regex("^(?:> )+")

    fun stripPrompt(line: String): String = line.replaceFirst(LEADING_PROMPT, "")

    fun echoedCommand(line: String): String? =
        if (line.startsWith("#")) line.substring(1) else null

    data class Machine(
        val protocol: Int,
        val tx: String,
        val command: String,
        val status: String,
        val code: String,
        val firmware: String?,
        val capabilities: List<String>,
        val files: Long,
        val bytes: Long,
        val rebooting: Boolean,
    )

    fun machine(line: String): Machine? {
        if (!line.startsWith(MACHINE_PREFIX)) return null
        val body = line.substring(MACHINE_PREFIX.length).trim()
        val obj: JsonObject = try {
            json.parseToJsonElement(body).jsonObject
        } catch (e: Exception) {
            return null
        }
        return Machine(
            protocol = obj.int("protocol"),
            tx = obj.str("tx").orEmpty(),
            command = obj.str("command").orEmpty(),
            status = obj.str("status").orEmpty(),
            code = obj.str("code").orEmpty(),
            firmware = obj.str("firmware"),
            capabilities = obj.strList("capabilities"),
            files = obj.long("files"),
            bytes = obj.long("bytes"),
            rebooting = obj.bool("rebooting"),
        )
    }

    private val AP_ROW = Regex("""^\[(\d+)]\[CH:(-?\d+)] (.*) (-?\d+)( \(selected\))?$""")

    fun apListRow(line: String): DeviceMessage.Ap? {
        val m = AP_ROW.matchEntire(line) ?: return null
        return DeviceMessage.Ap(
            index = m.groupValues[1].toInt(),
            channel = m.groupValues[2].toInt(),
            essid = m.groupValues[3],
            rssi = m.groupValues[4].toInt(),
            selected = m.groupValues[5].isNotEmpty(),
            packets = 0, sec = 0, wps = false, stations = 0, bssid = "",
        )
    }

    private val INDEXED_ROW = Regex("""^\[(\d+)] (.*)$""")

    fun ssidListRow(line: String): DeviceMessage.SsidRow? {
        val m = INDEXED_ROW.matchEntire(line) ?: return null
        var essid = m.groupValues[2]
        val selected = essid.endsWith(" (selected)")
        if (selected) essid = essid.removeSuffix(" (selected)")
        return DeviceMessage.SsidRow(index = m.groupValues[1].toInt(), channel = 0, selected = selected, essid = essid)
    }

    private val IP_ROW = Regex("""^\[(\d+)] (\d{1,3}(?:\.\d{1,3}){3})$""")

    fun ipListRow(line: String): DeviceMessage.Ip? {
        val m = IP_ROW.matchEntire(line) ?: return null
        return DeviceMessage.Ip(index = m.groupValues[1].toInt(), ip = m.groupValues[2])
    }

    fun probeListRow(line: String): DeviceMessage.Probe? {
        val m = INDEXED_ROW.matchEntire(line) ?: return null
        return DeviceMessage.Probe(index = m.groupValues[1].toInt(), requests = 0, selected = false, essid = m.groupValues[2])
    }

    private val AIRTAG_ROW = Regex("""^\[(\d+)]MAC: (.+)$""")

    fun airtagListRow(line: String): DeviceMessage.Airtag? {
        val m = AIRTAG_ROW.matchEntire(line) ?: return null
        return DeviceMessage.Airtag(index = m.groupValues[1].toInt(), rssi = 0, selected = false, mac = m.groupValues[2])
    }

    private val STA_HEADER = Regex("""^\[(\d+)] (.*) (-?\d+):$""")
    private val STA_ROW = Regex("""^ {2}\[(\d+)] ([0-9a-fA-F:]{17})( \(selected\))?$""")

    fun staListHeaderApIndex(line: String): Int? =
        STA_HEADER.matchEntire(line)?.groupValues?.get(1)?.toInt()

    fun staListRow(line: String, apIndex: Int): DeviceMessage.Sta? {
        val m = STA_ROW.matchEntire(line) ?: return null
        return DeviceMessage.Sta(
            ap = apIndex,
            index = m.groupValues[1].toInt(),
            mac = m.groupValues[2],
            selected = m.groupValues[3].isNotEmpty(),
            packets = 0,
        )
    }

    fun foxLabelListRow(line: String): DeviceMessage.FoxTargetItem? {
        val m = INDEXED_ROW.matchEntire(line) ?: return null
        return DeviceMessage.FoxTargetItem(index = m.groupValues[1].toInt(), label = m.groupValues[2])
    }

    private val FLIPPER_ROW = Regex("""^\[(\d+)]MAC: (.+)$""")

    fun flipperListRow(line: String): DeviceMessage.FoxTargetItem? {
        val m = FLIPPER_ROW.matchEntire(line) ?: return null
        return DeviceMessage.FoxTargetItem(index = m.groupValues[1].toInt(), label = m.groupValues[2].trim())
    }

    private val BLE_ROW = Regex("""^\[(\d+)]\[RSSI:(-?\d+)] (.*)$""")

    data class BleRow(val index: Int, val rssi: Int, val name: String)

    fun bleListRow(line: String): BleRow? {
        val m = BLE_ROW.matchEntire(line) ?: return null
        return BleRow(m.groupValues[1].toInt(), m.groupValues[2].toInt(), m.groupValues[3])
    }

    private val SETTING_FIELD = Regex("""^(Name|Type|Value): (.*)$""")

    fun settingField(line: String): Pair<String, String>? {
        val m = SETTING_FIELD.matchEntire(line) ?: return null
        return m.groupValues[1] to m.groupValues[2]
    }

    private val GEOFENCE_ROW =
        Regex("""^(\d+): (.*) \| (-?\d+\.\d+), (-?\d+\.\d+) \| (\d+\.\d+) mi$""")

    data class GeofenceRow(val slot: Int, val name: String, val lat: Double, val lon: Double, val radiusMiles: Double)

    fun geofenceListRow(line: String): GeofenceRow? {
        val m = GEOFENCE_ROW.matchEntire(line) ?: return null
        return GeofenceRow(
            slot = m.groupValues[1].toInt(),
            name = m.groupValues[2],
            lat = m.groupValues[3].toDouble(),
            lon = m.groupValues[4].toDouble(),
            radiusMiles = m.groupValues[5].toDouble(),
        )
    }

    data class WardriveRow(val mac: String, val essid: String, val channel: Int, val rssi: Int, val isBle: Boolean)

    fun wardriveRow(line: String): WardriveRow? {
        val t = line.trimEnd()
        val isBle = t.endsWith(",BLE")
        if (!isBle && !t.endsWith(",WIFI")) return null
        val body = if (" | " in t) t.substringAfter(" | ") else t
        val f = body.split(",")
        if (f.size < 11) return null
        val mac = f[0].trim()
        if (mac.isEmpty()) return null
        val rssi = f[5].toIntOrNull() ?: 0
        return if (isBle) WardriveRow(mac, "", 0, rssi, true)
        else WardriveRow(mac, f[1], f[4].toIntOrNull() ?: 0, rssi, false)
    }

    private val FLOCK_MAC = Regex("""^\s*([0-9A-Fa-f]{2}(?::[0-9A-Fa-f]{2}){5})\s*$""")

    fun flockMac(line: String): String? =
        FLOCK_MAC.matchEntire(line)?.groupValues?.get(1)?.uppercase()

    private val META_DEVICE_ROW = Regex("""^Meta Device: (-?\d+) (.+)$""")

    fun metaDeviceRow(line: String): Pair<Int, String>? {
        val m = META_DEVICE_ROW.matchEntire(line) ?: return null
        return m.groupValues[1].toInt() to m.groupValues[2].trim()
    }

    private val AIRTAG_STREAM_ROW =
        Regex("""^(-?\d+) ([0-9A-Fa-f]{2}(?::[0-9A-Fa-f]{2}){5})$""")

    fun airtagStreamRow(line: String): Pair<Int, String>? {
        val m = AIRTAG_STREAM_ROW.matchEntire(line.trim()) ?: return null
        return m.groupValues[1].toInt() to m.groupValues[2].uppercase()
    }

    private val COUNTS = Regex("""^(\d+) selected(?:, (\d+) unselected)?$""")

    data class Counts(val selected: Int, val unselected: Int?)

    fun counts(line: String): Counts? {
        val m = COUNTS.matchEntire(line) ?: return null
        return Counts(m.groupValues[1].toInt(), m.groupValues[2].takeIf { it.isNotEmpty() }?.toInt())
    }

    private val BEACON_ROW = Regex("""^(-?\d+) Ch: (\d+) ([0-9a-fA-F:]{17}) ESSID: (.*)$""")
    private val BEACON_TAIL = Regex("""\s+[0-9A-F]{2} [0-9A-F]{2}\s*$""")

    data class ScanAp(val rssi: Int, val channel: Int, val bssid: String, val essid: String)

    fun beaconScanRow(line: String): ScanAp? {
        val m = BEACON_ROW.matchEntire(line) ?: return null
        val essid = m.groupValues[4].replace(BEACON_TAIL, "").trim()
        return ScanAp(m.groupValues[1].toInt(), m.groupValues[2].toInt(), m.groupValues[3], essid)
    }

    private val PROBE_SCAN_ROW = Regex("""^(-?\d+) Ch: (\d+) Client: ([0-9a-fA-F:]{17}) Requesting: (.*)$""")

    data class ScanProbe(val rssi: Int, val channel: Int, val client: String, val essid: String)

    fun probeScanRow(line: String): ScanProbe? {
        val m = PROBE_SCAN_ROW.matchEntire(line) ?: return null
        return ScanProbe(m.groupValues[1].toInt(), m.groupValues[2].toInt(), m.groupValues[3], m.groupValues[4])
    }

    private val APSTA_ROW = Regex("""^(\d+): (ap|sta): ([0-9a-fA-F:]{17}) -> (ap|sta): ([0-9a-fA-F:]{17})\s*$""")

    data class ApStaLink(val apMac: String, val staMac: String)

    fun apStaRow(line: String): ApStaLink? {
        val m = APSTA_ROW.matchEntire(line) ?: return null
        val firstIsAp = m.groupValues[2] == "ap"
        val first = m.groupValues[3]
        val second = m.groupValues[5]
        return if (firstIsAp) ApStaLink(apMac = first, staMac = second) else ApStaLink(apMac = second, staMac = first)
    }

    private val SRC_DST_ROW = Regex("""^(-?\d+) Ch: (\d+) ([0-9a-fA-F:]{17}) -> ([0-9a-fA-F:]{17}).*$""")

    data class SrcDst(val rssi: Int, val channel: Int, val src: String, val dst: String)

    fun srcDstRow(line: String): SrcDst? {
        val m = SRC_DST_ROW.matchEntire(line) ?: return null
        return SrcDst(m.groupValues[1].toInt(), m.groupValues[2].toInt(), m.groupValues[3], m.groupValues[4])
    }

    private val EAPOL_ROW = Regex("""^Received EAPOL: (.+)$""")

    fun eapolRow(line: String): String? = EAPOL_ROW.matchEntire(line)?.groupValues?.get(1)

    private val MACTRACK_ROW = Regex("""^([0-9a-fA-F:]{17}) Frames: (\d+) Last Seen: (\d+)s$""")

    data class MacTrack(val mac: String, val frames: Long, val lastSeenSecs: Long)

    fun macTrackRow(line: String): MacTrack? {
        val m = MACTRACK_ROW.matchEntire(line) ?: return null
        return MacTrack(m.groupValues[1], m.groupValues[2].toLong(), m.groupValues[3].toLong())
    }

    private val RAW_STAT_ROW =
        Regex("""^\s*(Mgmt|Data|Channel|Beacon|Probe Req|Probe Res|Deauth|EAPOL): (\d+)$""")

    data class RawStat(val label: String, val value: Long)

    fun rawStatRow(line: String): RawStat? {
        val m = RAW_STAT_ROW.matchEntire(line) ?: return null
        return RawStat(m.groupValues[1], m.groupValues[2].toLong())
    }

    private val RAW_RSSI_ROW = Regex("""^\s*RSSI: (-?\d+) - (-?\d+)$""")

    fun rawRssiRow(line: String): Pair<Int, Int>? {
        val m = RAW_RSSI_ROW.matchEntire(line) ?: return null
        return m.groupValues[1].toInt() to m.groupValues[2].toInt()
    }

    private val FOXHUNT_ROW = Regex("""^(\S.*) RSSI: (-?\d+)$""")

    fun foxHuntRssi(line: String): Pair<String, Int>? {
        val m = FOXHUNT_ROW.matchEntire(line) ?: return null
        return m.groupValues[1] to m.groupValues[2].toInt()
    }

    private val BLE_ADVERT_ROW = Regex("""^(-?\d+) Device: (.*)$""")

    fun bleAdvert(line: String): Pair<Int, String>? {
        val m = BLE_ADVERT_ROW.matchEntire(line) ?: return null
        return m.groupValues[1].toInt() to m.groupValues[2]
    }

    data class RemoteId(
        val id: String,
        val channel: Int,
        val rssi: Int,
        val lost: Boolean,
        val aircraftLat: Double,
        val aircraftLon: Double,
        val operatorLat: Double,
        val operatorLon: Double,
    )

    fun remoteId(line: String): RemoteId? {
        if (!line.startsWith("RID,")) return null
        val f = line.substring(4).split(",")
        if (f.size < 25) return null
        return RemoteId(
            id = f[1],
            channel = f[4].toIntOrNull() ?: 0,
            rssi = f[5].toIntOrNull() ?: 0,
            lost = f[8] == "lost",
            aircraftLat = f[13].toDoubleOrNull() ?: 0.0,
            aircraftLon = f[14].toDoubleOrNull() ?: 0.0,
            operatorLat = f[23].toDoubleOrNull() ?: 0.0,
            operatorLon = f[24].toDoubleOrNull() ?: 0.0,
        )
    }

    private val CRED_ROW = Regex("""^u: (.*) p: (.*)$""")

    fun cred(line: String): DeviceMessage.Cred? {
        val m = CRED_ROW.matchEntire(line) ?: return null
        return DeviceMessage.Cred(user = m.groupValues[1], pass = m.groupValues[2])
    }

    private val HARDWARE = Regex("""^Hardware: (.+)$""")

    fun hardwareName(line: String): String? = HARDWARE.matchEntire(line)?.groupValues?.get(1)

    private fun JsonObject.prim(key: String): JsonPrimitive? = this[key] as? JsonPrimitive
    private fun JsonObject.str(key: String): String? = prim(key)?.contentOrNull
    private fun JsonObject.int(key: String): Int = prim(key)?.intOrNull ?: 0
    private fun JsonObject.long(key: String): Long = prim(key)?.longOrNull ?: 0L
    private fun JsonObject.bool(key: String): Boolean = prim(key)?.booleanOrNull ?: false
    private fun JsonObject.strList(key: String): List<String> =
        (this[key] as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull } ?: emptyList()
}
