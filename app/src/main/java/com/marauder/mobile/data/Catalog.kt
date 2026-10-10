package com.marauder.mobile.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.automirrored.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.automirrored.filled.Dvr
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Http
import androidx.compose.material.icons.filled.Https
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Luggage
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PermScanWifi
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PhoneIphone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Satellite
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SettingsEthernet
import androidx.compose.material.icons.filled.SettingsInputAntenna
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.ui.graphics.vector.ImageVector

enum class ListType(val code: String, val title: String) {
    ACCESS_POINTS("a", "Access Points"),
    SSIDS("s", "SSIDs"),
    STATIONS("c", "Stations"),
    IPS("i", "IP List"),
    PROBES("p", "Probe Requests"),
    AIRTAGS("t", "AirTags"),
    BLE_DEVICES("b", "BLE Devices"),
    PINEAPPLES("x", "WiFi Pineapples"),
    MULTISSID("m", "MultiSSID"),
    FLIPPERS("f", "Flippers"),
}

enum class Accent { WIFI, BLUETOOTH, GPS, DEVICE, NEUTRAL, SNIFFER, SCANNER, ATTACK, GENERAL, PRIMARY }

sealed interface MenuAction {

    data class Send(val command: String) : MenuAction

    data class SendPrompt(val commandPrefix: String, val promptTitle: String, val hint: String) : MenuAction

    data class Submenu(val id: String) : MenuAction

    data class OpenList(val type: ListType) : MenuAction

    data object OpenConsole : MenuAction

    data object OpenInstaller : MenuAction

    data object OpenStorage : MenuAction

    data object OpenEvilPortal : MenuAction

    data object OpenApiKeys : MenuAction

    data class UploadWardrive(val service: String) : MenuAction

    data class OpenGraphic(val kind: String) : MenuAction

    data object OpenUploadLogs : MenuAction

    data object OpenIBeaconInfo : MenuAction

    data object OpenStartAp : MenuAction

    data object OpenHostApInfo : MenuAction

    data object OpenManageSavedWifi : MenuAction

    data object OpenEpHtmlPicker : MenuAction

    data class OpenFoxHunt(val category: String) : MenuAction

    data class PickAndRun(
        val title: String,
        val list: ListType,
        val commandPrefix: String,
        val needsPassword: Boolean = false,
    ) : MenuAction

    data object OpenDeviceSettings : MenuAction

    data object OpenGeofences : MenuAction

    data object OpenFindMyMonitor : MenuAction
}

data class MenuItem(
    val title: String,
    val subtitle: String? = null,
    val icon: ImageVector,
    val accent: Accent,
    val action: MenuAction,
    val dangerous: Boolean = false,
)

data class MenuScreen(
    val id: String,
    val title: String,
    val items: List<MenuItem>,
)

object Catalog {

    const val ROOT = "main"

    const val INSTALLER_URL = "https://justcallmekoko.github.io/MarauderInstaller/"

    val screens: Map<String, MenuScreen> = buildList {

        add(
            MenuScreen(
                id = "main",
                title = "Marauder",
                items = listOf(
                    MenuItem("Recon", "Passive WiFi / BLE recon", Icons.Filled.Radar, Accent.SCANNER, MenuAction.Submenu("recon")),
                    MenuItem("WiFi", "Sniffers · Scanners · Attacks · General", Icons.Filled.Wifi, Accent.WIFI, MenuAction.Submenu("wifi")),
                    MenuItem("Bluetooth", "Sniffers · Attacks", Icons.Filled.Bluetooth, Accent.BLUETOOTH, MenuAction.Submenu("bluetooth")),
                    MenuItem("GPS", "Data · NMEA · Tracker · POI", Icons.Filled.MyLocation, Accent.GPS, MenuAction.Submenu("gps")),
                    MenuItem("Device", "Firmware · Files · Settings · Geofences", Icons.Filled.DeveloperBoard, Accent.DEVICE, MenuAction.Submenu("device")),
                    MenuItem("Storage", "On-phone SD card (logs, captures, pages)", Icons.Filled.Folder, Accent.NEUTRAL, MenuAction.OpenStorage),
                    MenuItem("Reboot", "Restart the ESP32", Icons.Filled.RestartAlt, Accent.NEUTRAL, MenuAction.Send("reboot")),
                ),
            ),
        )

        add(
            MenuScreen(
                "recon", "Recon",
                listOf(
                    MenuItem("Recon WiFi", "Live WiFi recon dashboard", Icons.Filled.Wifi, Accent.SCANNER, MenuAction.OpenGraphic("recon_wifi")),
                    MenuItem("Recon BLE", "Live BLE recon dashboard", Icons.Filled.Bluetooth, Accent.SCANNER, MenuAction.OpenGraphic("recon_ble")),
                ),
            ),
        )

        add(
            MenuScreen(
                "wifi", "WiFi",
                listOf(
                    MenuItem("Sniffers", "Passive capture & detect", Icons.Filled.Sensors, Accent.SNIFFER, MenuAction.Submenu("wifi_sniffers")),
                    MenuItem("Scanners", "Network / port scans", Icons.Filled.Radar, Accent.SCANNER, MenuAction.Submenu("wifi_scanners")),
                    MenuItem("Attacks", "Deauth · Beacon · Portal", Icons.Filled.Bolt, Accent.ATTACK, MenuAction.Submenu("wifi_attacks")),
                    MenuItem("General", "SSIDs · APs · Join · AP · MACs", Icons.Filled.Tune, Accent.GENERAL, MenuAction.Submenu("wifi_general")),
                ),
            ),
        )
        add(
            MenuScreen(
                "wifi_sniffers", "Sniffers",
                listOf(
                    MenuItem("Probe Request Sniff", "sniffprobe", Icons.Filled.Podcasts, Accent.SNIFFER, MenuAction.Send("sniffprobe")),
                    MenuItem("Beacon Sniff", "sniffbeacon", Icons.Filled.CellTower, Accent.SNIFFER, MenuAction.Send("sniffbeacon")),
                    MenuItem("Deauth Sniff", "sniffdeauth", Icons.Filled.WifiOff, Accent.SNIFFER, MenuAction.Send("sniffdeauth")),
                    MenuItem("Remote ID", "Drone / UAV Remote ID", Icons.Filled.Radar, Accent.SNIFFER, MenuAction.Submenu("wifi_remoteid")),
                    MenuItem("Packet Count", "packetcount", Icons.Filled.Numbers, Accent.SNIFFER, MenuAction.Send("packetcount")),
                    MenuItem("EAPOL/PMKID Scan", "sniffpmkid", Icons.Filled.Key, Accent.SNIFFER, MenuAction.Send("sniffpmkid")),
                    MenuItem("Packet Monitor", "Live frame-type trends", Icons.Filled.Insights, Accent.SNIFFER, MenuAction.OpenGraphic("packet_monitor")),
                    MenuItem("Channel Analyzer", "Full-spectrum activity", Icons.Filled.BarChart, Accent.SNIFFER, MenuAction.OpenGraphic("channel_analyzer")),
                    MenuItem("Channel Summary", "Per-channel activity", Icons.Filled.BarChart, Accent.SNIFFER, MenuAction.OpenGraphic("channel_summary")),
                    MenuItem("Raw Capture", "sniffraw", Icons.Filled.GraphicEq, Accent.SNIFFER, MenuAction.Send("sniffraw")),
                    MenuItem("Detect Pwnagotchi", "sniffpwn", Icons.Filled.Pets, Accent.SNIFFER, MenuAction.Send("sniffpwn")),
                    MenuItem("Detect Pineapple", "sniffpinescan", Icons.Filled.Router, Accent.SNIFFER, MenuAction.Send("sniffpinescan")),
                    MenuItem("Detect MultiSSID", "sniffmultissid", Icons.Filled.Dns, Accent.SNIFFER, MenuAction.Send("sniffmultissid")),
                    MenuItem("Scan AP/STA", "scanall", Icons.Filled.PermScanWifi, Accent.SNIFFER, MenuAction.Send("scanall")),
                    MenuItem("Fox Hunt", "Track a target by signal strength", Icons.Filled.SettingsInputAntenna, Accent.SNIFFER, MenuAction.Submenu("wifi_fox_hunt")),
                    MenuItem("MAC Monitor", "mactrack", Icons.Filled.TrackChanges, Accent.SNIFFER, MenuAction.Send("mactrack")),
                    MenuItem("SAE Commit", "sniffsae", Icons.Filled.Lock, Accent.SNIFFER, MenuAction.Send("sniffsae")),
                    MenuItem("Wardrive", "WiFi + GPS → CSV on phone", Icons.Filled.Route, Accent.SNIFFER, MenuAction.OpenGraphic("wardrive")),
                ),
            ),
        )
        add(
            MenuScreen(
                "wifi_scanners", "Scanners",
                listOf(
                    MenuItem("Ping Scan", "pingscan", Icons.Filled.NetworkCheck, Accent.SCANNER, MenuAction.Send("pingscan")),
                    MenuItem("ARP Scan", "arpscan -f", Icons.Filled.Lan, Accent.SCANNER, MenuAction.Send("arpscan -f")),
                    MenuItem("Port Scan All", "active IPs → scan one", Icons.Filled.SettingsEthernet, Accent.SCANNER, MenuAction.PickAndRun("Port Scan All", ListType.IPS, "portscan -a -t ")),
                    MenuItem("SSH Scan", "portscan -s ssh", Icons.Filled.Terminal, Accent.SCANNER, MenuAction.Send("portscan -s ssh")),
                    MenuItem("Telnet Scan", "portscan -s telnet", Icons.AutoMirrored.Filled.Dvr, Accent.SCANNER, MenuAction.Send("portscan -s telnet")),
                    MenuItem("SMTP Scan", "portscan -s smtp", Icons.Filled.Email, Accent.SCANNER, MenuAction.Send("portscan -s smtp")),
                    MenuItem("DNS Scan", "portscan -s dns", Icons.Filled.Dns, Accent.SCANNER, MenuAction.Send("portscan -s dns")),
                    MenuItem("HTTP Scan", "portscan -s http", Icons.Filled.Http, Accent.SCANNER, MenuAction.Send("portscan -s http")),
                    MenuItem("HTTPS Scan", "portscan -s https", Icons.Filled.Https, Accent.SCANNER, MenuAction.Send("portscan -s https")),
                    MenuItem("RDP Scan", "portscan -s rdp", Icons.Filled.DesktopWindows, Accent.SCANNER, MenuAction.Send("portscan -s rdp")),
                ),
            ),
        )
        add(
            MenuScreen(
                "wifi_remoteid", "Remote ID",
                listOf(
                    MenuItem("Remote ID Scan", "Live radar", Icons.Filled.Radar, Accent.SCANNER, MenuAction.OpenGraphic("remote_id")),
                    MenuItem("Remote ID List", "remoteid list", Icons.AutoMirrored.Filled.FormatListBulleted, Accent.SCANNER, MenuAction.Send("remoteid list")),
                    MenuItem("Track a Device", "remoteid track <index>", Icons.Filled.TrackChanges, Accent.SCANNER, MenuAction.SendPrompt("remoteid track ", "Track Remote ID", "device index (from the list, e.g. 0)")),
                    MenuItem("Clear List", "remoteid clear", Icons.Filled.DeleteSweep, Accent.NEUTRAL, MenuAction.Send("remoteid clear")),
                ),
            ),
        )
        add(
            MenuScreen(
                "wifi_fox_hunt", "Fox Hunt",
                listOf(
                    MenuItem("APs", "Hunt a scanned access point", Icons.Filled.Wifi, Accent.SNIFFER, MenuAction.OpenFoxHunt("ap")),
                    MenuItem("Stations", "Hunt a scanned station", Icons.Filled.Devices, Accent.SNIFFER, MenuAction.OpenFoxHunt("station")),
                    MenuItem("WiFi Pineapples", "Detected pineapples", Icons.Filled.Router, Accent.SNIFFER, MenuAction.OpenFoxHunt("pineapple")),
                    MenuItem("MultiSSID", "Detected MultiSSID APs", Icons.Filled.Dns, Accent.SNIFFER, MenuAction.OpenFoxHunt("multissid")),
                ),
            ),
        )
        add(
            MenuScreen(
                "wifi_attacks", "Attacks",
                listOf(
                    MenuItem("Beacon Spam List", "attack -t beacon -l", Icons.Filled.Campaign, Accent.ATTACK, MenuAction.Send("attack -t beacon -l"), dangerous = true),
                    MenuItem("Beacon Spam Random", "attack -t beacon -r", Icons.Filled.Shuffle, Accent.ATTACK, MenuAction.Send("attack -t beacon -r"), dangerous = true),
                    MenuItem("Funny SSID Beacon", "attack -t funny", Icons.Filled.Mood, Accent.ATTACK, MenuAction.Send("attack -t funny"), dangerous = true),
                    MenuItem("Rick Roll Beacon", "attack -t rickroll", Icons.Filled.MusicNote, Accent.ATTACK, MenuAction.Send("attack -t rickroll"), dangerous = true),
                    MenuItem("Probe Req Flood", "attack -t probe", Icons.Filled.Podcasts, Accent.ATTACK, MenuAction.Send("attack -t probe"), dangerous = true),
                    MenuItem("Evil Portal", "Captive portal", Icons.Filled.Language, Accent.ATTACK, MenuAction.Submenu("evil_portal"), dangerous = true),
                    MenuItem("Deauth Flood", "attack -t deauth", Icons.Filled.WifiOff, Accent.ATTACK, MenuAction.Send("attack -t deauth"), dangerous = true),
                    MenuItem("AP Clone Spam", "attack -t beacon -a", Icons.Filled.ContentCopy, Accent.ATTACK, MenuAction.Send("attack -t beacon -a"), dangerous = true),
                    MenuItem("Deauth Targeted", "attack -t deauth -c", Icons.Filled.GpsFixed, Accent.ATTACK, MenuAction.Send("attack -t deauth -c"), dangerous = true),
                    MenuItem("Karma", "probe SSID list → karma", Icons.Filled.Whatshot, Accent.ATTACK, MenuAction.PickAndRun("Karma", ListType.PROBES, "karma -p "), dangerous = true),
                    MenuItem("Bad Msg", "attack -t badmsg", Icons.Filled.Warning, Accent.ATTACK, MenuAction.Send("attack -t badmsg"), dangerous = true),
                    MenuItem("Bad Msg Targeted", "attack -t badmsg -c", Icons.Filled.Warning, Accent.ATTACK, MenuAction.Send("attack -t badmsg -c"), dangerous = true),
                    MenuItem("Assoc Sleep", "attack -t sleep", Icons.Filled.Bedtime, Accent.ATTACK, MenuAction.Send("attack -t sleep"), dangerous = true),
                    MenuItem("Assoc Sleep Targ", "attack -t sleep -c", Icons.Filled.Bedtime, Accent.ATTACK, MenuAction.Send("attack -t sleep -c"), dangerous = true),
                    MenuItem("SAE Commit Flood", "attack -t sae", Icons.Filled.Lock, Accent.ATTACK, MenuAction.Send("attack -t sae"), dangerous = true),
                    MenuItem("Channel Switch", "attack -t csa", Icons.Filled.SwapHoriz, Accent.ATTACK, MenuAction.Send("attack -t csa"), dangerous = true),
                    MenuItem("Quiet Time", "attack -t quiet", Icons.AutoMirrored.Filled.VolumeOff, Accent.ATTACK, MenuAction.Send("attack -t quiet"), dangerous = true),
                ),
            ),
        )
        add(
            MenuScreen(
                "evil_portal", "Evil Portal",
                listOf(
                    MenuItem("Portal (phone HTML)", "Pick a page from the phone, no SD card", Icons.Filled.Language, Accent.ATTACK, MenuAction.OpenEvilPortal, dangerous = true),
                    MenuItem("Scan APs first", "Find a target AP to impersonate", Icons.Filled.Wifi, Accent.GENERAL, MenuAction.OpenList(ListType.ACCESS_POINTS)),
                    MenuItem("Start (SD index.html)", "evilportal -c start -w index.html", Icons.Filled.PlayCircle, Accent.ATTACK, MenuAction.Send("evilportal -c start -w index.html"), dangerous = true),
                ),
            ),
        )
        add(
            MenuScreen(
                "wifi_general", "General",
                listOf(
                    MenuItem("Generate SSIDs", "ssid -a -g 20", Icons.Filled.Shuffle, Accent.GENERAL, MenuAction.Send("ssid -a -g 20")),
                    MenuItem("Select probe SSIDs", "Captured probe SSIDs", Icons.Filled.Podcasts, Accent.GENERAL, MenuAction.OpenList(ListType.PROBES)),
                    MenuItem("Add SSIDs", "ssid -a -n <name>", Icons.Filled.Campaign, Accent.GENERAL, MenuAction.SendPrompt("ssid -a -n ", "Add SSID", "SSID name to add")),
                    MenuItem("Clear SSIDs", "clearlist -s", Icons.Filled.DeleteSweep, Accent.NEUTRAL, MenuAction.Send("clearlist -s")),
                    MenuItem("Clear APs", "clearlist -a", Icons.Filled.DeleteSweep, Accent.NEUTRAL, MenuAction.Send("clearlist -a")),
                    MenuItem("Clear Stations", "clearlist -c", Icons.Filled.DeleteSweep, Accent.NEUTRAL, MenuAction.Send("clearlist -c")),
                    MenuItem("Select EP HTML File", "Saved HTML pages", Icons.Filled.Language, Accent.GENERAL, MenuAction.OpenEpHtmlPicker),
                    MenuItem("Select APs", "AP list + Select ALL", Icons.Filled.Wifi, Accent.GENERAL, MenuAction.OpenList(ListType.ACCESS_POINTS)),
                    MenuItem("View AP Info", "AP list → info", Icons.Filled.Info, Accent.GENERAL, MenuAction.OpenList(ListType.ACCESS_POINTS)),
                    MenuItem("Select Stations", "Station list", Icons.Filled.Devices, Accent.GENERAL, MenuAction.OpenList(ListType.STATIONS)),
                    MenuItem("Join WiFi", "AP list → password", Icons.AutoMirrored.Filled.Login, Accent.GENERAL, MenuAction.PickAndRun("Join WiFi", ListType.ACCESS_POINTS, "join -a ", needsPassword = true)),
                    MenuItem("Join Saved WiFi", "join -s", Icons.AutoMirrored.Filled.Login, Accent.GENERAL, MenuAction.Send("join -s")),
                    MenuItem("Manage Saved WiFi", "Saved networks (app-side)", Icons.AutoMirrored.Filled.FormatListBulleted, Accent.GENERAL, MenuAction.OpenManageSavedWifi),
                    MenuItem("Start AP", "Phone-hosted AP", Icons.Filled.Router, Accent.GENERAL, MenuAction.OpenStartAp),
                    MenuItem("Host AP Info", "Phone AP readout", Icons.Filled.Info, Accent.GENERAL, MenuAction.OpenHostApInfo),
                    MenuItem("Set MACs", "Randomise / clone", Icons.Filled.Badge, Accent.GENERAL, MenuAction.Submenu("wifi_macs")),
                    MenuItem("Shutdown WiFi", "stopscan -f", Icons.Filled.PowerSettingsNew, Accent.GENERAL, MenuAction.Send("stopscan -f")),
                    MenuItem("Upload Wardrive Logs", "WiGLE / WDG", Icons.Filled.CloudUpload, Accent.GENERAL, MenuAction.OpenUploadLogs),
                ),
            ),
        )
        add(
            MenuScreen(
                "wifi_macs", "Set MACs",
                listOf(
                    MenuItem("Generate AP MAC", "randapmac", Icons.Filled.Shuffle, Accent.GENERAL, MenuAction.Send("randapmac")),
                    MenuItem("Generate STA MAC", "randstamac", Icons.Filled.Shuffle, Accent.GENERAL, MenuAction.Send("randstamac")),
                    MenuItem("Clone AP MAC", "cloneapmac -a <index>", Icons.Filled.ContentCopy, Accent.GENERAL, MenuAction.SendPrompt("cloneapmac -a ", "Clone AP MAC", "AP index (from the AP list, e.g. 0)")),
                    MenuItem("Clone STA MAC", "clonestamac -s <index>", Icons.Filled.ContentCopy, Accent.GENERAL, MenuAction.SendPrompt("clonestamac -s ", "Clone Station MAC", "station index (from the station list, e.g. 0)")),
                ),
            ),
        )

        add(
            MenuScreen(
                "bluetooth", "Bluetooth",
                listOf(
                    MenuItem("Sniffers", "Detect & analyze", Icons.AutoMirrored.Filled.BluetoothSearching, Accent.BLUETOOTH, MenuAction.Submenu("bt_sniffers")),
                    MenuItem("Bluetooth Attacks", "BLE spam & spoof", Icons.Filled.Bolt, Accent.ATTACK, MenuAction.Submenu("bt_attacks")),
                ),
            ),
        )
        add(
            MenuScreen(
                "bt_sniffers", "Sniffers",
                listOf(
                    MenuItem("Bluetooth Sniffer", "sniffbt", Icons.Filled.Bluetooth, Accent.BLUETOOTH, MenuAction.Send("sniffbt")),
                    MenuItem("Flipper Sniff", "sniffbt -t flipper", Icons.Filled.SmartToy, Accent.BLUETOOTH, MenuAction.Send("sniffbt -t flipper")),
                    MenuItem("FindMy Sniff", "sniffbt -t airtag", Icons.Filled.Luggage, Accent.BLUETOOTH, MenuAction.Send("sniffbt -t airtag")),
                    MenuItem("iBeacon Sniff", "sniffbt -t ibeacon", Icons.Filled.Sensors, Accent.BLUETOOTH, MenuAction.Send("sniffbt -t ibeacon")),
                    MenuItem("iBeacon Info", "Detected iBeacons + detail", Icons.Filled.Sensors, Accent.BLUETOOTH, MenuAction.OpenIBeaconInfo),
                    MenuItem("FindMy Monitor", "Live FindMy monitor", Icons.Filled.Luggage, Accent.BLUETOOTH, MenuAction.OpenFindMyMonitor),
                    MenuItem("Detect Card Skimmers", "sniffskim", Icons.Filled.CreditCard, Accent.BLUETOOTH, MenuAction.Send("sniffskim")),
                    MenuItem("Bluetooth Analyzer", "Live BLE advert rate", Icons.Filled.BarChart, Accent.BLUETOOTH, MenuAction.OpenGraphic("bluetooth_analyzer")),
                    MenuItem("Flock Sniff", "sniffbt -t flock", Icons.Filled.Videocam, Accent.BLUETOOTH, MenuAction.Send("sniffbt -t flock")),
                    MenuItem("Meta Detect", "sniffbt -t meta", Icons.Filled.Visibility, Accent.BLUETOOTH, MenuAction.Send("sniffbt -t meta")),
                    MenuItem("Fox Hunt", "Track a BLE target by signal", Icons.Filled.SettingsInputAntenna, Accent.BLUETOOTH, MenuAction.Submenu("bt_fox_hunt")),
                ),
            ),
        )
        add(
            MenuScreen(
                "bt_fox_hunt", "Fox Hunt",
                listOf(
                    MenuItem("BLE Devices", "Hunt a scanned BLE device", Icons.Filled.Bluetooth, Accent.BLUETOOTH, MenuAction.OpenFoxHunt("ble")),
                    MenuItem("FindMy", "Hunt a FindMy / AirTag", Icons.Filled.Luggage, Accent.BLUETOOTH, MenuAction.OpenFoxHunt("findmy")),
                    MenuItem("Flipper Zero", "Hunt a Flipper on the device", Icons.Filled.SmartToy, Accent.BLUETOOTH, MenuAction.OpenFoxHunt("flipper")),
                    MenuItem("iBeacons", "Hunt an iBeacon on the device", Icons.Filled.Sensors, Accent.BLUETOOTH, MenuAction.OpenFoxHunt("ibeacon")),
                    MenuItem("Meta", "Hunt a Meta device (phone radio)", Icons.Filled.Visibility, Accent.BLUETOOTH, MenuAction.OpenFoxHunt("meta")),
                    MenuItem("Flock", "Hunt a Flock camera on the device", Icons.Filled.Videocam, Accent.BLUETOOTH, MenuAction.OpenFoxHunt("flock")),
                ),
            ),
        )
        add(
            MenuScreen(
                "bt_attacks", "Bluetooth Attacks",
                listOf(
                    MenuItem("Sour Apple", "blespam -t sourapple", Icons.Filled.PhoneIphone, Accent.ATTACK, MenuAction.Send("blespam -t sourapple"), dangerous = true),
                    MenuItem("Apple Juice", "blespam -t applejuice", Icons.Filled.PhoneIphone, Accent.ATTACK, MenuAction.Send("blespam -t applejuice"), dangerous = true),
                    MenuItem("Swiftpair Spam", "blespam -t windows", Icons.Filled.DesktopWindows, Accent.ATTACK, MenuAction.Send("blespam -t windows"), dangerous = true),
                    MenuItem("Samsung BLE Spam", "blespam -t samsung", Icons.Filled.Smartphone, Accent.ATTACK, MenuAction.Send("blespam -t samsung"), dangerous = true),
                    MenuItem("Google BLE Spam", "blespam -t google", Icons.Filled.Android, Accent.ATTACK, MenuAction.Send("blespam -t google"), dangerous = true),
                    MenuItem("Flipper BLE Spam", "blespam -t flipper", Icons.Filled.SmartToy, Accent.ATTACK, MenuAction.Send("blespam -t flipper"), dangerous = true),
                    MenuItem("BLE Spam All", "blespam -t all", Icons.Filled.Campaign, Accent.ATTACK, MenuAction.Send("blespam -t all"), dangerous = true),
                    MenuItem("Spoof Airtag", "AirTag list → spoof", Icons.Filled.Luggage, Accent.ATTACK, MenuAction.PickAndRun("Spoof Airtag", ListType.AIRTAGS, "spoofat -t "), dangerous = true),
                    MenuItem("FindMy Sound", "AirTag list → play sound", Icons.Filled.Campaign, Accent.ATTACK, MenuAction.PickAndRun("FindMy Sound", ListType.AIRTAGS, "findmy -t ")),
                ),
            ),
        )

        add(
            MenuScreen(
                "gps", "GPS",
                listOf(
                    MenuItem("GPS Data", "gpsdata", Icons.Filled.MyLocation, Accent.GPS, MenuAction.Send("gpsdata")),
                    MenuItem("NMEA Stream", "nmea", Icons.Filled.Satellite, Accent.GPS, MenuAction.Send("nmea")),
                    MenuItem("GPS Tracker", "gpstracker -c start", Icons.Filled.Route, Accent.GPS, MenuAction.Send("gpstracker -c start")),
                    MenuItem("GPS POI", "Mark points of interest", Icons.Filled.PushPin, Accent.GPS, MenuAction.Submenu("gps_poi")),
                ),
            ),
        )
        add(
            MenuScreen(
                "gps_poi", "GPS POI",
                listOf(
                    MenuItem("Mark POI", "gpspoi -m", Icons.Filled.PushPin, Accent.GPS, MenuAction.Send("gpspoi -m")),
                ),
            ),
        )

        add(
            MenuScreen(
                "device", "Device",
                listOf(
                    MenuItem("Flash Firmware", "Open the web installer", Icons.Filled.SystemUpdate, Accent.DEVICE, MenuAction.OpenInstaller),
                    MenuItem("Console", "Raw serial terminal", Icons.Filled.Terminal, Accent.DEVICE, MenuAction.OpenConsole),
                    MenuItem("Backup SPIFFS", "backupspiffs", Icons.Filled.Backup, Accent.DEVICE, MenuAction.Send("backupspiffs")),
                    MenuItem("Restore SPIFFS", "restorespiffs", Icons.Filled.Restore, Accent.DEVICE, MenuAction.Send("restorespiffs"), dangerous = true),
                    MenuItem("Save/Load Files", "Save / load lists", Icons.Filled.Folder, Accent.DEVICE, MenuAction.Submenu("device_files")),
                    MenuItem("Device Info", "info", Icons.Filled.Info, Accent.DEVICE, MenuAction.Send("info")),
                    MenuItem("Settings", "Toggle device settings", Icons.Filled.Settings, Accent.DEVICE, MenuAction.OpenDeviceSettings),
                    MenuItem("Geofences", "Slots 1-5 · set / current GPS / clear", Icons.Filled.MyLocation, Accent.DEVICE, MenuAction.OpenGeofences),
                ),
            ),
        )
        add(
            MenuScreen(
                "device_files", "Save / Load Files",
                listOf(
                    MenuItem("Save SSIDs", "save -s", Icons.Filled.Folder, Accent.DEVICE, MenuAction.Send("save -s")),
                    MenuItem("Load SSIDs", "load -s", Icons.Filled.FolderOpen, Accent.DEVICE, MenuAction.Send("load -s")),
                    MenuItem("Save APs", "save -a", Icons.Filled.Folder, Accent.DEVICE, MenuAction.Send("save -a")),
                    MenuItem("Load APs", "load -a", Icons.Filled.FolderOpen, Accent.DEVICE, MenuAction.Send("load -a")),
                ),
            ),
        )
    }.associateBy { it.id }

    fun screen(id: String): MenuScreen? = screens[id]
}
