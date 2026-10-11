
package com.penandpaper.app.backup
import android.content.Context
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import java.io.FileInputStream
import java.io.FileOutputStream

object BackupManager {
    fun backupToFile(context: Context, dbPath: File, blobsDir: File, outFile: File) {
        ZipOutputStream(FileOutputStream(outFile)).use { zip ->
            fun putFile(basePath: String, file: File) {
                val entryName = basePath + file.name
                zip.putNextEntry(ZipEntry(entryName))
                file.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
            putFile("", dbPath)
            blobsDir.walkTopDown().filter { it.isFile }.forEach { f ->
                val rel = f.relativeTo(blobsDir).path.replace("\\", "/")
                zip.putNextEntry(ZipEntry("blobs/" + rel))
                f.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
    }
    fun restoreFromFile(context: Context, zipFile: File, dbOut: File, blobsDir: File) {
        ZipInputStream(FileInputStream(zipFile)).use { zin ->
            var e = zin.nextEntry
            while (e != null) {
                if (e.name == dbOut.name) {
                    FileOutputStream(dbOut).use { zin.copyTo(it) }
                } else if (e.name.startsWith("blobs/")) {
                    val out = File(blobsDir, e.name.removePrefix("blobs/"))
                    out.parentFile?.mkdirs()
                    FileOutputStream(out).use { zin.copyTo(it) }
                }
                zin.closeEntry()
                e = zin.nextEntry
            }
        }
    }
}


fun encryptBackupZip(rawZip: File, outFile: File, key: ByteArray) {
    val data = rawZip.readBytes()
    val enc = com.penandpaper.app.crypto.CryptoManager.encryptAesGcm(data, key)
    outFile.writeBytes(enc)
}
fun decryptBackupZip(encFile: File, outZip: File, key: ByteArray) {
    val dec = com.penandpaper.app.crypto.CryptoManager.decryptAesGcm(encFile.readBytes(), key)
    outZip.writeBytes(dec)
}
