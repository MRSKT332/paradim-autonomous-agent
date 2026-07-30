package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun PatternLockView(
    selectedPattern: List<Int>,
    onPatternChanged: (List<Int>) -> Unit,
    onPatternComplete: (List<Int>) -> Unit,
    modifier: Modifier = Modifier,
    lineColor: Color = SproutPrimary,
    nodeActiveColor: Color = SproutPrimaryBright
) {
    var currentDragNodes by remember { mutableStateOf(selectedPattern) }

    Box(
        modifier = modifier
            .size(260.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(SproutSurfaceVariant.copy(alpha = 0.5f))
            .border(1.dp, SproutBorder, RoundedCornerShape(20.dp))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            for (row in 0..2) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (col in 0..2) {
                        val nodeIndex = row * 3 + col
                        val isSelected = currentDragNodes.contains(nodeIndex)

                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) nodeActiveColor else SproutSurface)
                                .border(
                                    2.dp,
                                    if (isSelected) lineColor else SproutBorder,
                                    CircleShape
                                )
                                .clickable {
                                    val updated = if (currentDragNodes.contains(nodeIndex)) {
                                        currentDragNodes.filter { it != nodeIndex }
                                    } else {
                                        currentDragNodes + nodeIndex
                                    }
                                    currentDragNodes = updated
                                    onPatternChanged(updated)
                                    if (updated.size >= 4) {
                                        onPatternComplete(updated)
                                    }
                                }
                                .testTag("pattern_node_$nodeIndex"),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                )
                            } else {
                                Text(
                                    text = "${nodeIndex + 1}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = SproutTextMuted,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
