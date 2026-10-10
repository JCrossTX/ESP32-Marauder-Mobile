package com.marauder.mobile.wifi

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.Looper
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SoftApHost(context: Context) {

    private val appContext = context.applicationContext
    private val manager: WifiP2pManager? =
        appContext.getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager
    private var channel: WifiP2pManager.Channel? = null

    data class SoftApState(
        val active: Boolean = false,
        val starting: Boolean = false,
        val ssid: String = "",
        val passphrase: String = "",
        val ownerAddress: String = "",
        val clients: Int = 0,
        val error: String? = null,
    )

    private val _state = MutableStateFlow(SoftApState())
    val state: StateFlow<SoftApState> = _state.asStateFlow()

    fun requiredPermission(): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.NEARBY_WIFI_DEVICES
        else Manifest.permission.ACCESS_FINE_LOCATION

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(appContext, requiredPermission()) == PackageManager.PERMISSION_GRANTED

    private fun ensureChannel(): WifiP2pManager.Channel? {
        if (manager == null) return null
        if (channel == null) channel = manager.initialize(appContext, Looper.getMainLooper(), null)
        return channel
    }

    @SuppressLint("MissingPermission")
    fun start(ssid: String, passphrase: String): Boolean {
        val mgr = manager ?: run { fail("Wi-Fi Direct not available on this phone"); return false }
        val ch = ensureChannel() ?: run { fail("Wi-Fi Direct not available on this phone"); return false }
        if (!hasPermission()) { fail("Grant the nearby-Wi-Fi / location permission to host an AP"); return false }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (passphrase.length !in 8..63) { fail("Password must be 8–63 characters"); return false }
            if (ssid.isBlank()) { fail("Enter a network name"); return false }
        }
        _state.value = SoftApState(starting = true, ssid = ssid, passphrase = passphrase)

        val listener = object : WifiP2pManager.ActionListener {
            override fun onSuccess() { refresh() }
            override fun onFailure(reason: Int) = fail("Could not start AP (${reasonText(reason)})")
        }

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val config = WifiP2pConfig.Builder()
                    .setNetworkName(directName(ssid))
                    .setPassphrase(passphrase)
                    .enablePersistentMode(false)
                    .setGroupOperatingBand(WifiP2pConfig.GROUP_OWNER_BAND_AUTO)
                    .build()
                mgr.createGroup(ch, config, listener)
            } else {
                mgr.createGroup(ch, listener)
            }
            true
        } catch (e: SecurityException) {
            fail("Permission denied starting the AP"); false
        } catch (e: IllegalArgumentException) {
            fail("Invalid network name or password"); false
        }
    }

    @SuppressLint("MissingPermission")
    fun stop() {
        val mgr = manager ?: return
        val ch = channel ?: return
        runCatching {
            mgr.removeGroup(ch, object : WifiP2pManager.ActionListener {
                override fun onSuccess() { _state.value = SoftApState() }
                override fun onFailure(reason: Int) { _state.value = SoftApState() }
            })
        }.onFailure { _state.value = SoftApState() }
    }

    @SuppressLint("MissingPermission")
    fun refresh() {
        val mgr = manager ?: return
        val ch = channel ?: return
        if (!hasPermission()) return
        runCatching {
            mgr.requestGroupInfo(ch) { group ->
                if (group == null) {
                    _state.value = SoftApState()
                } else {
                    _state.value = _state.value.copy(
                        active = true,
                        starting = false,
                        ssid = group.networkName ?: _state.value.ssid,
                        passphrase = group.passphrase ?: _state.value.passphrase,
                        clients = group.clientList?.size ?: 0,
                        error = null,
                    )
                }
            }
            mgr.requestConnectionInfo(ch) { info ->
                val addr = info?.groupOwnerAddress?.hostAddress
                if (!addr.isNullOrBlank()) _state.value = _state.value.copy(ownerAddress = addr)
            }
        }
    }

    private fun fail(message: String) {
        _state.value = _state.value.copy(active = false, starting = false, error = message)
    }

    private fun directName(ssid: String): String =
        if (ssid.startsWith("DIRECT-")) ssid else "DIRECT-MB-$ssid"

    private fun reasonText(reason: Int): String = when (reason) {
        WifiP2pManager.P2P_UNSUPPORTED -> "Wi-Fi Direct unsupported"
        WifiP2pManager.BUSY -> "framework busy"
        WifiP2pManager.ERROR -> "internal error"
        else -> "error $reason"
    }
}
