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
// GLEISPLAN: MITTLERE EBENE
// ############################################################################

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun MittlereEbene(
    blockStates: Map<String, Boolean>,
    switchStates: Map<String, Boolean>,
    onAction: (String) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {

        // --- POSITIONIERUNG ---
        val padding = 10.dp

        val painter = painterResource(id = R.drawable.mittlereebene)
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

        Box(modifier = Modifier.fillMaxSize().padding(10.dp).border(10.dp, Color.White))

        // Definition der STRECKENBLÖCKE
        val blocks = listOf(
            Triple("B200", 0.985f, 0.42f), Triple("B201", 0.935f, 0.42f),
            Triple("B202", 0.71f, 0.2f), Triple("B203", 0.7425f, 0.83f),
            Triple("B204", 0.6925f, 0.85f), Triple("B205", 0.6425f, 0.83f),
            Triple("B206", 0.67f, 0.3375f), Triple("B207", 0.6325f, 0.265f),
            Triple("B208", 0.58f, 0.54f), Triple("B209", 0.48f, 0.9f),
            Triple("B210", 0.47f, 0.65f), Triple("B211", 0.47f, 0.40f),
            Triple("B212", 0.48f, 0.15f), Triple("B213", 0.25f, 0.4f),
            Triple("B214", 0.12f, 0.4f), Triple("B215", 0.15f, 0.9f),
            Triple("B216", 0.17f, 0.12f), Triple("B217", 0.4f, 0.17f),
            Triple("B218", 0.38f, 0.86f), Triple("B219", 0.695f, 0.515f)
        )

        blocks.forEach { (id, x, y) ->
            TrackBlock(id, x, y, renderWidth, renderHeight, finalOffsetX, finalOffsetY, blockStates, onAction)
        }

        // Definition der WEICHEN
        val switches = listOf(
            Triple("W200", 0.68f, 0.94f), Triple("W201", 0.77f, 0.94f),
            Triple("W202", 0.7375f, 0.0225f), Triple("W203", 0.6925f, 0.0225f),
            Triple("W204", 0.645f, 0.0225f), Triple("W205", 0.71f, 0.12f),
            Triple("W206", 0.28f, 0.09f), Triple("W207", 0.28f, 0.94f),
            Triple("W208", 0.45f, 0.74f), Triple("W209", 0.28f, 0.09f),
            Triple("W210", 0.28f, 0.94f), Triple("W211", 0.685f, 0.41f),

            Triple("K200", 0.49f, 0.52f)
        )

        switches.forEach { (id, x, y) ->
            TrackSwitch(id, x, y, renderWidth, renderHeight, finalOffsetX, finalOffsetY, switchStates, onAction)
        }
    }
}