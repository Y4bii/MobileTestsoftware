package com.example.mobiletestsoftware

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
import java.net.InetAddress
import java.net.ServerSocket

/**
 * Hauptklasse der App.
 * Steuert die UI, die Netzwerkverbindung (UDP/TCP) und den Logik-Status der Modellbahn.
 */
class MainActivity : ComponentActivity() {

    // --- KONFIGURATION ---
    private val UDP_PORT = 5005 // Port für ausgehende Befehle (Broadcast)
    private val TCP_PORT = 6000 // Port für eingehende Bestätigungen (ACKs)

    // --- VERBINDUNGS-STATUS ---
    private var statusText by mutableStateOf("Warte auf Initialisierung...") // Text in der Header-Leiste
    private var isConnected by mutableStateOf(false)      // Haben wir aktuell eine bestätigte Verbindung?
    private var isConnecting by mutableStateOf(false)     // Versuchen wir gerade zu verbinden?
    private var wantsConnection by mutableStateOf(false)  // Hat der User "CONNECT" gedrückt?
    private var lastResponseTime by mutableStateOf(0L)    // Zeitstempel der letzten Antwort (für Watchdog)

    // --- STOP / PAUSE LOGIK & ZENTRALER STATUS ---
    // true = System ist im PAUSE-Modus (Sicherheits-Halt).
    // false = System läuft (Befehle werden live gesendet).
    private var isEmergencyStopActive by mutableStateOf(true)

    // Speichert den Zustand ALLER Blöcke zentral ("Repräsentation").
    // Key: ID (z.B. "B101"), Value: true (AN) / false (AUS)
    private val blockStates = mutableStateMapOf<String, Boolean>()

    // --- UI NAVIGATION ---
    private var selectedLevel by mutableStateOf("Ablaufberg") // Aktuell sichtbare Ebene
    private var isLevelMenuExpanded by mutableStateOf(false)  // Status des Dropdown-Menüs
    private val levels = listOf("Ablaufberg", "Obere Ebene", "Mittlere Ebene")

    // --- BACKGROUND JOBS ---
    private var heartbeatJob: Job? = null // Sendet regelmäßig "Ich bin da" Signale
    private var watchdogJob: Job? = null  // Prüft, ob die Anlage noch antwortet

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Startet den TCP-Server im Hintergrund, um Nachrichten der Anlage zu empfangen
        startTcpListener()

        setContent {
            MobileTestsoftwareTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    MainLayoutWrapper(status = statusText)
                }
            }
        }
    }

    // ########################################################################
    // LIFECYCLE MANAGEMENT
    // Sorgt dafür, dass die Verbindung sauber pausiert, wenn die App minimiert wird.
    // ########################################################################

    override fun onPause() {
        super.onPause()
        // App geht in den Hintergrund -> Jobs stoppen, um Akku/Daten zu sparen
        // und Timeouts zu verhindern, während das Handy in der Tasche ist.
        if (wantsConnection) {
            stopConnectionLogic()
        }
    }

    override fun onResume() {
        super.onResume()
        // App kommt zurück -> Wenn Verbindung gewünscht war, sofort wiederherstellen.
        if (wantsConnection) {
            statusText = "Reaktiviere Verbindung..."
            lastResponseTime = System.currentTimeMillis() // Watchdog-Timer resetten

            // Verbindungswächter und Heartbeat neu starten
            startConnectionWatchdog()
            startHeartbeat()

            // Sofort prüfen, ob die Anlage erreichbar ist
            if (!isConnected) isConnecting = true
            sendUdpBroadcast("PING_STATUS")
        }
    }

    // ########################################################################
    // NETZWERK KOMMUNIKATION
    // ########################################################################

    /**
     * Sendet ein UDP-Paket an alle Geräte im Netzwerk (Broadcast).
     * Wird für Steuerbefehle verwendet.
     */
    fun sendUdpBroadcast(message: String) {
        Thread {
            try {
                val socket = DatagramSocket()
                socket.broadcast = true
                val data = message.toByteArray()
                // 255.255.255.255 erreicht alle Geräte im lokalen Subnetz
                val broadcastAddress = InetAddress.getByName("255.255.255.255")
                val packet = DatagramPacket(data, data.size, broadcastAddress, UDP_PORT)
                socket.send(packet)
                socket.close()
                runOnUiThread { statusText = "Gesendet: $message" }
            } catch (e: Exception) {
                runOnUiThread { statusText = "Fehler: ${e.message}" }
            }
        }.start()
    }

    /**
     * Lauscht auf eingehende TCP-Verbindungen von der Anlage (z.B. ACKs).
     * Läuft in einem separaten Thread, um die UI nicht zu blockieren.
     */
    private fun startTcpListener() {
        Thread {
            try {
                val serverSocket = ServerSocket(TCP_PORT)
                while (true) {
                    // Wartet auf eingehende Verbindung (blockierend)
                    val client = serverSocket.accept()
                    val input = client.getInputStream().bufferedReader().readLine()

                    // Update auf dem UI-Thread
                    runOnUiThread {
                        statusText = "ACK: $input"
                        lastResponseTime = System.currentTimeMillis() // Lebenszeichen empfangen!
                        if (wantsConnection) {
                            isConnected = true
                            isConnecting = false
                        }
                    }
                    client.close()
                }
            } catch (e: Exception) { e.printStackTrace() }
        }.start()
    }

    // ########################################################################
    // STEUERUNGS-LOGIK (CORE)
    // ########################################################################

    /**
     * Verarbeitet Klicks auf Elemente im Gleisplan.
     * Unterscheidet zwischen Weichen (sofort) und Blöcken (abhängig vom Status).
     */
    private fun handleTrackAction(action: String) {
        val id = action.substring(0, action.length - 1) // z.B. "B101"
        val state = action.last() == '1'               // z.B. true (AN)

        if (id.startsWith("W")) {
            // WEICHEN: Werden IMMER sofort gesendet, egal ob Pause aktiv ist.
            sendUdpBroadcast(action)
        } else if (id.startsWith("B")) {
            // BLÖCKE: Zustand wird IMMER in der App gespeichert (Repräsentation).
            blockStates[id] = state

            // Befehl wird NUR gesendet, wenn das System LÄUFT (nicht in Pause ist).
            if (!isEmergencyStopActive) {
                sendUdpBroadcast(action)
            }
        }
    }

    /**
     * Schaltet zwischen START (Betrieb) und STOP (Pause/Sicherheitsmodus) um.
     * Synchronisiert beim Starten die App-Daten mit der Anlage.
     */
    private fun toggleSystemState() {
        isEmergencyStopActive = !isEmergencyStopActive

        if (isEmergencyStopActive) {
            // --- MODUS: STOP / PAUSE ---
            // 1. Signal senden
            sendUdpBroadcast("EMERGENCY_STOP_ON")
            // 2. Alle aktuell aktiven Blöcke physisch ausschalten (Sicherheit)
            // Die Anzeige in der App bleibt aber "Grün", damit man den Plan noch sieht.
            blockStates.forEach { (id, isActive) ->
                if (isActive) sendUdpBroadcast("${id}0")
            }
            statusText = "SYSTEM GESTOPPT (Pause)"
        } else {
            // --- MODUS: START / RUN ---
            // 1. Signal senden
            sendUdpBroadcast("EMERGENCY_STOP_OFF")
            // 2. Synchronisation: Alle in der App eingestellten Zustände an die Anlage senden.
            blockStates.forEach { (id, active) ->
                sendUdpBroadcast("${id}${if (active) "1" else "0"}")
            }
            statusText = "SYSTEM LÄUFT"
        }
    }

    // ########################################################################
    // CONNECTION WATCHDOGS
    // ########################################################################

    private fun toggleConnection() {
        if (wantsConnection) {
            // Trennen
            wantsConnection = false
            isConnected = false
            isConnecting = false
            stopConnectionLogic()
            statusText = "Verbindung getrennt"
        } else {
            // Verbinden
            wantsConnection = true
            lastResponseTime = System.currentTimeMillis()
            startConnectionWatchdog()
            startHeartbeat()
            if (!isConnected) isConnecting = true
            sendUdpBroadcast("PING_STATUS")
        }
    }

    private fun stopConnectionLogic() {
        heartbeatJob?.cancel()
        watchdogJob?.cancel()
    }

    /**
     * Prüft regelmäßig, ob wir noch Antworten von der Anlage erhalten.
     * Wenn > 5 Sekunden keine Antwort -> Status auf "Verloren" setzen.
     */
    private fun startConnectionWatchdog() {
        watchdogJob?.cancel()
        watchdogJob = lifecycleScope.launch(Dispatchers.Default) {
            while (isActive) {
                if (isConnected && (System.currentTimeMillis() - lastResponseTime > 5000)) {
                    isConnected = false
                    isConnecting = false
                    runOnUiThread { statusText = "Verbindung verloren" }
                }
                delay(1000)
            }
        }
    }

    /**
     * Sendet alle 10 Sekunden ein Heartbeat-Signal, damit die Verbindung offen bleibt.
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

    // ########################################################################
    // UI COMPOSABLES
    // ########################################################################

    @Composable
    fun MainLayoutWrapper(status: String) {
        val headerColor = MaterialTheme.colorScheme.surfaceVariant
        val footerColor = MaterialTheme.colorScheme.surfaceVariant

        Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
            // Platzhalter für Statusleiste
            Spacer(modifier = Modifier.fillMaxWidth().windowInsetsTopHeight(WindowInsets.statusBars).background(headerColor))

            // --- HEADER (Ebenen-Auswahl & Status) ---
            Surface(modifier = Modifier.weight(0.08f).fillMaxWidth(), color = headerColor) {
                Box(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
                    // Linker Bereich: Dropdown Menü
                    Box(modifier = Modifier.align(Alignment.CenterStart)) {
                        Button(
                            onClick = { isLevelMenuExpanded = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00394A)),
                            modifier = Modifier.height(32.dp).width(150.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(text = selectedLevel, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        DropdownMenu(expanded = isLevelMenuExpanded, onDismissRequest = { isLevelMenuExpanded = false }) {
                            levels.forEach { level ->
                                DropdownMenuItem(text = { Text(level, fontSize = 12.sp) }, onClick = { selectedLevel = level; isLevelMenuExpanded = false })
                            }
                        }
                    }
                    // Mitte: Status Text
                    Text(text = status, modifier = Modifier.align(Alignment.Center), fontSize = 12.sp)

                    // Rechts: Preset Button (Platzhalter)
                    Button(
                        onClick = {},
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00394A)),
                        modifier = Modifier.align(Alignment.CenterEnd).height(32.dp).width(150.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("PRESETS", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // --- MAIN CONTENT (Gleispläne) ---
            Box(modifier = Modifier.weight(0.84f).fillMaxWidth().background(Color.White)) {
                // Lädt die entsprechende Ebene basierend auf der Auswahl
                // Übergibt die blockStates (Anzeige) und handleTrackAction (Logik)
                when (selectedLevel) {
                    "Ablaufberg" -> Ablaufberg(blockStates, ::handleTrackAction)
                    "Obere Ebene" -> ObereEbene(blockStates, ::handleTrackAction)
                    "Mittlere Ebene" -> MittlereEbene(blockStates, ::handleTrackAction)
                }
            }

            // --- FOOTER (Control Buttons) ---
            Surface(modifier = Modifier.weight(0.08f).fillMaxWidth(), color = footerColor) {
                Box(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Linker Button: Verbindung
                        Button(
                            onClick = { toggleConnection() },
                            colors = ButtonDefaults.buttonColors(containerColor = when { isConnected -> Color(0xFF4CAF50); isConnecting -> Color(0xFFFF9800); else -> Color(0xFFF44336) }),
                            modifier = Modifier.height(32.dp).width(150.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(text = if (isConnected) "CONNECTED" else if (isConnecting) "CONNECTING..." else "DISCONNECTED", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        // Rechter Button: START / STOP
                        // Grün (START) wenn System gestoppt ist. Rot (STOP) wenn System läuft.
                        Button(
                            onClick = { toggleSystemState() },
                            colors = ButtonDefaults.buttonColors(containerColor = if (isEmergencyStopActive) Color(0xFF4CAF50) else Color(0xFFF44336)),
                            modifier = Modifier.height(32.dp).width(150.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(text = if (isEmergencyStopActive) "START" else "STOP", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                        }
                    }
                    // Legende in der Mitte
                    Text(text = "C = Curved, S = Straight", modifier = Modifier.align(Alignment.Center), fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.fillMaxWidth().windowInsetsBottomHeight(WindowInsets.navigationBars).background(footerColor))
        }
    }
}