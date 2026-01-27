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
import androidx.compose.ui.unit.times

// ############################################################################
// GLEISPLAN: ABLAUFBERG
// ############################################################################

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun Ablaufberg(blockStates: Map<String, Boolean>, onAction: (String) -> Unit) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {

        // --- POSITIONIERUNG ---
        val padding = 10.dp

        val painter = painterResource(id = R.drawable.ablaufberg)
        val srcSize = painter.intrinsicSize
        val imageRatio = if (srcSize.height > 0) srcSize.width / srcSize.height else 1f

        val availableWidth = maxWidth - (padding * 2)
        val availableHeight = maxHeight - (padding * 2)
        val containerRatio = if (availableHeight > 0.dp) availableWidth / availableHeight else 1f

        val renderWidth: Dp
        val renderHeight: Dp
        val internalOffsetX: Dp
        val internalOffsetY: Dp

        if (containerRatio > imageRatio) {
            renderHeight = availableHeight
            renderWidth = renderHeight * imageRatio
            internalOffsetX = (availableWidth - renderWidth) / 2
            internalOffsetY = 0.dp
        } else {
            renderWidth = availableWidth
            renderHeight = renderWidth / imageRatio
            internalOffsetX = 0.dp
            internalOffsetY = (availableHeight - renderHeight) / 2
        }

        val finalOffsetX = padding + internalOffsetX
        val finalOffsetY = padding + internalOffsetY
        // ----------------------

        Image(
            painter = painter,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentScale = ContentScale.Fit,
            alignment = Alignment.Center
        )

        Box(modifier = Modifier.fillMaxSize().padding(padding).border(10.dp, Color.White))

        // Definition der STRECKENBLÖCKE
        val blocks = listOf(
            Triple("B000", 0.79f, 0.6775f),
            Triple("B001", 0.79f, 0.765f),
            Triple("B002", 0.79f, 0.8485f),
            Triple("B003", 0.79f, 0.9375f),
            Triple("B004", 0.5575f, 0.675f),
            Triple("B006", 0.23f, 0.75f),
            Triple("B007", 0.23f, 0.83f),
            Triple("B008", 0.4925f, 0.09f)
        )

        blocks.forEach { (id, x, y) ->
            TrackBlock(id, x, y, renderWidth, renderHeight, finalOffsetX, finalOffsetY, blockStates, onAction)
        }

        // Definition der WEICHEN
        val switches = listOf(
            Triple("W000", 0.63f, 0.675f),
            Triple("W001", 0.6f, 0.83f),
            Triple("W002", 0.55f, 0.88f),
            Triple("W003", 0.4925f, 0.675f),
            Triple("W004", 0.0975f, 0.61f)
        )

        switches.forEach { (id, x, y) ->
            TrackSwitch(id, x, y, renderWidth, renderHeight, finalOffsetX, finalOffsetY, onAction)
        }
    }
}

// ############################################################################
// HELFER-KOMPONENTEN
// ############################################################################

@Composable
fun TrackBlock(
    id: String,
    xPos: Float,
    yPos: Float,
    renderWidth: Dp,
    renderHeight: Dp,
    offsetX: Dp,
    offsetY: Dp,
    blockStates: Map<String, Boolean>,
    onAction: (String) -> Unit
) {
    val isActive = blockStates[id] ?: false

    val displayId = try {
        val number = id.substring(2, 4).toInt() + 1
        "${id.take(1)}${String.format("%02d", number)}"
    } catch (e: Exception) {
        id
    }

    TrackElement(xPos, yPos, renderWidth, renderHeight, offsetX, offsetY) {
        Button(
            onClick = { onAction("${id}${if (isActive) "0" else "1"}") },
            modifier = Modifier.size(40.dp),
            shape = RectangleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isActive) Color(0xFF4CAF50) else Color(0xFFF44336)
            ),
            contentPadding = PaddingValues(0.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy((-10).dp, Alignment.CenterVertically)
            ) {
                Text(displayId, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(if (isActive) "ON" else "OFF", fontSize = 10.sp, color = Color.White)
            }
        }
    }
}

@Composable
fun TrackSwitch(
    id: String,
    xPos: Float,
    yPos: Float,
    renderWidth: Dp,
    renderHeight: Dp,
    offsetX: Dp,
    offsetY: Dp,
    onAction: (String) -> Unit
) {
    var isStraight by remember { mutableStateOf(true) }

    val displayId = try {
        val number = id.substring(2, 4).toInt() + 1
        "${id.take(1)}${String.format("%02d", number)}"
    } catch (e: Exception) {
        id
    }

    TrackElement(xPos, yPos, renderWidth, renderHeight, offsetX, offsetY) {
        Button(
            onClick = {
                isStraight = !isStraight
                onAction("${id}${if (isStraight) "1" else "0"}")
            },
            modifier = Modifier.size(40.dp),
            shape = RectangleShape,
            colors = ButtonDefaults.buttonColors(
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
                    text = displayId,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isStraight) Color.White else Color.Black
                )
                Text(
                    text = if (isStraight) "S" else "C",
                    fontSize = 10.sp,
                    color = if (isStraight) Color.White else Color.Black
                )
            }
        }
    }
}

@Composable
fun TrackElement(
    xPos: Float,
    yPos: Float,
    width: Dp,
    height: Dp,
    offsetX: Dp,
    offsetY: Dp,
    content: @Composable () -> Unit
) {
    // Berechnung: Position - 20.dp (halbe Größe von 40.dp)
    Box(modifier = Modifier.offset(
        x = offsetX + (width * xPos) - 20.dp,
        y = offsetY + (height * yPos) - 20.dp
    )) {
        content()
    }
}