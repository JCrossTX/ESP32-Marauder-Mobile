package com.marauder.mobile.ui.screens

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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.marauder.mobile.ui.components.SectionLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiKeysScreen(
    wigleName: String,
    wigleKey: String,
    wigleSaved: Boolean,
    wdgKey: String,
    wdgSaved: Boolean,
    onBack: () -> Unit = {},
    onWigleNameChange: (String) -> Unit = {},
    onWigleKeyChange: (String) -> Unit = {},
    onSaveWigle: () -> Unit = {},
    onClearWigle: () -> Unit = {},
    onWdgKeyChange: (String) -> Unit = {},
    onSaveWdg: () -> Unit = {},
    onClearWdg: () -> Unit = {},
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("API Keys") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
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
            SectionLabel("WiGLE", modifier = Modifier.padding(top = 8.dp))
            OutlinedTextField(
                value = wigleName,
                onValueChange = onWigleNameChange,
                label = { Text("API Name") },
                singleLine = true,
                readOnly = wigleSaved,
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = wigleKey,
                onValueChange = onWigleKeyChange,
                label = { Text("API Key") },
                singleLine = true,
                readOnly = wigleSaved,
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onSaveWigle) { Text("Save") }
                OutlinedButton(onClick = onClearWigle) { Text("Clear") }
            }

            SectionLabel("WDG Wars", modifier = Modifier.padding(top = 12.dp))
            OutlinedTextField(
                value = wdgKey,
                onValueChange = onWdgKeyChange,
                label = { Text("API Key") },
                singleLine = true,
                readOnly = wdgSaved,
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onSaveWdg) { Text("Save") }
                OutlinedButton(onClick = onClearWdg) { Text("Clear") }
            }
        }
    }
}
