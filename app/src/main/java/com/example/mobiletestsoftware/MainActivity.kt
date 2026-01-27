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
    private var statusSent by mutableStateOf("-")
    private var statusReceived by mutableStateOf("-")

    private var isConnected by mutableStateOf(false)      // Haben wir aktuell eine bestätigte Verbindung?
    private var isConnecting by mutableStateOf(false)     // Versuchen wir gerade zu verbinden?
    private var wantsConnection by mutableStateOf(false)  // Hat der User "CONNECT" gedrückt?
    private var lastResponseTime by mutableStateOf(0L)    // Zeitstempel der letzten Antwort (für Watchdog)

    // --- STOP / PAUSE LOGIK & ZENTRALER STATUS ---
    // Startet standardmäßig im STOP-Modus (Sicherheit)
    private var isEmergencyStopActive by mutableStateOf(true)

    // Speichert den Zustand ALLER Blöcke zentral ("Repräsentation").
    private val blockStates = mutableStateMapOf<String, Boolean>()

    // --- UI NAVIGATION ---
    private var selectedLevel by mutableStateOf("Ablaufberg") // Aktuell sichtbare Ebene
    private var isLevelMenuExpanded by mutableStateOf(false)  // Status des Dropdown-Menüs
    private val levels = listOf("Ablaufberg", "Obere Ebene", "Mittlere Ebene")

    // --- BACKGROUND JOBS ---
    private var heartbeatJob: Job? = null
    private var watchdogJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        startTcpListener()

        setContent {
            MobileTestsoftwareTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    MainLayoutWrapper(textSent = statusSent, textReceived = statusReceived)
                }
            }
        }
    }

    // --- AUTOMATISCHER STOP BEI APP-MINIMIERUNG ---
    override fun onPause() {
        super.onPause()
        if (wantsConnection) {
            setSystemState(true)
            stopConnectionLogic()
        }
    }

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

    // ########################################################################
    // NETZWERK KOMMUNIKATION
    // ########################################################################

    fun sendUdpBroadcast(message: String) {
        Thread {
            try {
                val socket = DatagramSocket()
                socket.broadcast = true
                val data = message.toByteArray()
                val broadcastAddress = InetAddress.getByName("255.255.255.255")
                val packet = DatagramPacket(data, data.size, broadcastAddress, UDP_PORT)
                socket.send(packet)
                socket.close()
                runOnUiThread { statusSent = message }
            } catch (e: Exception) {
                runOnUiThread { statusSent = "Fehler: ${e.message}" }
            }
        }.start()
    }

    private fun startTcpListener() {
        Thread {
            try {
                val serverSocket = ServerSocket(TCP_PORT)
                while (true) {
                    val client = serverSocket.accept()
                    val input = client.getInputStream().bufferedReader().readLine()

                    runOnUiThread {
                        statusReceived = input ?: ""
                        lastResponseTime = System.currentTimeMillis()
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
     * Setzt den System-Status (START vs. STOP).
     * Diese Funktion zentralisiert die Logik für Start, Stop, Pause und Resume.
     * * @param stop true = STOP/PAUSE (Sicherheitsmodus), false = START/RUN (Live).
     */
    private fun setSystemState(stop: Boolean) {
        isEmergencyStopActive = stop

        if (isEmergencyStopActive) {
            // --- STOP MODUS ---
            // 1. Hardware sicherheitshalber stoppen
            sendUdpBroadcast("EMERGENCY_STOP_ON")
            // 2. Alle aktuell in der App aktiven Blöcke physisch ausschalten
            // (Die Variable 'blockStates' bleibt aber true -> Visuell in der App noch grün!)
            blockStates.forEach { (id, isActive) ->
                if (isActive) sendUdpBroadcast("${id}0")
            }
            statusSent = "SYSTEM GESTOPPT (Pause)"
        } else {
            // --- RUN MODUS ---
            // 1. Hardware freigeben
            sendUdpBroadcast("EMERGENCY_STOP_OFF")
            // 2. Zustand synchronisieren: Alles was in der App "Grün" ist, wird wieder an die Anlage gesendet
            blockStates.forEach { (id, active) ->
                sendUdpBroadcast("${id}${if (active) "1" else "0"}")
            }
            statusSent = "SYSTEM LÄUFT"
        }
    }

    private fun handleTrackAction(action: String) {
        val id = action.substring(0, action.length - 1)
        val state = action.last() == '1'

        if (id.startsWith("W")) {
            // Weichen immer sofort senden
            sendUdpBroadcast(action)
        } else if (id.startsWith("B")) {
            // Blöcke: Nur speichern. Senden nur, wenn System LÄUFT.
            blockStates[id] = state
            if (!isEmergencyStopActive) {
                sendUdpBroadcast(action)
            }
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
            statusSent = "Getrennt"
            statusReceived = "-"
        } else {
            // Verbinden
            wantsConnection = true
            lastResponseTime = System.currentTimeMillis()
            startConnectionWatchdog()
            startHeartbeat()
            if (!isConnected) isConnecting = true

            // Initialisierung:
            // 1. PING senden
            sendUdpBroadcast("PING_STATUS")
            // 2. Sofort in den STOP-Zustand (Basiszustand) gehen, damit Anlage sicher ist (alles aus).
            setSystemState(true)
        }
    }

    private fun stopConnectionLogic() {
        heartbeatJob?.cancel()
        watchdogJob?.cancel()
    }

    private fun startConnectionWatchdog() {
        watchdogJob?.cancel()
        watchdogJob = lifecycleScope.launch(Dispatchers.Default) {
            while (isActive) {
                if (isConnected && (System.currentTimeMillis() - lastResponseTime > 5000)) {
                    isConnected = false
                    isConnecting = false
                    runOnUiThread { statusSent = "Verbindung verloren" }
                }
                delay(1000)
            }
        }
    }

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
    fun MainLayoutWrapper(textSent: String, textReceived: String) {
        val headerColor = Color(0xFFE0E0E0)
        val footerColor = Color(0xFFE0E0E0)
        val dividerColor = Color(0xFFCCCCCC)

        Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
            // Platzhalter für Statusleiste
            Spacer(modifier = Modifier.fillMaxWidth().windowInsetsTopHeight(WindowInsets.statusBars).background(headerColor))

            // --- HEADER ---
            Surface(modifier = Modifier.weight(0.1f).fillMaxWidth(), color = headerColor) {
                Box(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {

                    // Linker Bereich: Dropdown
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

                    // --- MITTLERER BEREICH (Status) ---
                    Row(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .width(400.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("GESENDET", fontSize = 13.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(text = textSent, fontSize = 12.sp, color = Color.Black, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("EMPFANGEN", fontSize = 13.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(text = textReceived, fontSize = 12.sp, color = Color(0xFF00394A), fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }

                    // Rechter Bereich: Preset
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

            Divider(color = dividerColor, thickness = 1.dp)

            // --- MAIN CONTENT ---
            Box(modifier = Modifier.weight(0.8f).fillMaxWidth().background(Color.White)) {
                when (selectedLevel) {
                    "Ablaufberg" -> Ablaufberg(blockStates, ::handleTrackAction)
                    "Obere Ebene" -> ObereEbene(blockStates, ::handleTrackAction)
                    "Mittlere Ebene" -> MittlereEbene(blockStates, ::handleTrackAction)
                }
            }

            Divider(color = dividerColor, thickness = 1.dp)

            // --- FOOTER ---
            Surface(modifier = Modifier.weight(0.1f).fillMaxWidth(), color = footerColor) {
                Box(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(
                            onClick = { toggleConnection() },
                            colors = ButtonDefaults.buttonColors(containerColor = when { isConnected -> Color(0xFF4CAF50); isConnecting -> Color(0xFFFF9800); else -> Color(0xFFF44336) }),
                            modifier = Modifier.height(48.dp).width(200.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(text = if (isConnected) "CONNECTED" else if (isConnecting) "CONNECTING..." else "DISCONNECTED", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        // START/STOP Button nutzt jetzt die neue setSystemState Logik
                        Button(
                            onClick = { setSystemState(!isEmergencyStopActive) },
                            colors = ButtonDefaults.buttonColors(containerColor = if (isEmergencyStopActive) Color(0xFF4CAF50) else Color(0xFFF44336)),
                            modifier = Modifier.height(48.dp).width(200.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(text = if (isEmergencyStopActive) "START" else "STOP", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                        }
                    }
                    Text(text = "C = Curved, S = Straight", modifier = Modifier.align(Alignment.Center), fontSize = 13.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.fillMaxWidth().windowInsetsBottomHeight(WindowInsets.navigationBars).background(footerColor))
        }
    }
}