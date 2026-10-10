package com.marauder.mobile.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.marauder.mobile.MarauderApp
import com.marauder.mobile.capture.CaptureSink
import com.marauder.mobile.data.GpxAssembler
import com.marauder.mobile.data.ListType
import com.marauder.mobile.data.LiveClassifier
import com.marauder.mobile.data.NmeaAssembler
import com.marauder.mobile.data.WardriveAssembler
import com.marauder.mobile.data.WardriveUploader
import com.marauder.mobile.location.LocationRepository
import com.marauder.mobile.protocol.CaptureFrame
import com.marauder.mobile.protocol.DeviceMessage
import com.marauder.mobile.protocol.UpstreamParser
import com.marauder.mobile.usb.UsbSerialManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

enum class LineKind { INPUT, OUTPUT, SYSTEM, ERROR }

data class ConsoleEntry(val id: Long, val text: String, val kind: LineKind)

data class CaptureUiState(
    val active: Boolean = false,
    val path: String? = null,
    val kind: String = "",
    val bytes: Long = 0,
    val rows: Int = 0,
)

data class EvilPortalUiState(
    val phase: Phase = Phase.NeedsHtml,
    val fileName: String? = null,
    val htmlBytes: Int = 0,
    val targetApIndex: Int? = null,
    val message: String? = null,
    val creds: List<DeviceMessage.Cred> = emptyList(),
) {
    enum class Phase { NeedsHtml, Uploading, Ready, Running, Error }
    val htmlReady: Boolean get() = phase == Phase.Ready || phase == Phase.Running
}

private const val KEY_MASK = "********"

private const val PM_HISTORY = 48

data class ApiKeysUiState(
    val wigleName: String = "",
    val wigleKey: String = "",
    val wigleSaved: Boolean = false,
    val wdgKey: String = "",
    val wdgSaved: Boolean = false,
)

data class UploadLine(val text: String, val ok: Boolean)

data class UploadUiState(
    val title: String,
    val percent: Int,
    val running: Boolean,
    val lines: List<UploadLine> = emptyList(),
)

data class FoxHuntUiState(
    val targetName: String = "",
    val rssi: Int = 0,
    val channel: Int? = null,
    val acquired: Boolean = false,
)

data class PacketMonitorUiState(
    val channel: Int = 6,
    val beacon: List<Int> = emptyList(),
    val deauth: List<Int> = emptyList(),
    val probe: List<Int> = emptyList(),
)

data class ChannelSummaryUiState(
    val activity: List<Int> = List(14) { 0 },
    val current: Int = 1,
    val page: Int = 0,
)

data class BluetoothAnalyzerUiState(
    val spikes: List<Int> = emptyList(),
    val topRssi: Int? = null,
    val topName: String = "",
)

data class RidDevice(
    val id: String,
    val rssi: Int,
    val lost: Boolean,
    val aircraftLat: Double,
    val aircraftLon: Double,
    val operatorLat: Double,
    val operatorLon: Double,
)

data class RemoteIdUiState(
    val devices: List<RidDevice> = emptyList(),
    val channel: Int = 0,
    val phoneLat: Double? = null,
    val phoneLon: Double? = null,
)

data class DeviceSetting(val name: String, val type: String, val value: String) {
    val isBool: Boolean get() = type.equals("bool", ignoreCase = true)
    val boolValue: Boolean get() = value.equals("true", ignoreCase = true)
}

data class WardriveUiState(
    val logging: Boolean = false,
    val wifi: Int = 0,
    val bt: Int = 0,
    val flock: Int = 0,
    val sats: Int = 0,
    val file: String = "",
    val sizeKb: Double = 0.0,
    val geofence: String = "CLEAR",
    val poi: Int = 0,
)

data class ReconUiState(
    val mode: String = "W",
    val elapsedSec: Int = 0,
    val ap: Int = 0,
    val station: Int = 0,

    val probe: Int = -1,
    val update: Int = -1,
    val deauth: Int = -1,
    val churnPlus: Int = 0,
    val churnMinus: Int = 0,
    val band2g: Int = 0,
    val band5g: Int = 0,
    val channels: List<Int> = List(14) { 0 },
    val associations: List<String> = emptyList(),

    val bleList: List<DeviceMessage.Ble> = emptyList(),
    val geofence: String = "CLEAR",
)

data class BtStreamTarget(val key: String, val name: String, val rssi: Int)

data class GeofenceSlot(
    val slot: Int,
    val configured: Boolean,
    val name: String = "",
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    val radiusMiles: Double = 0.0,
)

class MarauderViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as MarauderApp
    private val usb: UsbSerialManager = app.usb
    private val settings = app.settings
    private val captureSink: CaptureSink = app.captureSink
    private val location: LocationRepository = app.location
    private val storage = app.storage
    private val apiKeys = app.apiKeys
    private val ble = app.ble
    private val savedWifi = app.savedWifi
    private val softAp = app.softAp

    val status: StateFlow<UsbSerialManager.Status> = usb.status
    val connectedName: StateFlow<String?> = usb.connectedName

    val darkTheme: StateFlow<Boolean> =
        settings.darkTheme.stateIn(viewModelScope, SharingStarted.Eagerly, true)

    fun toggleTheme() {
        viewModelScope.launch { settings.setDarkTheme(!darkTheme.value) }
    }

    private val _info = MutableStateFlow<DeviceMessage.Info?>(null)
    val info: StateFlow<DeviceMessage.Info?> = _info.asStateFlow()

    private val _deviceStatus = MutableStateFlow<DeviceMessage.Status?>(null)
    val deviceStatus: StateFlow<DeviceMessage.Status?> = _deviceStatus.asStateFlow()

    private val _aps = MutableStateFlow<List<DeviceMessage.Ap>>(emptyList())
    val aps: StateFlow<List<DeviceMessage.Ap>> = _aps.asStateFlow()
    private val _stations = MutableStateFlow<List<DeviceMessage.Sta>>(emptyList())
    val stations: StateFlow<List<DeviceMessage.Sta>> = _stations.asStateFlow()
    private val _ssids = MutableStateFlow<List<DeviceMessage.SsidRow>>(emptyList())
    val ssids: StateFlow<List<DeviceMessage.SsidRow>> = _ssids.asStateFlow()
    private val _ips = MutableStateFlow<List<DeviceMessage.Ip>>(emptyList())
    val ips: StateFlow<List<DeviceMessage.Ip>> = _ips.asStateFlow()
    private val _probes = MutableStateFlow<List<DeviceMessage.Probe>>(emptyList())
    val probes: StateFlow<List<DeviceMessage.Probe>> = _probes.asStateFlow()
    private val _airtags = MutableStateFlow<List<DeviceMessage.Airtag>>(emptyList())
    val airtags: StateFlow<List<DeviceMessage.Airtag>> = _airtags.asStateFlow()
    private val _bleDevices = MutableStateFlow<List<DeviceMessage.Ble>>(emptyList())
    val bleDevices: StateFlow<List<DeviceMessage.Ble>> = _bleDevices.asStateFlow()

    private val _pineapples = MutableStateFlow<List<DeviceMessage.FoxTargetItem>>(emptyList())
    val pineapples: StateFlow<List<DeviceMessage.FoxTargetItem>> = _pineapples.asStateFlow()
    private val _multissid = MutableStateFlow<List<DeviceMessage.FoxTargetItem>>(emptyList())
    val multissid: StateFlow<List<DeviceMessage.FoxTargetItem>> = _multissid.asStateFlow()
    private val _flippers = MutableStateFlow<List<DeviceMessage.FoxTargetItem>>(emptyList())
    val flippers: StateFlow<List<DeviceMessage.FoxTargetItem>> = _flippers.asStateFlow()

    private val _listLoading = MutableStateFlow<ListType?>(null)
    val listLoading: StateFlow<ListType?> = _listLoading.asStateFlow()

    private val _console = MutableStateFlow<List<ConsoleEntry>>(emptyList())
    val console: StateFlow<List<ConsoleEntry>> = _console.asStateFlow()

    private val _capture = MutableStateFlow(CaptureUiState())
    val capture: StateFlow<CaptureUiState> = _capture.asStateFlow()

    private val _evilPortal = MutableStateFlow(EvilPortalUiState())
    val evilPortal: StateFlow<EvilPortalUiState> = _evilPortal.asStateFlow()

    private val _foxHunt = MutableStateFlow<FoxHuntUiState?>(null)
    val foxHunt: StateFlow<FoxHuntUiState?> = _foxHunt.asStateFlow()
    private var foxHuntActive = false

    fun startFoxHunt(command: String, channel: Int? = null) {
        foxHuntActive = true
        _foxHunt.value = FoxHuntUiState(channel = channel, acquired = false)
        running = true
        modeLabel = "Fox Hunt"
        recomputeStatus()
        viewModelScope.launch {

            usb.send("stopscan")
            delay(350)
            if (!foxHuntActive) return@launch
            appendConsole("> $command", LineKind.INPUT)
            usb.send(command)
        }
    }

    fun stopFoxHunt() {
        if (!foxHuntActive) return
        foxHuntActive = false
        _foxHunt.value = null
        usb.send("stopscan")
        running = false
        modeLabel = "Idle"
        recomputeStatus()
    }

    private val _packetMonitor = MutableStateFlow(PacketMonitorUiState())
    val packetMonitor: StateFlow<PacketMonitorUiState> = _packetMonitor.asStateFlow()
    private var pmActive = false
    private var pmChannel = 6
    private val pmBlock = HashMap<String, Long>()
    private var pmPrev: Map<String, Long>? = null
    private val pmBeacon = ArrayDeque<Int>()
    private val pmDeauth = ArrayDeque<Int>()
    private val pmProbe = ArrayDeque<Int>()

    fun startPacketMonitor(channel: Int = 6) {
        pmActive = true
        pmChannel = channel.coerceIn(1, 14)
        pmPrev = null
        pmBlock.clear(); pmBeacon.clear(); pmDeauth.clear(); pmProbe.clear()
        _packetMonitor.value = PacketMonitorUiState(channel = pmChannel)
        usb.send("settings -s ChanHop disable")
        usb.send("channel -s $pmChannel")
        usb.send("sniffraw")
        running = true; modeLabel = "Packet Monitor"; recomputeStatus()
    }

    fun stopPacketMonitor() {
        if (!pmActive) return
        pmActive = false
        usb.send("stopscan")
        running = false; modeLabel = "Idle"; recomputeStatus()
    }

    fun packetMonitorChannel(delta: Int) {
        if (!pmActive) return
        pmChannel = (pmChannel + delta).coerceIn(1, 14)
        pmPrev = null
        usb.send("channel -s $pmChannel")
        _packetMonitor.value = _packetMonitor.value.copy(channel = pmChannel)
    }

    private fun pushBounded(dq: ArrayDeque<Int>, v: Int) {
        dq.addLast(v); while (dq.size > PM_HISTORY) dq.removeFirst()
    }

    private val _channelSummary = MutableStateFlow(ChannelSummaryUiState())
    val channelSummary: StateFlow<ChannelSummaryUiState> = _channelSummary.asStateFlow()
    private var csActive = false
    private val csActivity = IntArray(14)
    private var csChannel = 1
    private var csPage = 0
    private var csPrevTotal: Long? = null
    private val csBlock = HashMap<String, Long>()
    private var csJob: Job? = null

    fun startChannelSummary() {
        csActive = true
        csActivity.fill(0); csChannel = 1; csPage = 0; csPrevTotal = null; csBlock.clear()
        _channelSummary.value = ChannelSummaryUiState()
        usb.send("settings -s ChanHop disable")
        usb.send("sniffraw")
        usb.send("channel -s 1")
        running = true; modeLabel = "Channel Summary"; recomputeStatus()
        csJob = viewModelScope.launch {
            while (isActive && csActive) {
                delay(1000)
                if (!csActive) break
                csChannel = if (csChannel >= 14) 1 else csChannel + 1
                usb.send("channel -s $csChannel")
            }
        }
    }

    fun stopChannelSummary() {
        if (!csActive) return
        csActive = false
        csJob?.cancel(); csJob = null
        usb.send("stopscan")
        running = false; modeLabel = "Idle"; recomputeStatus()
    }

    fun channelSummaryPage(delta: Int) {
        csPage = (csPage + delta).coerceIn(0, 1)
        _channelSummary.value = _channelSummary.value.copy(page = csPage)
    }

    private val _remoteId = MutableStateFlow(RemoteIdUiState())
    val remoteId: StateFlow<RemoteIdUiState> = _remoteId.asStateFlow()
    private var ridActive = false
    private val ridDevices = LinkedHashMap<String, RidDevice>()

    fun startRemoteIdRadar() {
        ridActive = true
        ridDevices.clear()
        _remoteId.value = RemoteIdUiState()
        location.start()
        usb.send("remoteid")
        running = true; modeLabel = "Remote ID"; recomputeStatus()
    }

    fun stopRemoteIdRadar() {
        if (!ridActive) return
        ridActive = false
        usb.send("stopscan")
        maybeStopLocation()
        running = false; modeLabel = "Idle"; recomputeStatus()
    }

    private val _btAnalyzer = MutableStateFlow(BluetoothAnalyzerUiState())
    val btAnalyzer: StateFlow<BluetoothAnalyzerUiState> = _btAnalyzer.asStateFlow()
    private var btaActive = false
    private var btaCount = 0
    private var btaTopRssi = -200
    private var btaTopName = ""
    private val btaSpikes = ArrayDeque<Int>()
    private var btaJob: Job? = null

    fun startBluetoothAnalyzer() {
        btaActive = true; btaCount = 0; btaTopRssi = -200; btaTopName = ""; btaSpikes.clear()
        _btAnalyzer.value = BluetoothAnalyzerUiState()
        usb.send("sniffbt")
        running = true; modeLabel = "Bluetooth Analyzer"; recomputeStatus()
        btaJob = viewModelScope.launch {
            while (isActive && btaActive) {
                delay(100)
                btaSpikes.addLast(btaCount)
                while (btaSpikes.size > PM_HISTORY) btaSpikes.removeFirst()
                btaCount = 0
                _btAnalyzer.value = BluetoothAnalyzerUiState(
                    spikes = btaSpikes.toList(),
                    topRssi = if (btaTopRssi > -200) btaTopRssi else null,
                    topName = btaTopName,
                )
            }
        }
    }

    fun stopBluetoothAnalyzer() {
        if (!btaActive) return
        btaActive = false
        btaJob?.cancel(); btaJob = null
        usb.send("stopscan")
        running = false; modeLabel = "Idle"; recomputeStatus()
    }

    private fun finalizeChannelSummaryBlock() {
        if (csBlock.isEmpty()) return
        val ch = (csBlock["Channel"]?.toInt() ?: csChannel).coerceIn(1, 14)
        val total = (csBlock["Mgmt"] ?: 0L) + (csBlock["Data"] ?: 0L)
        val prev = csPrevTotal
        if (prev != null) {
            val delta = (if (total >= prev) total - prev else total).toInt()
            csActivity[ch - 1] = delta
            _channelSummary.value = _channelSummary.value.copy(activity = csActivity.toList(), current = csChannel, page = csPage)
        }
        csPrevTotal = total
        csBlock.clear()
    }

    private fun finalizePacketMonitorBlock() {
        if (pmBlock.isEmpty()) return
        pmBlock["Channel"]?.let { pmChannel = it.toInt() }
        val prev = pmPrev
        if (prev != null) {
            fun delta(label: String): Int {
                val cur = pmBlock[label] ?: 0L
                val p = prev[label] ?: cur
                return (if (cur >= p) cur - p else cur).toInt()
            }
            pushBounded(pmBeacon, delta("Beacon"))
            pushBounded(pmDeauth, delta("Deauth"))
            pushBounded(pmProbe, delta("Probe Req"))
            _packetMonitor.value = PacketMonitorUiState(
                channel = pmChannel,
                beacon = pmBeacon.toList(),
                deauth = pmDeauth.toList(),
                probe = pmProbe.toList(),
            )
        }
        pmPrev = HashMap(pmBlock)
        pmBlock.clear()
    }

    private val _btTargets = MutableStateFlow<List<BtStreamTarget>>(emptyList())
    val btTargets: StateFlow<List<BtStreamTarget>> = _btTargets.asStateFlow()
    private val btTargetMap = LinkedHashMap<String, BtStreamTarget>()

    private var btStreamMode: String? = null

    private var btFoxKey: String? = null

    private var flockRssi: Int? = null
    private var flockMacPending: String? = null
    private var flockNamePending: String = ""

    private var btDiscoveryJob: Job? = null
    private var findMyMonitorActive = false

    val findMyRssi = HashMap<String, Int>()

    fun startBtDiscovery(category: String) {
        resetBtStream()
        btStreamMode = if (category == "flock" || category == "meta") category else null
        val cmd = when (category) {
            "ble" -> "sniffbt"
            "ibeacon" -> "sniffbt -t ibeacon"
            "findmy" -> "sniffbt -t airtag"
            "flipper" -> "sniffbt -t flipper"
            "flock" -> "sniffbt -t flock"
            "meta" -> "sniffbt -t meta"
            else -> null
        }
        val pollType = when (category) {
            "ble", "ibeacon" -> ListType.BLE_DEVICES
            "findmy" -> ListType.AIRTAGS
            "flipper" -> ListType.FLIPPERS
            else -> null
        }
        running = true; modeLabel = "BT Sniff"; recomputeStatus()
        btDiscoveryJob?.cancel()
        btDiscoveryJob = viewModelScope.launch {
            usb.send("stopscan"); delay(350)
            if (!isActive) return@launch
            cmd?.let { appendConsole("> $it", LineKind.INPUT); usb.send(it) }
            if (pollType != null) {
                delay(600)
                while (isActive && usb.status.value == UsbSerialManager.Status.CONNECTED) {
                    refreshList(pollType); delay(1500)
                }
            }
        }
    }

    fun stopBtDiscovery() {
        btDiscoveryJob?.cancel(); btDiscoveryJob = null
        resetBtStream()
        usb.send("stopscan")
        running = false; modeLabel = "Idle"; recomputeStatus()
    }

    fun startIBeaconInfo() {
        _bleDevices.value = emptyList()
        running = true; modeLabel = "iBeacon Info"; recomputeStatus()
        btDiscoveryJob?.cancel()
        btDiscoveryJob = viewModelScope.launch {
            usb.send("stopscan"); delay(350)
            if (!isActive) return@launch
            appendConsole("> sniffbt -t ibeacon", LineKind.INPUT)
            usb.send("sniffbt -t ibeacon")
            delay(600)
            while (isActive && usb.status.value == UsbSerialManager.Status.CONNECTED) {
                refreshList(ListType.BLE_DEVICES); delay(1500)
            }
        }
    }

    fun stopIBeaconInfo() {
        btDiscoveryJob?.cancel(); btDiscoveryJob = null
        usb.send("stopscan")
        running = false; modeLabel = "Idle"; recomputeStatus()
    }

    fun startFindMyMonitor() {
        _airtags.value = emptyList()
        findMyRssi.clear()
        findMyMonitorActive = true
        running = true; modeLabel = "FindMy Monitor"; recomputeStatus()
        btDiscoveryJob?.cancel()
        btDiscoveryJob = viewModelScope.launch {
            usb.send("stopscan"); delay(350)
            if (!isActive) return@launch
            appendConsole("> sniffbt -t airtag", LineKind.INPUT)
            usb.send("sniffbt -t airtag")
            delay(600)
            while (isActive && usb.status.value == UsbSerialManager.Status.CONNECTED) {
                refreshList(ListType.AIRTAGS); delay(1500)
            }
        }
    }

    fun stopFindMyMonitor() {
        findMyMonitorActive = false
        btDiscoveryJob?.cancel(); btDiscoveryJob = null
        usb.send("stopscan")
        running = false; modeLabel = "Idle"; recomputeStatus()
    }

    fun startBtStreamFoxHunt(category: String, key: String, name: String) {
        resetBtStream()
        btStreamMode = category
        btFoxKey = key.uppercase()
        foxHuntActive = false
        _foxHunt.value = FoxHuntUiState(targetName = name, acquired = false)
        running = true; modeLabel = "Fox Hunt"; recomputeStatus()
        viewModelScope.launch {

            usb.send("stopscan")
            delay(350)
            if (btStreamMode != category) return@launch
            val cmd = "sniffbt -t $category"
            appendConsole("> $cmd", LineKind.INPUT)
            usb.send(cmd)
        }
    }

    fun stopBtStreamFoxHunt() {
        resetBtStream()
        _foxHunt.value = null
        usb.send("stopscan")
        running = false; modeLabel = "Idle"; recomputeStatus()
    }

    private fun resetBtStream() {
        btStreamMode = null; btFoxKey = null
        btTargetMap.clear(); _btTargets.value = emptyList()
        flockRssi = null; flockMacPending = null; flockNamePending = ""
    }

    private fun parseBtStreamLine(line: String): Boolean {
        val mode = btStreamMode ?: return false
        if (mode == "meta") {
            val m = UpstreamParser.metaDeviceRow(line) ?: return false
            upsertBtTarget(m.second, m.second, m.first)
            return true
        }

        val t = line.trim()
        when {
            flockRssi == null && t.matches(Regex("^-?\\d{1,3}$")) -> {
                flockRssi = t.toIntOrNull(); return true
            }
            flockRssi != null && flockMacPending == null && UpstreamParser.flockMac(line) != null -> {
                flockMacPending = UpstreamParser.flockMac(line); return true
            }
            flockMacPending != null && t.startsWith("Name:") -> {
                flockNamePending = t.removePrefix("Name:").trim(); return true
            }
            flockMacPending != null && t.startsWith("Serial:") -> {
                val serial = t.removePrefix("Serial:").trim()
                val mac = flockMacPending!!
                val label = flockNamePending.ifBlank {
                    serial.takeIf { it.isNotBlank() && it != "N/A" } ?: mac
                }
                upsertBtTarget(mac, label, flockRssi ?: 0)
                flockRssi = null; flockMacPending = null; flockNamePending = ""
                return true
            }
        }
        return false
    }

    private fun upsertBtTarget(key: String, name: String, rssi: Int) {
        val k = key.uppercase()
        btTargetMap[k] = BtStreamTarget(k, name, rssi)
        _btTargets.value = btTargetMap.values.sortedByDescending { it.rssi }
        if (btFoxKey != null && k == btFoxKey) {
            _foxHunt.value = (_foxHunt.value ?: FoxHuntUiState()).copy(
                targetName = name, rssi = rssi, acquired = true,
            )
        }
    }

    val bleSeen: StateFlow<List<com.marauder.mobile.ble.BleScanner.BleSeen>> get() = ble.devices

    fun blePermission(): String = ble.requiredPermission()
    fun hasBlePermission(): Boolean = ble.hasPermission()
    fun bluetoothOn(): Boolean = ble.isBluetoothOn()

    fun startBleScan() {
        if (!ble.hasPermission()) { _snackbar.tryEmit("Grant the Bluetooth scan permission"); return }
        if (!ble.isBluetoothOn()) { _snackbar.tryEmit("Turn Bluetooth on to scan"); return }
        if (!ble.start()) _snackbar.tryEmit("Could not start the phone BLE scan")
    }

    fun stopBleScan() = ble.stop()

    private var nativeFoxMac: String? = null
    private var nativeFoxJob: Job? = null

    fun startNativeFoxHunt(mac: String, name: String) {
        nativeFoxMac = mac.uppercase()
        foxHuntActive = false
        _foxHunt.value = FoxHuntUiState(targetName = name, acquired = false)
        modeLabel = "Fox Hunt"; running = true; recomputeStatus()
        if (!ble.hasPermission()) { _snackbar.tryEmit("Grant the Bluetooth scan permission"); return }
        if (!ble.isBluetoothOn()) { _snackbar.tryEmit("Turn Bluetooth on to hunt"); return }
        ble.start()
        nativeFoxJob?.cancel()
        nativeFoxJob = viewModelScope.launch {
            ble.devices.collect { list ->
                val d = list.firstOrNull { it.mac.equals(nativeFoxMac, ignoreCase = true) } ?: return@collect
                _foxHunt.value = (_foxHunt.value ?: FoxHuntUiState()).copy(
                    targetName = d.name.ifBlank { name }.ifBlank { d.mac },
                    rssi = d.rssi,
                    acquired = true,
                )
            }
        }
    }

    fun stopNativeFoxHunt() {
        nativeFoxJob?.cancel(); nativeFoxJob = null
        nativeFoxMac = null
        _foxHunt.value = null
        ble.stop()
        running = false; modeLabel = "Idle"; recomputeStatus()
    }

    private val _savedNetworks =
        MutableStateFlow<List<com.marauder.mobile.data.SavedWifiStore.SavedNetwork>>(emptyList())
    val savedNetworks: StateFlow<List<com.marauder.mobile.data.SavedWifiStore.SavedNetwork>> =
        _savedNetworks.asStateFlow()

    fun refreshSavedWifi() { _savedNetworks.value = savedWifi.list() }
    fun addSavedWifi(ssid: String, password: String) {
        if (ssid.isBlank()) { _snackbar.tryEmit("Enter a network name"); return }
        _savedNetworks.value = savedWifi.add(ssid, password)
        _snackbar.tryEmit("Saved \"${ssid.trim()}\"")
    }
    fun removeSavedWifi(ssid: String) { _savedNetworks.value = savedWifi.remove(ssid) }

    val softApState: StateFlow<com.marauder.mobile.wifi.SoftApHost.SoftApState> get() = softAp.state

    fun softApPermission(): String = softAp.requiredPermission()
    fun hasSoftApPermission(): Boolean = softAp.hasPermission()
    fun startSoftAp(ssid: String, password: String) { softAp.start(ssid, password) }
    fun stopSoftAp() = softAp.stop()
    fun refreshSoftAp() = softAp.refresh()

    fun savedHtmlFiles(): List<com.marauder.mobile.data.StorageRepository.StoredFile> =
        storage.files().filter { it.kind == "html" }

    fun selectEpHtml(path: String) {
        val f = File(path)
        val bytes = runCatching { f.readBytes() }.getOrNull()
        if (bytes == null) { _snackbar.tryEmit("Could not read ${f.name}"); return }
        uploadPortalHtml(bytes, f.name)
    }

    private val _deviceSettings = MutableStateFlow<List<DeviceSetting>>(emptyList())
    val deviceSettings: StateFlow<List<DeviceSetting>> = _deviceSettings.asStateFlow()
    private val settingsBuf = ArrayList<DeviceSetting>()
    private var curSettingName: String? = null
    private var curSettingType: String? = null
    private var collectingSettings = false
    private var pendingSettingsTx: String? = null

    fun refreshSettings() {
        if (usb.status.value != UsbSerialManager.Status.CONNECTED) return
        settingsBuf.clear(); curSettingName = null; curSettingType = null
        collectingSettings = true
        val tx = "S${listSeq++}"
        pendingSettingsTx = tx
        usb.send("settings")
        usb.send("protocolinfo --machine $tx")
    }

    fun setDeviceSetting(name: String, enable: Boolean) {
        usb.send("settings -s $name ${if (enable) "enable" else "disable"}")
        refreshSettings()
    }

    fun resetDeviceSettings() { usb.send("settings -r"); refreshSettings() }

    private val _geofences = MutableStateFlow<List<GeofenceSlot>>(emptyList())
    val geofences: StateFlow<List<GeofenceSlot>> = _geofences.asStateFlow()
    private val geofenceRows = ArrayList<UpstreamParser.GeofenceRow>()
    private var collectingGeofences = false
    private var pendingGeofenceTx: String? = null

    fun refreshGeofences() {
        if (usb.status.value != UsbSerialManager.Status.CONNECTED) return
        geofenceRows.clear()
        collectingGeofences = true
        val tx = "G${listSeq++}"
        pendingGeofenceTx = tx
        usb.send("geofence list")
        usb.send("protocolinfo --machine $tx")
    }

    fun setGeofence(slot: Int, lat: Double, lon: Double, radiusMiles: Double, name: String) {
        usb.send("geofence set $slot $lat $lon $radiusMiles \"$name\"")
        refreshGeofences()
    }

    fun clearGeofence(slot: Int) { usb.send("geofence clear $slot"); refreshGeofences() }

    fun currentPhoneFix(): Pair<Double, Double>? {
        location.start()
        val l = location.location.value ?: return null
        return l.lat to l.lon
    }

    private val _wardriveUi = MutableStateFlow(WardriveUiState())
    val wardriveUi: StateFlow<WardriveUiState> = _wardriveUi.asStateFlow()
    private var wardrivePoiCount = 0
    private var wardriveUiJob: Job? = null

    fun startWardriveMode() {
        wardrivePoiCount = 0
        wardriveSeen.clear(); wardriveBtSeen.clear(); wardriveFlockSeen.clear()
        wardriving = true
        wardriveActive = true
        modeLabel = "Wardrive"; running = true
        location.start()
        val path = captureSink.beginFile("wardrive", "log")
        captureSink.appendBytes(WardriveAssembler.header(APP_RELEASE).toByteArray(Charsets.UTF_8))
        _capture.value = CaptureUiState(active = true, path = path, kind = "wardrive")
        refreshGeofences()
        appendConsole("> wardrive (app: scanall + sniffbt + flock, phone GPS)", LineKind.INPUT)
        recomputeStatus()
        pushWardriveUi()
        wardriveUiJob?.cancel()
        wardriveUiJob = viewModelScope.launch {

            while (isActive && wardriveActive) {
                runWardrivePhase("wifi", "scanall", 4000)
                runWardrivePhase("bt", "sniffbt", 4000)
                runWardrivePhase("flock", "sniffbt -t flock", 4000)
            }
        }
    }

    private suspend fun runWardrivePhase(phase: String, command: String, durationMs: Long) {
        if (!wardriveActive) return
        wardrivePhase = phase
        usb.send(command)
        var elapsed = 0L
        while (elapsed < durationMs && wardriveActive) {
            delay(1000); elapsed += 1000
            pushWardriveUi()
        }
        usb.send("stopscan")
        delay(400)
    }

    private fun pushWardriveUi() {
        val cap = _capture.value
        val fix = location.location.value
        _wardriveUi.value = WardriveUiState(
            logging = wardriveActive,
            wifi = wardriveSeen.size,
            bt = wardriveBtSeen.size,
            flock = wardriveFlockSeen.size,
            sats = location.satellites.value,
            file = cap.path?.substringAfterLast('/')?.substringAfterLast('\\').orEmpty(),
            sizeKb = cap.bytes / 1024.0,
            geofence = geofenceLabelFor(fix?.lat, fix?.lon),
            poi = wardrivePoiCount,
        )
    }

    fun stopWardriveMode() {
        wardriveActive = false
        wardriveUiJob?.cancel(); wardriveUiJob = null
        usb.send("stopscan")
        if (wardriving) stopWardrive()
        _wardriveUi.value = _wardriveUi.value.copy(logging = false)

        activeScanHead = null
        running = false; modeLabel = "Idle"
        recomputeStatus()
    }

    fun wardriveMarkPoi() {
        runCommand("wardrivepoi")
        wardrivePoiCount++
        _wardriveUi.value = _wardriveUi.value.copy(poi = wardrivePoiCount)
    }

    private fun geofenceLabelFor(lat: Double?, lon: Double?): String {
        if (lat == null || lon == null) return "CLEAR"
        for (f in _geofences.value) {
            if (!f.configured) continue
            if (haversineMeters(lat, lon, f.lat, f.lon) <= f.radiusMiles * 1609.34)
                return f.name.ifBlank { "Slot ${f.slot}" }
        }
        return "CLEAR"
    }

    private val _recon = MutableStateFlow(ReconUiState())
    val recon: StateFlow<ReconUiState> = _recon.asStateFlow()
    private var reconActive = false
    private var reconJob: Job? = null
    private var reconStartMs = 0L
    private var reconPrevBssids = emptySet<String>()
    private var reconPrevBleCount = 0

    fun startReconLive(isBle: Boolean) {
        reconActive = true
        reconStartMs = System.currentTimeMillis()
        reconPrevBssids = emptySet()
        reconPrevBleCount = 0
        _recon.value = ReconUiState(mode = if (isBle) "B" else "W")
        running = true; modeLabel = "Recon"; recomputeStatus()
        reconJob = viewModelScope.launch {

            usb.send("stopscan")
            delay(500)
            if (!isActive || !reconActive) return@launch
            val cmd = if (isBle) "recon ble" else "recon wifi"
            appendConsole("> $cmd", LineKind.INPUT)
            usb.send(cmd)
            delay(500)
            while (isActive && reconActive) {

                if (isBle) {
                    refreshList(ListType.BLE_DEVICES); awaitListIdle()
                } else {
                    refreshList(ListType.ACCESS_POINTS); awaitListIdle()
                    if (!reconActive) break
                    refreshList(ListType.STATIONS); awaitListIdle()
                }
                updateReconState(isBle)
                delay(1000)
            }
        }
    }

    private suspend fun awaitListIdle(timeoutMs: Int = 3000) {
        var waited = 0
        while (collectingList != null && waited < timeoutMs) { delay(80); waited += 80 }
    }

    fun stopReconLive() {
        if (!reconActive) return
        reconActive = false
        reconJob?.cancel(); reconJob = null

        usb.send("recon stop")
        running = false; modeLabel = "Idle"; recomputeStatus()
    }

    private fun updateReconState(isBle: Boolean) {
        val elapsed = ((System.currentTimeMillis() - reconStartMs) / 1000).toInt()
        val fix = location.location.value
        if (isBle) {
            val devs = _bleDevices.value
            val cur = devs.size
            val plus = (cur - reconPrevBleCount).coerceAtLeast(0)
            val minus = (reconPrevBleCount - cur).coerceAtLeast(0)
            reconPrevBleCount = cur
            _recon.value = _recon.value.copy(
                mode = "B", elapsedSec = elapsed, ap = cur, station = 0,
                churnPlus = plus, churnMinus = minus,
                bleList = devs.sortedByDescending { it.rssi }.take(40),
                geofence = geofenceLabelFor(fix?.lat, fix?.lon),
            )
            return
        }
        val aps = _aps.value
        val channels = IntArray(14)
        var b2 = 0; var b5 = 0
        aps.forEach { ap ->
            val ch = ap.channel
            if (ch in 1..14) { channels[ch - 1]++; b2++ } else if (ch > 14) b5++
        }
        val total = (b2 + b5).coerceAtLeast(1)
        val curBssids = aps.mapNotNull { it.bssid.ifBlank { null } }.toSet()
        val plus = (curBssids - reconPrevBssids).size
        val minus = (reconPrevBssids - curBssids).size
        reconPrevBssids = curBssids
        val apsByIndex = _aps.value.associateBy { it.index }
        val assoc = _stations.value.take(6).map { sta ->
            val ssid = apsByIndex[sta.ap]?.essid?.takeIf { it.isNotBlank() } ?: "(hidden)"
            "${sta.mac} → $ssid"
        }
        _recon.value = _recon.value.copy(
            mode = "W", elapsedSec = elapsed,
            ap = aps.size, station = _stations.value.size,
            churnPlus = plus, churnMinus = minus,
            band2g = b2 * 100 / total, band5g = b5 * 100 / total,
            channels = channels.toList(),
            associations = assoc,
            geofence = geofenceLabelFor(fix?.lat, fix?.lon),
        )
    }

    private fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
            Math.sin(dLon / 2) * Math.sin(dLon / 2)
        return r * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    }

    private val _wigleNameInput = MutableStateFlow("")
    private val _wigleKeyInput = MutableStateFlow("")
    private val _wdgKeyInput = MutableStateFlow("")

    val apiKeysUi: StateFlow<ApiKeysUiState> = combine(
        combine(apiKeys.wigleNameSet, apiKeys.wigleKeySet, apiKeys.wdgKeySet) {
            nameSet, keySet, wdgSet -> Triple(nameSet, keySet, wdgSet)
        },
        combine(_wigleNameInput, _wigleKeyInput, _wdgKeyInput) {
            name, key, wdg -> Triple(name, key, wdg)
        },
    ) { stored, input ->
        val wigleSaved = stored.first && stored.second
        ApiKeysUiState(
            wigleName = if (wigleSaved) KEY_MASK else input.first,
            wigleKey = if (wigleSaved) KEY_MASK else input.second,
            wigleSaved = wigleSaved,
            wdgKey = if (stored.third) KEY_MASK else input.third,
            wdgSaved = stored.third,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ApiKeysUiState())

    fun onWigleNameChange(value: String) { _wigleNameInput.value = value }
    fun onWigleKeyChange(value: String) { _wigleKeyInput.value = value }
    fun onWdgKeyChange(value: String) { _wdgKeyInput.value = value }

    fun saveWigle() {
        val name = _wigleNameInput.value.trim()
        val key = _wigleKeyInput.value.trim()

        if (name.isBlank() || key.isBlank()) {
            _snackbar.tryEmit("Enter both the WiGLE API name and key")
            return
        }
        viewModelScope.launch {
            apiKeys.saveWigle(name, key)
            _wigleNameInput.value = ""
            _wigleKeyInput.value = ""
            _snackbar.tryEmit("WiGLE credentials saved")
        }
    }

    fun clearWigle() {
        viewModelScope.launch {
            apiKeys.clearWigle()
            _wigleNameInput.value = ""
            _wigleKeyInput.value = ""
            _snackbar.tryEmit("WiGLE credentials cleared")
        }
    }

    fun saveWdg() {
        val key = _wdgKeyInput.value.trim()
        if (key.isBlank()) return
        viewModelScope.launch {
            apiKeys.saveWdg(key)
            _wdgKeyInput.value = ""
            _snackbar.tryEmit("WDG Wars API key saved")
        }
    }

    fun clearWdg() {
        viewModelScope.launch {
            apiKeys.clearWdg()
            _wdgKeyInput.value = ""
            _snackbar.tryEmit("WDG Wars API key cleared")
        }
    }

    private val _upload = MutableStateFlow<UploadUiState?>(null)
    val upload: StateFlow<UploadUiState?> = _upload.asStateFlow()

    fun dismissUpload() { _upload.value = null }

    fun wardriveFiles(): List<com.marauder.mobile.data.StorageRepository.StoredFile> =
        storage.files().filter { it.kind == "wardrive" }

    fun uploadWardriveFile(service: String, path: String) {
        viewModelScope.launch { runUpload(service, listOf(File(path))) }
    }

    fun uploadWardrive(service: String) {
        viewModelScope.launch {
            val files = withContext(Dispatchers.IO) { wardriveFiles().map { File(it.path) } }
            runUpload(service, files)
        }
    }

    fun deleteAllWardrive(): Int {
        val files = wardriveFiles()
        files.forEach { storage.delete(it.path) }
        _snackbar.tryEmit("Deleted ${files.size} wardrive log(s)")
        return files.size
    }

    private suspend fun runUpload(service: String, files: List<File>) {
        if (files.isEmpty()) {
            _snackbar.tryEmit("No wardrive log on the phone to upload")
            return
        }
        val lines = mutableListOf<UploadLine>()
        _upload.value = UploadUiState(title = "Uploading", percent = 0, running = true)

        suspend fun one(file: File, which: String): Boolean {
            val label = if (which == "wigle") "WiGLE" else "WDG Wars"
            _upload.value = _upload.value?.copy(title = "${file.name} → $label", percent = 0)
            val result = withContext(Dispatchers.IO) {
                if (which == "wigle") {
                    val name = apiKeys.wigleNamePlain()
                    val key = apiKeys.wigleKeyPlain()
                    if (name.isNullOrBlank() || key.isNullOrBlank())
                        WardriveUploader.Result.Failure("WiGLE: add your API name and key first")
                    else WardriveUploader.uploadWigle(file, name, key) { p ->
                        _upload.value = _upload.value?.copy(percent = p)
                    }
                } else {
                    val key = apiKeys.wdgKeyPlain()
                    if (key.isNullOrBlank())
                        WardriveUploader.Result.Failure("WDG Wars: add your API key first")
                    else WardriveUploader.uploadWdg(file, key) { p ->
                        _upload.value = _upload.value?.copy(percent = p)
                    }
                }
            }
            val ok = result is WardriveUploader.Result.Success
            lines.add(UploadLine("${file.name}: ${result.message}", ok))
            _upload.value = _upload.value?.copy(percent = 100, lines = lines.toList())
            return ok
        }

        for (file in files) {
            if (service == "wigle" || service == "both") one(file, "wigle")
            if (service == "wdg" || service == "both") one(file, "wdg")
        }
        _upload.value = _upload.value?.copy(title = "Upload complete", running = false, percent = 100)
    }

    private val _snackbar = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val snackbar: SharedFlow<String> = _snackbar.asSharedFlow()

    private val portalSignals = MutableSharedFlow<String>(extraBufferCapacity = 16)
    private var portalJob: Job? = null

    private val apBuf = ArrayList<DeviceMessage.Ap>()
    private val staBuf = ArrayList<DeviceMessage.Sta>()
    private val ssidBuf = ArrayList<DeviceMessage.SsidRow>()
    private val ipBuf = ArrayList<DeviceMessage.Ip>()
    private val probeBuf = ArrayList<DeviceMessage.Probe>()
    private val airtagBuf = ArrayList<DeviceMessage.Airtag>()
    private val bleBuf = ArrayList<DeviceMessage.Ble>()
    private val pineappleBuf = ArrayList<DeviceMessage.FoxTargetItem>()
    private val multissidBuf = ArrayList<DeviceMessage.FoxTargetItem>()
    private val flipperBuf = ArrayList<DeviceMessage.FoxTargetItem>()
    private var collectingList: ListType? = null
    private var pendingListTx: String? = null
    private var staCollectAp = -1
    private var listSeq = 0L

    private val bssidByEssid = HashMap<String, UpstreamParser.ScanAp>()

    private var activeScanHead: String? = null
    private var running = false
    private var modeLabel = "Idle"

    private var wardriving = false
    private val wardriveSeen = HashSet<String>()

    private val wardriveBtSeen = HashSet<String>()
    private val wardriveFlockSeen = HashSet<String>()
    private var wardriveActive = false
    private var wardrivePhase = "wifi"

    private var gpsData = false
    private var gpsTracking = false
    private var poiOpen = false
    private var poiOut: OutputStream? = null
    private var poiPath: String? = null
    private var poiCount = 0

    private var consoleSeq = 0L
    private var sessionJob: Job? = null
    private var liveJob: Job? = null

    init {
        viewModelScope.launch { usb.lines.collect(::onLine) }
        viewModelScope.launch {
            usb.captureFrames.collect { frame ->
                if (captureSink.isActive) {
                    captureSink.write(frame)
                    _capture.value = _capture.value.copy(bytes = captureSink.bytes)
                }
            }
        }
        viewModelScope.launch {
            usb.events.collect { msg ->
                appendConsole(msg, LineKind.SYSTEM)
                _snackbar.tryEmit(msg)
            }
        }
        viewModelScope.launch {
            usb.status.collect { st ->
                when (st) {
                    UsbSerialManager.Status.CONNECTED -> startSession()
                    UsbSerialManager.Status.DISCONNECTED -> {
                        sessionJob?.cancel()
                        onDisconnected()
                    }
                    else -> {}
                }
            }
        }
    }

    fun availableDevices(): List<UsbSerialManager.DeviceOption> = usb.availableDevices()
    fun connect(option: UsbSerialManager.DeviceOption) = usb.connect(option)
    fun connectFirst(): Boolean = usb.connectFirst()
    fun disconnect() = usb.disconnect()

    fun runCommand(command: String) {
        val text = command.trim()
        if (text.isEmpty()) return
        appendConsole("> $text", LineKind.INPUT)
        usb.send(text)
    }

    fun stopScan() {
        when {
            wardriveActive -> stopWardriveMode()
            reconActive -> stopReconLive()
            else -> {
                runCommand("stopscan")
                liveJob?.cancel(); liveJob = null
                btDiscoveryJob?.cancel(); btDiscoveryJob = null
                findMyMonitorActive = false
                resetBtStream()
                nativeFoxJob?.cancel(); nativeFoxJob = null; nativeFoxMac = null; ble.stop()
                foxHuntActive = false
                if (wardriving) stopWardrive()
                if (captureSink.isActive && !gpsTracking) endCapture()
                activeScanHead = null
                running = false; modeLabel = "Idle"
                recomputeStatus()
            }
        }
    }

    fun consoleMark(): Long = consoleSeq

    fun refreshList(type: ListType) {
        if (usb.status.value != UsbSerialManager.Status.CONNECTED) return
        bufferFor(type).clear()
        staCollectAp = -1
        collectingList = type
        _listLoading.value = type
        val tx = "L${listSeq++}"
        pendingListTx = tx
        usb.send("list ${flag(type)}")
        usb.send("protocolinfo --machine $tx")
    }

    fun startLiveActivity(command: String, list: ListType?) {
        val trimmed = command.trim()
        val head = trimmed.substringBefore(' ')

        if (head == "wardrive") { startWardrive(trimmed); return }
        when (head) {
            "gpsdata" -> { startGpsConsole(nmea = false); return }
            "nmea" -> { startGpsConsole(nmea = true); return }
            "gpstracker" -> { if (trimmed.contains("stop")) stopTracking() else startTracking(); return }
            "gpspoi" -> { handlePoi(trimmed); return }
        }

        val spec = LiveClassifier.of(trimmed)
        activeScanHead = head
        running = spec.continuous
        modeLabel = commandModeLabel(trimmed)

        val capturing = spec.capture
        if (capturing) {
            val path = captureSink.begin(head, CaptureFrame.TYPE_PCAP)
            _capture.value = CaptureUiState(active = true, path = path, kind = "pcap")
        }

        appendConsole("> $trimmed", LineKind.INPUT)
        usb.send(if (capturing) "$trimmed -serial" else trimmed)

        list?.let {
            bufferFor(it).clear()
            _listLoading.value = it
        }
        recomputeStatus()

        liveJob?.cancel()
        if (list != null) {
            liveJob = viewModelScope.launch {
                delay(600)
                while (isActive && usb.status.value == UsbSerialManager.Status.CONNECTED) {
                    refreshList(list)
                    delay(1500)
                }
            }
        }
    }

    fun stopLiveActivity(continuous: Boolean) {
        liveJob?.cancel()
        liveJob = null
        _listLoading.value = null
        collectingList = null
        if (wardriving) stopWardrive()
        if (gpsTracking) { stopTracking(); return }
        if (gpsData) { gpsData = false; maybeStopLocation() }
        if (captureSink.isActive) endCapture()
        if (continuous && !gpsData) usb.send("stopscan")
        activeScanHead = null
        running = false
        modeLabel = "Idle"
        recomputeStatus()
    }

    fun clearConsole() { _console.value = emptyList() }

    fun storageFiles(): List<com.marauder.mobile.data.StorageRepository.StoredFile> = storage.files()
    fun storageTotalBytes(): Long = storage.totalBytes()
    fun deleteStorageFile(path: String): Boolean = storage.delete(path)
    val storageDir: String get() = storage.root.absolutePath

    fun hasLocationPermission(): Boolean = location.hasPermission()

    fun onLocationPermissionResult(granted: Boolean) {
        if (granted) {
            if (wardriving || gpsData || gpsTracking || poiOpen) location.start()
        } else {
            _snackbar.tryEmit("Location permission denied — GPS features need it")
        }
    }

    private fun startGpsConsole(nmea: Boolean) {
        gpsData = true
        modeLabel = if (nmea) "GPS NMEA (phone)" else "GPS Data (phone)"
        recomputeStatus()
        val ok = location.start()
        appendConsole("> ${if (nmea) "nmea" else "gpsdata"} (phone GPS)", LineKind.INPUT)
        if (!ok) appendConsole("Grant the location permission and turn GPS on to see fixes.", LineKind.SYSTEM)
        liveJob?.cancel()
        liveJob = viewModelScope.launch {
            while (isActive) {
                val l = location.location.value
                when {
                    l == null -> {}
                    nmea -> {
                        appendConsole(NmeaAssembler.gga(l), LineKind.OUTPUT)
                        appendConsole(NmeaAssembler.rmc(l), LineKind.OUTPUT)
                    }
                    else -> appendConsole(
                        "◈ ${"%.6f".format(l.lat)}, ${"%.6f".format(l.lon)} · " +
                            "alt ${"%.1f".format(l.altMeters)} m · ±${"%.0f".format(l.accuracyMeters)} m",
                        LineKind.OUTPUT,
                    )
                }
                delay(1500)
            }
        }
    }

    private fun startTracking() {
        gpsTracking = true
        modeLabel = "GPS Tracker (phone)"
        recomputeStatus()
        location.start()
        val path = captureSink.begin("tracker", CaptureFrame.TYPE_GPX)
        captureSink.appendBytes(GpxAssembler.TRACK_HEADER.toByteArray(Charsets.UTF_8))
        _capture.value = CaptureUiState(active = true, path = path, kind = "gpx")
        appendConsole("> gpstracker (phone GPS)", LineKind.INPUT)
        liveJob?.cancel()
        liveJob = viewModelScope.launch {
            location.location.collectLatest { l ->
                if (l != null && captureSink.isActive) {
                    captureSink.appendBytes(GpxAssembler.trackPoint(l).toByteArray(Charsets.UTF_8))
                    _capture.value = _capture.value.copy(bytes = captureSink.bytes, rows = _capture.value.rows + 1)
                }
            }
        }
    }

    private fun stopTracking() {
        gpsTracking = false
        liveJob?.cancel(); liveJob = null
        if (captureSink.isActive) {
            captureSink.appendBytes(GpxAssembler.TRACK_FOOTER.toByteArray(Charsets.UTF_8))
            endCapture()
        }
        maybeStopLocation()
        appendConsole("Tracker stopped", LineKind.SYSTEM)
        modeLabel = "Idle"; recomputeStatus()
    }

    private fun handlePoi(cmd: String) {
        location.start()
        when {
            cmd.contains("-e") -> closePoi()
            cmd.contains("-m") -> {
                openPoiIfNeeded()
                val l = location.location.value
                if (l == null) appendConsole("No GPS fix yet — POI not marked", LineKind.SYSTEM)
                else {
                    poiCount++

                    runCatching {
                        poiOut?.write(GpxAssembler.waypoint(l, "POI $poiCount").toByteArray(Charsets.UTF_8))
                        poiOut?.flush()
                    }
                    appendConsole("POI #$poiCount marked at ${"%.6f".format(l.lat)}, ${"%.6f".format(l.lon)}", LineKind.SYSTEM)
                }
            }
            else -> { openPoiIfNeeded(); appendConsole("POI session started (phone GPS)", LineKind.INPUT) }
        }
    }

    private fun openPoiIfNeeded() {
        if (poiOut != null) return
        runCatching {
            val dir = File(app.getExternalFilesDir(null), "captures").apply { mkdirs() }
            var i = 0
            var f: File
            do { f = File(dir, "poi_$i.gpx"); i++ } while (f.exists())
            poiOut = BufferedOutputStream(FileOutputStream(f)).also {
                it.write(GpxAssembler.POI_HEADER.toByteArray(Charsets.UTF_8))
            }
            poiPath = f.absolutePath
            poiCount = 0
            poiOpen = true
        }
    }

    private fun closePoi() {
        val out = poiOut ?: run { appendConsole("No POI session open", LineKind.SYSTEM); return }
        runCatching {
            out.write(GpxAssembler.POI_FOOTER.toByteArray(Charsets.UTF_8))
            out.flush(); out.close()
        }
        appendConsole("POI saved ($poiCount points): $poiPath", LineKind.SYSTEM)
        _snackbar.tryEmit("POI saved: $poiPath")
        poiOut = null; poiPath = null; poiOpen = false
        maybeStopLocation()
    }

    private fun maybeStopLocation() {
        if (!wardriving && !gpsData && !gpsTracking && !poiOpen) location.stop()
    }

    private fun endCapture() {
        val summary = captureSink.end()
        _capture.value = _capture.value.copy(
            active = false,
            path = summary?.path ?: _capture.value.path,
            bytes = summary?.bytes ?: _capture.value.bytes,
        )
        summary?.let { _snackbar.tryEmit("Capture saved: ${it.bytes} B → ${it.path}") }
    }

    private fun startWardrive(command: String) {
        wardriving = true
        wardriveSeen.clear()
        modeLabel = "Wardrive (phone GPS)"
        running = true
        val located = location.start()
        val path = captureSink.beginFile("wardrive", "log")
        captureSink.appendBytes(WardriveAssembler.header(APP_RELEASE).toByteArray(Charsets.UTF_8))
        _capture.value = CaptureUiState(active = true, path = path, kind = "wardrive")
        if (!located) _snackbar.tryEmit("Location permission/GPS off — rows will log without a fix")

        appendConsole("> $command (phone GPS → scanall)", LineKind.INPUT)
        activeScanHead = "scanall"
        usb.send("scanall")
        recomputeStatus()
        liveJob?.cancel()
    }

    private fun stopWardrive() {
        wardriving = false
        if (captureSink.isActive) endCapture()
        maybeStopLocation()
    }

    private fun onWardriveAp(ap: UpstreamParser.ScanAp) {
        if (ap.bssid.isBlank() || !wardriveSeen.add(ap.bssid)) return
        val row = DeviceMessage.Ap(
            index = -1, channel = ap.channel, rssi = ap.rssi, selected = false,
            packets = 0, sec = -1, wps = false, stations = 0, bssid = ap.bssid, essid = ap.essid,
        )
        captureSink.appendBytes(WardriveAssembler.line(row, location.location.value).toByteArray(Charsets.UTF_8))
        _capture.value = _capture.value.copy(bytes = captureSink.bytes, rows = wardriveSeen.size)
    }

    fun resetEvilPortal() {
        portalJob?.cancel()
        _evilPortal.value = EvilPortalUiState()
    }

    fun setPortalTargetAp(index: Int?) {
        _evilPortal.value = _evilPortal.value.copy(targetApIndex = index)
    }

    fun uploadPortalHtml(bytes: ByteArray, fileName: String) {
        if (bytes.isEmpty()) { _snackbar.tryEmit("Empty HTML file"); return }
        if (usb.status.value != UsbSerialManager.Status.CONNECTED) { _snackbar.tryEmit("Not connected"); return }
        portalJob?.cancel()
        portalJob = viewModelScope.launch {
            _evilPortal.value = _evilPortal.value.copy(
                phase = EvilPortalUiState.Phase.Uploading,
                fileName = fileName,
                htmlBytes = bytes.size,
                message = "Uploading ${bytes.size} B…",
            )
            var ready = false
            var done = false

            usb.withWriteLock {
                coroutineScope {
                    val readyWait = async { withTimeoutOrNull(3000) { portalSignals.first { it.startsWith("Setting HTML") } } }
                    appendConsole("> evilportal -c sethtmlstr", LineKind.INPUT)
                    usb.sendLineLocked("evilportal -c sethtmlstr")
                    ready = readyWait.await() != null
                    appendConsole(
                        if (ready) "portal ▸ device ready for page" else "portal ▸ no 'ready' in 3 s (V8 boards suppress it)",
                        LineKind.SYSTEM,
                    )
                    val doneWait = async { withTimeoutOrNull(9000) { portalSignals.first { it == "html set" } } }
                    appendConsole("portal ▸ streaming ${bytes.size} B…", LineKind.SYSTEM)
                    usb.sendRawLocked(bytes)
                    done = doneWait.await() != null
                    appendConsole(
                        if (done) "portal ▸ html set" else "portal ▸ no 'html set' confirmation",
                        LineKind.SYSTEM,
                    )
                }
            }
            _evilPortal.value = if (done) {
                _evilPortal.value.copy(phase = EvilPortalUiState.Phase.Ready, message = "Page set on device (${bytes.size} B)")
            } else {
                _evilPortal.value.copy(
                    phase = EvilPortalUiState.Phase.Error,
                    message = "Device did not confirm the page. Keep it small (RAM-held) and retry.",
                )
            }
        }
    }

    fun startEvilPortal() {
        val st = _evilPortal.value
        if (!st.htmlReady) { _snackbar.tryEmit("Upload an HTML page first"); return }
        st.targetApIndex?.let { runCommand("evilportal -c setap $it") }
        runCommand("evilportal -c start")
        activeScanHead = "evilportal"
        running = true
        modeLabel = "Evil Portal"
        recomputeStatus()
        _evilPortal.value = st.copy(phase = EvilPortalUiState.Phase.Running, creds = emptyList())
    }

    fun stopEvilPortal() {
        runCommand("stopscan")
        activeScanHead = null; running = false; modeLabel = "Idle"; recomputeStatus()
        if (_evilPortal.value.phase == EvilPortalUiState.Phase.Running) {
            _evilPortal.value = _evilPortal.value.copy(phase = EvilPortalUiState.Phase.Ready)
        }
    }

    private fun onLine(raw: String) {
        val line = UpstreamParser.stripPrompt(raw.trimEnd()).let { if (it.isBlank()) return else it }

        UpstreamParser.machine(line)?.let { handleMachine(it); return }

        if (UpstreamParser.echoedCommand(line) != null) return

        if (foxHuntActive) {
            UpstreamParser.foxHuntRssi(line)?.let { (name, rssi) ->
                _foxHunt.value = (_foxHunt.value ?: FoxHuntUiState()).copy(
                    targetName = name, rssi = rssi, acquired = true,
                )
                return
            }
        }

        if (pmActive) {
            UpstreamParser.rawStatRow(line)?.let { pmBlock[it.label] = it.value; return }
            if (UpstreamParser.rawRssiRow(line) != null) { finalizePacketMonitorBlock(); return }
        }

        if (csActive) {
            UpstreamParser.rawStatRow(line)?.let { csBlock[it.label] = it.value; return }
            if (UpstreamParser.rawRssiRow(line) != null) { finalizeChannelSummaryBlock(); return }
        }

        if (btaActive) {
            UpstreamParser.bleAdvert(line)?.let { (rssi, name) ->
                btaCount++
                if (rssi > btaTopRssi) { btaTopRssi = rssi; btaTopName = name }
                return
            }
        }

        if (findMyMonitorActive) {
            UpstreamParser.airtagStreamRow(line)?.let { (rssi, mac) ->
                findMyRssi[mac.uppercase()] = rssi
            }
        }

        if (btStreamMode != null) parseBtStreamLine(line)

        if (ridActive) {
            UpstreamParser.remoteId(line)?.let { r ->
                ridDevices[r.id] = RidDevice(r.id, r.rssi, r.lost, r.aircraftLat, r.aircraftLon, r.operatorLat, r.operatorLon)
                val loc = location.location.value
                _remoteId.value = RemoteIdUiState(
                    devices = ridDevices.values.toList(),
                    channel = r.channel,
                    phoneLat = loc?.lat,
                    phoneLon = loc?.lon,
                )
                return
            }
        }

        if (wardriveActive) {
            when (wardrivePhase) {
                "bt" -> UpstreamParser.bleAdvert(line)?.let { (rssi, name) ->
                    val key = name.ifBlank { "?" }
                    if (wardriveBtSeen.add(key)) {
                        captureSink.appendBytes(WardriveAssembler.bleLine(key, rssi, location.location.value).toByteArray(Charsets.UTF_8))
                        _capture.value = _capture.value.copy(bytes = captureSink.bytes)
                        pushWardriveUi()
                    }
                    return
                }
                "flock" -> UpstreamParser.flockMac(line)?.let { mac ->
                    if (wardriveFlockSeen.add(mac)) {
                        captureSink.appendBytes(WardriveAssembler.bleLine(mac, 0, location.location.value).toByteArray(Charsets.UTF_8))
                        _capture.value = _capture.value.copy(bytes = captureSink.bytes)
                        pushWardriveUi()
                    }
                    return
                }
            }
        }

        if (collectingSettings) {
            UpstreamParser.settingField(line)?.let { (k, v) ->
                when (k) {
                    "Name" -> curSettingName = v
                    "Type" -> curSettingType = v
                    "Value" -> {
                        val n = curSettingName; val t = curSettingType
                        if (n != null && t != null) settingsBuf.add(DeviceSetting(n, t, v))
                        curSettingName = null; curSettingType = null
                    }
                }
                return
            }
            if (line == "Settings" || line.startsWith("----")) return
        }

        if (collectingGeofences) {
            UpstreamParser.geofenceListRow(line)?.let { geofenceRows.add(it); return }
            if (line == "No geofences configured") return
        }

        if (line.startsWith("Setting HTML from serial")) { portalSignals.tryEmit("Setting HTML from serial"); return }
        if (line == "html set") { portalSignals.tryEmit("html set"); appendConsole(line, LineKind.SYSTEM); return }
        UpstreamParser.cred(line)?.let { cred ->
            _evilPortal.value = _evilPortal.value.copy(creds = _evilPortal.value.creds + cred)
            appendConsole("⚑ credential — ${cred.user} : ${cred.pass}", LineKind.OUTPUT)
            return
        }

        UpstreamParser.hardwareName(line)?.let { hw ->
            _info.value = (_info.value ?: DeviceMessage.Info("", 0, "", emptyList())).copy(board = hw)
        }

        if (collectingList != null && routeListRow(line)) return

        routeScanRow(line)

        appendConsole(line, LineKind.OUTPUT)
    }

    private fun routeListRow(line: String): Boolean {
        when (collectingList) {
            ListType.ACCESS_POINTS -> UpstreamParser.apListRow(line)?.let { ap ->
                val enriched = bssidByEssid[ap.essid]?.let { ap.copy(bssid = it.bssid) } ?: ap
                apBuf.add(enriched); return true
            }
            ListType.SSIDS -> UpstreamParser.ssidListRow(line)?.let { ssidBuf.add(it); return true }
            ListType.STATIONS -> {
                UpstreamParser.staListHeaderApIndex(line)?.let { staCollectAp = it; return true }
                UpstreamParser.staListRow(line, staCollectAp)?.let { staBuf.add(it); return true }
            }
            ListType.IPS -> UpstreamParser.ipListRow(line)?.let { ipBuf.add(it); return true }
            ListType.PROBES -> UpstreamParser.probeListRow(line)?.let { probeBuf.add(it); return true }
            ListType.AIRTAGS -> UpstreamParser.airtagListRow(line)?.let { airtagBuf.add(it); return true }
            ListType.BLE_DEVICES -> UpstreamParser.bleListRow(line)?.let {
                bleBuf.add(DeviceMessage.Ble(it.index, it.rssi, it.name)); return true
            }
            ListType.PINEAPPLES -> UpstreamParser.foxLabelListRow(line)?.let { pineappleBuf.add(it); return true }
            ListType.MULTISSID -> UpstreamParser.foxLabelListRow(line)?.let { multissidBuf.add(it); return true }
            ListType.FLIPPERS -> UpstreamParser.flipperListRow(line)?.let { flipperBuf.add(it); return true }
            null -> {}
        }

        return UpstreamParser.counts(line) != null
    }

    private fun routeScanRow(line: String) {
        UpstreamParser.beaconScanRow(line)?.let { ap ->
            if (ap.essid.isNotEmpty()) bssidByEssid[ap.essid] = ap
            if (wardriving) onWardriveAp(ap)
        }
    }

    private fun handleMachine(m: UpstreamParser.Machine) {
        when {

            m.command == "protocolinfo" && m.tx == pendingListTx -> {
                pendingListTx = null
                commitList(collectingList)
                collectingList = null
            }

            m.command == "protocolinfo" && m.tx == pendingSettingsTx -> {
                pendingSettingsTx = null
                collectingSettings = false
                _deviceSettings.value = settingsBuf.toList()
            }

            m.command == "protocolinfo" && m.tx == pendingGeofenceTx -> {
                pendingGeofenceTx = null
                collectingGeofences = false
                val bySlot = geofenceRows.associateBy { it.slot }
                _geofences.value = (1..5).map { s ->
                    val r = bySlot[s]
                    if (r != null) GeofenceSlot(s, true, r.name, r.lat, r.lon, r.radiusMiles)
                    else GeofenceSlot(s, false)
                }
            }

            m.command == "protocolinfo" && m.firmware != null -> {
                val caps = m.capabilities.toMutableList()
                _info.value = (_info.value ?: DeviceMessage.Info("", 0, "", emptyList())).copy(
                    fw = m.firmware,
                    proto = m.protocol,
                    caps = caps,
                )
                appendConsole("◈ fw ${m.firmware} · protocol ${m.protocol}", LineKind.SYSTEM)
            }

            else -> appendConsole(
                "⚙ ${m.command}: ${m.status} (${m.code})" +
                    if (m.files > 0 || m.bytes > 0) " · ${m.files} files, ${m.bytes} B" else "",
                LineKind.SYSTEM,
            )
        }
    }

    private fun commitList(type: ListType?) {
        when (type) {
            ListType.ACCESS_POINTS -> _aps.value = apBuf.toList()
            ListType.SSIDS -> _ssids.value = ssidBuf.toList()
            ListType.STATIONS -> _stations.value = staBuf.toList()
            ListType.IPS -> _ips.value = ipBuf.toList()
            ListType.PROBES -> _probes.value = probeBuf.toList()
            ListType.AIRTAGS -> _airtags.value = airtagBuf.toList()
            ListType.BLE_DEVICES -> _bleDevices.value = bleBuf.toList()
            ListType.PINEAPPLES -> _pineapples.value = pineappleBuf.toList()
            ListType.MULTISSID -> _multissid.value = multissidBuf.toList()
            ListType.FLIPPERS -> _flippers.value = flipperBuf.toList()
            null -> {}
        }
        _listLoading.value = null
        recomputeStatus()
    }

    private fun recomputeStatus() {
        if (usb.status.value != UsbSerialManager.Status.CONNECTED) { _deviceStatus.value = null; return }
        _deviceStatus.value = DeviceMessage.Status(
            modeLabel = modeLabel,
            running = running,
            free = -1,
            aps = _aps.value.size,
            stations = _stations.value.size,
            ssids = _ssids.value.size,
            ips = _ips.value.size,
            probes = _probes.value.size,
            airtags = _airtags.value.size,
        )
    }

    private var statusJob: Job? = null
    private val statusListTypes = listOf(
        ListType.ACCESS_POINTS, ListType.STATIONS, ListType.SSIDS,
        ListType.IPS, ListType.PROBES, ListType.AIRTAGS,
    )

    fun startStatusPolling() {
        if (statusJob?.isActive == true) return
        statusJob = viewModelScope.launch {
            delay(1500)
            while (isActive && usb.status.value == UsbSerialManager.Status.CONNECTED) {

                if (!running && collectingList == null && !reconActive && !wardriveActive &&
                    !pmActive && !csActive && !btaActive && !ridActive && !foxHuntActive &&
                    btStreamMode == null && !collectingSettings && !collectingGeofences
                ) {
                    for (type in statusListTypes) {
                        if (!isActive || running || usb.status.value != UsbSerialManager.Status.CONNECTED) break
                        refreshList(type)

                        var waited = 0
                        while (isActive && collectingList != null && waited < 2500) { delay(100); waited += 100 }
                    }
                }
                delay(4000)
            }
        }
    }

    fun stopStatusPolling() { statusJob?.cancel(); statusJob = null }

    private fun startSession() {
        sessionJob?.cancel()
        sessionJob = viewModelScope.launch {

            modeLabel = "Idle"; running = false; recomputeStatus()
            delay(400)
            usb.send("info")
            delay(400)
            usb.send("protocolinfo --machine H0")
            delay(1500)
            if (!isActive) return@launch
            if (_info.value?.fw.isNullOrEmpty()) {
                usb.send("protocolinfo --machine H1")
            }
        }
    }

    private fun onDisconnected() {
        liveJob?.cancel(); liveJob = null
        _info.value = null
        _deviceStatus.value = null
        _aps.value = emptyList(); _stations.value = emptyList(); _ssids.value = emptyList()
        _ips.value = emptyList(); _probes.value = emptyList(); _airtags.value = emptyList()
        _bleDevices.value = emptyList()
        _pineapples.value = emptyList(); _multissid.value = emptyList(); _flippers.value = emptyList()
        _deviceSettings.value = emptyList(); _geofences.value = emptyList()
        collectingSettings = false; pendingSettingsTx = null
        collectingGeofences = false; pendingGeofenceTx = null
        reconActive = false; reconJob?.cancel(); reconJob = null; _recon.value = ReconUiState()
        statusJob?.cancel(); statusJob = null
        btDiscoveryJob?.cancel(); btDiscoveryJob = null
        findMyMonitorActive = false; findMyRssi.clear()
        resetBtStream()
        nativeFoxJob?.cancel(); nativeFoxJob = null; nativeFoxMac = null
        ble.stop()
        _listLoading.value = null
        collectingList = null; pendingListTx = null
        bssidByEssid.clear()
        if (captureSink.isActive && !gpsTracking) captureSink.end()
        wardriving = false
        wardriveActive = false; wardriveUiJob?.cancel(); wardriveUiJob = null
        wardriveBtSeen.clear(); wardriveFlockSeen.clear()
        maybeStopLocation()
        activeScanHead = null; running = false; modeLabel = "Idle"
        if (!gpsTracking) _capture.value = CaptureUiState()
    }

    private fun bufferFor(type: ListType): ArrayList<*> = when (type) {
        ListType.ACCESS_POINTS -> apBuf
        ListType.SSIDS -> ssidBuf
        ListType.STATIONS -> staBuf
        ListType.IPS -> ipBuf
        ListType.PROBES -> probeBuf
        ListType.AIRTAGS -> airtagBuf
        ListType.BLE_DEVICES -> bleBuf
        ListType.PINEAPPLES -> pineappleBuf
        ListType.MULTISSID -> multissidBuf
        ListType.FLIPPERS -> flipperBuf
    }

    private fun flag(type: ListType): String = when (type) {
        ListType.ACCESS_POINTS -> "-a"
        ListType.SSIDS -> "-s"
        ListType.STATIONS -> "-c"
        ListType.IPS -> "-i"
        ListType.PROBES -> "-p"
        ListType.AIRTAGS -> "-t"
        ListType.BLE_DEVICES -> "-b"
        ListType.PINEAPPLES -> "-x"
        ListType.MULTISSID -> "-m"
        ListType.FLIPPERS -> "-f"
    }

    private fun appendConsole(text: String, kind: LineKind) {
        val entry = ConsoleEntry(consoleSeq++, text, kind)
        _console.value = (_console.value + entry).takeLast(MAX_CONSOLE)
    }

    companion object {
        private const val MAX_CONSOLE = 800
        private const val APP_RELEASE = "marauder-mobile"

        fun commandModeLabel(command: String): String {
            val head = command.trim().substringBefore(' ')
            return when (head) {
                "sniffbeacon" -> "Beacon Sniff"
                "sniffprobe" -> "Probe Sniff"
                "sniffdeauth" -> "Deauth Sniff"
                "sniffpwn" -> "Pwnagotchi Sniff"
                "sniffpinescan" -> "Pineapple Sniff"
                "sniffmultissid" -> "MultiSSID Sniff"
                "sniffpmkid" -> "EAPOL/PMKID Sniff"
                "sniffsae" -> "SAE Sniff"
                "sniffraw" -> "Raw Capture"
                "scanall" -> "Scan AP/STA"
                "packetcount" -> "Packet Count"
                "mactrack" -> "MAC Tracker"
                "foxhunt" -> "Fox Hunt"
                "pingscan" -> "Ping Scan"
                "arpscan" -> "ARP Scan"
                "portscan" -> "Port Scan"
                "remoteid" -> "Remote ID"
                "sniffbt" -> "Bluetooth Sniff"
                "sniffskim" -> "Skimmer Scan"
                "blespam" -> "BLE Spam"
                "spoofat" -> "Spoof AirTag"
                "attack" -> "WiFi Attack"
                "karma" -> "Karma"
                "evilportal" -> "Evil Portal"
                else -> head.ifEmpty { "Idle" }
            }
        }
    }
}
