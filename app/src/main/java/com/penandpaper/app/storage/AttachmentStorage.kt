
package com.penandpaper.app.storage
import android.content.Context
import com.penandpaper.app.crypto.CryptoManager
import com.penandpaper.app.crypto.KeyHolder
import java.io.File

object AttachmentStorage {
    fun baseDir(ctx: Context): File = File(ctx.filesDir, "notes").apply { mkdirs() }
    fun writeBytes(ctx: Context, noteId: Long, filename: String, data: ByteArray): String {
        val dir = File(baseDir(ctx), noteId.toString()).apply { mkdirs() }
        val out = File(dir, filename)
        val key = KeyHolder.getKey()
        val toWrite = if (key != null) CryptoManager.encryptAesGcm(data, key) else data
        out.writeBytes(toWrite)
        return out.absolutePath
    }
    fun readBytes(ctx: Context, path: String): ByteArray {
        val data = File(path).readBytes()
        val key = KeyHolder.getKey() ?: return data
        return try { CryptoManager.decryptAesGcm(data, key) } catch (t: Throwable) { data }
    }
}
