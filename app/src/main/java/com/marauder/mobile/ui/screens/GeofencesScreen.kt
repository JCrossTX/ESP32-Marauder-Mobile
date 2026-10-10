package com.marauder.mobile.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.unit.dp
import com.marauder.mobile.ui.theme.Danger
import com.marauder.mobile.ui.theme.MarauderCyan
import com.marauder.mobile.ui.theme.MarauderTextDim
import com.marauder.mobile.ui.theme.Success
import com.marauder.mobile.vm.GeofenceSlot
import com.marauder.mobile.vm.MarauderViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeofencesScreen(vm: MarauderViewModel, onBack: () -> Unit) {
    val slots by vm.geofences.collectAsState()
    var actionSlot by remember { mutableStateOf<GeofenceSlot?>(null) }
    var editor by remember { mutableStateOf<GeofenceEditor?>(null) }

    LaunchedEffect(Unit) { vm.refreshGeofences() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Geofences") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    IconButton(onClick = { vm.refreshGeofences() }) {
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
        val display = if (slots.isEmpty()) (1..5).map { GeofenceSlot(it, false) } else slots
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(inner),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(display, key = { it.slot }) { slot ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth().clickable { actionSlot = slot },
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("${slot.slot}", style = MaterialTheme.typography.titleLarge, color = if (slot.configured) Success else MarauderTextDim, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.size(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (slot.configured) slot.name.ifBlank { "(unnamed)" } else "Empty",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold,
                            )
                            if (slot.configured) {
                                Text(
                                    "%.5f, %.5f · %.2f mi".format(slot.lat, slot.lon, slot.radiusMiles),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = MarauderTextDim,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    actionSlot?.let { slot ->
        AlertDialog(
            onDismissRequest = { actionSlot = null },
            title = { Text("Geofence ${slot.slot}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = {
                        actionSlot = null
                        editor = GeofenceEditor(slot.slot, "", "", if (slot.configured) "%.2f".format(slot.radiusMiles) else "0.5", if (slot.configured) slot.name else "", fromGps = false)
                    }) { Text("Enter coordinates") }
                    TextButton(onClick = {
                        val fix = vm.currentPhoneFix()
                        actionSlot = null
                        editor = if (fix != null)
                            GeofenceEditor(slot.slot, "%.6f".format(fix.first), "%.6f".format(fix.second), "0.5", if (slot.configured) slot.name else "", fromGps = true)
                        else
                            GeofenceEditor(slot.slot, "", "", "0.5", if (slot.configured) slot.name else "", fromGps = false)
                    }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp), tint = Success)
                            Spacer(Modifier.size(6.dp))
                            Text("Use current GPS")
                        }
                    }
                    if (slot.configured) {
                        TextButton(onClick = { vm.clearGeofence(slot.slot); actionSlot = null }) {
                            Text("Clear geofence", color = Danger)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { actionSlot = null }) { Text("Cancel") } },
        )
    }

    editor?.let { e ->
        var lat by remember(e) { mutableStateOf(e.lat) }
        var lon by remember(e) { mutableStateOf(e.lon) }
        var radius by remember(e) { mutableStateOf(e.radius) }
        var name by remember(e) { mutableStateOf(e.name) }
        AlertDialog(
            onDismissRequest = { editor = null },
            title = { Text("Geofence ${e.slot}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = lat, onValueChange = { lat = it }, label = { Text("Latitude") }, singleLine = true, readOnly = e.fromGps)
                    OutlinedTextField(value = lon, onValueChange = { lon = it }, label = { Text("Longitude") }, singleLine = true, readOnly = e.fromGps)
                    OutlinedTextField(value = radius, onValueChange = { radius = it }, label = { Text("Radius (0.1-1.0 mi)") }, singleLine = true)
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val la = lat.toDoubleOrNull(); val lo = lon.toDoubleOrNull(); val ra = radius.toDoubleOrNull()
                    if (la != null && lo != null && ra != null && name.isNotBlank()) {
                        vm.setGeofence(e.slot, la, lo, ra, name.trim())
                        editor = null
                    }
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { editor = null }) { Text("Cancel") } },
        )
    }
}

private data class GeofenceEditor(
    val slot: Int,
    val lat: String,
    val lon: String,
    val radius: String,
    val name: String,
    val fromGps: Boolean,
)
