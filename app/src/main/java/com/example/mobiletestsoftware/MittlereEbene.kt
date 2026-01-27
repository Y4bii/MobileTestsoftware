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
import androidx.compose.ui.unit.times

// ############################################################################
// GLEISPLAN: MITTLERE EBENE
// ############################################################################

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun MittlereEbene(blockStates: Map<String, Boolean>, onAction: (String) -> Unit) {
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

        val blocks = listOf(
            Triple("B200", 0.1f, 0.1f), Triple("B201", 0.1f, 0.15f),
            Triple("B202", 0.1f, 0.2f), Triple("B203", 0.1f, 0.25f),
            Triple("B204", 0.1f, 0.3f), Triple("B205", 0.1f, 0.35f),
            Triple("B206", 0.1f, 0.4f), Triple("B207", 0.1f, 0.45f),
            Triple("B208", 0.1f, 0.5f), Triple("B209", 0.1f, 0.55f),
            Triple("B210", 0.1f, 0.6f), Triple("B211", 0.1f, 0.65f),
            Triple("B212", 0.1f, 0.7f), Triple("B213", 0.1f, 0.75f),
            Triple("B214", 0.1f, 0.8f), Triple("B215", 0.1f, 0.85f),
            Triple("B216", 0.1f, 0.9f), Triple("B217", 0.2f, 0.1f),
            Triple("B218", 0.2f, 0.15f), Triple("B219", 0.2f, 0.2f)
        )

        blocks.forEach { (id, x, y) ->
            TrackBlock(id, x, y, renderWidth, renderHeight, finalOffsetX, finalOffsetY, blockStates, onAction)
        }

        val switches = listOf(
            Triple("W200", 0.4f, 0.1f), Triple("W201", 0.4f, 0.15f),
            Triple("W202", 0.4f, 0.2f), Triple("W203", 0.4f, 0.25f),
            Triple("W204", 0.4f, 0.3f), Triple("W205", 0.4f, 0.35f),
            Triple("W206", 0.4f, 0.4f), Triple("W207", 0.4f, 0.45f),
            Triple("W208", 0.4f, 0.5f), Triple("W209", 0.4f, 0.55f),
            Triple("W210", 0.4f, 0.6f), Triple("W211", 0.4f, 0.65f),

            Triple("K200", 0.6f, 0.1f)

        )

        switches.forEach { (id, x, y) ->
            TrackSwitch(id, x, y, renderWidth, renderHeight, finalOffsetX, finalOffsetY, onAction)
        }
    }
}