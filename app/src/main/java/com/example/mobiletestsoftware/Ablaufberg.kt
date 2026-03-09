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

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun Ablaufberg(
    blockStates: Map<String, Boolean>,
    switchStates: Map<String, Boolean>,
    onAction: (String) -> Unit
) {
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
            Triple("B000", 0.82f, 0.69f),
            Triple("B001", 0.82f, 0.765f),
            Triple("B002", 0.82f, 0.8485f),
            Triple("B003", 0.82f, 0.92f),
            Triple("B004", 0.565f, 0.69f),
            Triple("B006", 0.21f, 0.765f),
            Triple("B007", 0.21f, 0.84f),
            Triple("B008", 0.4925f, 0.04f)
        )

        blocks.forEach { (id, x, y) ->
            TrackBlock(id, x, y, renderWidth, renderHeight, finalOffsetX, finalOffsetY, blockStates, onAction)
        }

        // Definition der WEICHEN
        val switches = listOf(
            Triple("W000", 0.64f, 0.69f),
            Triple("W001", 0.6f, 0.83f),
            Triple("W002", 0.55f, 0.88f),
            Triple("W003", 0.49f, 0.69f),
            Triple("W004", 0.07f, 0.63f),

            Triple("K000", 0.505f, 0.83f)
        )

        switches.forEach { (id, x, y) ->
            TrackSwitch(id, x, y, renderWidth, renderHeight, finalOffsetX, finalOffsetY, switchStates, onAction)
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
    switchStates: Map<String, Boolean>,
    onAction: (String) -> Unit
) {
    // Zustand wird aus der zentralen Map bezogen
    val isStraight = switchStates[id] ?: true

    val displayId = try {
        val number = id.substring(2, 4).toInt() + 1
        "${id.take(1)}${String.format("%02d", number)}"
    } catch (e: Exception) {
        id
    }

    TrackElement(xPos, yPos, renderWidth, renderHeight, offsetX, offsetY) {
        Button(
            onClick = {
                val newState = !isStraight
                onAction("${id}${if (newState) "1" else "0"}")
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
    // Berechnung der Position relativ zum Bildzentrum
    Box(modifier = Modifier.offset(
        x = offsetX + (width * xPos) - 20.dp,
        y = offsetY + (height * yPos) - 20.dp
    )) {
        content()
    }
}