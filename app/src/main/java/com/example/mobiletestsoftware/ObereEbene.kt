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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ############################################################################
// GLEISPLAN: OBERE EBENE
// ############################################################################

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun ObereEbene(blockStates: Map<String, Boolean>, onAction: (String) -> Unit) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {

        // --- POSITIONIERUNG ---
        val padding = 10.dp

        val painter = painterResource(id = R.drawable.obereebene)
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

        Box(modifier = Modifier.fillMaxSize().padding(2.dp).border(10.dp, Color.White))

        val blocks = listOf(
            Triple("B100", 0.2f, 0.45f), Triple("B101", 0.38f, 0.45f),
            Triple("B102", 0.64f, 0.45f), Triple("B103", 0.87f, 0.45f),
            Triple("B104", 0.51f, 0.6f), Triple("B105", 0.51f, 0.7f),
            Triple("B106", 0.51f, 0.8f), Triple("B107", 0.51f, 0.88f),
            Triple("B108", 0.51f, 0.95f), Triple("B109", 0.51f, 0.1f),
            Triple("B110", 0.51f, 0.2f), Triple("B111", 0.49f, 0.38f),
            Triple("B112", 0.04f, 0.7f), Triple("B113", 0.07f, 0.45f),
            Triple("B114", 0.13f, 0.45f), Triple("B115", 0.92f, 0.45f),
            Triple("B116", 0.83f, 0.79f), Triple("B117", 0.83f, 0.88f),
            Triple("B118", 0.83f, 0.1f)
        )

        blocks.forEach { (id, x, y) ->
            TrackBlock(id, x, y, renderWidth, renderHeight, finalOffsetX, finalOffsetY, blockStates, onAction)
        }

        val switches = listOf(
            Triple("W100", 0.28f, 0.3f), Triple("W101", 0.42f, 0.31f),
            Triple("W102", 0.75f, 0.3f), Triple("W103", 0.76f, 0.61f),
            Triple("W104", 0.28f, 0.61f), Triple("W105", 0.74f, 0.69f),
            Triple("W106", 0.19f, 0.62f), Triple("W107", 0.33f, 0.1f),
            Triple("W108", 0.71f, 0.1f), Triple("W109", 0.96f, 0.28f),
            Triple("W110", 0.96f, 0.67f), Triple("W111", 0.94f, 0.84f),
            Triple("W112", 0.64f, 0.92f), Triple("W113", 0.22f, 0.92f),

            Triple("K100", 0.6f, 0.31f),
            Triple("K101", 0.36f, 0.21f),
            Triple("K102", 0.64f, 0.2f),
            Triple("K103", 0.68f, 0.8f)
        )

        switches.forEach { (id, x, y) ->
            TrackSwitch(id, x, y, renderWidth, renderHeight, finalOffsetX, finalOffsetY, onAction)
        }
    }
}