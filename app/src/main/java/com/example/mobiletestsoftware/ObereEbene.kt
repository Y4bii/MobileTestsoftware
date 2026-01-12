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
fun ObereEbene(onAction: (String) -> Unit) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        val screenWidth = maxWidth
        val screenHeight = maxHeight

        // Gleisplan Hintergrund
        Image(
            painter = painterResource(id = R.drawable.obereebene),
            contentDescription = "Gleisplan",
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            contentScale = ContentScale.Fit,
            alignment = Alignment.Center
        )

        // Weißer Rahmen überdeckt Bildschatten
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(2.dp)
                .border(width = 10.dp, color = Color.White)
        )

        // STRECKENBLÖCKE (B01 - B19)
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

        blocks.forEach { (id, x, y) ->
            TrackBlock(id, x, y, screenWidth, screenHeight, onAction)
        }

        // WEICHEN (W01 - W14)
        val switches = listOf(
            Triple("W01", 0.3f, 0.1f), Triple("W02", 0.3f, 0.15f),
            Triple("W03", 0.3f, 0.2f), Triple("W04", 0.3f, 0.25f),
            Triple("W05", 0.3f, 0.3f), Triple("W06", 0.3f, 0.35f),
            Triple("W07", 0.3f, 0.4f), Triple("W08", 0.3f, 0.45f),
            Triple("W09", 0.3f, 0.5f), Triple("W10", 0.3f, 0.55f),
            Triple("W11", 0.3f, 0.6f), Triple("W12", 0.3f, 0.65f),
            Triple("W13", 0.3f, 0.7f), Triple("W14", 0.3f, 0.75f)
        )

        switches.forEach { (id, x, y) ->
            TrackSwitch(id, x, y, screenWidth, screenHeight, onAction)
        }
    }
}