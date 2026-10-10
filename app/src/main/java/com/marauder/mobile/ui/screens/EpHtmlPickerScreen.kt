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
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import com.marauder.mobile.data.StorageRepository
import com.marauder.mobile.ui.components.EmptyState
import com.marauder.mobile.ui.theme.MarauderCyan
import com.marauder.mobile.ui.theme.MarauderTextDim
import com.marauder.mobile.ui.theme.Success
import com.marauder.mobile.vm.EvilPortalUiState
import com.marauder.mobile.vm.MarauderViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpHtmlPickerScreen(vm: MarauderViewModel, onBack: () -> Unit) {
    var files by remember { mutableStateOf(vm.savedHtmlFiles()) }
    val portal by vm.evilPortal.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select EP HTML File") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    IconButton(onClick = { files = vm.savedHtmlFiles() }) {
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
        Column(Modifier.fillMaxSize().padding(inner)) {
            if (portal.fileName != null) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Success.copy(alpha = 0.10f),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            when (portal.phase) {
                                EvilPortalUiState.Phase.Ready -> "Set on device: ${portal.fileName}"
                                EvilPortalUiState.Phase.Uploading -> "Streaming ${portal.fileName}…"
                                EvilPortalUiState.Phase.Error -> "Failed: ${portal.fileName}"
                                else -> portal.fileName.orEmpty()
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        portal.message?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MarauderTextDim) }
                    }
                }
            }

            if (files.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(
                        Icons.Filled.Language,
                        "No saved HTML pages",
                        "Put .html captive-portal pages in the on-phone storage folder (Storage), then refresh.",
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(files, key = { it.path }) { f ->
                        HtmlRow(f) { vm.selectEpHtml(f.path) }
                    }
                }
            }
        }
    }
}

@Composable
private fun HtmlRow(file: StorageRepository.StoredFile, onSelect: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth().clickable { onSelect() },
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Language, contentDescription = null, tint = MarauderCyan, modifier = Modifier.size(22.dp))
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(file.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.size(2.dp))
                Text("${humanKb(file.size)} · ${dateFmt.format(Date(file.modified))}", style = MaterialTheme.typography.labelSmall, color = MarauderTextDim)
            }
        }
    }
}

private val dateFmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
private fun humanKb(bytes: Long): String = if (bytes >= 1024) "%.1f KB".format(bytes / 1024.0) else "$bytes B"
