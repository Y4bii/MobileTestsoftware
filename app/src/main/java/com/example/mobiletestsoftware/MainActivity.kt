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

class MainActivity : ComponentActivity() {

    private val UDP_PORT = 5005
    private val TCP_PORT = 6000

    private var statusText by mutableStateOf("Warte auf Initialisierung...")
    private var isConnected by mutableStateOf(false)
    private var isConnecting by mutableStateOf(false)
    private var wantsConnection by mutableStateOf(false)
    private var lastResponseTime by mutableStateOf(0L)
    private var isEmergencyStopActive by mutableStateOf(false)

    private var selectedLevel by mutableStateOf("Ablaufberg")
    private var isLevelMenuExpanded by mutableStateOf(false)
    private val levels = listOf("Ablaufberg", "Obere Ebene", "Mittlere Ebene")

    private var heartbeatJob: Job? = null
    private var watchdogJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        startTcpListener()
        setContent {
            MobileTestsoftwareTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    MainLayoutWrapper(status = statusText)
                }
            }
        }
    }

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
                runOnUiThread { statusText = "Gesendet: $message" }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread { statusText = "Fehler: ${e.message}" }
            }
        }.start()
    }

    private fun sendEmergencyStop() {
        isEmergencyStopActive = !isEmergencyStopActive
        sendUdpBroadcast(if (isEmergencyStopActive) "EMERGENCY_STOP_ON" else "EMERGENCY_STOP_OFF")
    }

    private fun toggleConnection() {
        if (wantsConnection) {
            wantsConnection = false
            isConnected = false
            isConnecting = false
            stopConnectionLogic()
            statusText = "Verbindung getrennt"
        } else {
            wantsConnection = true
            lastResponseTime = System.currentTimeMillis()
            startConnectionWatchdog()
            startHeartbeat()
            triggerConnectionCheck()
        }
    }

    private fun triggerConnectionCheck() {
        if (!isConnected) isConnecting = true
        sendUdpBroadcast("PING_STATUS")
    }

    private fun stopConnectionLogic() {
        heartbeatJob?.cancel()
        watchdogJob?.cancel()
    }

    private fun startConnectionWatchdog() {
        watchdogJob?.cancel()
        watchdogJob = lifecycleScope.launch(Dispatchers.Default) {
            while (isActive) {
                val currentTime = System.currentTimeMillis()
                if (isConnected && (currentTime - lastResponseTime > 5000)) {
                    isConnected = false
                    isConnecting = false
                    runOnUiThread { statusText = "Verbindung verloren" }
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
                delay(2000)
            }
        }
    }

    private fun startTcpListener() {
        Thread {
            try {
                val serverSocket = ServerSocket(TCP_PORT)
                while (true) {
                    val client = serverSocket.accept()
                    val input = client.getInputStream().bufferedReader().readLine()
                    runOnUiThread {
                        statusText = "ACK: $input"
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

    @Composable
    fun MainLayoutWrapper(status: String) {
        val headerColor = MaterialTheme.colorScheme.surfaceVariant
        val footerColor = MaterialTheme.colorScheme.surfaceVariant

        Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
            Spacer(modifier = Modifier.fillMaxWidth().windowInsetsTopHeight(WindowInsets.statusBars).background(headerColor))

            // --- HEADER ---
            Surface(
                modifier = Modifier.weight(0.08f).fillMaxWidth(),
                color = headerColor,
            ) {
                Box(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
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
                                DropdownMenuItem(
                                    text = { Text(level, fontSize = 12.sp) },
                                    onClick = { selectedLevel = level; isLevelMenuExpanded = false }
                                )
                            }
                        }
                    }
                    Text(text = status, modifier = Modifier.align(Alignment.Center), fontSize = 12.sp)

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

            // --- MAIN CONTENT ---
            Box(modifier = Modifier.weight(0.84f).fillMaxWidth().background(Color.White)) {
                when (selectedLevel) {
                    "Ablaufberg" -> Ablaufberg(onAction = { sendUdpBroadcast(it) })
                    "Obere Ebene" -> ObereEbene(onAction = { sendUdpBroadcast(it) })
                    "Mittlere Ebene" -> MittlereEbene(onAction = { sendUdpBroadcast(it) })
                }
            }

            // --- FOOTER ---
            Surface(
                modifier = Modifier.weight(0.08f).fillMaxWidth(),
                color = footerColor,
            ) {
                Box(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
                    // Buttons an den Seiten
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(
                            onClick = { toggleConnection() },
                            colors = ButtonDefaults.buttonColors(containerColor = when { isConnected -> Color(0xFF4CAF50); isConnecting -> Color(0xFFFF9800); else -> Color(0xFFF44336) }),
                            modifier = Modifier.height(32.dp).width(150.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = if (isConnected) "CONNECTED" else if (isConnecting) "CONNECTING..." else "DISCONNECTED",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        Button(
                            onClick = { sendEmergencyStop() },
                            colors = ButtonDefaults.buttonColors(containerColor = if (isEmergencyStopActive) Color(0xFFF44336) else Color(0xFF757575)),
                            modifier = Modifier.height(32.dp).width(150.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("STOP", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                        }
                    }

                    // LEGENDE (Zentriert in der Mitte des Footers)
                    Text(
                        text = "C = Curved, S = Straight",
                        modifier = Modifier.align(Alignment.Center),
                        fontSize = 10.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.fillMaxWidth().windowInsetsBottomHeight(WindowInsets.navigationBars).background(footerColor))
        }
    }
}