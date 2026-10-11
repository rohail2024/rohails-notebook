package com.penandpaper.app.ui.editor

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.penandpaper.app.storage.AttachmentStorage
import com.penandpaper.app.ui.AutoSaver
import com.penandpaper.app.ui.theme.FolderTheme
import com.penandpaper.domain.model.BlockType
import com.penandpaper.domain.model.NoteBlock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class BlockStyle(
    val fontFamily: String = "Sans",
    val fontSize: Int = 16,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    noteId: Long,
    onBack: () -> Unit,
    onScan: (Long) -> Unit = {},
    onCamera: (Long) -> Unit = {},
    onAnnotate: (Long, Long) -> Unit = { _, _ -> },
    vm: EditorViewModel = hiltViewModel()
) {
    LaunchedEffect(noteId) { vm.load(noteId) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME) vm.refreshBlocks()
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }
    val title by vm.title.collectAsState()
    val blocks by vm.blocks.collectAsState()
    val saver = remember { AutoSaver(vm.viewModelScope) }
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var addMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = FolderTheme.appBarColors(),
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
                title = { Text(title.ifEmpty { "Untitled" }) }
            )
        },
        floatingActionButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box {
                    ExtendedFloatingActionButton(
                        text = { Text("Add") },
                        onClick = { addMenu = true }
                    )
                    DropdownMenu(expanded = addMenu, onDismissRequest = { addMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Scan document") },
                            onClick = { addMenu = false; onScan(vm.noteId) }
                        )
                        DropdownMenuItem(
                            text = { Text("Take photo") },
                            onClick = { addMenu = false; onCamera(vm.noteId) }
                        )
                    }
                }
                ExtendedFloatingActionButton(text = { Text("Text") }, onClick = vm::addTextBlock)
            }
        }
    ) { pad ->
        Box(Modifier.padding(pad)) {
            PageBackground("ruled")
            Column(Modifier.padding(16.dp)) {
                var t by remember(title) { mutableStateOf(title) }
                OutlinedTextField(t, onValueChange = {
                    t = it; vm.setTitle(it); saver.schedule { vm.persistDraft() }
                }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(blocks.size) { i ->
                        val block = blocks[i]
                        when (block.type) {
                            BlockType.TEXT -> TextBlockCard(
                                index = i,
                                blockId = block.id,
                                vm = vm,
                                saver = saver
                            )
                            BlockType.IMAGE, BlockType.SCAN -> ImageBlockCard(
                                block = block,
                                onAnnotate = { onAnnotate(vm.noteId, block.id) },
                                onOcr = if (block.type == BlockType.SCAN) {
                                    {
                                        vm.runOcrOnBlock(block.id) { ok ->
                                            Toast.makeText(
                                                ctx,
                                                if (ok) "Text extracted" else "No text found",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                } else null
                            )
                        }
                    }
                    item { Spacer(Modifier.height(72.dp)) }
                }
            }
        }
    }
}

@Composable
private fun TextBlockCard(
    index: Int,
    blockId: Long,
    vm: EditorViewModel,
    saver: AutoSaver
) {
    val styleState = remember(blockId) { mutableStateOf(vm.getBlockStyle(index)) }
    val st = styleState.value
    OutlinedCard(Modifier.fillMaxWidth()) {
        EditorToolbar(
            fontFamily = st.fontFamily,
            onFontFamily = { ff -> styleState.value = st.copy(fontFamily = ff); vm.setBlockStyle(index, styleState.value); saver.schedule { vm.persistDraft() } },
            fontSize = st.fontSize,
            onFontSize = { fs -> styleState.value = st.copy(fontSize = fs); vm.setBlockStyle(index, styleState.value); saver.schedule { vm.persistDraft() } },
            bold = st.bold, onBold = { b -> styleState.value = st.copy(bold = b); vm.setBlockStyle(index, styleState.value); saver.schedule { vm.persistDraft() } },
            italic = st.italic, onItalic = { itl -> styleState.value = st.copy(italic = itl); vm.setBlockStyle(index, styleState.value); saver.schedule { vm.persistDraft() } },
            underline = st.underline, onUnderline = { u -> styleState.value = st.copy(underline = u); vm.setBlockStyle(index, styleState.value); saver.schedule { vm.persistDraft() } }
        )
        var content by remember(blockId) { mutableStateOf(vm.getTextContent(index)) }
        val ff = when (st.fontFamily) {
            "Serif" -> FontFamily.Serif
            "Mono" -> FontFamily.Monospace
            else -> FontFamily.SansSerif
        }
        val deco = if (st.underline) TextDecoration.Underline else TextDecoration.None
        val weight = if (st.bold) FontWeight.Bold else FontWeight.Normal
        val fontStyle = if (st.italic) FontStyle.Italic else FontStyle.Normal
        BasicTextField(
            value = content,
            onValueChange = { content = it; vm.setTextContent(index, it); saver.schedule { vm.persistDraft() } },
            textStyle = TextStyle(fontFamily = ff, fontSize = st.fontSize.sp, fontWeight = weight, fontStyle = fontStyle, textDecoration = deco),
            modifier = Modifier.padding(16.dp).fillMaxWidth()
        )
    }
}

@Composable
private fun ImageBlockCard(
    block: NoteBlock,
    onAnnotate: () -> Unit,
    onOcr: (() -> Unit)?
) {
    val ctx = LocalContext.current
    val bitmap by produceState<Bitmap?>(null, block.id, block.payloadRef) {
        value = withContext(Dispatchers.IO) {
            try {
                block.payloadRef
                    ?.let { AttachmentStorage.readBytes(ctx, it) }
                    ?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
            } catch (t: Throwable) {
                null
            }
        }
    }
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val bmp = bitmap
            if (bmp != null) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = if (block.type == BlockType.SCAN) "Scanned page" else "Photo",
                    modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp).clickable { onAnnotate() },
                    contentScale = ContentScale.Fit
                )
            } else {
                Text("Loading image…", modifier = Modifier.padding(16.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onAnnotate) { Text("Annotate") }
                if (onOcr != null) {
                    TextButton(onClick = onOcr) { Text("Extract text") }
                }
            }
        }
    }
}
