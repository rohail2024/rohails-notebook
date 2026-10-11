package com.penandpaper.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

object FolderTheme {
    var color: Color? by mutableStateOf(null)
    var contentMode: String by mutableStateOf("auto") // auto | light | dark | themeOnPrimary | themeOnSurface
    var preferLight: Boolean by mutableStateOf(false)
    var useAccentForHighlights: Boolean by mutableStateOf(true)

    @Composable
    fun appBarColors(): TopAppBarColors {
        val container = color ?: MaterialTheme.colorScheme.surface
        val content = when (contentMode) {
            "light" -> Color.White
            "dark" -> Color.Black
            "themeOnPrimary" -> MaterialTheme.colorScheme.onPrimary
            "themeOnSurface" -> MaterialTheme.colorScheme.onSurface
            else -> {
                val auto = if (container.luminance() < 0.5f) Color.White else Color.Black
                if (preferLight) Color.White else auto
            }
        }
        return TopAppBarDefaults.topAppBarColors(
            containerColor = container,
            titleContentColor = content,
            navigationIconContentColor = content,
            actionIconContentColor = content
        )
    }

    fun accentColor(default: Color): Color {
        return color?.copy(alpha = 0.85f) ?: default
    }
}
