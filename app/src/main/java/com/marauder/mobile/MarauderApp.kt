package com.marauder.mobile

import android.app.Application
import com.marauder.mobile.ble.BleScanner
import com.marauder.mobile.capture.CaptureSink
import com.marauder.mobile.data.ApiKeyStore
import com.marauder.mobile.data.SavedWifiStore
import com.marauder.mobile.data.SettingsRepository
import com.marauder.mobile.data.StorageRepository
import com.marauder.mobile.location.LocationRepository
import com.marauder.mobile.usb.UsbSerialManager
import com.marauder.mobile.wifi.SoftApHost

class MarauderApp : Application() {

    lateinit var usb: UsbSerialManager
        private set

    lateinit var settings: SettingsRepository
        private set

    lateinit var captureSink: CaptureSink
        private set

    lateinit var location: LocationRepository
        private set

    lateinit var storage: StorageRepository
        private set

    lateinit var apiKeys: ApiKeyStore
        private set

    lateinit var ble: BleScanner
        private set

    lateinit var savedWifi: SavedWifiStore
        private set

    lateinit var softAp: SoftApHost
        private set

    override fun onCreate() {
        super.onCreate()
        usb = UsbSerialManager(this)
        settings = SettingsRepository(this)
        captureSink = CaptureSink(this)
        location = LocationRepository(this)
        storage = StorageRepository(this)
        apiKeys = ApiKeyStore(this)
        ble = BleScanner(this)
        savedWifi = SavedWifiStore(this)
        softAp = SoftApHost(this)
    }
}
