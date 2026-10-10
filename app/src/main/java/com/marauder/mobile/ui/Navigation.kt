package com.marauder.mobile.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.marauder.mobile.data.Catalog
import com.marauder.mobile.data.ListType
import com.marauder.mobile.data.MenuAction
import com.marauder.mobile.data.MenuItem
import com.marauder.mobile.ui.screens.AboutScreen
import com.marauder.mobile.ui.screens.ApiKeysScreen
import com.marauder.mobile.ui.screens.BluetoothAnalyzerScreen
import com.marauder.mobile.ui.screens.ChannelAnalyzerScreen
import com.marauder.mobile.ui.screens.ChannelSummaryScreen
import com.marauder.mobile.ui.screens.CommandListScreen
import com.marauder.mobile.ui.screens.ConnectScreen
import com.marauder.mobile.ui.screens.ConsoleScreen
import com.marauder.mobile.ui.screens.DeviceSettingsScreen
import com.marauder.mobile.ui.screens.GeofencesScreen
import com.marauder.mobile.ui.screens.EpHtmlPickerScreen
import com.marauder.mobile.ui.screens.EvilPortalScreen
import com.marauder.mobile.ui.screens.FindMyMonitorScreen
import com.marauder.mobile.ui.screens.FoxHuntScreen
import com.marauder.mobile.ui.screens.FoxHuntTargetListScreen
import com.marauder.mobile.ui.screens.HostApInfoScreen
import com.marauder.mobile.ui.screens.IBeaconScreen
import com.marauder.mobile.ui.screens.ListScreen
import com.marauder.mobile.ui.screens.ManageSavedWifiScreen
import com.marauder.mobile.ui.screens.LiveActivityScreen
import com.marauder.mobile.ui.screens.MenuScreen
import com.marauder.mobile.ui.screens.PacketMetric
import com.marauder.mobile.ui.screens.RemoteIdRadarScreen
import com.marauder.mobile.ui.screens.PacketMonitorScreen
import com.marauder.mobile.ui.screens.ReconLiveScreen
import com.marauder.mobile.ui.screens.StartApScreen
import com.marauder.mobile.ui.screens.StorageScreen
import com.marauder.mobile.ui.screens.UploadLogsScreen
import com.marauder.mobile.ui.screens.WardriveScreen
import com.marauder.mobile.ui.theme.CatDevice
import com.marauder.mobile.ui.theme.Danger
import com.marauder.mobile.ui.theme.Success
import com.marauder.mobile.ui.screens.UploadProgressDialog
import com.marauder.mobile.usb.UsbSerialManager
import com.marauder.mobile.vm.MarauderViewModel

private object Routes {
    const val CONNECT = "connect"
    const val MENU = "menu"
    const val LIST = "list"
    const val CONSOLE = "console"
    const val LIVE = "live"
    const val ABOUT = "about"
    const val STORAGE = "storage"
    const val EVIL_PORTAL = "evil_portal_screen"
    const val API_KEYS = "api_keys"
    const val FOX_HUNT = "fox_hunt"
    const val PACKET_MONITOR = "packet_monitor"
    const val CHANNEL_ANALYZER = "channel_analyzer"
    const val CHANNEL_SUMMARY = "channel_summary"
    const val BLUETOOTH_ANALYZER = "bluetooth_analyzer"
    const val REMOTE_ID_RADAR = "remote_id_radar"
    const val UPLOAD_LOGS = "upload_logs"
    const val IBEACON_INFO = "ibeacon_info"
    const val START_AP = "start_ap"
    const val HOST_AP_INFO = "host_ap_info"
    const val MANAGE_SAVED_WIFI = "manage_saved_wifi"
    const val EP_HTML_PICKER = "ep_html_picker"
    const val FOX_HUNT_LIST = "fox_hunt_list"
    const val STREAM_FOX_HUNT = "stream_fox_hunt"
    const val NATIVE_FOX_HUNT = "native_fox_hunt"
    const val PICK = "pick"
    const val DEVICE_SETTINGS = "device_settings"
    const val GEOFENCES = "geofences_screen"
    const val FINDMY_MONITOR = "findmy_monitor"
    const val WARDRIVE = "wardrive_screen"
    const val RECON_LIVE = "recon_live"
    fun menu(id: String) = "$MENU/$id"
    fun list(type: ListType) = "$LIST/${type.name}"
    fun live(title: String, command: String) = "$LIVE/${Uri.encode(title)}/${Uri.encode(command)}"
    fun foxHunt(command: String) = "$FOX_HUNT/${Uri.encode(command)}"
    fun foxHuntList(category: String) = "$FOX_HUNT_LIST/${Uri.encode(category)}"
    fun streamFoxHunt(category: String, key: String, name: String) =
        "$STREAM_FOX_HUNT/${Uri.encode(category)}/${Uri.encode(key)}/${Uri.encode(name)}"
    fun nativeFoxHunt(mac: String, name: String) =
        "$NATIVE_FOX_HUNT/${Uri.encode(mac)}/${Uri.encode(name)}"
    fun pick(title: String, list: ListType, prefix: String, needsPw: Boolean) =
        "$PICK/${Uri.encode(title)}/${list.name}/${Uri.encode(prefix)}/$needsPw"
    fun reconLive(ble: Boolean) = "$RECON_LIVE/$ble"
}

@Composable
fun AppRoot() {
    val vm: MarauderViewModel = viewModel()
    val nav = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        vm.snackbar.collect { snackbarHostState.showSnackbar(it) }
    }

    ConnectionGate(vm, nav)

    Box(Modifier.fillMaxSize()) {
        NavHost(navController = nav, startDestination = Routes.CONNECT) {

            composable(Routes.CONNECT) {
                ConnectScreen(vm, onOpenInstaller = { openInstaller(context) })
            }

            composable(
                route = "${Routes.MENU}/{id}",
                arguments = listOf(navArgument("id") { type = NavType.StringType }),
            ) { entry ->
                val id = entry.arguments?.getString("id") ?: Catalog.ROOT
                MenuScreen(
                    screenId = id,
                    vm = vm,
                    onItemClick = { item -> handleAction(item, nav, vm, context) },
                    onBack = { nav.popBackStack() },
                    onOpenConsole = { nav.navigate(Routes.CONSOLE) },
                    onOpenAbout = { nav.navigate(Routes.ABOUT) },
                    onDisconnect = { vm.disconnect() },
                )
            }

            composable(
                route = "${Routes.LIST}/{type}",
                arguments = listOf(navArgument("type") { type = NavType.StringType }),
            ) { entry ->
                val type = runCatching { ListType.valueOf(entry.arguments?.getString("type").orEmpty()) }
                    .getOrDefault(ListType.ACCESS_POINTS)
                ListScreen(type = type, vm = vm, onBack = { nav.popBackStack() })
            }

            composable(Routes.CONSOLE) {
                ConsoleScreen(vm = vm, onBack = { nav.popBackStack() })
            }

            composable(Routes.ABOUT) {
                AboutScreen(onBack = { nav.popBackStack() })
            }

            composable(Routes.STORAGE) {
                StorageScreen(vm = vm, onBack = { nav.popBackStack() })
            }

            composable(Routes.EVIL_PORTAL) {
                EvilPortalScreen(vm = vm, onBack = { nav.popBackStack() })
            }

            composable(Routes.API_KEYS) {
                val keys by vm.apiKeysUi.collectAsState()
                ApiKeysScreen(
                    wigleName = keys.wigleName,
                    wigleKey = keys.wigleKey,
                    wigleSaved = keys.wigleSaved,
                    wdgKey = keys.wdgKey,
                    wdgSaved = keys.wdgSaved,
                    onBack = { nav.popBackStack() },
                    onWigleNameChange = vm::onWigleNameChange,
                    onWigleKeyChange = vm::onWigleKeyChange,
                    onSaveWigle = vm::saveWigle,
                    onClearWigle = vm::clearWigle,
                    onWdgKeyChange = vm::onWdgKeyChange,
                    onSaveWdg = vm::saveWdg,
                    onClearWdg = vm::clearWdg,
                )
            }

            composable(
                route = "${Routes.LIVE}/{title}/{cmd}",
                arguments = listOf(
                    navArgument("title") { type = NavType.StringType },
                    navArgument("cmd") { type = NavType.StringType },
                ),
            ) { entry ->
                val title = Uri.decode(entry.arguments?.getString("title").orEmpty())
                val command = Uri.decode(entry.arguments?.getString("cmd").orEmpty())
                LiveActivityScreen(title = title, command = command, vm = vm, onBack = { nav.popBackStack() })
            }

            composable(
                route = "${Routes.FOX_HUNT}/{cmd}",
                arguments = listOf(navArgument("cmd") { type = NavType.StringType }),
            ) { entry ->
                val command = Uri.decode(entry.arguments?.getString("cmd").orEmpty())
                val fox by vm.foxHunt.collectAsState()
                LaunchedEffect(command) { vm.startFoxHunt(command) }
                DisposableEffect(Unit) { onDispose { vm.stopFoxHunt() } }
                FoxHuntScreen(
                    targetName = fox?.targetName.orEmpty(),
                    rssi = fox?.rssi ?: 0,
                    channel = fox?.channel,
                    acquired = fox?.acquired == true,
                    onBack = { nav.popBackStack() },
                )
            }

            composable(Routes.UPLOAD_LOGS) {
                UploadLogsScreen(
                    vm = vm,
                    onBack = { nav.popBackStack() },
                    onApiKeys = { nav.navigate(Routes.API_KEYS) },
                )
            }

            composable(Routes.REMOTE_ID_RADAR) {
                val rid by vm.remoteId.collectAsState()
                LaunchedEffect(Unit) { vm.startRemoteIdRadar() }
                DisposableEffect(Unit) { onDispose { vm.stopRemoteIdRadar() } }
                RemoteIdRadarScreen(
                    devices = rid.devices,
                    channel = rid.channel,
                    phoneLat = rid.phoneLat,
                    phoneLon = rid.phoneLon,
                    onBack = { nav.popBackStack() },
                )
            }

            composable(Routes.BLUETOOTH_ANALYZER) {
                val bta by vm.btAnalyzer.collectAsState()
                LaunchedEffect(Unit) { vm.startBluetoothAnalyzer() }
                DisposableEffect(Unit) { onDispose { vm.stopBluetoothAnalyzer() } }
                BluetoothAnalyzerScreen(
                    spikes = bta.spikes,
                    topRssi = bta.topRssi,
                    topName = bta.topName,
                    onBack = { nav.popBackStack() },
                )
            }

            composable(Routes.CHANNEL_ANALYZER) {
                val cs by vm.channelSummary.collectAsState()
                LaunchedEffect(Unit) { vm.startChannelSummary() }
                DisposableEffect(Unit) { onDispose { vm.stopChannelSummary() } }
                ChannelAnalyzerScreen(activity = cs.activity, onBack = { nav.popBackStack() })
            }

            composable(Routes.CHANNEL_SUMMARY) {
                val cs by vm.channelSummary.collectAsState()
                LaunchedEffect(Unit) { vm.startChannelSummary() }
                DisposableEffect(Unit) { onDispose { vm.stopChannelSummary() } }
                ChannelSummaryScreen(
                    activity = cs.activity,
                    current = cs.current,
                    page = cs.page,
                    onPageDown = { vm.channelSummaryPage(-1) },
                    onPageUp = { vm.channelSummaryPage(1) },
                    onBack = { nav.popBackStack() },
                )
            }

            composable(Routes.PACKET_MONITOR) {
                val pm by vm.packetMonitor.collectAsState()
                LaunchedEffect(Unit) { vm.startPacketMonitor() }
                DisposableEffect(Unit) { onDispose { vm.stopPacketMonitor() } }
                PacketMonitorScreen(
                    metrics = listOf(
                        PacketMetric("BCN", "Beacons", Success, pm.beacon.lastOrNull() ?: 0, (pm.beacon.maxOrNull() ?: 1).coerceAtLeast(1), pm.beacon),
                        PacketMetric("DEA", "Deauths", Danger, pm.deauth.lastOrNull() ?: 0, (pm.deauth.maxOrNull() ?: 1).coerceAtLeast(1), pm.deauth),
                        PacketMetric("PRB", "Probe req", CatDevice, pm.probe.lastOrNull() ?: 0, (pm.probe.maxOrNull() ?: 1).coerceAtLeast(1), pm.probe),
                    ),
                    channel = pm.channel,
                    onChannelDown = { vm.packetMonitorChannel(-1) },
                    onChannelUp = { vm.packetMonitorChannel(1) },
                    onBack = { nav.popBackStack() },
                )
            }

            composable(Routes.IBEACON_INFO) {
                IBeaconScreen(vm = vm, onBack = { nav.popBackStack() })
            }

            composable(Routes.START_AP) {
                StartApScreen(vm = vm, onBack = { nav.popBackStack() })
            }

            composable(Routes.HOST_AP_INFO) {
                HostApInfoScreen(vm = vm, onBack = { nav.popBackStack() })
            }

            composable(Routes.MANAGE_SAVED_WIFI) {
                ManageSavedWifiScreen(vm = vm, onBack = { nav.popBackStack() })
            }

            composable(Routes.EP_HTML_PICKER) {
                EpHtmlPickerScreen(vm = vm, onBack = { nav.popBackStack() })
            }

            composable(Routes.DEVICE_SETTINGS) {
                DeviceSettingsScreen(vm = vm, onBack = { nav.popBackStack() })
            }

            composable(Routes.GEOFENCES) {
                GeofencesScreen(vm = vm, onBack = { nav.popBackStack() })
            }

            composable(Routes.FINDMY_MONITOR) {
                FindMyMonitorScreen(vm = vm, onBack = { nav.popBackStack() })
            }

            composable(Routes.WARDRIVE) {
                WardriveScreen(vm = vm, onBack = { nav.popBackStack() })
            }

            composable(
                route = "${Routes.RECON_LIVE}/{ble}",
                arguments = listOf(navArgument("ble") { type = NavType.StringType }),
            ) { entry ->
                val ble = entry.arguments?.getString("ble") == "true"
                ReconLiveScreen(ble = ble, vm = vm, onBack = { nav.popBackStack() })
            }

            composable(
                route = "${Routes.FOX_HUNT_LIST}/{category}",
                arguments = listOf(navArgument("category") { type = NavType.StringType }),
            ) { entry ->
                val category = Uri.decode(entry.arguments?.getString("category").orEmpty())
                FoxHuntTargetListScreen(
                    category = category,
                    vm = vm,
                    onPickDevice = { command -> nav.navigate(Routes.foxHunt(command)) },
                    onPickStream = { cat, key, name -> nav.navigate(Routes.streamFoxHunt(cat, key, name)) },
                    onPickNative = { mac, name -> nav.navigate(Routes.nativeFoxHunt(mac, name)) },
                    onBack = { nav.popBackStack() },
                )
            }

            composable(
                route = "${Routes.PICK}/{title}/{list}/{prefix}/{needsPw}",
                arguments = listOf(
                    navArgument("title") { type = NavType.StringType },
                    navArgument("list") { type = NavType.StringType },
                    navArgument("prefix") { type = NavType.StringType },
                    navArgument("needsPw") { type = NavType.StringType },
                ),
            ) { entry ->
                val title = Uri.decode(entry.arguments?.getString("title").orEmpty())
                val listType = runCatching { ListType.valueOf(entry.arguments?.getString("list").orEmpty()) }
                    .getOrDefault(ListType.ACCESS_POINTS)
                val prefix = Uri.decode(entry.arguments?.getString("prefix").orEmpty())
                val needsPw = entry.arguments?.getString("needsPw") == "true"
                CommandListScreen(
                    title = title,
                    listType = listType,
                    commandPrefix = prefix,
                    needsPassword = needsPw,
                    vm = vm,
                    onRun = { cmd -> nav.navigate(Routes.live(title, cmd)) },
                    onBack = { nav.popBackStack() },
                )
            }

            composable(
                route = "${Routes.STREAM_FOX_HUNT}/{category}/{key}/{name}",
                arguments = listOf(
                    navArgument("category") { type = NavType.StringType },
                    navArgument("key") { type = NavType.StringType },
                    navArgument("name") { type = NavType.StringType },
                ),
            ) { entry ->
                val category = Uri.decode(entry.arguments?.getString("category").orEmpty())
                val key = Uri.decode(entry.arguments?.getString("key").orEmpty())
                val name = Uri.decode(entry.arguments?.getString("name").orEmpty())
                val fox by vm.foxHunt.collectAsState()
                LaunchedEffect(key) { vm.startBtStreamFoxHunt(category, key, name) }
                DisposableEffect(Unit) { onDispose { vm.stopBtStreamFoxHunt() } }
                FoxHuntScreen(
                    targetName = fox?.targetName.orEmpty(),
                    rssi = fox?.rssi ?: 0,
                    channel = fox?.channel,
                    acquired = fox?.acquired == true,
                    onBack = { nav.popBackStack() },
                )
            }

            composable(
                route = "${Routes.NATIVE_FOX_HUNT}/{mac}/{name}",
                arguments = listOf(
                    navArgument("mac") { type = NavType.StringType },
                    navArgument("name") { type = NavType.StringType },
                ),
            ) { entry ->
                val mac = Uri.decode(entry.arguments?.getString("mac").orEmpty())
                val name = Uri.decode(entry.arguments?.getString("name").orEmpty())
                val fox by vm.foxHunt.collectAsState()
                LaunchedEffect(mac) { vm.startNativeFoxHunt(mac, name) }
                DisposableEffect(Unit) { onDispose { vm.stopNativeFoxHunt() } }
                FoxHuntScreen(
                    targetName = fox?.targetName.orEmpty(),
                    rssi = fox?.rssi ?: 0,
                    channel = fox?.channel,
                    acquired = fox?.acquired == true,
                    onBack = { nav.popBackStack() },
                )
            }
        }

        val upload by vm.upload.collectAsState()
        UploadProgressDialog(upload = upload, onDismiss = vm::dismissUpload)

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun ConnectionGate(vm: MarauderViewModel, nav: NavHostController) {
    val status by vm.status.collectAsState()
    val backStackEntry by nav.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    LaunchedEffect(status, currentRoute) {
        when (status) {
            UsbSerialManager.Status.CONNECTED -> {
                if (currentRoute == Routes.CONNECT) {
                    nav.navigate(Routes.menu(Catalog.ROOT)) {
                        popUpTo(Routes.CONNECT) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }
            UsbSerialManager.Status.DISCONNECTED -> {
                if (currentRoute != null && currentRoute != Routes.CONNECT) {
                    nav.navigate(Routes.CONNECT) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }
            else -> {}
        }
    }
}

private fun handleAction(
    item: MenuItem,
    nav: NavHostController,
    vm: MarauderViewModel,
    context: Context,
) {
    when (val action = item.action) {
        is MenuAction.Submenu -> nav.navigate(Routes.menu(action.id))
        is MenuAction.Send ->
            if (action.command.startsWith("foxhunt")) nav.navigate(Routes.foxHunt(action.command))
            else nav.navigate(Routes.live(item.title, action.command))

        is MenuAction.SendPrompt -> {}
        is MenuAction.OpenList -> {
            vm.refreshList(action.type)
            nav.navigate(Routes.list(action.type))
        }
        MenuAction.OpenConsole -> nav.navigate(Routes.CONSOLE)
        MenuAction.OpenEvilPortal -> nav.navigate(Routes.EVIL_PORTAL)
        MenuAction.OpenStorage -> nav.navigate(Routes.STORAGE)
        MenuAction.OpenInstaller -> openInstaller(context)
        MenuAction.OpenApiKeys -> nav.navigate(Routes.API_KEYS)
        is MenuAction.UploadWardrive -> vm.uploadWardrive(action.service)
        is MenuAction.OpenGraphic -> when (action.kind) {
            "packet_monitor" -> nav.navigate(Routes.PACKET_MONITOR)
            "channel_analyzer" -> nav.navigate(Routes.CHANNEL_ANALYZER)
            "channel_summary" -> nav.navigate(Routes.CHANNEL_SUMMARY)
            "bluetooth_analyzer" -> nav.navigate(Routes.BLUETOOTH_ANALYZER)
            "remote_id" -> nav.navigate(Routes.REMOTE_ID_RADAR)
            "wardrive" -> nav.navigate(Routes.WARDRIVE)
            "recon_wifi" -> nav.navigate(Routes.reconLive(false))
            "recon_ble" -> nav.navigate(Routes.reconLive(true))
            else -> {}
        }
        MenuAction.OpenUploadLogs -> nav.navigate(Routes.UPLOAD_LOGS)
        MenuAction.OpenIBeaconInfo -> nav.navigate(Routes.IBEACON_INFO)
        MenuAction.OpenStartAp -> nav.navigate(Routes.START_AP)
        MenuAction.OpenHostApInfo -> nav.navigate(Routes.HOST_AP_INFO)
        MenuAction.OpenManageSavedWifi -> nav.navigate(Routes.MANAGE_SAVED_WIFI)
        MenuAction.OpenEpHtmlPicker -> nav.navigate(Routes.EP_HTML_PICKER)
        is MenuAction.OpenFoxHunt -> nav.navigate(Routes.foxHuntList(action.category))
        is MenuAction.PickAndRun -> nav.navigate(Routes.pick(action.title, action.list, action.commandPrefix, action.needsPassword))
        MenuAction.OpenDeviceSettings -> nav.navigate(Routes.DEVICE_SETTINGS)
        MenuAction.OpenGeofences -> nav.navigate(Routes.GEOFENCES)
        MenuAction.OpenFindMyMonitor -> nav.navigate(Routes.FINDMY_MONITOR)
    }
}

private fun openInstaller(context: Context) {
    try {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(Catalog.INSTALLER_URL))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "No browser found to open the installer", Toast.LENGTH_LONG).show()
    }
}
