package com.example.mobiletestsoftware

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun Ablaufberg(onAction: (String) -> Unit) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        val screenWidth = maxWidth
        val screenHeight = maxHeight

        // Gleisplan Hintergrund
        Image(
            painter = painterResource(id = R.drawable.ablaufberg),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            contentScale = ContentScale.Fit,
            alignment = Alignment.Center
        )

        // Weißer Rahmen überdeckt Bildschatten
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(10.dp)
            .border(10.dp, Color.White)
        )

        // STRECKENBLÖCKE (B01 - B09)
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

        blocks.forEach { (id, x, y) ->
            TrackBlock(id, x, y, screenWidth, screenHeight, onAction)
        }

        // WEICHEN (W01 - W05)
        val switches = listOf(
            Triple("W001", 0.63f, 0.675f),
            Triple("W002", 0.6f, 0.83f),
            Triple("W003", 0.55f, 0.88f),
            Triple("W004", 0.4925f, 0.675f),
            Triple("W005", 0.0975f, 0.61f)
        )

        switches.forEach { (id, x, y) ->
            TrackSwitch(id, x, y, screenWidth, screenHeight, onAction)
        }
    }
}

@Composable
fun TrackBlock(
    id: String,
    xPos: Float,
    yPos: Float,
    screenWidth: Dp,
    screenHeight: Dp,
    onAction: (String) -> Unit
) {
    var active by remember { mutableStateOf(false) }

    TrackElement(xPos, yPos, screenWidth, screenHeight) {
        Button(
            onClick = {
                active = !active
                onAction("${id}${if (active) "1" else "0"}")
            },
            modifier = Modifier.size(30.dp),
            shape = RectangleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (active) Color(0xFF4CAF50) else Color(0xFFF44336)
            ),
            contentPadding = PaddingValues(0.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy((-10).dp, Alignment.CenterVertically)
            ) {
                Text(id, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(if (active) "ON" else "OFF", fontSize = 9.sp, color = Color.White)
            }
        }
    }
}

@Composable
fun TrackSwitch(
    id: String,
    xPos: Float,
    yPos: Float,
    screenWidth: Dp,
    screenHeight: Dp,
    onAction: (String) -> Unit
) {
    var isStraight by remember { mutableStateOf(true) }

    TrackElement(xPos, yPos, screenWidth, screenHeight) {
        Button(
            onClick = {
                isStraight = !isStraight
                onAction("${id}${if (isStraight) "1" else "0"}")
            },
            modifier = Modifier.size(30.dp),
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
                    text = id,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isStraight) Color.White else Color.Black
                )
                Text(
                    text = if (isStraight) "S" else "C",
                    fontSize = 9.sp,
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
    screenWidth: Dp,
    screenHeight: Dp,
    content: @Composable () -> Unit
) {
    // Offset angepasst auf die Hälfte von 30dp (15dp), damit das Element zentriert ist
    Box(modifier = Modifier.offset(
        x = screenWidth * xPos - 15.dp,
        y = screenHeight * yPos - 15.dp
    )) {
        content()
    }
}