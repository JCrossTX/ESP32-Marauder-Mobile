package com.marauder.mobile.ble

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BleScanner(private val context: Context) {

    data class BleSeen(
        val mac: String,
        val name: String,
        val rssi: Int,
        val type: String,
        val uuid: String? = null,
        val major: Int? = null,
        val minor: Int? = null,
        val txPower: Int? = null,
    )

    private val _devices = MutableStateFlow<List<BleSeen>>(emptyList())
    val devices: StateFlow<List<BleSeen>> = _devices.asStateFlow()

    private val seen = LinkedHashMap<String, BleSeen>()
    private var scanning = false

    private val manager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager

    fun requiredPermission(): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Manifest.permission.BLUETOOTH_SCAN
        else Manifest.permission.ACCESS_FINE_LOCATION

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, requiredPermission()) == PackageManager.PERMISSION_GRANTED

    fun isBluetoothOn(): Boolean = manager?.adapter?.isEnabled == true

    @SuppressLint("MissingPermission")
    fun start(): Boolean {
        if (scanning) return true
        if (!hasPermission() || !isBluetoothOn()) return false
        val scanner = manager?.adapter?.bluetoothLeScanner ?: return false
        seen.clear(); _devices.value = emptyList()
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        return try {
            scanner.startScan(null, settings, callback)
            scanning = true
            true
        } catch (_: SecurityException) {
            false
        } catch (_: IllegalStateException) {
            false
        }
    }

    @SuppressLint("MissingPermission")
    fun stop() {
        if (!scanning) return
        scanning = false
        try {
            manager?.adapter?.bluetoothLeScanner?.stopScan(callback)
        } catch (_: SecurityException) {
        } catch (_: IllegalStateException) {
        }
    }

    private val callback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) = handle(result)
        override fun onBatchScanResults(results: MutableList<ScanResult>) { results.forEach(::handle) }
    }

    @SuppressLint("MissingPermission")
    private fun handle(result: ScanResult) {
        val mac = result.device?.address ?: return
        val record = result.scanRecord
        val name = (record?.deviceName ?: "").trim()
        val rssi = result.rssi

        val ib = record?.let { parseIBeacon(it) }
        val seenEntry = when {
            ib != null -> BleSeen(mac, name.ifBlank { "iBeacon ${ib.major}:${ib.minor}" }, rssi, "iBeacon", ib.uuid, ib.major, ib.minor, ib.txPower)
            record != null && isMeta(record) -> BleSeen(mac, name, rssi, "Meta")
            else -> BleSeen(mac, name, rssi, "BLE")
        }
        seen[mac] = seenEntry
        _devices.value = seen.values.toList()
    }

    private data class Ib(val uuid: String, val major: Int, val minor: Int, val txPower: Int)

    private fun parseIBeacon(record: android.bluetooth.le.ScanRecord): Ib? {

        val apple = record.getManufacturerSpecificData(APPLE_COMPANY_ID) ?: return null
        if (apple.size < 23 || apple[0] != 0x02.toByte() || apple[1] != 0x15.toByte()) return null
        val uuidBytes = apple.copyOfRange(2, 18)
        val uuid = buildString {
            uuidBytes.forEachIndexed { i, b ->
                append("%02x".format(b.toInt() and 0xFF))
                if (i == 3 || i == 5 || i == 7 || i == 9) append('-')
            }
        }
        val major = ((apple[18].toInt() and 0xFF) shl 8) or (apple[19].toInt() and 0xFF)
        val minor = ((apple[20].toInt() and 0xFF) shl 8) or (apple[21].toInt() and 0xFF)
        val txPower = apple[22].toInt()
        return Ib(uuid, major, minor, txPower)
    }

    private fun isMeta(record: android.bluetooth.le.ScanRecord): Boolean {
        var meta = false
        var blocked = false
        val mfg = record.manufacturerSpecificData
        if (mfg != null) {
            for (i in 0 until mfg.size()) {
                val id = mfg.keyAt(i) and 0xFFFF
                if (id in META_IDENTIFIERS) meta = true
                if (id in BLOCKED_IDENTIFIERS) blocked = true
            }
        }
        record.serviceUuids?.forEach { parcel ->
            val id = ((parcel.uuid.mostSignificantBits ushr 32) and 0xFFFF).toInt()
            if (id in META_IDENTIFIERS) meta = true
            if (id in BLOCKED_IDENTIFIERS) blocked = true
        }
        return meta && !blocked
    }

    companion object {
        private const val APPLE_COMPANY_ID = 0x004C
        private val META_IDENTIFIERS = setOf(0xFD5F, 0xFEB7, 0xFEB8, 0x01AB, 0x058E, 0x0D53)
        private val BLOCKED_IDENTIFIERS = setOf(0xFD5A, 0xFD69, 0x004C, 0x0006, 0xFEF3)
    }
}
