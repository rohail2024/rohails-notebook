package com.penandpaper.app.ui.editor

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.penandpaper.app.camera.CameraImport
import com.penandpaper.app.ocr.Ocr
import com.penandpaper.app.scan.ScanImport
import com.penandpaper.app.storage.AttachmentStorage
import com.penandpaper.domain.model.BlockType
import com.penandpaper.domain.model.Note
import com.penandpaper.domain.model.NoteBlock
import com.penandpaper.domain.repo.NotesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import javax.inject.Inject

@HiltViewModel
class EditorViewModel @Inject constructor(
    private val repo: NotesRepository,
    @ApplicationContext private val appContext: Context
) : ViewModel() {
    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title
    private val _blocks = MutableStateFlow<List<NoteBlock>>(emptyList())
    val blocks: StateFlow<List<NoteBlock>> = _blocks
    var noteId: Long = 0L
        private set

    fun load(noteId: Long) {
        this.noteId = noteId
        viewModelScope.launch {
            if (noteId == 0L) {
                val now = System.currentTimeMillis()
                this@EditorViewModel.noteId = repo.upsertNote(Note(0, "Untitled", now, now, false, false, null))
                _title.value = "Untitled"
                _blocks.value = emptyList()
            } else {
                val note = repo.getNote(noteId)
                _title.value = note?.title ?: ""
                refreshBlocks()
            }
        }
    }

    fun refreshBlocks() {
        viewModelScope.launch {
            _blocks.value = try {
                repo.blocksForNote(noteId)
            } catch (t: Throwable) {
                Log.w("EditorVM", "blocksForNote failed", t)
                emptyList()
            }
        }
    }

    fun setTitle(t: String) { _title.value = t }

    private fun emptyTextMeta(): String = JSONObject().apply {
        put("content", "")
        put("style", JSONObject().apply {
            put("fontFamily", "Sans"); put("fontSize", 16)
            put("bold", false); put("italic", false); put("underline", false)
        })
    }.toString()

    fun addTextBlock() {
        viewModelScope.launch {
            val newBlock = NoteBlock(0, noteId, BlockType.TEXT, _blocks.value.size, null, emptyTextMeta())
            val id = repo.addBlock(newBlock)
            _blocks.value = _blocks.value + newBlock.copy(id = id)
        }
    }

    /** Adds a TEXT block with the given content (used for OCR results). */
    fun addTextBlockWithContent(content: String) {
        viewModelScope.launch {
            val meta = JSONObject(emptyTextMeta()).apply { put("content", content) }.toString()
            val newBlock = NoteBlock(0, noteId, BlockType.TEXT, _blocks.value.size, null, meta)
            val id = repo.addBlock(newBlock)
            _blocks.value = _blocks.value + newBlock.copy(id = id)
        }
    }

    fun getBlockMeta(index: Int): String = _blocks.value.getOrNull(index)?.metaJson ?: "{}"

    fun setBlockMeta(index: Int, meta: String) {
        val list = _blocks.value.toMutableList()
        list[index] = list[index].copy(metaJson = meta)
        _blocks.value = list
    }

    fun getTextContent(index: Int): String =
        _blocks.value.getOrNull(index)?.metaJson?.let {
            try { JSONObject(it).optString("content", "") } catch (t: Throwable) { "" }
        } ?: ""

    fun setTextContent(index: Int, text: String) {
        val list = _blocks.value.toMutableList()
        val cur = list.getOrNull(index) ?: return
        val obj = try { JSONObject(cur.metaJson ?: "{}") } catch (t: Throwable) { JSONObject() }
        obj.put("content", text)
        list[index] = cur.copy(metaJson = obj.toString())
        _blocks.value = list
    }

    fun getBlockStyle(index: Int): BlockStyle {
        val obj = _blocks.value.getOrNull(index)?.metaJson?.let {
            try { JSONObject(it).optJSONObject("style") } catch (t: Throwable) { null }
        }
        return BlockStyle(
            fontFamily = obj?.optString("fontFamily", "Sans") ?: "Sans",
            fontSize = obj?.optInt("fontSize", 16) ?: 16,
            bold = obj?.optBoolean("bold", false) ?: false,
            italic = obj?.optBoolean("italic", false) ?: false,
            underline = obj?.optBoolean("underline", false) ?: false
        )
    }

    fun setBlockStyle(index: Int, st: BlockStyle) {
        val list = _blocks.value.toMutableList()
        val cur = list.getOrNull(index) ?: return
        val obj = try { JSONObject(cur.metaJson ?: "{}") } catch (t: Throwable) { JSONObject() }
        val s = (obj.optJSONObject("style") ?: JSONObject())
        s.put("fontFamily", st.fontFamily)
        s.put("fontSize", st.fontSize)
        s.put("bold", st.bold)
        s.put("italic", st.italic)
        s.put("underline", st.underline)
        obj.put("style", s)
        list[index] = cur.copy(metaJson = obj.toString())
        _blocks.value = list
    }

    // ---------- Camera / scan / annotation integration ----------

    /** Imports scanned page URIs as SCAN blocks (suspending; callers refresh after). */
    suspend fun importScans(forNoteId: Long, uris: List<Uri>) {
        ScanImport.importScannedPages(appContext, forNoteId, uris, repo)
        if (forNoteId == noteId) refreshBlocks()
    }

    /** Imports a captured photo file as an IMAGE block. */
    suspend fun importPhoto(forNoteId: Long, file: File) {
        CameraImport.importPhoto(appContext, forNoteId, file, repo)
        if (forNoteId == noteId) refreshBlocks()
    }

    /** Adds an IMAGE block pointing at an already-stored attachment path. */
    suspend fun addImageBlock(path: String) {
        val meta = JSONObject().put("width", 0).put("height", 0).toString()
        val newBlock = NoteBlock(0, noteId, BlockType.IMAGE, _blocks.value.size, path, meta)
        val id = repo.addBlock(newBlock)
        _blocks.value = _blocks.value + newBlock.copy(id = id)
    }

    /** Points an image block at the newly saved annotated file. */
    suspend fun annotateBlockDone(blockId: Long, newPath: String) {
        val b = _blocks.value.firstOrNull { it.id == blockId } ?: return
        repo.updateBlock(b.copy(payloadRef = newPath))
        refreshBlocks()
    }

    /** Runs on-device OCR on a SCAN/IMAGE block and appends the text as a TEXT block. */
    fun runOcrOnBlock(blockId: Long, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val block = _blocks.value.firstOrNull { it.id == blockId }
                    ?: return@launch onResult(false)
                val ref = block.payloadRef ?: return@launch onResult(false)
                val bytes = withContext(Dispatchers.IO) { AttachmentStorage.readBytes(appContext, ref) }
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    ?: return@launch onResult(false)
                val text = Ocr.recognizeText(appContext, bmp)
                bmp.recycle()
                if (text.isBlank()) return@launch onResult(false)
                addTextBlockWithContent(text)
                onResult(true)
            } catch (t: Throwable) {
                Log.w("EditorVM", "OCR failed", t)
                onResult(false)
            }
        }
    }

    suspend fun persistDraft() {
        val now = System.currentTimeMillis()
        repo.upsertNote(Note(noteId, _title.value.ifEmpty { "Untitled" }, now, now, false, false, null))
        _blocks.value.forEach { repo.updateBlock(it) }
    }
}
