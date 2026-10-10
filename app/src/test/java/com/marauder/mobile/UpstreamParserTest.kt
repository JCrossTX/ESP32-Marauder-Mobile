package com.marauder.mobile

import com.marauder.mobile.protocol.UpstreamParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpstreamParserTest {

    @Test fun stripsGluedPrompt() {

        assertEquals("3 selected", UpstreamParser.stripPrompt("> 3 selected"))
        assertEquals("x", UpstreamParser.stripPrompt("> > x"))
        assertEquals("plain", UpstreamParser.stripPrompt("plain"))
    }

    @Test fun recognisesCommandEcho() {

        assertEquals("list -a", UpstreamParser.echoedCommand("#list -a"))
        assertNull(UpstreamParser.echoedCommand("[0][CH:6] Net -40"))
    }

    @Test fun parsesProtocolInfoHandshake() {
        val line = "@MARAUDER:{\"protocol\":1,\"tx\":\"H0\",\"command\":\"protocolinfo\"," +
            "\"status\":\"success\",\"code\":\"OK\",\"firmware\":\"v1.18.0\"," +
            "\"capabilities\":[\"spiffs-backup\",\"spiffs-restore\"],\"backupPath\":\"/spiffs\"}"
        val m = UpstreamParser.machine(line)
        assertNotNull(m)
        m!!
        assertEquals(1, m.protocol)
        assertEquals("H0", m.tx)
        assertEquals("protocolinfo", m.command)
        assertEquals("v1.18.0", m.firmware)
        assertTrue(m.capabilities.contains("spiffs-backup"))
    }

    @Test fun parsesMachineResultWithCounts() {
        val line = "@MARAUDER:{\"protocol\":1,\"tx\":\"t1\",\"command\":\"backupspiffs\"," +
            "\"status\":\"success\",\"code\":\"OK\",\"files\":4,\"bytes\":2048,\"rebooting\":false}"
        val m = UpstreamParser.machine(line)!!
        assertEquals("backupspiffs", m.command)
        assertEquals(4L, m.files)
        assertEquals(2048L, m.bytes)
        assertFalse(m.rebooting)
    }

    @Test fun nonMachineLineIsNull() {
        assertNull(UpstreamParser.machine("Starting Beacon sniff. Stop with stopscan"))
        assertNull(UpstreamParser.machine("@MARAUDER:{not json"))
    }

    @Test fun parsesApListRow() {

        val a = UpstreamParser.apListRow("[0][CH:6] My Home WiFi -42")!!
        assertEquals(0, a.index); assertEquals(6, a.channel)
        assertEquals("My Home WiFi", a.essid); assertEquals(-42, a.rssi)
        assertFalse(a.selected)

        val b = UpstreamParser.apListRow("[12][CH:11] Net -88 (selected)")!!
        assertEquals(12, b.index); assertEquals("Net", b.essid)
        assertEquals(-88, b.rssi); assertTrue(b.selected)
    }

    @Test fun parsesSsidListRow() {
        val s = UpstreamParser.ssidListRow("[3] FreeWifi (selected)")!!
        assertEquals(3, s.index); assertEquals("FreeWifi", s.essid); assertTrue(s.selected)
        assertEquals("plain", UpstreamParser.ssidListRow("[1] plain")!!.essid)
    }

    @Test fun parsesIpAndProbeAndAirtagRows() {
        assertEquals("192.168.1.42", UpstreamParser.ipListRow("[2] 192.168.1.42")!!.ip)
        assertEquals("nearby_ssid", UpstreamParser.probeListRow("[5] nearby_ssid")!!.essid)
        val t = UpstreamParser.airtagListRow("[1]MAC: aa:bb:cc:dd:ee:ff")!!
        assertEquals(1, t.index); assertEquals("aa:bb:cc:dd:ee:ff", t.mac)

        assertNull(UpstreamParser.airtagListRow("[2] 192.168.1.42"))
    }

    @Test fun parsesStationList() {

        assertEquals(0, UpstreamParser.staListHeaderApIndex("[0] HomeNet -40:"))
        val sta = UpstreamParser.staListRow("  [4] 11:22:33:44:55:66 (selected)", apIndex = 0)!!
        assertEquals(4, sta.index); assertEquals(0, sta.ap)
        assertEquals("11:22:33:44:55:66", sta.mac); assertTrue(sta.selected)
        assertNull(UpstreamParser.staListRow("[4] 11:22:33:44:55:66", apIndex = 0))
    }

    @Test fun parsesBleListRow() {
        val b = UpstreamParser.bleListRow("[7][RSSI:-55] Flipper abcd")!!
        assertEquals(7, b.index); assertEquals(-55, b.rssi); assertEquals("Flipper abcd", b.name)
    }

    @Test fun parsesCounts() {
        assertEquals(3, UpstreamParser.counts("3 selected")!!.selected)
        val c = UpstreamParser.counts("5 selected, 10 unselected")!!
        assertEquals(5, c.selected); assertEquals(10, c.unselected)
        assertNull(UpstreamParser.counts("random text"))
    }

    @Test fun parsesBeaconRowFromSniffbeacon() {

        val ap = UpstreamParser.beaconScanRow("-40 Ch: 6 aa:bb:cc:dd:ee:ff ESSID: My Net")!!
        assertEquals(-40, ap.rssi); assertEquals(6, ap.channel)
        assertEquals("aa:bb:cc:dd:ee:ff", ap.bssid); assertEquals("My Net", ap.essid)
    }

    @Test fun parsesBeaconRowFromScanallStrippingBeaconBytes() {

        val ap = UpstreamParser.beaconScanRow("-72 Ch: 11 00:11:22:33:44:55 ESSID: CoffeeShop 64 00 ")!!
        assertEquals("CoffeeShop", ap.essid); assertEquals("00:11:22:33:44:55", ap.bssid)
        assertEquals(11, ap.channel)
    }

    @Test fun parsesProbeScanRow() {
        val p = UpstreamParser.probeScanRow("-66 Ch: 1 Client: de:ad:be:ef:00:01 Requesting: HomeNet")!!
        assertEquals(-66, p.rssi); assertEquals("de:ad:be:ef:00:01", p.client); assertEquals("HomeNet", p.essid)
    }

    @Test fun parsesApStaLinkBothOrderings() {
        val a = UpstreamParser.apStaRow("3: ap: aa:aa:aa:aa:aa:aa -> sta: bb:bb:bb:bb:bb:bb")!!
        assertEquals("aa:aa:aa:aa:aa:aa", a.apMac); assertEquals("bb:bb:bb:bb:bb:bb", a.staMac)
        val b = UpstreamParser.apStaRow("4: sta: cc:cc:cc:cc:cc:cc -> ap: dd:dd:dd:dd:dd:dd ")!!
        assertEquals("dd:dd:dd:dd:dd:dd", b.apMac); assertEquals("cc:cc:cc:cc:cc:cc", b.staMac)
    }

    @Test fun parsesSrcDstEapolAndMacTrack() {
        val sd = UpstreamParser.srcDstRow("-50 Ch: 6 11:11:11:11:11:11 -> 22:22:22:22:22:22")!!
        assertEquals("11:11:11:11:11:11", sd.src); assertEquals("22:22:22:22:22:22", sd.dst)
        assertEquals("aa:bb:cc:dd:ee:ff", UpstreamParser.eapolRow("Received EAPOL: aa:bb:cc:dd:ee:ff"))
        val mt = UpstreamParser.macTrackRow("aa:bb:cc:dd:ee:ff Frames: 123 Last Seen: 4s")!!
        assertEquals(123L, mt.frames); assertEquals(4L, mt.lastSeenSecs)
    }

    @Test fun parsesCredential() {

        val c = UpstreamParser.cred("u: alice@example.com p: s3cr3t p@ss")!!
        assertEquals("alice@example.com", c.user); assertEquals("s3cr3t p@ss", c.pass)
        assertNull(UpstreamParser.cred("plain line"))
    }

    @Test fun parsesHardwareName() {

        assertEquals("LilyGo T-Dongle C5", UpstreamParser.hardwareName("Hardware: LilyGo T-Dongle C5"))
        assertNull(UpstreamParser.hardwareName("Something else"))
    }
}
