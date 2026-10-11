package com.penandpaper.app.ui.folders
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.penandpaper.domain.model.Folder
import com.penandpaper.app.ui.list.FoldersViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderManagerScreen(vm: FoldersViewModel = hiltViewModel()) {
    val folders by vm.folders.collectAsState()
    val scope = rememberCoroutineScope()
    Scaffold(topBar = { TopAppBar(title = { Text("Folders") }) }) { pad ->
        LazyColumn(Modifier.padding(pad).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(folders, key = { it.id }) { f ->
                FolderRow(folder = f,
                    onRename = { name -> scope.launch { vm.updateFolder(f.copy(name = name)) } },
                    onColor = { color -> scope.launch { vm.updateFolder(f.copy(color = color)) } },
                    onLayout = { layout -> scope.launch { vm.updateFolder(f.copy(layout = layout)) } },
                    onSize = { size -> scope.launch { vm.updateFolder(f.copy(iconSize = size)) } },
                    onContentMode = { mode -> scope.launch { vm.updateFolder(f.copy(contentMode = mode)) } },
                    onPreferLight = { pref -> scope.launch { vm.updateFolder(f.copy(preferLight = pref)) } },
                    onMoveUp = { scope.launch { vm.reorderUp(f.id) } },
                    onMoveDown = { scope.launch { vm.reorderDown(f.id) } },
                    onDelete = { scope.launch { vm.deleteFolderCascade(f.id) } }
                )
            }
            item {
                Button(onClick = { scope.launch { vm.addFolder("New Folder") } }) { Text("Add folder") }
            }
        }
    }
}

@Composable
private fun FolderRow(
    folder: Folder,
    onRename: (String) -> Unit,
    onColor: (Long?) -> Unit,
    onLayout: (String) -> Unit,
    onSize: (String) -> Unit,
    onContentMode: (String) -> Unit,
    onPreferLight: (Boolean) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit
) {
    var name by remember { mutableStateOf(folder.name) }
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onRename(name) }) { Text("Rename") }
                Button(onClick = onMoveUp) { Text("Move up") }
                Button(onClick = onMoveDown) { Text("Move down") }
                Button(colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)), onClick = onDelete) { Text("Delete") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Layout:")
                FilterChip(selected = folder.layout == "list", onClick = { onLayout("list") }, label = { Text("List") })
                FilterChip(selected = folder.layout == "grid", onClick = { onLayout("grid") }, label = { Text("Grid") })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Size:")
                FilterChip(selected = folder.iconSize == "small", onClick = { onSize("small") }, label = { Text("Small") })
                FilterChip(selected = folder.iconSize == "medium", onClick = { onSize("medium") }, label = { Text("Medium") })
                FilterChip(selected = folder.iconSize == "large", onClick = { onSize("large") }, label = { Text("Large") })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Color:")
                listOf(0xFFEF9A9A, 0xFFA5D6A7, 0xFF90CAF9, 0xFFFFF59D, 0xFFD1C4E9).forEach { c ->
                    FilledTonalButton(onClick = { onColor(c) }, content = {}, colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(c)))
                }
                FilledTonalButton(onClick = { onColor(null) }) { Text("Clear") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Content:")
                listOf("auto", "light", "dark").forEach { m ->
                    FilterChip(selected = folder.contentMode == m, onClick = { onContentMode(m) }, label = { Text(m) })
                }
                Row {
                    Checkbox(checked = folder.preferLight, onCheckedChange = onPreferLight)
                    Text("Prefer light")
                }
            }
        }
    }
}
