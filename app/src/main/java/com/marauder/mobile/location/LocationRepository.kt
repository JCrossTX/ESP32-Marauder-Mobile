package com.marauder.mobile.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import androidx.core.content.ContextCompat
import com.marauder.mobile.data.PhoneLocation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LocationRepository(context: Context) {

    private val appContext = context.applicationContext
    private val lm = appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private val _location = MutableStateFlow<PhoneLocation?>(null)
    val location: StateFlow<PhoneLocation?> = _location.asStateFlow()

    private val _satellites = MutableStateFlow(0)
    val satellites: StateFlow<Int> = _satellites.asStateFlow()

    private val listener = LocationListener { loc -> _location.value = loc.toPhone() }

    private val gnssCallback = object : android.location.GnssStatus.Callback() {
        override fun onSatelliteStatusChanged(status: android.location.GnssStatus) {
            var used = 0
            for (i in 0 until status.satelliteCount) if (status.usedInFix(i)) used++
            _satellites.value = if (used > 0) used else status.satelliteCount
        }
    }

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun start(): Boolean {
        if (!hasPermission()) return false
        return try {
            lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)?.let { _location.value = it.toPhone() }
            lm.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                MIN_INTERVAL_MS,
                MIN_DISTANCE_M,
                listener,
                Looper.getMainLooper(),
            )
            runCatching { lm.registerGnssStatusCallback(gnssCallback, android.os.Handler(Looper.getMainLooper())) }
            true
        } catch (e: SecurityException) {
            false
        } catch (e: IllegalArgumentException) {
            false
        }
    }

    fun stop() {
        runCatching { lm.removeUpdates(listener) }
        runCatching { lm.unregisterGnssStatusCallback(gnssCallback) }
        _satellites.value = 0
    }

    private fun Location.toPhone() = PhoneLocation(
        lat = latitude,
        lon = longitude,
        altMeters = if (hasAltitude()) altitude else 0.0,
        accuracyMeters = if (hasAccuracy()) accuracy.toDouble() else 0.0,
        timeMillis = if (time > 0) time else System.currentTimeMillis(),
    )

    private companion object {
        const val MIN_INTERVAL_MS = 1000L
        const val MIN_DISTANCE_M = 0f
    }
}
