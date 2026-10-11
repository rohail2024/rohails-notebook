
package com.penandpaper.app.ui.editor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.penandpaper.app.ui.theme.FolderTheme

@Composable
fun PageBackground(
    template: String = "ruled", // ruled, dotted, grid
    background: Color = Color(0xFFFAFAFA)
) {
    val accent = FolderTheme.accentColor(Color(0xFF90CAF9)).copy(alpha = 0.35f)
    Box(Modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            when (template) {
                "grid" -> {
                    val step = 24f
                    var y = 0f
                    while (y < size.height) {
                        drawLine(accent, start = Offset(0f, y), end = Offset(size.width, y), strokeWidth = 1f)
                        y += step
                    }
                    var x = 0f
                    while (x < size.width) {
                        drawLine(accent, start = Offset(x, 0f), end = Offset(x, size.height), strokeWidth = 1f)
                        x += step
                    }
                }
                "dotted" -> {
                    val step = 24f
                    var y = 0f
                    while (y < size.height) {
                        var x = 0f
                        while (x < size.width) {
                            drawCircle(accent, radius = 1.5f, center = Offset(x, y), style = Stroke(width = 3f))
                            x += step
                        }
                        y += step
                    }
                }
                else -> { // ruled
                    val step = 24f
                    var y = step
                    while (y < size.height) {
                        drawLine(accent, start = Offset(0f, y), end = Offset(size.width, y), strokeWidth = 1f)
                        y += step
                    }
                }
            }
        }
    }
}
