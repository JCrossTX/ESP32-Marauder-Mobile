package com.marauder.mobile.ui.screens

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
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.marauder.mobile.data.StorageRepository
import com.marauder.mobile.ui.components.EmptyState
import com.marauder.mobile.ui.theme.Danger
import com.marauder.mobile.ui.theme.MarauderCyan
import com.marauder.mobile.ui.theme.MarauderTextDim
import com.marauder.mobile.vm.MarauderViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadLogsScreen(vm: MarauderViewModel, onBack: () -> Unit, onApiKeys: () -> Unit) {
    var files by remember { mutableStateOf(vm.wardriveFiles()) }
    var pickDestFor by remember { mutableStateOf<String?>(null) }
    var confirmDeleteAll by remember { mutableStateOf(false) }

    fun refresh() { files = vm.wardriveFiles() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Upload Logs") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    IconButton(onClick = onApiKeys) { Icon(Icons.Filled.Key, contentDescription = "API Keys", tint = MarauderCyan) }
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
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(onClick = { pickDestFor = "ALL" }, enabled = files.isNotEmpty(), modifier = Modifier.weight(1f)) {
                    Text("Upload All")
                }
                OutlinedButton(onClick = { confirmDeleteAll = true }, enabled = files.isNotEmpty(), modifier = Modifier.weight(1f)) {
                    Text("Delete All", color = Danger)
                }
            }

            if (files.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(
                        icon = Icons.Filled.Folder,
                        title = "No wardrive logs",
                        subtitle = "Run a Wardrive and the WigleWiFi CSVs will appear here to upload.",
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(files, key = { it.path }) { f -> LogRow(f) { pickDestFor = f.path } }
                }
            }
        }
    }

    pickDestFor?.let { target ->
        fun choose(service: String) {
            if (target == "ALL") vm.uploadWardrive(service) else vm.uploadWardriveFile(service, target)
            pickDestFor = null
        }
        AlertDialog(
            onDismissRequest = { pickDestFor = null },
            title = { Text(if (target == "ALL") "Upload all to" else "Upload to") },
            text = {
                Column {
                    TextButton(onClick = { choose("wigle") }) { Text("WiGLE") }
                    TextButton(onClick = { choose("wdg") }) { Text("WDG Wars") }
                    TextButton(onClick = { choose("both") }) { Text("Both") }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { pickDestFor = null }) { Text("Cancel") } },
        )
    }

    if (confirmDeleteAll) {
        AlertDialog(
            onDismissRequest = { confirmDeleteAll = false },
            title = { Text("Delete all wardrive logs?") },
            text = { Text("${files.size} file(s) will be removed from the phone.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteAllWardrive()
                    confirmDeleteAll = false
                    refresh()
                }) { Text("Delete", color = Danger) }
            },
            dismissButton = { TextButton(onClick = { confirmDeleteAll = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun LogRow(file: StorageRepository.StoredFile, onUpload: () -> Unit) {
    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth().clickable(onClick = onUpload)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(file.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.size(2.dp))
                Text(
                    "WARDRIVE · ${humanSizeLogs(file.size)} · ${dateFmtLogs.format(Date(file.modified))}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MarauderTextDim,
                    fontFamily = FontFamily.Monospace,
                )
            }
            Icon(Icons.Filled.CloudUpload, contentDescription = "Upload", tint = MarauderCyan)
        }
    }
}

private val dateFmtLogs = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
private fun humanSizeLogs(bytes: Long): String = when {
    bytes >= 1_048_576 -> "%.1f MB".format(bytes / 1_048_576.0)
    bytes >= 1024 -> "%.1f KB".format(bytes / 1024.0)
    else -> "$bytes B"
}
