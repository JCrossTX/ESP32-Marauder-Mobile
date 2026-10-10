package com.marauder.mobile.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marauder.mobile.ui.theme.CatGeneral
import com.marauder.mobile.ui.theme.MarauderCyan
import com.marauder.mobile.ui.theme.MarauderTextDim
import com.marauder.mobile.ui.theme.Success
import com.marauder.mobile.vm.RidDevice
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun RemoteIdRadarScreen(
    devices: List<RidDevice>,
    channel: Int,
    phoneLat: Double?,
    phoneLon: Double?,
    onBack: () -> Unit = {},
) {
    GraphScaffold("Remote ID Scan", onBack) {
        Text(
            buildString {
                append("RID devices: ${devices.size}")
                if (channel > 0) append("  ·  CH $channel")
            },
            color = MarauderCyan,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
        )
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.Center) {
            LegendDot(Success, "M Marauder"); Spacer(Modifier.size(14.dp))
            LegendDot(MarauderCyan, "A Aircraft"); Spacer(Modifier.size(14.dp))
            LegendDot(CatGeneral, "O Operator")
        }
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
                .border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                .padding(12.dp),
        ) {
            val ring = MaterialTheme.colorScheme.outline
            val axis = MaterialTheme.colorScheme.surfaceVariant
            val dim = MarauderTextDim
            val hasFix = phoneLat != null && phoneLon != null

            val pts = ArrayList<Triple<androidx.compose.ui.graphics.Color, Double, Double>>()
            var maxRange = 0.0
            if (hasFix) {
                devices.forEach { d ->
                    if (d.aircraftLat != 0.0 || d.aircraftLon != 0.0) {
                        val dist = haversine(phoneLat!!, phoneLon!!, d.aircraftLat, d.aircraftLon)
                        pts.add(Triple(MarauderCyan, bearing(phoneLat, phoneLon, d.aircraftLat, d.aircraftLon), dist))
                        maxRange = max(maxRange, dist)
                    }
                    if (d.operatorLat != 0.0 || d.operatorLon != 0.0) {
                        val dist = haversine(phoneLat!!, phoneLon!!, d.operatorLat, d.operatorLon)
                        pts.add(Triple(CatGeneral, bearing(phoneLat, phoneLon, d.operatorLat, d.operatorLon), dist))
                        maxRange = max(maxRange, dist)
                    }
                }
            }
            val range = maxRange.coerceAtLeast(50.0)
            Canvas(Modifier.fillMaxSize()) {
                val cx = size.width / 2
                val cy = size.height / 2
                val rMax = (minOf(size.width, size.height) / 2) - 16f

                for (i in 1..3) drawCircle(ring, rMax * i / 3, Offset(cx, cy), style = Stroke(1f))

                drawLine(axis, Offset(cx, cy - rMax), Offset(cx, cy + rMax), 1f)
                drawLine(axis, Offset(cx - rMax, cy), Offset(cx + rMax, cy), 1f)
                val axisPaint = android.graphics.Paint().apply {
                    color = dim.toArgb(); textSize = 11.sp.toPx(); typeface = android.graphics.Typeface.MONOSPACE
                    textAlign = android.graphics.Paint.Align.CENTER
                }
                drawContext.canvas.nativeCanvas.drawText("N", cx, cy - rMax - 4f, axisPaint)
                drawContext.canvas.nativeCanvas.drawText("S", cx, cy + rMax + 14f, axisPaint)
                drawContext.canvas.nativeCanvas.drawText("E", cx + rMax + 10f, cy + 4f, axisPaint)
                drawContext.canvas.nativeCanvas.drawText("W", cx - rMax - 10f, cy + 4f, axisPaint)

                pts.forEach { (color, brg, dist) ->
                    val rr = (dist / range).toFloat().coerceIn(0f, 1f) * rMax
                    val rad = Math.toRadians(brg).toFloat()
                    val px = cx + rr * sin(rad)
                    val py = cy - rr * cos(rad)
                    drawCircle(color, 5f, Offset(px, py))
                }

                drawCircle(Success, 5f, Offset(cx, cy))
                val mPaint = android.graphics.Paint().apply {
                    color = Success.toArgb(); textSize = 11.sp.toPx(); typeface = android.graphics.Typeface.MONOSPACE
                }
                drawContext.canvas.nativeCanvas.drawText("M", cx + 8f, cy + 4f, mPaint)

                val rangePaint = android.graphics.Paint().apply {
                    color = Success.toArgb(); textSize = 11.sp.toPx(); typeface = android.graphics.Typeface.MONOSPACE
                }
                drawContext.canvas.nativeCanvas.drawText("R ${range.toInt()}m", 2f, size.height - 2f, rangePaint)
                if (!hasFix) {
                    val wp = android.graphics.Paint().apply {
                        color = dim.toArgb(); textSize = 12.sp.toPx(); typeface = android.graphics.Typeface.MONOSPACE
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                    drawContext.canvas.nativeCanvas.drawText("Waiting for GPS fix", cx, cy + rMax + 2f, wp)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun LegendDot(color: androidx.compose.ui.graphics.Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).background(color, RoundedCornerShape(4.dp)))
        Spacer(Modifier.size(5.dp))
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontFamily = FontFamily.Monospace, fontSize = 10.5.sp)
    }
}

private const val EARTH_R = 6371000.0
private fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2) * sin(dLat / 2) +
        cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2) * sin(dLon / 2)
    return EARTH_R * 2 * atan2(sqrt(a), sqrt(1 - a))
}

private fun bearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val dLon = Math.toRadians(lon2 - lon1)
    val y = sin(dLon) * cos(Math.toRadians(lat2))
    val x = cos(Math.toRadians(lat1)) * sin(Math.toRadians(lat2)) -
        sin(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * cos(dLon)
    return (Math.toDegrees(atan2(y, x)) + 360) % 360
}
