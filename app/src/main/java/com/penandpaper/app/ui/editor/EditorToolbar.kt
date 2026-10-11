package com.penandpaper.app.ui.editor
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorToolbar(
    fontFamily: String,
    onFontFamily: (String) -> Unit,
    fontSize: Int,
    onFontSize: (Int) -> Unit,
    bold: Boolean, onBold: (Boolean) -> Unit,
    italic: Boolean, onItalic: (Boolean) -> Unit,
    underline: Boolean, onUnderline: (Boolean) -> Unit
) {
    Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        var showFont by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(expanded = showFont, onExpandedChange = { showFont = it }) {
            OutlinedTextField(value = fontFamily, onValueChange = {}, readOnly = true, label = { Text("Font") })
            ExposedDropdownMenu(expanded = showFont, onDismissRequest = { showFont = false }) {
                listOf("Sans","Serif","Mono").forEach { f ->
                    DropdownMenuItem(text = { Text(f) }, onClick = { onFontFamily(f); showFont = false })
                }
            }
        }
        var showSize by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(expanded = showSize, onExpandedChange = { showSize = it }) {
            OutlinedTextField(value = fontSize.toString(), onValueChange = {}, readOnly = true, label = { Text("Size") })
            ExposedDropdownMenu(expanded = showSize, onDismissRequest = { showSize = false }) {
                listOf(12,14,16,18,20,24,28).forEach { s ->
                    DropdownMenuItem(text = { Text("$s") }, onClick = { onFontSize(s); showSize = false })
                }
            }
        }
        FilterChip(selected = bold, onClick = { onBold(!bold) }, label = { Text("B") })
        FilterChip(selected = italic, onClick = { onItalic(!italic) }, label = { Text("I") })
        FilterChip(selected = underline, onClick = { onUnderline(!underline) }, label = { Text("U") })
    }
}
