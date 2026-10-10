package com.marauder.mobile.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import com.marauder.mobile.ui.components.EmptyState
import com.marauder.mobile.ui.theme.MarauderCyan
import com.marauder.mobile.ui.theme.MarauderTextDim
import com.marauder.mobile.vm.MarauderViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IBeaconScreen(vm: MarauderViewModel, onBack: () -> Unit) {
    val devices by vm.bleDevices.collectAsState()
    val phone by vm.bleSeen.collectAsState()
    val beacons = devices.filter { it.name.startsWith("iBeacon") }
    var expanded by remember { mutableStateOf<Int?>(null) }
    var granted by remember { mutableStateOf(vm.hasBlePermission()) }

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        granted = ok
        if (ok) vm.startBleScan()
    }

    DisposableEffect(granted) {
        vm.startIBeaconInfo()
        if (granted) vm.startBleScan()
        onDispose { vm.stopIBeaconInfo(); vm.stopBleScan() }
    }

    val detail = remember(phone) {
        phone.filter { it.type == "iBeacon" && it.major != null && it.minor != null }
            .associateBy { "${it.major}:${it.minor}" }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("iBeacon Info") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
    ) { inner ->
        Column(Modifier.fillMaxSize().padding(inner)) {
            if (!granted) {
                Surface(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "UUID / Tx Power / MAC aren't sent over serial. Grant phone Bluetooth to resolve them.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MarauderTextDim,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { permLauncher.launch(vm.blePermission()) }) { Text("Grant") }
                    }
                }
            }
            if (beacons.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(Icons.Filled.Sensors, "No iBeacons yet", "Sniffing iBeacons on the connected device…")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(beacons, key = { it.index }) { b ->
                        val (major, minor) = majorMinor(b.name)
                        val d = detail["$major:$minor"]
                        IBeaconRow(
                            name = b.name,
                            major = major,
                            minor = minor,
                            rssi = b.rssi,
                            uuid = d?.uuid,
                            txPower = d?.txPower,
                            mac = d?.mac,
                            phoneAvailable = granted,
                            expanded = expanded == b.index,
                            onToggle = { expanded = if (expanded == b.index) null else b.index },
                        )
                    }
                }
            }
        }
    }
}

private fun majorMinor(name: String): Pair<String, String> {
    val body = name.removePrefix("iBeacon").trim()
    val parts = body.split(":")
    return (parts.getOrNull(0)?.trim().orEmpty().ifBlank { "—" }) to
        (parts.getOrNull(1)?.trim().orEmpty().ifBlank { "—" })
}

@Composable
private fun IBeaconRow(
    name: String,
    major: String,
    minor: String,
    rssi: Int,
    uuid: String?,
    txPower: Int?,
    mac: String?,
    phoneAvailable: Boolean,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    val fallback = if (phoneAvailable) "— not seen on phone" else "— enable phone Bluetooth"
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (expanded) MarauderCyan else MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth().clickable { onToggle() },
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text("$rssi dBm", style = MaterialTheme.typography.labelMedium, color = rssiTint(rssi), fontWeight = FontWeight.SemiBold)
            }
            if (expanded) {
                Spacer(Modifier.size(8.dp))
                DetailLine("Major", major)
                DetailLine("Minor", minor)
                DetailLine("RSSI", "$rssi dBm")
                DetailLine("UUID", uuid ?: fallback)
                DetailLine("Tx Power", txPower?.let { "$it dBm" } ?: fallback)
                DetailLine("MAC", mac ?: fallback)
            }
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MarauderTextDim, modifier = Modifier.width(84.dp))
        Text(value, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurface)
    }
}
