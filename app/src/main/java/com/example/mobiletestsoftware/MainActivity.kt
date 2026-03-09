package com.example.mobiletestsoftware

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.example.mobiletestsoftware.ui.theme.MobileTestsoftwareTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.ServerSocket

/**
 * Hauptklasse der App.
 *
 * Diese Activity steuert:
 * 1. Die Benutzeroberfläche (UI) mit Jetpack Compose.
 * 2. Die Netzwerkkommunikation (UDP Broadcasts für Befehle, TCP Listener für Rückmeldungen).
 * 3. Die zentrale Logik der Modellbahn (Sicherheits-Status, Block-Verwaltung).
 */
class MainActivity : ComponentActivity() {

    // ========================================================================
    // KONFIGURATION
    // ========================================================================
    private val UDP_PORT = 5005 // Port für ausgehende Befehle (Broadcast)
    private val TCP_PORT = 6005 // Port für eingehende Bestätigungen (ACKs)

    private val IP_ADDRESS = getLocalIpAddress(); //lokale IP-Adresse erhalten

    // ========================================================================
    // STATUS-VARIABLEN (STATE)
    // ========================================================================

    // --- Netzwerk & Verbindung ---
    private var statusSent by mutableStateOf("-")       // UI-Text: Letzter gesendeter Befehl
    private var statusReceived by mutableStateOf("-")   // UI-Text: Letzte empfangene Nachricht

    private var isConnected by mutableStateOf(false)      // Status: Ist die physische Verbindung bestätigt?
    private var isConnecting by mutableStateOf(false)     // Status: Verbindungsaufbau läuft?
    private var wantsConnection by mutableStateOf(false)  // User-Intention: Wurde "CONNECT" gedrückt?
    private var lastResponseTime by mutableStateOf(0L)    // Zeitstempel für den Watchdog (Verbindungsabbruch-Erkennung)

    // --- System-Logik (Start/Stop) ---
    // true = STOP-Modus (Sicherheitshalt, keine aktiven Blöcke an der Anlage).
    // false = START-Modus (Normalbetrieb, Befehle werden live umgesetzt).
    private var isEmergencyStopActive by mutableStateOf(true)

    // Zentrale Datenhaltung ("Single Source of Truth").
    // Speichert den visuellen Zustand aller Gleis-Elemente.
    // Key: ID (z.B. "B101"), Value: true (AN/Grün/S) / false (AUS/Rot/C).
    private val blockStates = mutableStateMapOf<String, Boolean>()
    private val switchStates = mutableStateMapOf<String, Boolean>() // Zentrale Speicherung der Weichenzustände

    private var activeLiftLevel by mutableStateOf<Int?>(null)

    // --- UI Navigation ---
    private var selectedLevel by mutableStateOf("Ablaufberg") // Aktuell angezeigter Gleisplan
    private var isLevelMenuExpanded by mutableStateOf(false)  // Steuert das Dropdown-Menü
    private val levels = listOf("Ablaufberg", "Obere Ebene", "Mittlere Ebene")

    // --- Hintergrund-Jobs ---
    private var heartbeatJob: Job? = null // Job für regelmäßige Keep-Alive Signale
    private var watchdogJob: Job? = null  // Job zur Überwachung der Verbindungsqualität

    // ========================================================================
    // LIFECYCLE METHODEN
    // ========================================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Startet den TCP-Server, um Nachrichten (ACKs) der Anlage zu empfangen
        startTcpListener()

        setContent {
            MobileTestsoftwareTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    MainLayoutWrapper(textSent = statusSent, textReceived = statusReceived)
                }
            }
        }
    }

    /**
     * Wird aufgerufen, wenn die App in den Hintergrund geht (z.B. Home-Button).
     * SICHERHEITS-FEATURE: Die Anlage wird sofort gestoppt, da keine Kontrolle mehr möglich ist.
     */
    override fun onPause() {
        super.onPause()
        if (wantsConnection) {
            setSystemState(true) // Anlage stoppen (Safety Halt)
            stopConnectionLogic() // Hintergrund-Prozesse pausieren
        }
    }

    /**
     * Wird aufgerufen, wenn die App wieder in den Vordergrund kommt.
     * Versucht, die Verbindung wiederherzustellen, bleibt aber im STOP-Modus (Sicherheit).
     */
    override fun onResume() {
        super.onResume()
        if (wantsConnection) {
            statusSent = "Reaktiviere..."
            lastResponseTime = System.currentTimeMillis()
            startConnectionWatchdog()
            startHeartbeat()

            if (!isConnected) isConnecting = true
            sendUdpBroadcast("PING_STATUS")
        }
    }

    // ========================================================================
    // NETZWERK KOMMUNIKATION
    // ========================================================================

    /**
     * Sendet eine Nachricht per UDP Broadcast an alle Geräte im Netzwerk.
     * Wird für Steuerbefehle (Weichen, Blöcke, Systemstatus) verwendet.
     *
     * @param message Der zu sendende String (z.B. "W0011").
     */
    fun sendUdpBroadcast(message: String) {
        Thread {
            try {
                val socket = DatagramSocket()
                socket.broadcast = true
                val data = message.toByteArray()
                // Broadcast an alle im lokalen Subnetz (255.255.255.255)
                val broadcastAddress = InetAddress.getByName("255.255.255.255")
                val packet = DatagramPacket(data, data.size, broadcastAddress, UDP_PORT)
                socket.send(packet)
                socket.close()

                // UI-Update auf dem Hauptthread
                runOnUiThread { statusSent = message }
            } catch (e: Exception) {
                runOnUiThread { statusSent = "Fehler: ${e.message}" }
            }
        }.start()
    }

    /**
     * Startet einen TCP-Server in einem Hintergrund-Thread.
     * Wartet auf den TCP-Handshake der Anlage als Lebenszeichen.
     */
    private fun startTcpListener() {
        Thread {
            try {
                val serverSocket = ServerSocket(TCP_PORT)

                runOnUiThread {
                    statusReceived = "Warte auf Verbindung: ${getLocalIpAddress()}:$TCP_PORT"
                }

                while (true) {
                    // Blockiert, bis ein TCP-Handshake (ACK) erfolgt
                    val client = serverSocket.accept()
                    runOnUiThread {
                        statusReceived = "Handshake erfolgreich"
                        lastResponseTime = System.currentTimeMillis() // Watchdog-Reset durch Handshake

                        if (wantsConnection) {
                            if (!isConnected) {
                                forceSyncSwitches()
                            }

                            isConnected = true
                            isConnecting = false
                        }
                    }
                    // Socket sofort schließen, da kein Text-Payload erwartet wird
                    client.close()
                }
            } catch (e: Exception) { e.printStackTrace() }
        }.start()
    }


    // eigene IP Adresse herausfinden
    private fun getLocalIpAddress(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            for (intf in interfaces) {
                val addresses = intf.inetAddresses
                for (addr in addresses) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        return addr.hostAddress
                    }
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return null
    }

    // ========================================================================
    // STEUERUNGS-LOGIK (CORE)
    // ========================================================================

    /**
     * Schaltet den globalen Systemzustand zwischen START und STOP.
     *
     * @param stop
     * true (STOP): Sendet Nothalt und schaltet alle Ausgänge physisch ab.
     * Der visuelle Status in der App bleibt erhalten.
     * false (START): Hebt Nothalt auf und synchronisiert den Status aller
     * in der App aktiven Blöcke mit der Anlage.
     */
    private fun setSystemState(stop: Boolean) {
        isEmergencyStopActive = stop

        if (isEmergencyStopActive) {
            // --- STOP MODUS ---
            sendUdpBroadcast("EMERGENCY_STOP_ON")
            // Sicherheits-Logik: Alle aktiven Blöcke physisch ausschalten ("0" senden)
            blockStates.forEach { (id, isActive) ->
                if (isActive) sendUdpBroadcast("${id}0")
            }
            statusSent = "SYSTEM GESTOPPT (Pause)"
        } else {
            // --- RUN MODUS ---
            sendUdpBroadcast("EMERGENCY_STOP_OFF")
            // Resync-Logik: Alle visuellen Zustände an die Hardware senden
            blockStates.forEach { (id, active) ->
                sendUdpBroadcast("${id}${if (active) "1" else "0"}")
            }
            statusSent = "SYSTEM LÄUFT"
        }
    }

    /**
     * Verarbeitet Interaktionen mit dem Gleisplan.
     *
     * @param action Der Befehlscode (z.B. "W0011" oder "B1000").
     */
    private fun handleTrackAction(action: String) {
        // Logik für Lift
        if (action.startsWith("L")) {
            if (!isEmergencyStopActive) {
                val level = action.last().digitToInt()
                activeLiftLevel = level
                sendUdpBroadcast(action)
            }
            return
        }

        val id = action.substring(0, action.length - 1) // ID extrahieren (z.B. "B100")
        val state = action.last() == '1'               // Status extrahieren ('1' = true)

        if (id.startsWith("W") || id.startsWith("K")) {
            // WEICHEN: Zustand speichern und sofort senden
            switchStates[id] = state
            sendUdpBroadcast(action)
        } else if (id.startsWith("B")) {
            // BLÖCKE: Status wird immer gespeichert.
            blockStates[id] = state
            // Physisches Senden erfolgt nur, wenn das System NICHT gestoppt ist.
            if (!isEmergencyStopActive) {
                sendUdpBroadcast(action)
            }
        }
    }

    /**
     * Synchronisiert alle Weichenzustände der App mit der physischen Anlage.
     */
    private fun forceSyncSwitches() {
        val allSwitches = listOf(
            "W000", "W001", "W002", "W003", "W004", "K000",
            "W100", "W101", "W102", "W103", "W104", "W105", "W106", "W107", "W108", "W109", "W110", "W111", "W112", "W113", "K100", "K101", "K102", "K103",
            "W200", "W201", "W202", "W203", "W204", "W205", "W206", "W207", "W208", "W209", "W210", "W211", "K200"
        )
        Thread {
            allSwitches.forEach { id ->
                val state = switchStates[id] ?: true
                sendUdpBroadcast("${id}${if (state) "1" else "0"}")

                // Delay zwischen Nachrichten
                Thread.sleep(150)
            }
        }.start()
    }

    /**
     * Die Lift-Steuerung mit Buttons im gleichen Stil wie die restliche App.
     */
    @Composable
    fun LiftControl(activeLevel: Int?, blockStates: Map<String, Boolean>, onAction: (String) -> Unit) {
        Column(
            modifier = Modifier.fillMaxSize().padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("LIFT", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
            Spacer(modifier = Modifier.height(10.dp))

            val liftLevels = listOf(1, 2, 3)
            liftLevels.forEach { num ->
                val isActive = activeLevel == num
                // Klickbar nur wenn: nicht aktiv UND System läuft
                val canClick = !isActive && !isEmergencyStopActive

                Button(
                    onClick = { onAction("L400$num") },
                    enabled = canClick,
                    modifier = Modifier.padding(vertical = 4.dp).size(45.dp),
                    shape = RectangleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isActive) Color(0xFF4CAF50) else Color(0xFF00394A),
                        disabledContainerColor = if (isActive) Color(0xFF4CAF50) else Color(0xFF9E9E9E),
                        disabledContentColor = Color.White
                    ),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("L0$num", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Divider(modifier = Modifier.width(40.dp), color = Color.LightGray)
            Spacer(modifier = Modifier.height(20.dp))

            // Streckenblock auf dem Lift B400
            val isBlockActive = blockStates["B400"] ?: false
            Button(
                onClick = { onAction("B400${if (isBlockActive) "0" else "1"}") },
                modifier = Modifier.size(45.dp),
                shape = RectangleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isBlockActive) Color(0xFF4CAF50) else Color(0xFFF44336)
                ),
                contentPadding = PaddingValues(0.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("B400", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(if (isBlockActive) "ON" else "OFF", fontSize = 9.sp)
                }
            }
        }
    }

    // ========================================================================
    // CONNECTION WATCHDOGS & HEARTBEAT
    // ========================================================================

    /**
     * Handhabt den Klick auf den Verbinden/Trennen Button.
     */
    private fun toggleConnection() {
        if (wantsConnection) {
            // --- TRENNEN ---
            wantsConnection = false
            isConnected = false
            isConnecting = false
            stopConnectionLogic()
            statusSent = "Getrennt"
            statusReceived = "-"
        } else {
            // --- VERBINDEN ---
            wantsConnection = true
            lastResponseTime = System.currentTimeMillis()
            startConnectionWatchdog()
            startHeartbeat()
            if (!isConnected) isConnecting = true
            sendUdpBroadcast("Mobile-Testsoftware:" + IP_ADDRESS)

            setSystemState(true)
        }
    }

    private fun stopConnectionLogic() {
        heartbeatJob?.cancel()
        watchdogJob?.cancel()
    }

    /**
     * Überwacht die Verbindung. Wenn > 15 Sekunden keine Antwort kommt,
     * wird der Status auf "Verloren" gesetzt.
     */
    private fun startConnectionWatchdog() {
        watchdogJob?.cancel()
        watchdogJob = lifecycleScope.launch(Dispatchers.Default) {
            while (isActive) {
                delay(5000)

                val silenceDuration = System.currentTimeMillis() - lastResponseTime

                // Timeout erst nach 25 Sekunden (erlaubt 2 verpasste Heartbeats)
                if (isConnected && (silenceDuration > 25000)) {
                    runOnUiThread {
                        isConnected = false
                        isConnecting = false
                        wantsConnection = false
                        statusSent = "Verbindung verloren (Timeout)"

                        stopConnectionLogic()
                    }
                }
            }
        }
    }

    /**
     * Sendet alle 10 Sekunden ein Lebenszeichen an die Anlage.
     */
    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = lifecycleScope.launch(Dispatchers.IO) {
            while (isActive) {
                sendUdpBroadcast("HEARTBEAT")
                delay(10000)
            }
        }
    }

    // ========================================================================
    // UI COMPOSABLES (Layout)
    // ========================================================================

    @Composable
    fun MainLayoutWrapper(textSent: String, textReceived: String) {
        // Farbdefinitionen für das Layout (angepasst für bessere Trennung)
        val headerColor = Color(0xFFE0E0E0) // Hellgrau für Header/Footer
        val footerColor = Color(0xFFE0E0E0)
        val dividerColor = Color(0xFFCCCCCC) // Etwas dunkleres Grau für Trennlinien

        Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
            // Platzhalter für Statusleiste (Status Bar Bereich einfärben)
            Spacer(modifier = Modifier.fillMaxWidth().windowInsetsTopHeight(WindowInsets.statusBars).background(headerColor))

            // ----------------------------------------------------------------
            // HEADER BEREICH
            // Enthält: Dropdown-Menü, Status-Anzeigen (Gesendet/Empfangen), Preset-Button
            // ----------------------------------------------------------------
            Surface(modifier = Modifier.weight(0.1f).fillMaxWidth(), color = headerColor) {
                Box(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {

                    // LINKS: Dropdown Menü für Ebenen-Auswahl
                    Box(modifier = Modifier.align(Alignment.CenterStart)) {
                        Button(
                            onClick = { isLevelMenuExpanded = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00394A)),
                            modifier = Modifier.height(48.dp).width(200.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = selectedLevel,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        }
                        DropdownMenu(
                            expanded = isLevelMenuExpanded,
                            onDismissRequest = { isLevelMenuExpanded = false },
                            modifier = Modifier.width(200.dp).background(Color.White)
                        ) {
                            levels.forEach { level ->
                                DropdownMenuItem(
                                    text = {
                                        Text(text = level, fontSize = 14.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = Color.Black)
                                    },
                                    onClick = { selectedLevel = level; isLevelMenuExpanded = false }
                                )
                            }
                        }
                    }

                    // MITTE: Status Anzeige (Gesendet / Empfangen)
                    Row(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .width(400.dp), // Feste Breite für stabilen Aufbau
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Spalte 1: Gesendet
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("GESENDET", fontSize = 13.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(text = textSent, fontSize = 12.sp, color = Color.Black, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        // Spalte 2: Empfangen
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("EMPFANGEN", fontSize = 13.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(text = textReceived, fontSize = 12.sp, color = Color(0xFF00394A), fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }

                    // RECHTS: Preset Button (Platzhalter für zukünftige Funktionen)
                    Button(
                        onClick = {},
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00394A)),
                        modifier = Modifier.align(Alignment.CenterEnd).height(48.dp).width(200.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("PRESETS", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            // Trennlinie Header -> Content
            Divider(color = dividerColor, thickness = 1.dp)

            // ----------------------------------------------------------------
            // MAIN CONTENT BEREICH (Gleispläne)
            // ----------------------------------------------------------------
            Row(modifier = Modifier.weight(0.8f).fillMaxWidth()) {

                // LINKS: Lift-Bereich (Immer sichtbar)
                Surface(
                    modifier = Modifier.width(80.dp).fillMaxHeight(),
                    color = Color(0xFFF0F0F0)
                ) {
                    LiftControl(activeLiftLevel, blockStates, ::handleTrackAction)
                }

                Box(modifier = Modifier.fillMaxHeight().width(1.dp).background(dividerColor))

                // RECHTS: Gleisplan-Inhalt
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    when (selectedLevel) {
                        "Ablaufberg" -> Ablaufberg(blockStates, switchStates, ::handleTrackAction)
                        "Obere Ebene" -> ObereEbene(blockStates, switchStates, ::handleTrackAction)
                        "Mittlere Ebene" -> MittlereEbene(blockStates, switchStates, ::handleTrackAction)
                    }
                }
            }

            Divider(color = dividerColor, thickness = 1.dp)

            // ----------------------------------------------------------------
            // FOOTER BEREICH
            // Enthält: Verbindungs-Button, Start/Stop-Button, Legende
            // ----------------------------------------------------------------
            Surface(modifier = Modifier.weight(0.1f).fillMaxWidth(), color = footerColor) {
                Box(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Button: Verbindung (Connect/Disconnect)
                        Button(
                            onClick = { toggleConnection() },
                            colors = ButtonDefaults.buttonColors(containerColor = when { isConnected -> Color(0xFF4CAF50); isConnecting -> Color(0xFFFF9800); else -> Color(0xFFF44336) }),
                            modifier = Modifier.height(48.dp).width(200.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(text = if (isConnected) "CONNECTED" else if (isConnecting) "CONNECTING..." else "DISCONNECTED", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        // Button: System START / STOP (Nutzt setSystemState Logik)
                        Button(
                            onClick = { setSystemState(!isEmergencyStopActive) },
                            colors = ButtonDefaults.buttonColors(containerColor = if (isEmergencyStopActive) Color(0xFF4CAF50) else Color(0xFFF44336)),
                            modifier = Modifier.height(48.dp).width(200.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(text = if (isEmergencyStopActive) "START" else "STOP", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                        }
                    }
                    // Legende
                    Text(text = "C = Curved, S = Straight", modifier = Modifier.align(Alignment.Center), fontSize = 13.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
            // Platzhalter für Navigation Bar (unten)
            Spacer(modifier = Modifier.fillMaxWidth().windowInsetsBottomHeight(WindowInsets.navigationBars).background(footerColor))
        }
    }
}