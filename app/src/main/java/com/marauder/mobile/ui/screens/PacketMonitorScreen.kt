package com.marauder.mobile.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marauder.mobile.ui.theme.CatDevice
import com.marauder.mobile.ui.theme.Danger
import com.marauder.mobile.ui.theme.MarauderCyan
import com.marauder.mobile.ui.theme.Success

data class PacketMetric(
    val code: String,
    val label: String,
    val color: Color,
    val current: Int,
    val max: Int,
    val history: List<Int>,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PacketMonitorScreen(
    metrics: List<PacketMetric>,
    channel: Int,
    onChannelDown: () -> Unit = {},
    onChannelUp: () -> Unit = {},
    onBack: () -> Unit = {},
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Packet Monitor") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = { LiveDot() },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MarauderCyan,
                ),
            )
        },
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Spacer(Modifier.height(2.dp))
            metrics.forEach { MetricCard(it) }
            Spacer(Modifier.weight(1f))
            ChannelStepper(channel = channel, label = "CH $channel", onDown = onChannelDown, onUp = onChannelUp)
            Spacer(Modifier.height(6.dp))
        }
    }
}

@Composable
private fun LiveDot() {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 14.dp)) {
        Box(Modifier.size(7.dp).background(Success, CircleShape))
        Spacer(Modifier.width(5.dp))
        Text("live", color = Success, fontSize = 11.sp)
    }
}

@Composable
private fun MetricCard(m: PacketMetric) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
            .border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(9.dp).background(m.color, CircleShape))
            Spacer(Modifier.width(8.dp))
            Text(m.code, color = m.color, fontFamily = FontFamily.Monospace, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.width(8.dp))
            Text(m.label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            Spacer(Modifier.weight(1f))
            Text(m.current.toString(), color = MaterialTheme.colorScheme.onSurface, fontFamily = FontFamily.Monospace, fontSize = 22.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.height(8.dp))
        Sparkline(history = m.history, max = m.max, color = m.color)
    }
}

@Composable
private fun Sparkline(history: List<Int>, max: Int, color: Color) {
    val outline = MaterialTheme.colorScheme.outline
    val dim = MaterialTheme.colorScheme.onSurfaceVariant
    Box(Modifier.fillMaxWidth().height(96.dp)) {
        Text(
            "max $max",
            color = dim,
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            modifier = Modifier.align(Alignment.TopStart),
        )
        Canvas(Modifier.fillMaxSize()) {
            val top = 14f
            val bottom = size.height - 6f
            val left = 2f
            val right = size.width - 2f

            drawLine(outline, androidx.compose.ui.geometry.Offset(left, bottom), androidx.compose.ui.geometry.Offset(right, bottom), 1f)
            drawLine(
                outline,
                androidx.compose.ui.geometry.Offset(left, top),
                androidx.compose.ui.geometry.Offset(right, top),
                1f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 8f)),
            )
            if (history.size < 2) return@Canvas
            val span = (max.coerceAtLeast(1)).toFloat()
            val stepX = (right - left) / (history.size - 1)
            fun x(i: Int) = left + stepX * i
            fun y(v: Int) = bottom - (v.coerceIn(0, max.coerceAtLeast(1)) / span) * (bottom - top)
            val line = Path().apply {
                moveTo(x(0), y(history[0]))
                for (i in 1 until history.size) lineTo(x(i), y(history[i]))
            }
            val area = Path().apply {
                addPath(line)
                lineTo(x(history.size - 1), bottom)
                lineTo(x(0), bottom)
                close()
            }
            drawPath(area, color, alpha = 0.15f)
            drawPath(line, color, style = Stroke(width = 2f))
        }
    }
}

@Composable
internal fun ChannelStepper(channel: Int, label: String, onDown: () -> Unit, onUp: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedButton(
            onClick = onDown,
            modifier = Modifier.size(width = 52.dp, height = 46.dp),
            shape = RoundedCornerShape(12.dp),
        ) { Text("−", color = CatDevice, fontSize = 22.sp) }
        Box(
            modifier = Modifier
                .weight(1f)
                .height(46.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(label, color = MaterialTheme.colorScheme.onSurface, fontFamily = FontFamily.Monospace, fontSize = 18.sp, fontWeight = FontWeight.Medium)
        }
        OutlinedButton(
            onClick = onUp,
            modifier = Modifier.size(width = 52.dp, height = 46.dp),
            shape = RoundedCornerShape(12.dp),
        ) { Text("+", color = CatDevice, fontSize = 22.sp) }
    }
}
