package com.penandpaper.app.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.penandpaper.app.storage.AttachmentStorage
import com.penandpaper.domain.model.BlockType
import com.penandpaper.domain.model.NoteBlock
import com.penandpaper.domain.repo.NotesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import kotlin.math.max

private const val MAX_DIM = 2480

/**
 * Imports a captured photo file into a note as an IMAGE block: downscales to
 * a max of 2480px, stores JPEG-92 through the encrypted AttachmentStorage.
 */
object CameraImport {
    suspend fun importPhoto(
        ctx: Context,
        noteId: Long,
        file: File,
        repo: NotesRepository
    ): NoteBlock = withContext(Dispatchers.IO) {
        val decoded = BitmapFactory.decodeFile(file.absolutePath)
            ?: throw IOException("Cannot decode photo: ${file.absolutePath}")
        try {
            val longest = max(decoded.width, decoded.height)
            val scaled = if (longest > MAX_DIM) {
                val s = MAX_DIM.toFloat() / longest
                Bitmap.createScaledBitmap(
                    decoded, (decoded.width * s).toInt(), (decoded.height * s).toInt(), true
                )
            } else decoded
            val bytes = ByteArrayOutputStream().also { bos ->
                scaled.compress(Bitmap.CompressFormat.JPEG, 92, bos)
            }.toByteArray()
            if (scaled !== decoded) scaled.recycle()
            val path = AttachmentStorage.writeBytes(
                ctx, noteId, "photo_${System.currentTimeMillis()}.jpg", bytes
            )
            val meta = JSONObject()
                .put("width", decoded.width)
                .put("height", decoded.height)
                .toString()
            val sortIndex = try { repo.blocksForNote(noteId).size } catch (t: Throwable) { 0 }
            val block = NoteBlock(0, noteId, BlockType.IMAGE, sortIndex, path, meta)
            block.copy(id = repo.addBlock(block))
        } finally {
            decoded.recycle()
            try { file.delete() } catch (t: Throwable) { Log.w("CameraImport", "tmp delete failed", t) }
        }
    }
}
