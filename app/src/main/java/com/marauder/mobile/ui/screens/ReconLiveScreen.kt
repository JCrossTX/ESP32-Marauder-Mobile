package com.marauder.mobile.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marauder.mobile.ui.theme.CatDevice
import com.marauder.mobile.ui.theme.CatGeneral
import com.marauder.mobile.ui.theme.Danger
import com.marauder.mobile.ui.theme.MarauderCyan
import com.marauder.mobile.ui.theme.MarauderText
import com.marauder.mobile.ui.theme.MarauderTextDim
import com.marauder.mobile.ui.theme.Success
import com.marauder.mobile.vm.MarauderViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReconLiveScreen(ble: Boolean, vm: MarauderViewModel, onBack: () -> Unit) {
    val s by vm.recon.collectAsState()

    DisposableEffect(ble) {
        vm.startReconLive(ble)
        onDispose { vm.stopReconLive() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (ble) "Recon BLE" else "Recon WiFi", color = MarauderCyan) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MarauderCyan,
                ),
            )
        },
    ) { inner ->
        Column(
            Modifier.fillMaxSize().padding(inner).padding(horizontal = 12.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            val mm = s.elapsedSec / 60
            val ss = s.elapsedSec % 60

            Box(
                Modifier.fillMaxWidth()
                    .border(0.5.dp, CatGeneral.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 7.dp),
            ) {
                Row {
                    val strip = if (ble) {
                        "REC B %d:%02d DEV%d U%d ".format(mm, ss, s.ap, s.churnPlus)
                    } else {
                        "REC W %d:%02d A%d S%d P%s ".format(mm, ss, s.ap, s.station, naOr(s.probe))
                    }
                    Text(strip, color = MarauderText, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    Text(if (s.geofence == "CLEAR") "G+" else "G!", color = if (s.geofence == "CLEAR") Success else Danger, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                }
            }

            if (ble) {
                BleReconBody(s)
            } else {
                WifiReconBody(s)
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.WifiReconBody(s: com.marauder.mobile.vm.ReconUiState) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(
            Modifier.weight(0.44f).border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)).padding(8.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Churn", color = MarauderText, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                Row {
                    Text("+${s.churnPlus}", color = Success, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    Spacer(Modifier.width(4.dp))
                    Text("-${s.churnMinus}", color = Danger, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(6.dp))
            Text("2G${s.band2g} 5G${s.band5g}", color = MarauderTextDim, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
        }
        Column(Modifier.weight(1f)) {
            Text("Live Activity", color = CatGeneral, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            Spacer(Modifier.height(3.dp))
            Activity("AP", s.ap, MarauderCyan)
            Activity("Station", s.station, MarauderText)
            Activity("Probe", s.probe, MarauderText)
            Activity("Update", s.update, MarauderText)
            Activity("Deauth", s.deauth, MarauderText)
        }
    }

    Text(
        "Probe / Update / Deauth are promiscuous-frame counters v1.18.0 keeps only for its own TFT and does not send over serial.",
        color = MarauderTextDim, fontSize = 10.sp,
    )

    Box(Modifier.fillMaxWidth().height(0.5.dp).background(MaterialTheme.colorScheme.outline))

    Column {
        Text("New Associations", color = CatGeneral, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        Spacer(Modifier.height(4.dp))
        if (s.associations.isEmpty()) {
            Text("(none yet)", color = MarauderTextDim, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
        } else {
            s.associations.forEach { a ->
                Text(a, color = MarauderText, fontFamily = FontFamily.Monospace, fontSize = 11.sp, maxLines = 1)
            }
        }
    }

    ChannelBars(s.channels)
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.BleReconBody(s: com.marauder.mobile.vm.ReconUiState) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(
            Modifier.weight(0.44f).border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)).padding(8.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Churn", color = MarauderText, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                Row {
                    Text("+${s.churnPlus}", color = Success, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    Spacer(Modifier.width(4.dp))
                    Text("-${s.churnMinus}", color = Danger, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                }
            }
        }
        Column(Modifier.weight(1f)) {
            Text("Live Activity", color = CatGeneral, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            Spacer(Modifier.height(3.dp))
            Activity("BLE Devices", s.ap, MarauderCyan)
        }
    }

    Box(Modifier.fillMaxWidth().height(0.5.dp).background(MaterialTheme.colorScheme.outline))

    Column {
        Text("BLE Devices", color = CatGeneral, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        Spacer(Modifier.height(4.dp))
        if (s.bleList.isEmpty()) {
            Text("(none yet)", color = MarauderTextDim, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
        } else {
            s.bleList.forEach { d ->
                Row(Modifier.fillMaxWidth().padding(vertical = 1.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        d.name.ifBlank { "(unnamed)" },
                        color = MarauderText, fontFamily = FontFamily.Monospace, fontSize = 11.sp,
                        maxLines = 1, modifier = Modifier.weight(1f),
                    )
                    Text("${d.rssi} dBm", color = rssiTint(d.rssi), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                }
            }
        }
    }
}

private fun naOr(value: Int): String = if (value < 0) "—" else value.toString()

@Composable
private fun Activity(label: String, value: Int, valueColor: Color) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MarauderTextDim, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
        Text(naOr(value), color = if (value < 0) MarauderTextDim else valueColor, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
    }
}

@Composable
private fun ChannelBars(channels: List<Int>) {
    val max = (channels.maxOrNull() ?: 1).coerceAtLeast(1)
    Column {
        Canvas(Modifier.fillMaxWidth().height(70.dp)) {
            val n = channels.size
            val gap = 4.dp.toPx()
            val bw = (size.width - gap * (n - 1)) / n
            val baseY = size.height - 12.dp.toPx()
            channels.forEachIndexed { i, v ->
                val h = (v.toFloat() / max) * (baseY - 2f)
                val x = i * (bw + gap)
                drawRect(
                    color = if (v > 0) CatDevice else Color(0xFF262C39),
                    topLeft = androidx.compose.ui.geometry.Offset(x, baseY - h),
                    size = androidx.compose.ui.geometry.Size(bw, h.coerceAtLeast(2f)),
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            (1..channels.size).forEach { ch ->
                Text("$ch", color = MarauderTextDim, fontFamily = FontFamily.Monospace, fontSize = 9.sp)
            }
        }
    }
}
