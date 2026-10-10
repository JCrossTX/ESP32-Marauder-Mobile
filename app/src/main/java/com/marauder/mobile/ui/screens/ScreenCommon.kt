package com.marauder.mobile.ui.screens

import androidx.compose.ui.graphics.Color
import com.marauder.mobile.ui.theme.Danger
import com.marauder.mobile.ui.theme.Success
import com.marauder.mobile.ui.theme.Warning

internal fun rssiTint(rssi: Int): Color = when {
    rssi >= -60 -> Success
    rssi >= -75 -> Warning
    else -> Danger
}
