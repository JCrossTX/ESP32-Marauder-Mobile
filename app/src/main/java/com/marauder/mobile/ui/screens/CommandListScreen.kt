package com.marauder.mobile.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
fun CommandListScreen(
    title: String,
    listType: ListType,
    commandPrefix: String,
    needsPassword: Boolean,
    vm: MarauderViewModel,
    onRun: (command: String) -> Unit,
    onBack: () -> Unit,
) {
    val loading by vm.listLoading.collectAsState()
    var pwFor by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(listType) { vm.refreshList(listType) }

    val rows: List<PickRow> = when (listType) {
        ListType.ACCESS_POINTS -> vm.aps.collectAsState().value.map { PickRow(it.index, it.essid.ifBlank { "(hidden)" }, "#${it.index} · ${it.bssid} · CH ${it.channel}", it.rssi) }
        ListType.AIRTAGS -> vm.airtags.collectAsState().value.map { PickRow(it.index, it.mac, "#${it.index}", it.rssi) }
        ListType.PROBES -> vm.probes.collectAsState().value.map { PickRow(it.index, it.essid.ifBlank { "(broadcast)" }, "#${it.index}", null) }
        ListType.STATIONS -> vm.stations.collectAsState().value.map { PickRow(it.index, it.mac, "AP #${it.ap} · #${it.index}", null) }
        ListType.BLE_DEVICES -> vm.bleDevices.collectAsState().value.map { PickRow(it.index, it.name.ifBlank { "(unnamed)" }, "#${it.index}", it.rssi) }
        ListType.SSIDS -> vm.ssids.collectAsState().value.map { PickRow(it.index, it.essid.ifBlank { "(empty)" }, "#${it.index}", null) }
        ListType.IPS -> vm.ips.collectAsState().value.mapIndexed { i, ip -> PickRow(i, ip.ip, "#$i", null) }
        ListType.PINEAPPLES -> vm.pineapples.collectAsState().value.map { PickRow(it.index, it.label, "#${it.index}", null) }
        ListType.MULTISSID -> vm.multissid.collectAsState().value.map { PickRow(it.index, it.label, "#${it.index}", null) }
        ListType.FLIPPERS -> vm.flippers.collectAsState().value.map { PickRow(it.index, it.label, "#${it.index}", null) }
    }

    fun run(index: Int, password: String?) {
        val cmd = if (needsPassword && password != null) "$commandPrefix$index -p $password" else "$commandPrefix$index"
        onRun(cmd)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    IconButton(onClick = { vm.refreshList(listType) }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = MarauderCyan)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
    ) { inner ->
        Box(Modifier.fillMaxSize().padding(inner)) {
            when {
                rows.isEmpty() && loading == listType ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = MarauderCyan) }
                rows.isEmpty() ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        EmptyState(Icons.Filled.Inbox, "Nothing to pick yet", "Run the matching scan on the device, then refresh.")
                    }
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(rows, key = { it.index }) { r ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier.fillMaxWidth().clickable {
                                if (needsPassword) pwFor = r.index else run(r.index, null)
                            },
                        ) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(r.label, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Spacer(Modifier.size(2.dp))
                                    Text(r.sub, style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace, color = MarauderTextDim)
                                }
                                if (r.rssi != null) Text("${r.rssi} dBm", style = MaterialTheme.typography.labelMedium, color = rssiTint(r.rssi), fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }

    pwFor?.let { index ->
        var password by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { pwFor = null },
            title = { Text("Password") },
            text = {
                OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("WiFi password") }, singleLine = true)
            },
            confirmButton = { TextButton(onClick = { run(index, password); pwFor = null }) { Text("Join") } },
            dismissButton = { TextButton(onClick = { pwFor = null }) { Text("Cancel") } },
        )
    }
}

private data class PickRow(val index: Int, val label: String, val sub: String, val rssi: Int?)
