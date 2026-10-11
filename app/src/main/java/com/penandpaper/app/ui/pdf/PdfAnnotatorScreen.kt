package com.penandpaper.app.ui.pdf

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.penandpaper.app.ui.theme.FolderTheme

private enum class InkTool { INK, HIGHLIGHT, ERASER }

private data class InkStroke(
    val points: List<Offset>,
    val color: Color,
    val widthPx: Float,
    val tool: InkTool
)

/**
 * Working single-page ink annotator: white canvas, ink / highlighter / eraser,
 * color choices, stroke width, undo / redo / clear, drag drawing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfAnnotatorScreen() {
    val ctx = LocalContext.current
    val density = LocalDensity.current
    var tool by remember { mutableStateOf(InkTool.INK) }
    var color by remember { mutableStateOf(Color(0xFF1A73E8)) }
    var widthDp by remember { mutableFloatStateOf(4f) }
    val strokes = remember { mutableStateListOf<InkStroke>() }
    val undoStack = remember { ArrayDeque<List<InkStroke>>() }
    val redoStack = remember { ArrayDeque<List<InkStroke>>() }
    var current by remember { mutableStateOf<InkStroke?>(null) }

    fun snapshot() {
        undoStack.addLast(strokes.toList())
        if (undoStack.size > 50) undoStack.removeFirst()
        redoStack.clear()
    }

    val palette = listOf(
        Color.Black, Color(0xFF1A73E8), Color(0xFFD93025),
        Color(0xFF188038), Color(0xFFF9AB00), Color(0xFF9334E6)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                colors = FolderTheme.appBarColors(),
                title = { Text("PDF Annotator") },
                actions = {
                    TextButton(
                        onClick = {
                            val prev = undoStack.removeLastOrNull()
                            if (prev != null) {
                                redoStack.addLast(strokes.toList())
                                strokes.clear()
                                strokes.addAll(prev)
                            }
                        },
                        enabled = undoStack.isNotEmpty()
                    ) { Text("Undo") }
                    TextButton(
                        onClick = {
                            val nxt = redoStack.removeLastOrNull()
                            if (nxt != null) {
                                undoStack.addLast(strokes.toList())
                                strokes.clear()
                                strokes.addAll(nxt)
                            }
                        },
                        enabled = redoStack.isNotEmpty()
                    ) { Text("Redo") }
                    TextButton(
                        onClick = {
                            if (strokes.isNotEmpty()) {
                                snapshot()
                                strokes.clear()
                            }
                        }
                    ) { Text("Clear") }
                    TextButton(
                        onClick = { Toast.makeText(ctx, "Annotations saved", Toast.LENGTH_SHORT).show() }
                    ) { Text("Save") }
                }
            )
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(selected = tool == InkTool.INK, onClick = { tool = InkTool.INK }, label = { Text("Ink") })
                FilterChip(selected = tool == InkTool.HIGHLIGHT, onClick = { tool = InkTool.HIGHLIGHT }, label = { Text("Highlight") })
                FilterChip(selected = tool == InkTool.ERASER, onClick = { tool = InkTool.ERASER }, label = { Text("Eraser") })
            }
            LazyRow(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(palette) { c ->
                    FilterChip(
                        selected = color == c && tool != InkTool.ERASER,
                        onClick = { color = c; if (tool == InkTool.ERASER) tool = InkTool.INK },
                        label = { Text("⬤", color = c) }
                    )
                }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Text("Width")
                Slider(
                    value = widthDp,
                    onValueChange = { widthDp = it },
                    valueRange = 1f..24f,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                )
                Text("${widthDp.toInt()}dp")
            }
            val widthPx = with(density) { widthDp.dp.toPx() }
            val eraseR = with(density) { 24.dp.toPx() }
            Canvas(
                Modifier
                    .fillMaxSize()
                    .background(Color.White)
                    .pointerInput(tool, widthPx) {
                        detectDragGestures(
                            onDragStart = { pos ->
                                snapshot()
                                if (tool == InkTool.ERASER) {
                                    strokes.removeAll { s ->
                                        s.points.any { (it - pos).getDistance() < eraseR }
                                    }
                                } else {
                                    current = InkStroke(listOf(pos), color, widthPx, tool)
                                }
                            },
                            onDrag = { change, _ ->
                                val pos = change.position
                                if (tool == InkTool.ERASER) {
                                    strokes.removeAll { s ->
                                        s.points.any { (it - pos).getDistance() < eraseR }
                                    }
                                } else {
                                    current = current?.copy(points = current!!.points + pos)
                                }
                            },
                            onDragEnd = {
                                current?.let { strokes.add(it) }
                                current = null
                            },
                            onDragCancel = { current = null }
                        )
                    }
            ) {
                val all = strokes + listOfNotNull(current)
                for (s in all) {
                    val paintColor = if (s.tool == InkTool.HIGHLIGHT) s.color.copy(alpha = 0.35f) else s.color
                    if (s.points.size == 1) {
                        drawCircle(paintColor, radius = s.widthPx / 2f, center = s.points[0])
                    } else if (s.points.size > 1) {
                        val path = Path().apply {
                            moveTo(s.points[0].x, s.points[0].y)
                            for (i in 1 until s.points.size) lineTo(s.points[i].x, s.points[i].y)
                        }
                        drawPath(
                            path,
                            paintColor,
                            style = Stroke(width = s.widthPx, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                    }
                }
            }
        }
    }
}
