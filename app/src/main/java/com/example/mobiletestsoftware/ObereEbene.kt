package com.example.mobiletestsoftware

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

// ############################################################################
// GLEISPLAN: OBERE EBENE
// ############################################################################

/**
 * Zeigt den Gleisplan der Ebene "Obere Ebene".
 * Diese Ebene verwendet IDs im 100er Bereich (z.B. B101, W101), um Konflikte mit anderen Ebenen zu vermeiden.
 *
 * @param blockStates Map mit dem aktuellen Status der Blöcke (für die Färbung Rot/Grün).
 * @param onAction    Callback zum Senden von Befehlen (z.B. "B1011") an die MainActivity.
 */
@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun ObereEbene(blockStates: Map<String, Boolean>, onAction: (String) -> Unit) {
    // BoxWithConstraints wird benötigt, um die verfügbare Breite/Höhe (maxWidth/maxHeight)
    // zu ermitteln, damit wir die Elemente prozentual positionieren können.
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {

        // 1. Hintergrundbild (Gleisplan Grafik)
        Image(
            painter = painterResource(id = R.drawable.obereebene),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp), // Kleiner Abstand nach innen
            contentScale = ContentScale.Fit, // Skaliert das Bild proportional
            alignment = Alignment.Center
        )

        // 2. Weißer Rahmen (Optische Verschönerung)
        // Dient dazu, eventuelle Schattenränder des Bildes zu überdecken.
        Box(modifier = Modifier.fillMaxSize().padding(2.dp).border(10.dp, Color.White))

        // 3. Definition der STRECKENBLÖCKE (B101 - B119)
        // Format: Triple("ID", X-Koordinate in %, Y-Koordinate in %)
        // Diese Buttons reagieren auf den 'blockStates' Status aus der MainActivity.
        val blocks = listOf(
            Triple("B101", 0.1f, 0.1f), Triple("B102", 0.1f, 0.15f),
            Triple("B103", 0.1f, 0.2f), Triple("B104", 0.1f, 0.25f),
            Triple("B105", 0.1f, 0.3f), Triple("B106", 0.1f, 0.35f),
            Triple("B107", 0.1f, 0.4f), Triple("B108", 0.1f, 0.45f),
            Triple("B109", 0.1f, 0.5f), Triple("B110", 0.1f, 0.55f),
            Triple("B111", 0.1f, 0.6f), Triple("B112", 0.1f, 0.65f),
            Triple("B113", 0.1f, 0.7f), Triple("B114", 0.1f, 0.75f),
            Triple("B115", 0.1f, 0.8f), Triple("B116", 0.1f, 0.85f),
            Triple("B117", 0.1f, 0.9f), Triple("B118", 0.2f, 0.1f),
            Triple("B119", 0.2f, 0.15f)
        )
        // Erzeugt die Block-Buttons an den entsprechenden Positionen
        blocks.forEach { (id, x, y) ->
            TrackBlock(id, x, y, maxWidth, maxHeight, blockStates, onAction)
        }

        // 4. Definition der WEICHEN (W101 - W114)
        // Diese Buttons speichern ihren visuellen Status lokal, da Weichen immer sofort schalten.
        val switches = listOf(
            Triple("W101", 0.3f, 0.1f), Triple("W102", 0.3f, 0.15f),
            Triple("W103", 0.3f, 0.2f), Triple("W104", 0.3f, 0.25f),
            Triple("W105", 0.3f, 0.3f), Triple("W106", 0.3f, 0.35f),
            Triple("W107", 0.3f, 0.4f), Triple("W108", 0.3f, 0.45f),
            Triple("W109", 0.3f, 0.5f), Triple("W110", 0.3f, 0.55f),
            Triple("W111", 0.3f, 0.6f), Triple("W112", 0.3f, 0.65f),
            Triple("W113", 0.3f, 0.7f), Triple("W114", 0.3f, 0.75f)
        )
        // Erzeugt die Weichen-Buttons
        switches.forEach { (id, x, y) ->
            TrackSwitch(id, x, y, maxWidth, maxHeight, onAction)
        }
    }
}