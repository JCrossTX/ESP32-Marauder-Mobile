package com.marauder.mobile.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.marauder.mobile.data.ListType
import com.marauder.mobile.ui.components.EmptyState
import com.marauder.mobile.ui.theme.MarauderCyan
import com.marauder.mobile.ui.theme.MarauderTextDim
import com.marauder.mobile.vm.MarauderViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoxHuntTargetListScreen(
    category: String,
    vm: MarauderViewModel,
    onPickDevice: (command: String) -> Unit,
    onPickStream: (category: String, key: String, name: String) -> Unit,
    onPickNative: (mac: String, name: String) -> Unit,
    onBack: () -> Unit,
) {
    val native = category == "meta"
    val stream = category == "flock"
    val btSniff = category in setOf("ble", "findmy", "flipper", "ibeacon")
    var sort by remember { mutableStateOf("Signal") }
    var band by remember { mutableStateOf("All") }

    val aps by vm.aps.collectAsState()
    val stations by vm.stations.collectAsState()
    val ble by vm.bleDevices.collectAsState()
    val airtags by vm.airtags.collectAsState()
    val pineapples by vm.pineapples.collectAsState()
    val multissid by vm.multissid.collectAsState()
    val flippers by vm.flippers.collectAsState()
    val btTargets by vm.btTargets.collectAsState()
    val bleSeen by vm.bleSeen.collectAsState()

    if (native) {
        var granted by remember { mutableStateOf(vm.hasBlePermission()) }
        val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
            granted = ok
            if (ok) vm.startBleScan()
        }
        DisposableEffect(granted) {
            if (granted) vm.startBleScan() else permLauncher.launch(vm.blePermission())
            onDispose { vm.stopBleScan() }
        }
    } else if (btSniff || stream) {
        DisposableEffect(category) {
            vm.startBtDiscovery(category)
            onDispose { vm.stopBtDiscovery() }
        }
    } else {
        LaunchedEffect(category) {
            when (category) {
                "ap" -> vm.refreshList(ListType.ACCESS_POINTS)
                "station" -> vm.refreshList(ListType.STATIONS)
                "pineapple" -> vm.refreshList(ListType.PINEAPPLES)
                "multissid" -> vm.refreshList(ListType.MULTISSID)
            }
        }
    }

    var targets: List<FoxTarget> = when (category) {
        "ap" -> aps.map { FoxTarget(it.essid.ifBlank { "(hidden)" }, "#${it.index} · ${it.bssid}", it.rssi, it.channel) { onPickDevice("foxhunt -w ${it.index}") } }
        "station" -> stations.map { FoxTarget(it.mac, "AP #${it.ap} · sta #${it.index}", -128, null) { onPickDevice("foxhunt -s ${it.ap} ${it.index}") } }
        "ble" -> ble.map { FoxTarget(it.name.ifBlank { "(unnamed)" }, "#${it.index}", it.rssi, null) { onPickDevice("foxhunt -b ${it.index}") } }
        "ibeacon" -> ble.filter { it.name.startsWith("iBeacon") }.map { FoxTarget(it.name, "#${it.index}", it.rssi, null) { onPickDevice("foxhunt -b ${it.index}") } }
        "findmy" -> airtags.map { FoxTarget(it.mac, "#${it.index}", it.rssi, null) { onPickDevice("foxhunt -t ${it.index}") } }
        "flipper" -> flippers.map { FoxTarget(it.label.ifBlank { "Flipper" }, "#${it.index}", -128, null) { onPickDevice("foxhunt -f ${it.index}") } }
        "pineapple" -> pineapples.map { FoxTarget(it.label.ifBlank { "Pineapple" }, "#${it.index}", -128, null) { onPickDevice("foxhunt -p ${it.index}") } }
        "multissid" -> multissid.map { FoxTarget(it.label.ifBlank { "MultiSSID" }, "#${it.index}", -128, null) { onPickDevice("foxhunt -m ${it.index}") } }

        "flock" -> btTargets.map { t -> FoxTarget(t.name.ifBlank { t.key }, t.key, t.rssi, null) { onPickStream("flock", t.key, t.name.ifBlank { t.key }) } }

        else -> bleSeen.filter { it.type == "Meta" }.map { d ->
            FoxTarget(d.name.ifBlank { d.mac }, d.mac, d.rssi, null) { onPickNative(d.mac, d.name.ifBlank { d.mac }) }
        }
    }

    if (band != "All") {
        targets = targets.filter { t ->
            val ch = t.channel ?: return@filter band == "All"
            if (band == "2.4 GHz") ch in 1..14 else ch > 14
        }
    }

    targets = when (sort) {
        "Name" -> targets.sortedBy { it.label.lowercase() }
        "Channel" -> targets.sortedBy { it.channel ?: Int.MAX_VALUE }
        else -> targets.sortedByDescending { it.rssi }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Fox Hunt · ${categoryLabel(category)}") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    IconButton(onClick = {
                        if (native) { vm.stopBleScan(); vm.startBleScan() }
                        else if (btSniff || stream) { vm.stopBtDiscovery(); vm.startBtDiscovery(category) }
                        else when (category) {
                            "ap" -> vm.refreshList(ListType.ACCESS_POINTS)
                            "station" -> vm.refreshList(ListType.STATIONS)
                            "pineapple" -> vm.refreshList(ListType.PINEAPPLES)
                            "multissid" -> vm.refreshList(ListType.MULTISSID)
                        }
                    }) { Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = MarauderCyan) }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
    ) { inner ->
        Column(Modifier.fillMaxSize().padding(inner)) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                listOf("Signal", "Name", "Channel").forEach { s ->
                    FilterChip(selected = sort == s, onClick = { sort = s }, label = { Text("Sort: $s", style = MaterialTheme.typography.labelMedium) })
                }
                listOf("All", "2.4 GHz", "5 GHz").forEach { b ->
                    FilterChip(selected = band == b, onClick = { band = b }, label = { Text(b, style = MaterialTheme.typography.labelMedium) })
                }
            }

            if (targets.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(
                        Icons.Filled.Radar,
                        "No targets yet",
                        when {
                            native -> "Scanning the phone's Bluetooth for Meta devices (the device can't track Meta)…"
                            btSniff || stream -> "Sniffing on the connected device…"
                            else -> "Run the matching scan on the device, then refresh."
                        },
                    )
                }
            } else {
                LazyColumnTargets(targets)
            }
        }
    }
}

@Composable
private fun LazyColumnTargets(targets: List<FoxTarget>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(targets, key = { it.label + it.sub }) { t -> TargetRow(t) }
    }
}

private data class FoxTarget(
    val label: String,
    val sub: String,
    val rssi: Int,
    val channel: Int?,
    val pick: () -> Unit,
)

@Composable
private fun TargetRow(t: FoxTarget) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth().clickable { t.pick() },
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(t.label, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.size(2.dp))
                Text(t.sub, style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace, color = MarauderTextDim)
            }
            if (t.channel != null) {
                Text("CH ${t.channel}", style = MaterialTheme.typography.labelMedium, color = MarauderTextDim)
                Spacer(Modifier.size(10.dp))
            }
            if (t.rssi > -128) Text("${t.rssi} dBm", style = MaterialTheme.typography.labelMedium, color = rssiTint(t.rssi), fontWeight = FontWeight.SemiBold)
        }
    }
}

private fun categoryLabel(category: String): String = when (category) {
    "ap" -> "APs"
    "station" -> "Stations"
    "ble" -> "BLE Devices"
    "findmy" -> "FindMy"
    "flipper" -> "Flipper Zero"
    "ibeacon" -> "iBeacons"
    "meta" -> "Meta"
    "flock" -> "Flock"
    "pineapple" -> "WiFi Pineapples"
    "multissid" -> "MultiSSID"
    else -> category
}
