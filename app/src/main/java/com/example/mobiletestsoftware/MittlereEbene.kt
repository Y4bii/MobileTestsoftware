package com.example.mobiletestsoftware

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun MittlereEbene(onAction: (String) -> Unit) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        val screenWidth = maxWidth
        val screenHeight = maxHeight

        // Gleisplan Hintergrund
        Image(
            painter = painterResource(id = R.drawable.mittlereebene),
            contentDescription = "Gleisplan",
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            contentScale = ContentScale.Fit,
            alignment = Alignment.Center
        )

        // Weißer Rahmen (überdeckt Bildschatten)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp)
                .border(width = 10.dp, color = Color.White)
        )

        // STRECKENBLÖCKE (B01 - B20)
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

        blocks.forEach { (id, x, y) ->
            TrackBlock(id, x, y, screenWidth, screenHeight, onAction)
        }

        // WEICHEN (W01 - W12)
        val switches = listOf(
            Triple("W201", 0.4f, 0.1f), Triple("W202", 0.4f, 0.15f),
            Triple("W203", 0.4f, 0.2f), Triple("W204", 0.4f, 0.25f),
            Triple("W205", 0.4f, 0.3f), Triple("W206", 0.4f, 0.35f),
            Triple("W207", 0.4f, 0.4f), Triple("W208", 0.4f, 0.45f),
            Triple("W209", 0.4f, 0.5f), Triple("W210", 0.4f, 0.55f),
            Triple("W211", 0.4f, 0.6f), Triple("W212", 0.4f, 0.65f)
        )

        switches.forEach { (id, x, y) ->
            TrackSwitch(id, x, y, screenWidth, screenHeight, onAction)
        }
    }
}