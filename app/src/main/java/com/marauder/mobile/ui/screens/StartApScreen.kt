package com.marauder.mobile.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.marauder.mobile.ui.components.SectionLabel
import com.marauder.mobile.ui.theme.Danger
import com.marauder.mobile.ui.theme.MarauderTextDim
import com.marauder.mobile.ui.theme.Success
import com.marauder.mobile.vm.MarauderViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartApScreen(vm: MarauderViewModel, onBack: () -> Unit) {
    val state by vm.softApState.collectAsState()
    var ssid by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var encryption by remember { mutableStateOf("WPA2") }
    var granted by remember { mutableStateOf(vm.hasSoftApPermission()) }

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        granted = ok
        if (ok) vm.startSoftAp(ssid, password)
    }

    LaunchedEffect(state.active) { if (state.active) vm.refreshSoftAp() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Start AP") },
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
        Column(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionLabel("Network", modifier = Modifier.padding(top = 8.dp))
            OutlinedTextField(
                value = ssid,
                onValueChange = { ssid = it },
                label = { Text("SSID (network name)") },
                singleLine = true,
                enabled = !state.active,
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password (8–63 chars)") },
                singleLine = true,
                enabled = !state.active,
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            )

            SectionLabel("Encryption")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("WPA2", "WPA3").forEach { opt ->
                    FilterChip(
                        selected = encryption == opt,
                        onClick = { if (!state.active) encryption = opt },
                        label = { Text(opt) },
                    )
                }
            }
            Text(
                "The phone hosts a WPA2/WPA3-Personal Wi-Fi Direct group. Open networks are not supported by Wi-Fi Direct.",
                style = MaterialTheme.typography.labelSmall,
                color = MarauderTextDim,
            )

            if (!state.active) {
                Button(
                    onClick = {
                        if (granted) vm.startSoftAp(ssid, password)
                        else permLauncher.launch(vm.softApPermission())
                    },
                    enabled = ssid.isNotBlank() && password.length in 8..63,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Start AP") }
            } else {
                OutlinedButton(onClick = { vm.stopSoftAp() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Stop AP", color = Danger)
                }
            }

            state.error?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = Danger)
            }

            if (state.active || state.starting) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Success.copy(alpha = 0.10f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            if (state.active) "AP running" else "Starting…",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                        )
                        InfoLine("SSID", state.ssid)
                        InfoLine("Password", state.passphrase)
                        if (state.ownerAddress.isNotBlank()) InfoLine("Gateway", state.ownerAddress)
                        InfoLine("Clients", state.clients.toString())
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MarauderTextDim, modifier = Modifier.fillMaxWidth(0.34f))
        Text(value, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurface)
    }
}
