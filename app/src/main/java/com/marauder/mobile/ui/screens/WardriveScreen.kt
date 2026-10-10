package com.marauder.mobile.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marauder.mobile.ui.theme.CatGeneral
import com.marauder.mobile.ui.theme.CatScanner
import com.marauder.mobile.ui.theme.MarauderCyan
import com.marauder.mobile.ui.theme.MarauderText
import com.marauder.mobile.ui.theme.MarauderTextDim
import com.marauder.mobile.ui.theme.Success
import com.marauder.mobile.vm.MarauderViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WardriveScreen(vm: MarauderViewModel, onBack: () -> Unit) {
    val s by vm.wardriveUi.collectAsState()

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) vm.onLocationPermissionResult(true)
    }

    DisposableEffect(Unit) {
        vm.startWardriveMode()
        onDispose { vm.stopWardriveMode() }
    }
    LaunchedEffect(Unit) {
        if (!vm.hasLocationPermission()) permLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        else vm.onLocationPermissionResult(true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Wardrive", color = MarauderCyan) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    if (s.logging) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 14.dp)) {
                            Box(Modifier.size(7.dp).background(Success, RoundedCornerShape(4.dp)))
                            Spacer(Modifier.width(5.dp))
                            Text("logging", color = Success, fontSize = 11.sp)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MarauderCyan,
                ),
            )
        },
    ) { inner ->
        Column(
            modifier = Modifier.fillMaxSize().padding(inner).padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatRow("WiFi", s.wifi, Success)
            StatRow("BT", s.bt, MarauderCyan)
            StatRow("Flock", s.flock, CatScanner)
            StatRow("Sats", s.sats, MarauderText)

            Spacer(Modifier.size(2.dp))
            Column {
                FileLine("File:", s.file.ifBlank { "—" })
                FileLine("Size:", "%.2fKB".format(s.sizeKb))
            }

            Box(
                Modifier.fillMaxWidth()
                    .border(0.5.dp, if (s.geofence == "CLEAR") Success else CatScanner, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 9.dp),
            ) {
                Row {
                    Text("GF: ", color = MarauderTextDim, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                    Text(s.geofence, color = if (s.geofence == "CLEAR") Success else CatScanner, fontFamily = FontFamily.Monospace, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }

            OutlinedButton(
                onClick = { vm.wardriveMarkPoi() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("POI (${s.poi})", color = CatGeneral, fontFamily = FontFamily.Monospace, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: Int, color: Color) {
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, color = MarauderTextDim, fontFamily = FontFamily.Monospace, fontSize = 13.sp, modifier = Modifier.width(56.dp))
        Text("$value", color = color, fontFamily = FontFamily.Monospace, fontSize = 26.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun FileLine(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, color = MarauderTextDim, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        Text(value, color = MarauderText, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
    }
}
