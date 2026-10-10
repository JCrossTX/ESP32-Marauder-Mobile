package com.marauder.mobile.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marauder.mobile.ui.theme.CatScanner
import com.marauder.mobile.ui.theme.Danger
import com.marauder.mobile.ui.theme.MarauderCyan
import com.marauder.mobile.ui.theme.Success

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelSummaryScreen(
    activity: List<Int>,
    current: Int,
    page: Int,
    onPageDown: () -> Unit = {},
    onPageUp: () -> Unit = {},
    onBack: () -> Unit = {},
) {
    val pageSize = 7
    val start = page * pageSize
    val visible = (start until (start + pageSize)).map { ch0 -> activity.getOrElse(ch0) { 0 } }
    val max = (activity.maxOrNull() ?: 1).coerceAtLeast(1)
    val nonZero = activity.filter { it > 0 }
    val average = if (nonZero.isEmpty()) 0 else nonZero.sum() / nonZero.size

    GraphScaffold("Channel Summary", onBack) {
        Legend(showChannelMarker = true)
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(260.dp)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
                .border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                .padding(10.dp),
        ) {
            val outline = MaterialTheme.colorScheme.outline
            val dim = MaterialTheme.colorScheme.onSurfaceVariant
            Canvas(Modifier.fillMaxSize()) {
                drawChannelBars(
                    values = visible,
                    firstChannel = start + 1,
                    markers = listOf(current),
                    max = max,
                    average = average,
                    outline = outline,
                    dim = dim,
                )
            }
        }
        Spacer(Modifier.weight(1f))
        ChannelStepper(channel = start + 1, label = "Ch ${start + 1}–${start + pageSize}", onDown = onPageDown, onUp = onPageUp)
        Spacer(Modifier.height(6.dp))
    }
}

private fun DrawScope.drawChannelBars(
    values: List<Int>,
    firstChannel: Int,
    markers: List<Int>,
    max: Int,
    average: Int,
    outline: Color,
    dim: Color,
) {
    val top = 26f
    val bottom = size.height - 8f
    val left = 10f
    val right = size.width - 6f
    val span = (right - left) / values.size
    val h = bottom - top

    drawLine(outline, Offset(left, bottom), Offset(right, bottom), 1f)

    val labelPaint = android.graphics.Paint().apply {
        color = dim.toArgb(); textAlign = android.graphics.Paint.Align.CENTER
        textSize = 11.sp.toPx(); typeface = android.graphics.Typeface.MONOSPACE
    }
    values.forEachIndexed { i, v ->
        val cx = left + span * i + span / 2
        val bw = span * 0.52f
        val bh = (v.coerceIn(0, max).toFloat() / max) * h
        drawRoundRect(
            color = MarauderCyan,
            topLeft = Offset(cx - bw / 2, bottom - bh),
            size = androidx.compose.ui.geometry.Size(bw, bh),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f),
        )
        drawContext.canvas.nativeCanvas.drawText("${firstChannel + i}", cx, top - 10f, labelPaint)
    }

    markers.forEach { m ->
        val idx = m - firstChannel
        if (idx in values.indices) {
            val cx = left + span * idx + span / 2
            drawLine(Danger, Offset(cx, top), Offset(cx, bottom), 2f)
        }
    }

    val maxPaint = android.graphics.Paint().apply {
        color = Success.toArgb(); textSize = 10.sp.toPx(); typeface = android.graphics.Typeface.MONOSPACE
    }
    drawLine(Success, Offset(left, top), Offset(right, top), 1f)
    drawContext.canvas.nativeCanvas.drawText("$max", left + 2f, top - 2f, maxPaint)

    if (average in 1..max) {
        val ay = bottom - (average.toFloat() / max) * h
        drawLine(CatScanner, Offset(left, ay), Offset(right, ay), 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 3f)))
        val avgPaint = android.graphics.Paint().apply {
            color = CatScanner.toArgb(); textSize = 10.sp.toPx(); typeface = android.graphics.Typeface.MONOSPACE
        }
        drawContext.canvas.nativeCanvas.drawText("$average", left + 2f, ay - 2f, avgPaint)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelAnalyzerScreen(activity: List<Int>, onBack: () -> Unit = {}) {
    var focused by remember { mutableStateOf(6) }
    val max = (activity.maxOrNull() ?: 1).coerceAtLeast(1)
    val nonZero = activity.filter { it > 0 }
    val average = if (nonZero.isEmpty()) 0 else nonZero.sum() / nonZero.size

    GraphScaffold("Channel Analyzer", onBack) {
        Text(
            "Frames/100ms",
            color = MaterialTheme.colorScheme.onSurface,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Legend(showChannelMarker = true)
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
                .border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                .padding(10.dp),
        ) {
            val outline = MaterialTheme.colorScheme.outline
            val dim = MaterialTheme.colorScheme.onSurfaceVariant
            Canvas(Modifier.fillMaxSize()) {
                drawChannelBars(
                    values = activity,
                    firstChannel = 1,
                    markers = listOf(1, 6, 11, focused).distinct(),
                    max = max,
                    average = average,
                    outline = outline,
                    dim = dim,
                )
            }
        }
        Spacer(Modifier.weight(1f))
        ChannelStepper(
            channel = focused,
            label = "CH $focused",
            onDown = { focused = (focused - 1).coerceIn(1, 14) },
            onUp = { focused = (focused + 1).coerceIn(1, 14) },
        )
        Spacer(Modifier.height(6.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GraphScaffold(title: String, onBack: () -> Unit, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 14.dp)) {
                        Box(Modifier.size(7.dp).background(Success, CircleShape))
                        Spacer(Modifier.size(5.dp))
                        Text("live", color = Success, fontSize = 11.sp)
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
            Modifier.padding(inner).fillMaxSize().padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Spacer(Modifier.height(4.dp))
            content()
        }
    }
}

@Composable
internal fun Legend(showChannelMarker: Boolean) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        LegendItem(Success, "Max")
        Spacer(Modifier.size(14.dp))
        LegendItem(CatScanner, "Average")
        if (showChannelMarker) {
            Spacer(Modifier.size(14.dp))
            LegendItem(Danger, "Channel Marker")
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(width = 9.dp, height = 2.dp).background(color))
        Spacer(Modifier.size(5.dp))
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.5.sp)
    }
}
