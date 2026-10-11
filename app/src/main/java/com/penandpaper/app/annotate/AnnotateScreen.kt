package com.penandpaper.app.annotate

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.penandpaper.app.storage.AttachmentStorage
import com.penandpaper.app.ui.editor.EditorViewModel
import com.penandpaper.app.ui.theme.FolderTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

enum class AnnotTool { PAN, PEN, PENCIL, HIGHLIGHT, ERASER, LINE, RECT, ELLIPSE, TEXT }

data class InkStroke(
    val points: List<Offset>,
    val color: Color,
    val widthPx: Float,
    val tool: AnnotTool,
    val text: String = ""
)

private fun rectOf(a: Offset, b: Offset): Rect {
    val left = minOf(a.x, b.x)
    val top = minOf(a.y, b.y)
    return Rect(Offset(left, top), Size(kotlin.math.abs(a.x - b.x), kotlin.math.abs(a.y - b.y)))
}

/**
 * Full annotation editor over an image attachment: zoom/pan canvas, pen,
 * pencil, highlighter, eraser, line/rect/ellipse shapes, text callouts,
 * undo/redo, and Save which flattens to a new JPEG stored via
 * AttachmentStorage (original preserved).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnnotateScreen(
    noteId: Long,
    imagePath: String,
    onSaved: (String) -> Unit,
    onCancel: () -> Unit
) {
    val ctx = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val textMeasurer = rememberTextMeasurer()

    var base by remember { mutableStateOf<Bitmap?>(null) }
    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }

    LaunchedEffect(imagePath) {
        loading = true
        base = withContext(Dispatchers.IO) {
            try {
                val bytes = AttachmentStorage.readBytes(ctx, imagePath)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            } catch (t: Throwable) {
                Log.w("Annotate", "decode failed", t)
                null
            }
        }
        loading = false
    }

    val strokes = remember { mutableStateListOf<InkStroke>() }
    val undoStack = remember { ArrayDeque<List<InkStroke>>() }
    val redoStack = remember { ArrayDeque<List<InkStroke>>() }
    fun snapshot() {
        undoStack.addLast(strokes.toList())
        if (undoStack.size > 60) undoStack.removeFirst()
        redoStack.clear()
    }

    var tool by remember { mutableStateOf(AnnotTool.PEN) }
    var color by remember { mutableStateOf(Color(0xFF1A73E8)) }
    var widthDp by remember { mutableFloatStateOf(4f) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val transformState = rememberTransformableState { zoom, pan, _ ->
        scale = (scale * zoom).coerceIn(0.5f, 6f)
        offset += pan
    }
    var current by remember { mutableStateOf<InkStroke?>(null) }
    var textAt by remember { mutableStateOf<Offset?>(null) }
    var textInput by remember { mutableStateOf("") }

    val palette = listOf(
        Color.Black, Color(0xFF1A73E8), Color(0xFFD93025),
        Color(0xFF188038), Color(0xFFF9AB00), Color(0xFF9334E6), Color.White
    )

    fun DrawScope.drawInk(s: InkStroke) {
        if (s.tool == AnnotTool.TEXT && s.text.isNotEmpty()) {
            val topLeft = s.points.firstOrNull() ?: return
            drawText(
                textMeasurer, s.text, topLeft = topLeft,
                style = TextStyle(color = s.color, fontSize = with(density) { (s.widthPx * 5f).toSp() })
            )
            return
        }
        val paintColor = when (s.tool) {
            AnnotTool.HIGHLIGHT -> s.color.copy(alpha = 0.35f)
            AnnotTool.PENCIL -> s.color.copy(alpha = 0.8f)
            else -> s.color
        }
        if (s.points.size == 1) {
            drawCircle(paintColor, radius = s.widthPx / 2f, center = s.points[0])
            return
        }
        if (s.points.size < 2) return
        val first = s.points.first()
        val last = s.points.last()
        when (s.tool) {
            AnnotTool.LINE -> drawLine(paintColor, first, last, strokeWidth = s.widthPx, cap = StrokeCap.Round)
            AnnotTool.RECT -> {
                val r = rectOf(first, last)
                drawRect(paintColor, topLeft = r.topLeft, size = r.size, style = Stroke(s.widthPx))
            }
            AnnotTool.ELLIPSE -> {
                val r = rectOf(first, last)
                drawOval(paintColor, topLeft = r.topLeft, size = r.size, style = Stroke(s.widthPx))
            }
            else -> {
                val path = Path().apply {
                    moveTo(first.x, first.y)
                    for (i in 1 until s.points.size) lineTo(s.points[i].x, s.points[i].y)
                }
                drawPath(path, paintColor, style = Stroke(s.widthPx, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
    }

    /** Renders base bitmap + all strokes at full resolution. Must be called
     *  with snapshots taken on the main thread. */
    fun flatten(src: Bitmap, all: List<InkStroke>, canvasW: Float, canvasH: Float): Bitmap {
        val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val c = android.graphics.Canvas(out)
        c.drawBitmap(src, 0f, 0f, null)
        val sx = src.width / canvasW
        val sy = src.height / canvasH
        for (s in all) {
            val paint = android.graphics.Paint().apply {
                isAntiAlias = true
                color = s.color.toArgb()
                strokeWidth = s.widthPx * sx
                style = android.graphics.Paint.Style.STROKE
                strokeCap = android.graphics.Paint.Cap.ROUND
                strokeJoin = android.graphics.Paint.Join.ROUND
                alpha = when (s.tool) {
                    AnnotTool.HIGHLIGHT -> 90
                    AnnotTool.PENCIL -> 204
                    else -> 255
                }
            }
            val pts = s.points.map { android.graphics.PointF(it.x * sx, it.y * sy) }
            if (s.tool == AnnotTool.TEXT && s.text.isNotEmpty()) {
                val p0 = pts.firstOrNull() ?: continue
                val tp = android.graphics.Paint().apply {
                    color = s.color.toArgb()
                    textSize = s.widthPx * 5f * sx
                    isAntiAlias = true
                }
                c.drawText(s.text, p0.x, p0.y, tp)
                continue
            }
            if (pts.size == 1) {
                paint.style = android.graphics.Paint.Style.FILL
                c.drawCircle(pts[0].x, pts[0].y, s.widthPx * sx / 2f, paint)
                continue
            }
            if (pts.size < 2) continue
            val a = pts.first()
            val b = pts.last()
            when (s.tool) {
                AnnotTool.LINE -> c.drawLine(a.x, a.y, b.x, b.y, paint)
                AnnotTool.RECT -> c.drawRect(
                    minOf(a.x, b.x), minOf(a.y, b.y), maxOf(a.x, b.x), maxOf(a.y, b.y), paint
                )
                AnnotTool.ELLIPSE -> c.drawOval(
                    minOf(a.x, b.x), minOf(a.y, b.y), maxOf(a.x, b.x), maxOf(a.y, b.y), paint
                )
                else -> {
                    val path = android.graphics.Path().apply {
                        moveTo(a.x, a.y)
                        for (i in 1 until pts.size) lineTo(pts[i].x, pts[i].y)
                    }
                    c.drawPath(path, paint)
                }
            }
        }
        return out
    }

    if (textAt != null) {
        AlertDialog(
            onDismissRequest = { textAt = null },
            title = { Text("Add text") },
            text = {
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    label = { Text("Text") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val pos = textAt
                    if (pos != null && textInput.isNotBlank()) {
                        snapshot()
                        strokes.add(
                            InkStroke(
                                listOf(pos), color,
                                with(density) { 4.dp.toPx() },
                                AnnotTool.TEXT, textInput
                            )
                        )
                    }
                    textAt = null
                }) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { textAt = null }) { Text("Cancel") } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = FolderTheme.appBarColors(),
                title = { Text("Annotate") },
                navigationIcon = { TextButton(onClick = onCancel) { Text("Close") } },
                actions = {
                    TextButton(
                        onClick = {
                            val prev = undoStack.removeLastOrNull()
                            if (prev != null) {
                                redoStack.addLast(strokes.toList())
                                strokes.clear(); strokes.addAll(prev)
                            }
                        },
                        enabled = undoStack.isNotEmpty()
                    ) { Text("Undo") }
                    TextButton(
                        onClick = {
                            val nxt = redoStack.removeLastOrNull()
                            if (nxt != null) {
                                undoStack.addLast(strokes.toList())
                                strokes.clear(); strokes.addAll(nxt)
                            }
                        },
                        enabled = redoStack.isNotEmpty()
                    ) { Text("Redo") }
                }
            )
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().background(Color(0xFF212121))) {
                val canvasW = with(density) { maxWidth.toPx() }
                val canvasH = with(density) { maxHeight.toPx() }
                if (loading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (base == null) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Could not load image", color = Color.White)
                    }
                } else {
                    val eraseR = with(density) { 24.dp.toPx() } / scale
                    val wPx = with(density) { widthDp.dp.toPx() }
                    Canvas(
                        Modifier
                            .fillMaxSize()
                            .transformable(transformState, enabled = tool == AnnotTool.PAN)
                            .pointerInput(tool, scale, offset) {
                                if (tool == AnnotTool.PAN) return@pointerInput
                                if (tool == AnnotTool.TEXT) {
                                    detectTapGestures { pos ->
                                        textAt = (pos - offset) / scale
                                        textInput = ""
                                    }
                                    return@pointerInput
                                }
                                detectDragGestures(
                                    onDragStart = { raw ->
                                        val pos = (raw - offset) / scale
                                        snapshot()
                                        if (tool == AnnotTool.ERASER) {
                                            strokes.removeAll { s ->
                                                s.points.any { (it - pos).getDistance() < eraseR }
                                            }
                                        } else {
                                            current = InkStroke(listOf(pos), color, wPx, tool)
                                        }
                                    },
                                    onDrag = { change, _ ->
                                        val pos = (change.position - offset) / scale
                                        if (tool == AnnotTool.ERASER) {
                                            strokes.removeAll { s ->
                                                s.points.any { (it - pos).getDistance() < eraseR }
                                            }
                                        } else {
                                            val cur = current
                                            current = when {
                                                cur == null -> null
                                                tool == AnnotTool.LINE || tool == AnnotTool.RECT || tool == AnnotTool.ELLIPSE ->
                                                    cur.copy(points = listOf(cur.points.first(), pos))
                                                else -> cur.copy(points = cur.points + pos)
                                            }
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
                        withTransform({
                            translate(offset.x, offset.y)
                            scale(scaleX = scale, scaleY = scale, pivot = Offset.Zero)
                        }) {
                            val bmp = base
                            if (bmp != null) {
                                drawImage(
                                    bmp.asImageBitmap(),
                                    dstSize = androidx.compose.ui.unit.IntSize(
                                        size.width.toInt(), size.height.toInt()
                                    )
                                )
                            }
                            for (s in strokes) drawInk(s)
                            current?.let { drawInk(it) }
                        }
                    }
                    if (saving) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }
                // Save bar (inside BoxWithConstraints to capture canvasW/H)
                Row(
                    Modifier.fillMaxWidth().padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = {
                            if (saving) return@TextButton
                            scope.launch {
                                saving = true
                                try {
                                    // Snapshot Compose state on the main thread first.
                                    val bmp = base
                                    val copy = strokes.toList()
                                    val flat = if (bmp != null) {
                                        withContext(Dispatchers.Default) { flatten(bmp, copy, canvasW, canvasH) }
                                    } else null
                                    if (flat != null) {
                                        val path = withContext(Dispatchers.IO) {
                                            val bos = ByteArrayOutputStream()
                                            flat.compress(Bitmap.CompressFormat.JPEG, 92, bos)
                                            AttachmentStorage.writeBytes(
                                                ctx, noteId,
                                                "annotated_${System.currentTimeMillis()}.jpg",
                                                bos.toByteArray()
                                            )
                                        }
                                        onSaved(path)
                                    }
                                } catch (t: Throwable) {
                                    Log.e("Annotate", "save failed", t)
                                } finally {
                                    saving = false
                                }
                            }
                        },
                        enabled = !saving && base != null
                    ) { Text(if (saving) "Saving…" else "Save") }
                }
            }
            // Tools
            LazyRow(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(AnnotTool.values().toList()) { t ->
                    FilterChip(
                        selected = tool == t,
                        onClick = { tool = t },
                        label = { Text(t.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }
            LazyRow(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(palette) { c ->
                    FilterChip(
                        selected = color == c,
                        onClick = { color = c },
                        label = { Text("⬤", color = c) }
                    )
                }
            }
            if (tool != AnnotTool.TEXT && tool != AnnotTool.ERASER && tool != AnnotTool.PAN) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Width")
                    Slider(
                        value = widthDp,
                        onValueChange = { widthDp = it },
                        valueRange = 1f..24f,
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                    )
                    Text("${widthDp.toInt()}dp")
                }
            }
        }
    }
}

/** Navigation route: annotates a note's image block, updates its payloadRef on save. */
@Composable
fun AnnotateRoute(
    noteId: Long,
    blockId: Long,
    onDone: () -> Unit,
    vm: EditorViewModel = hiltViewModel()
) {
    val scope = rememberCoroutineScope()
    val blocks by vm.blocks.collectAsState()
    LaunchedEffect(noteId) { vm.load(noteId) }
    val path = blocks.firstOrNull { it.id == blockId }?.payloadRef
    if (path == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        AnnotateScreen(
            noteId = noteId,
            imagePath = path,
            onSaved = { newPath ->
                scope.launch {
                    vm.annotateBlockDone(blockId, newPath)
                    onDone()
                }
            },
            onCancel = onDone
        )
    }
}
