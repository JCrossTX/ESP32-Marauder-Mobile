package com.marauder.mobile.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.marauder.mobile.ui.theme.Danger
import com.marauder.mobile.ui.theme.Success
import com.marauder.mobile.vm.UploadUiState

@Composable
fun UploadProgressDialog(upload: UploadUiState?, onDismiss: () -> Unit) {
    if (upload == null) return
    AlertDialog(
        onDismissRequest = { if (!upload.running) onDismiss() },
        title = { Text(upload.title) },
        text = {
            Column {
                if (upload.running) {
                    LinearProgressIndicator(
                        progress = { upload.percent / 100f },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("${upload.percent}%", style = MaterialTheme.typography.bodySmall)
                }
                upload.lines.forEach { line ->
                    Text(
                        line.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (line.ok) Success else Danger,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        },
        confirmButton = {
            if (!upload.running) {
                TextButton(onClick = onDismiss) { Text("Done") }
            }
        },
    )
}
