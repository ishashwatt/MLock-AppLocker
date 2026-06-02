package com.example

import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.unit.dp

import androidx.compose.ui.layout.onSizeChanged

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PatternLockView(modifier: Modifier = Modifier, onPatternComplete: (String) -> Unit) {
    var selectedDots by remember { mutableStateOf<List<Int>>(emptyList()) }
    var currentTouchPos by remember { mutableStateOf<Offset?>(null) }
    var dotPositions by remember { mutableStateOf<Map<Int, Offset>>(emptyMap()) }

    Box(modifier = modifier
        .aspectRatio(1f)
        .padding(32.dp)
        .onSizeChanged { size ->
            val w = size.width.toFloat()
            val h = size.height.toFloat()
            
            val padding = 40f
            val usableW = w - 2 * padding
            val usableH = h - 2 * padding

            val spacingX = usableW / 2f
            val spacingY = usableH / 2f
            
            val positions = mutableMapOf<Int, Offset>()
            for (i in 0 until 9) {
                val row = i / 3
                val col = i % 3
                positions[i] = Offset(padding + col * spacingX, padding + row * spacingY)
            }
            dotPositions = positions
        }
    ) {
        Canvas(modifier = Modifier.fillMaxSize().pointerInteropFilter { event ->
            val pos = Offset(event.x, event.y)
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    selectedDots = emptyList()
                    currentTouchPos = pos
                    val closest = dotPositions.entries.find { (pos - it.value).getDistance() < 80f }
                    if (closest != null) {
                        selectedDots = listOf(closest.key)
                    }
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    currentTouchPos = pos
                    val closest = dotPositions.entries.find { (pos - it.value).getDistance() < 80f }
                    if (closest != null && !selectedDots.contains(closest.key)) {
                        selectedDots = selectedDots + closest.key
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    // Pattern complete
                    // Allow smaller patterns or any length, just pass it
                    onPatternComplete(selectedDots.joinToString(""))
                    selectedDots = emptyList()
                    currentTouchPos = null
                    true
                }
                else -> false
            }
        }) {
            // Positions are computed in onSizeChanged on the Box

            dotPositions.forEach { (i, pos) ->
                val isSelected = selectedDots.contains(i)
                drawCircle(
                    color = if (isSelected) Color(0xFF4CAF50) else Color.Gray.copy(alpha = 0.5f),
                    radius = 30f,
                    center = pos
                )
            }

            if (selectedDots.isNotEmpty()) {
                val path = Path()
                path.moveTo(dotPositions[selectedDots[0]]!!.x, dotPositions[selectedDots[0]]!!.y)
                for (i in 1 until selectedDots.size) {
                    val pos = dotPositions[selectedDots[i]]!!
                    path.lineTo(pos.x, pos.y)
                }
                if (currentTouchPos != null) {
                    path.lineTo(currentTouchPos!!.x, currentTouchPos!!.y)
                }
                drawPath(path = path, color = Color(0xFF4CAF50), style = Stroke(width = 15f))
            }
        }
    }
}
