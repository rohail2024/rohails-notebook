package com.penandpaper.app.ui.list
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.penandpaper.app.export.PdfExporter
import com.penandpaper.app.export.DocxExporter
import com.penandpaper.app.share.ShareUtil
import com.penandpaper.domain.model.NoteBlock
import com.penandpaper.domain.model.BlockType
import com.penandpaper.domain.model.Folder
import com.penandpaper.app.ui.theme.FolderTheme
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NotesListScreen(
    onOpen: (Long) -> Unit,
    onCreate: () -> Unit,
    onManageFolders: () -> Unit,
    onAbout: () -> Unit,
    onScanNew: (Long) -> Unit = {},
    onCameraNew: (Long) -> Unit = {},
    vm: NotesListViewModel = hiltViewModel(),
    fvm: FoldersViewModel = hiltViewModel()
) {
    val notes by vm.notes.collectAsState()
    val folders by fvm.folders.collectAsState()
    val ctx = LocalContext.current
    var selectionMode by remember { mutableStateOf(false) }
    val selected = remember { mutableStateMapOf<Long, Boolean>() }
    var folderMenu by remember { mutableStateOf(false) }
    var currentFolderId by remember { mutableStateOf<Long?>(null) }
    val currentFolder: Folder? = folders.firstOrNull { it.id == currentFolderId }
        LaunchedEffect(currentFolderId, currentFolder) {
            FolderTheme.color = currentFolder?.color?.let { androidx.compose.ui.graphics.Color(it.toULong()) }
            FolderTheme.contentMode = currentFolder?.contentMode ?: "auto"
            FolderTheme.preferLight = currentFolder?.preferLight ?: false
        }
    val scope = rememberCoroutineScope()

    val grid = currentFolder?.layout == "grid"
    val size = when (currentFolder?.iconSize) {
        "small" -> 80.dp
        "large" -> 160.dp
        else -> 120.dp
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    ExposedDropdownMenuBox(expanded = folderMenu, onExpandedChange = { folderMenu = it }) {
                        val current = currentFolder?.name ?: "All notes"
                        OutlinedTextField(value = current, onValueChange = {}, readOnly = true, label = { Text("Folder") })
                        ExposedDropdownMenu(expanded = folderMenu, onDismissRequest = { folderMenu = false }) {
                            DropdownMenuItem(text = { Text("All notes") }, onClick = { currentFolderId = null; vm.setFolder(null); folderMenu = false })
                            folders.forEach { f ->
                                DropdownMenuItem(text = { Text(f.name) }, onClick = { currentFolderId = f.id; vm.setFolder(f.id); folderMenu = false })
                            }
                            Divider()
                            DropdownMenuItem(text = { Text("Manage folders…") }, onClick = { onManageFolders(); folderMenu = false })
                            DropdownMenuItem(text = { Text("About") }, onClick = { onAbout(); folderMenu = false })
                        }
                    }
                },
                actions = {
                    if (selectionMode) {
                        TextButton(onClick = {
                            scope.launch {
                                val out = File(ctx.cacheDir, "batch.pdf")
                                PdfExporter.exportSimple(ctx, "Batch Export", emptyList(), out)
                                ShareUtil.shareFile(ctx, out, "application/pdf")
                            }
                            selectionMode = false
                            selected.clear()
                        }) { Text("Export") }
                    }
                }
            )
        },
        floatingActionButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ExtendedFloatingActionButton(
                    onClick = { scope.launch { val id = vm.createNote(); onScanNew(id) } }
                ) {
                    Text("Scan")
                }
                ExtendedFloatingActionButton(
                    onClick = { scope.launch { val id = vm.createNote(); onCameraNew(id) } }
                ) {
                    Text("Camera")
                }
                FloatingActionButton(onClick = { onCreate() }) { Text("+") }
            }
        }
    ) { pad ->
        if (grid) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(size),
                modifier = Modifier.padding(pad).padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(notes, key = { it.id }) { n ->
                    ElevatedCard(Modifier.fillMaxWidth().height(size).clickable { onOpen(n.id) }) {
                        Column(Modifier.padding(12.dp)) {
                            Text(n.title, style = MaterialTheme.typography.titleMedium)
                            Text("Updated: " + n.updatedAt, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        } else {
            LazyColumn(Modifier.padding(pad)) {
                items(notes) { n ->
                    val isSel = selected[n.id] == true
                    ListItem(
                        headlineContent = { Text(n.title) },
                        supportingContent = { Text("Updated: " + n.updatedAt) },
                        modifier = Modifier.fillMaxWidth().clickable {
                            if (selectionMode) selected[n.id] = !(selected[n.id] ?: false)
                            else onOpen(n.id)
                        },
                        trailingContent = {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (selectionMode) {
                                    Checkbox(checked = isSel, onCheckedChange = { selected[n.id] = it })
                                } else {
                                    var menu by remember { mutableStateOf(false) }
                                    Box {
                                        TextButton(onClick = { menu = true }) { Text("⋮") }
                                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                                            DropdownMenuItem(text = { Text("Export PDF") }, onClick = {
                                                scope.launch {
                                                    val out = File(ctx.cacheDir, "${n.id}.pdf")
                                                    PdfExporter.exportSimple(ctx, n.title, listOf(NoteBlock(0, n.id, BlockType.TEXT, 0, null, "")), out)
                                                    ShareUtil.shareFile(ctx, out, "application/pdf")
                                                }
                                                menu = false
                                            })
                                            DropdownMenuItem(text = { Text("Export DOCX") }, onClick = {
                                                scope.launch {
                                                    val out = File(ctx.cacheDir, "${n.id}.docx")
                                                    DocxExporter.exportSimple(ctx, n.title, listOf(NoteBlock(0, n.id, BlockType.TEXT, 0, null, "")), out)
                                                    ShareUtil.shareFile(ctx, out, "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                                                }
                                                menu = false
                                            })
                                            DropdownMenuItem(text = { Text("Select") }, onClick = { selectionMode = true; selected[n.id] = true; menu = false })
                                        }
                                    }
                                }
                            }
                        }
                    )
                    Divider()
                }
            }
        }
    }
}
