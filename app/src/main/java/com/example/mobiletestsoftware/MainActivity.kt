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

// Netzwerk-Logik
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.ServerSocket

class MainActivity : ComponentActivity() {

    // Konstanten für die Ports
    private val UDP_PORT = 5005
    private val TCP_PORT = 6000

    private var statusText by mutableStateOf("Warte auf Initialisierung...")

    private var heartbeatJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        startTcpListener()

        setContent {
            MobileTestsoftwareTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainLayoutWrapper(
                        status = statusText,
                        onInitialize = { sendUdpBroadcast("INITIALIZE") },
                        onSendW05 = { sendUdpBroadcast("1 5 2 1 8 W05") }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        sendUdpBroadcast("PING_STATUS")

        startHeartbeat()
    }

    override fun onPause() {
        super.onPause()
        // Heartbeat sofort stoppen, damit nichts im Hintergrund läuft
        heartbeatJob?.cancel()
    }

    private fun startHeartbeat() {
        // Sicherstellen, dass nicht zwei Jobs gleichzeitig laufen
        heartbeatJob?.cancel()

        heartbeatJob = lifecycleScope.launch(Dispatchers.IO) {
            while (isActive) {
                sendUdpBroadcast("HEARTBEAT")
                delay(10000) // 10 Sekunden warten
            }
        }
    }

    /**
     * Sendet Nachrichten per UDP-Broadcast
     */
    private fun sendUdpBroadcast(message: String) {
        Thread {
            try {
                val socket = DatagramSocket()
                socket.broadcast = true

                val data = message.toByteArray()
                val broadcastAddress = InetAddress.getByName("255.255.255.255")

                val packet = DatagramPacket(data, data.size, broadcastAddress, UDP_PORT)
                socket.send(packet)
                socket.close()

                // Update des UI-Status über den State
                runOnUiThread {
                    statusText = "UDP gesendet: $message"
                }

            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    statusText = "Fehler beim UDP-Senden: ${e.message}"
                }
            }
        }.start()
    }

    /**
     * Wartet auf TCP-Bestätigungen
     */
    private fun startTcpListener() {
        Thread {
            try {
                val serverSocket = ServerSocket(TCP_PORT)
                runOnUiThread { statusText = "TCP Listener aktiv (Port $TCP_PORT)" }

                while (true) {
                    val client = serverSocket.accept()
                    val input = client.getInputStream().bufferedReader().readLine()

                    // Update des UI-Status bei Empfang eines ACKs
                    runOnUiThread {
                        statusText = "ACK erhalten: $input"
                    }

                    client.close()
                }

            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    statusText = "TCP Fehler: ${e.message}"
                }
            }
        }.start()
    }

    /**
     * Der Wrapper mit 10/80/10 Aufteilung mit Beachtung der Systemleisten
     */
    @Composable
    fun MainLayoutWrapper(
        status: String,
        onInitialize: () -> Unit,
        onSendW05: () -> Unit
    ) {
        val headerColor = MaterialTheme.colorScheme.surfaceVariant
        val footerColor = MaterialTheme.colorScheme.surfaceVariant

        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsTopHeight(WindowInsets.statusBars)
                    .background(headerColor)
            )

            Surface(
                modifier = Modifier.weight(0.1f).fillMaxWidth(),
                color = headerColor
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("HEADER", fontWeight = FontWeight.Bold)
                }
            }

            Box(
                modifier = Modifier.weight(0.8f).fillMaxWidth()
            ) {
                TrainControlScreen(status, onInitialize, onSendW05)
            }

            Surface(
                modifier = Modifier.weight(0.1f).fillMaxWidth(),
                color = footerColor
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("FOOTER", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsBottomHeight(WindowInsets.navigationBars)
                    .background(footerColor)
            )
        }
    }

    @Composable
    fun TrainControlScreen(
        status: String,
        onInitialize: () -> Unit,
        onSendW05: () -> Unit
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(text = "Zugsteuerung", fontSize = 24.sp, style = MaterialTheme.typography.headlineMedium)

            Button(onClick = onInitialize, modifier = Modifier.fillMaxWidth()) {
                Text("Initialize")
            }

            Button(onClick = onSendW05, modifier = Modifier.fillMaxWidth()) {
                Text("WEICHE 05 schalten 1")
            }

            Spacer(modifier = Modifier.height(20.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Text(text = status, modifier = Modifier.padding(20.dp), fontSize = 18.sp)
            }
        }
    }
}