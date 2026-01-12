package com.example.mobiletestsoftware

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ############################################################################
// GLEISPLAN: ABLAUFBERG
// ############################################################################

/**
 * Zeigt den Gleisplan der Ebene "Ablaufberg" an.
 *
 * @param blockStates Eine Map mit dem aktuellen Zustand aller Blöcke (true = AN, false = AUS).
 * Wird benötigt, um die Buttons grün oder rot zu färben.
 * @param onAction    Callback-Funktion, die aufgerufen wird, wenn ein Button gedrückt wird.
 * Sendet die ID (z.B. "B001") und den gewünschten Status an die MainActivity.
 */
@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun Ablaufberg(blockStates: Map<String, Boolean>, onAction: (String) -> Unit) {
    // BoxWithConstraints gibt uns Zugriff auf 'maxWidth' und 'maxHeight' des Bildschirms,
    // was wir für die prozentuale Positionierung der Buttons brauchen.
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {

        // 1. Hintergrundbild laden
        Image(
            painter = painterResource(id = R.drawable.ablaufberg),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp), // Kleiner Abstand zum Rand
            contentScale = ContentScale.Fit, // Bild passt sich an, ohne verzerrt zu werden
            alignment = Alignment.Center
        )

        // 2. Weißer Rahmen (überdeckt eventuelle Schatten des Bildes am Rand)
        Box(modifier = Modifier.fillMaxSize().padding(10.dp).border(10.dp, Color.White))

        // 3. Definition der STRECKENBLÖCKE
        // Triple Format: (ID, X-Position in %, Y-Position in %)
        // 0.0f = Ganz links/oben, 1.0f = Ganz rechts/unten
        val blocks = listOf(
            Triple("B001", 0.79f, 0.6775f),
            Triple("B002", 0.79f, 0.765f),
            Triple("B003", 0.79f, 0.8485f),
            Triple("B004", 0.79f, 0.9375f),
            Triple("B005", 0.5575f, 0.675f),
            Triple("B007", 0.23f, 0.75f),
            Triple("B008", 0.23f, 0.83f),
            Triple("B009", 0.4925f, 0.09f)
        )

        // Erzeugt für jeden Eintrag in der Liste einen Button an der richtigen Stelle
        blocks.forEach { (id, x, y) ->
            TrackBlock(id, x, y, maxWidth, maxHeight, blockStates, onAction)
        }

        // 4. Definition der WEICHEN
        val switches = listOf(
            Triple("W001", 0.63f, 0.675f),
            Triple("W002", 0.6f, 0.83f),
            Triple("W003", 0.55f, 0.88f),
            Triple("W004", 0.4925f, 0.675f),
            Triple("W005", 0.0975f, 0.61f)
        )

        switches.forEach { (id, x, y) ->
            TrackSwitch(id, x, y, maxWidth, maxHeight, onAction)
        }
    }
}

// ############################################################################
// HELFER-KOMPONENTEN (BUTTONS)
// ############################################################################

/**
 * Ein Button für einen Streckenblock (Signal/Strom).
 *
 * WICHTIG: Dieser Button speichert seinen Zustand NICHT selbst (kein 'remember').
 * Er liest den Zustand aus der 'blockStates'-Map der MainActivity.
 * Grund: Nur so können wir im Pause-Modus Änderungen anzeigen, ohne sie sofort an die Anlage zu senden.
 */
@Composable
fun TrackBlock(
    id: String,
    xPos: Float, // 0.0 bis 1.0
    yPos: Float, // 0.0 bis 1.0
    screenWidth: Dp,
    screenHeight: Dp,
    blockStates: Map<String, Boolean>, // Zentraler Speicher
    onAction: (String) -> Unit
) {
    // Ist dieser Block in der zentralen Map als "AN" markiert? (Default: false)
    val isActive = blockStates[id] ?: false

    TrackElement(xPos, yPos, screenWidth, screenHeight) {
        Button(
            onClick = {
                // Wir senden ID und den GEGENTEILIGEN Status (Toggle).
                // Die MainActivity entscheidet dann:
                // - Läuft das System? -> Sofort UDP senden.
                // - Ist Pause? -> Nur in der Map speichern (Button wird grün, Anlage bleibt aus).
                onAction("${id}${if (isActive) "0" else "1"}")
            },
            modifier = Modifier.size(30.dp), // Feste Größe für alle Buttons
            shape = RectangleShape,
            colors = ButtonDefaults.buttonColors(
                // Grün bei AN, Rot bei AUS
                containerColor = if (isActive) Color(0xFF4CAF50) else Color(0xFFF44336)
            ),
            contentPadding = PaddingValues(0.dp)
        ) {
            // Textinhalt des Buttons
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy((-10).dp, Alignment.CenterVertically)
            ) {
                Text(id, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(if (isActive) "ON" else "OFF", fontSize = 8.sp, color = Color.White)
            }
        }
    }
}

/**
 * Ein Button für eine Weiche.
 *
 * Im Gegensatz zu Blöcken wird der visuelle Status hier lokal ('remember') gespeichert,
 * da Weichen in deiner Logik immer sofort geschaltet werden (auch im Pause-Modus).
 */
@Composable
fun TrackSwitch(
    id: String,
    xPos: Float,
    yPos: Float,
    screenWidth: Dp,
    screenHeight: Dp,
    onAction: (String) -> Unit
) {
    // Lokaler Status für die Anzeige (Gerade vs. Abzweig)
    var isStraight by remember { mutableStateOf(true) }

    TrackElement(xPos, yPos, screenWidth, screenHeight) {
        Button(
            onClick = {
                isStraight = !isStraight
                // Weichen senden immer sofort (siehe MainActivity Logik)
                onAction("${id}${if (isStraight) "1" else "0"}")
            },
            modifier = Modifier.size(30.dp),
            shape = RectangleShape,
            colors = ButtonDefaults.buttonColors(
                // Dunkelblau bei Gerade (S), Orange/Gelb bei Curve (C)
                containerColor = if (isStraight) Color(0xFF00394A) else Color(0xFFFFB300)
            ),
            contentPadding = PaddingValues(0.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy((-10).dp, Alignment.CenterVertically)
            ) {
                Text(
                    text = id,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isStraight) Color.White else Color.Black
                )
                Text(
                    text = if (isStraight) "S" else "C", // S = Straight, C = Curved
                    fontSize = 9.sp,
                    color = if (isStraight) Color.White else Color.Black
                )
            }
        }
    }
}

/**
 * Ein Wrapper, der ein Element an einer absoluten Position auf dem Bildschirm platziert.
 * Rechnet prozentuale Koordinaten (0.1) in Pixel um.
 */
@Composable
fun TrackElement(
    xPos: Float,
    yPos: Float,
    screenWidth: Dp,
    screenHeight: Dp,
    content: @Composable () -> Unit
) {
    // Berechnung: Bildschirmbreite * Prozentwert
    // -15.dp Offset sorgt dafür, dass die Mitte des 30dp-Buttons genau auf dem Punkt liegt.
    Box(modifier = Modifier.offset(
        x = screenWidth * xPos - 15.dp,
        y = screenHeight * yPos - 15.dp
    )) {
        content()
    }
}