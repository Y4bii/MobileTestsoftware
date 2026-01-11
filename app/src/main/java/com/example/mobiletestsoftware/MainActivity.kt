package com.example.mobiletestsoftware

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mobiletestsoftware.ui.theme.MobileTestsoftwareTheme

// Netzwerk-Logik
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.ServerSocket

class MainActivity : ComponentActivity() {

    // Konstanten für die Ports
    private val UDP_PORT = 5005
    private val TCP_PORT = 6000

    // Ein "State", der den Text für das Frontend speichert.
    // Wenn sich dieser Wert ändert, aktualisiert Compose automatisch die Anzeige.
    private var statusText by mutableStateOf("Warte auf Initialisierung...")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Startet den Empfang von Bestätigungen im Hintergrund
        startTcpListener()

        setContent {
            // Nutzt das vorbereitete Theme
            MobileTestsoftwareTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Dein neues Frontend
                    TrainControlScreen(
                        status = statusText,
                        onInitialize = { sendUdpBroadcast("Mobile Testapplication") },
                        onSendW05 = { sendUdpBroadcast("1 5 2 1 8 W05") }
                    )
                }
            }
        }
    }

    /**
     * Das UI-Layout in Jetpack Compose.
     * Es ersetzt die alte activity_main.xml.
     */
    @Composable
    fun TrainControlScreen(
        status: String,
        onInitialize: () -> Unit,
        onSendW05: () -> Unit
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Zugsteuerung",
                fontSize = 24.sp,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            // Button zum Initialisieren
            Button(
                onClick = onInitialize,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Initialize")
            }

            // Button für Weiche 05
            Button(
                onClick = onSendW05,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("WEICHE 05 schalten 1")
            }

            // Platzhalter für weitere Buttons, die du noch designen willst
            OutlinedButton(
                onClick = { /* Hier Logik für Button 2 ergänzen */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("WEICHE 05 schalten 2")
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Die Statusanzeige
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    text = status,
                    modifier = Modifier.padding(20.dp),
                    fontSize = 18.sp
                )
            }
        }
    }

    /**
     * Sendet Nachrichten per UDP-Broadcast (Logik deines Partners)
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
     * Wartet auf TCP-Bestätigungen (Logik deines Partners)
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
}