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
// GLEISPLAN: MITTLERE EBENE
// ############################################################################

/**
 * Zeigt den Gleisplan der Ebene "Mittlere Ebene" an.
 * Diese Ebene nutzt IDs im 200er Bereich (z.B. B201, W201), um sie von den anderen Ebenen zu unterscheiden.
 *
 * @param blockStates Map mit dem aktuellen Status der Blöcke (für die Färbung Rot/Grün).
 * @param onAction    Callback zum Senden von Befehlen an die MainActivity.
 */
@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun MittlereEbene(blockStates: Map<String, Boolean>, onAction: (String) -> Unit) {
    // BoxWithConstraints liefert uns 'maxWidth' und 'maxHeight'.
    // Das ist entscheidend, um die Buttons relativ (in %) zur Bildschirmgröße zu positionieren.
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {

        // 1. Hintergrundbild (Gleisplan)
        Image(
            painter = painterResource(id = R.drawable.mittlereebene),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp), // Abstand, damit Bild nicht am Displayrand klebt
            contentScale = ContentScale.Fit, // Skaliert das Bild passend in den Bereich
            alignment = Alignment.Center
        )

        // 2. Weißer Rahmen (Optik)
        // Überdeckt unsaubere Ränder oder Schatten des Bildes.
        Box(modifier = Modifier.fillMaxSize().padding(10.dp).border(10.dp, Color.White))

        // 3. Definition der STRECKENBLÖCKE (B201 - B220)
        // Format: Triple("ID", X-Koordinate in %, Y-Koordinate in %)
        // Diese Blöcke zeigen den Status aus 'blockStates' an (Rot/Grün).
        val blocks = listOf(
            Triple("B201", 0.1f, 0.1f), Triple("B202", 0.1f, 0.15f),
            Triple("B203", 0.1f, 0.2f), Triple("B204", 0.1f, 0.25f),
            Triple("B205", 0.1f, 0.3f), Triple("B206", 0.1f, 0.35f),
            Triple("B207", 0.1f, 0.4f), Triple("B208", 0.1f, 0.45f),
            Triple("B209", 0.1f, 0.5f), Triple("B210", 0.1f, 0.55f),
            Triple("B211", 0.1f, 0.6f), Triple("B212", 0.1f, 0.65f),
            Triple("B213", 0.1f, 0.7f), Triple("B214", 0.1f, 0.75f),
            Triple("B215", 0.1f, 0.8f), Triple("B216", 0.1f, 0.85f),
            Triple("B217", 0.1f, 0.9f), Triple("B218", 0.2f, 0.1f),
            Triple("B219", 0.2f, 0.15f), Triple("B220", 0.2f, 0.2f)
        )
        // Erzeugt die Buttons an den berechneten Positionen
        blocks.forEach { (id, x, y) ->
            TrackBlock(id, x, y, maxWidth, maxHeight, blockStates, onAction)
        }

        // 4. Definition der WEICHEN (W201 - W212)
        // Diese Buttons speichern ihren visuellen Status (Gerade/Abzweig) selbst.
        val switches = listOf(
            Triple("W201", 0.4f, 0.1f), Triple("W202", 0.4f, 0.15f),
            Triple("W203", 0.4f, 0.2f), Triple("W204", 0.4f, 0.25f),
            Triple("W205", 0.4f, 0.3f), Triple("W206", 0.4f, 0.35f),
            Triple("W207", 0.4f, 0.4f), Triple("W208", 0.4f, 0.45f),
            Triple("W209", 0.4f, 0.5f), Triple("W210", 0.4f, 0.55f),
            Triple("W211", 0.4f, 0.6f), Triple("W212", 0.4f, 0.65f)
        )
        // Erzeugt die Weichen-Buttons
        switches.forEach { (id, x, y) ->
            TrackSwitch(id, x, y, maxWidth, maxHeight, onAction)
        }
    }
}