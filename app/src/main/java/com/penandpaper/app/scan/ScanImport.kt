package com.penandpaper.app.scan

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.penandpaper.app.storage.AttachmentStorage
import com.penandpaper.domain.model.BlockType
import com.penandpaper.domain.model.NoteBlock
import com.penandpaper.domain.repo.NotesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import kotlin.math.max

private const val MAX_DIM = 2480

private fun downscale(src: Bitmap, maxDim: Int): Bitmap {
    val w = src.width
    val h = src.height
    val longest = max(w, h)
    if (longest <= maxDim) return src
    val s = maxDim.toFloat() / longest
    return Bitmap.createScaledBitmap(src, (w * s).toInt(), (h * s).toInt(), true)
}

/**
 * Imports scanned page URIs into a note: downscales each page to a max of
 * 2480px, stores it as JPEG-92 through the encrypted AttachmentStorage, and
 * creates a SCAN block per page (payloadRef = stored path).
 */
object ScanImport {
    suspend fun importScannedPages(
        ctx: Context,
        noteId: Long,
        uris: List<Uri>,
        repo: NotesRepository
    ): List<NoteBlock> = withContext(Dispatchers.IO) {
        val baseIndex = try { repo.blocksForNote(noteId).size } catch (t: Throwable) { 0 }
        uris.mapIndexedNotNull { index, uri ->
            try {
                val raw = ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: return@mapIndexedNotNull null
                val decoded = BitmapFactory.decodeByteArray(raw, 0, raw.size)
                    ?: return@mapIndexedNotNull null
                val scaled = downscale(decoded, MAX_DIM)
                val out = ByteArrayOutputStream().also { bos ->
                    scaled.compress(Bitmap.CompressFormat.JPEG, 92, bos)
                }.toByteArray()
                if (scaled !== decoded) decoded.recycle()
                val path = AttachmentStorage.writeBytes(
                    ctx, noteId, "scan_${System.currentTimeMillis()}_$index.jpg", out
                )
                if (scaled !== decoded) scaled.recycle()
                val meta = JSONObject()
                    .put("pageIndex", index)
                    .put("ocrText", "")
                    .toString()
                val block = NoteBlock(0, noteId, BlockType.SCAN, baseIndex + index, path, meta)
                block.copy(id = repo.addBlock(block))
            } catch (t: Throwable) {
                Log.w("ScanImport", "failed to import $uri", t)
                null
            }
        }
    }
}
