package com.marauder.mobile.data

data class LiveSpec(val list: ListType?, val continuous: Boolean, val capture: Boolean = false) {
    val isList: Boolean get() = list != null
}

object LiveClassifier {

    private val continuousHeads = setOf(
        "sniffprobe", "sniffbeacon", "sniffdeauth", "sniffpwn", "sniffpinescan",
        "sniffmultissid", "sniffpmkid", "sniffsae", "sniffraw", "sniffbt", "sniffskim",
        "scanall", "mactrack", "packetcount", "foxhunt", "remoteid",
        "attack", "blespam", "karma", "evilportal", "spoofat",
        "pingscan", "arpscan", "portscan",
        "gpsdata", "nmea", "gpstracker", "wardrive",
    )

    fun of(command: String): LiveSpec {
        val c = command.trim()
        val head = c.substringBefore(' ')
        val list = when {
            head == "scanall" || head == "sniffbeacon" -> ListType.ACCESS_POINTS
            head == "sniffprobe" -> ListType.PROBES
            head == "sniffbt" && c.contains("airtag") -> ListType.AIRTAGS
            head == "pingscan" || head == "arpscan" -> ListType.IPS
            else -> null
        }

        val capture = head.startsWith("sniff") && head != "sniffbt" && head != "sniffskim"
        return LiveSpec(list = list, continuous = head in continuousHeads, capture = capture)
    }
}
