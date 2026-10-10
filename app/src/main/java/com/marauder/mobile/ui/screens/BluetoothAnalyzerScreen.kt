package com.marauder.mobile.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marauder.mobile.ui.theme.CatScanner
import com.marauder.mobile.ui.theme.MarauderCyan
import com.marauder.mobile.ui.theme.Success

@Composable
fun BluetoothAnalyzerScreen(
    spikes: List<Int>,
    topRssi: Int?,
    topName: String,
    onBack: () -> Unit = {},
) {
    val max = (spikes.maxOrNull() ?: 1).coerceAtLeast(1)
    val nonZero = spikes.filter { it > 0 }
    val average = if (nonZero.isEmpty()) 0 else nonZero.sum() / nonZero.size

    GraphScaffold("Bluetooth Analyzer", onBack) {
        Text(
            "BLE Beacons/100ms",
            color = MaterialTheme.colorScheme.onSurface,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
            textAlign = TextAlign.Center,
        )
        Legend(showChannelMarker = false)
        Spacer(Modifier.height(8.dp))
        if (topRssi != null) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("$topRssi", color = Success, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                Spacer(Modifier.size(8.dp))
                Text(topName, color = MaterialTheme.colorScheme.onSurface, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(360.dp)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
                .border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                .padding(10.dp),
        ) {
            val outline = MaterialTheme.colorScheme.outline
            val dim = MaterialTheme.colorScheme.onSurfaceVariant
            Canvas(Modifier.fillMaxWidth().height(340.dp)) {
                val top = 10f
                val bottom = size.height - 6f
                val left = 4f
                val right = size.width - 4f
                val h = bottom - top
                drawLine(outline, Offset(left, bottom), Offset(right, bottom), 1f)
                if (spikes.isNotEmpty()) {
                    val step = (right - left) / spikes.size
                    spikes.forEachIndexed { i, v ->
                        val bh = (v.coerceIn(0, max).toFloat() / max) * h
                        val x = left + step * i
                        drawLine(MarauderCyan, Offset(x, bottom), Offset(x, bottom - bh), (step * 0.7f).coerceAtLeast(1f))
                    }
                }

                drawLine(Success, Offset(left, top), Offset(right, top), 1f)
                val maxPaint = android.graphics.Paint().apply {
                    color = Success.toArgb(); textSize = 11.sp.toPx(); typeface = android.graphics.Typeface.MONOSPACE
                }
                drawContext.canvas.nativeCanvas.drawText("$max", left + 2f, top + 12f, maxPaint)

                if (average in 1..max) {
                    val ay = bottom - (average.toFloat() / max) * h
                    drawLine(CatScanner, Offset(left, ay), Offset(right, ay), 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 3f)))
                    val avgPaint = android.graphics.Paint().apply {
                        color = CatScanner.toArgb(); textSize = 11.sp.toPx(); typeface = android.graphics.Typeface.MONOSPACE
                    }
                    drawContext.canvas.nativeCanvas.drawText("$average", left + 2f, ay - 3f, avgPaint)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}
