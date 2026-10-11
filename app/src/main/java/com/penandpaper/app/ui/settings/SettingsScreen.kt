
package com.penandpaper.app.ui.settings
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.penandpaper.app.backup.BackupManager
import com.penandpaper.app.saf.rememberCreateDocumentLauncher
import com.penandpaper.app.saf.rememberOpenDocumentLauncher
import java.io.File

@Composable
fun SettingsScreen() {
    val ctx = LocalContext.current
    val createBackup = rememberCreateDocumentLauncher("application/zip") { uri: Uri? ->
        if (uri != null) {
            val db = ctx.getDatabasePath("penandpaper.db")
            val blobs = File(ctx.filesDir, "notes")
            val tmp = File(ctx.cacheDir, "backup.zip")
            BackupManager.backupToFile(ctx, db, blobs, tmp)
            ctx.contentResolver.openOutputStream(uri)?.use { tmp.inputStream().use { inp -> inp.copyTo(it) } }
            Toast.makeText(ctx, "Backup saved", Toast.LENGTH_LONG).show()
        }
    }
    val openZip = rememberOpenDocumentLauncher(arrayOf("application/zip")) { uri: Uri? ->
        if (uri != null) {
            val db = ctx.getDatabasePath("penandpaper.db")
            val blobs = File(ctx.filesDir, "notes")
            val tmp = File(ctx.cacheDir, "restore.zip")
            ctx.contentResolver.openInputStream(uri)?.use { input -> tmp.outputStream().use { input.copyTo(it) } }
            BackupManager.restoreFromFile(ctx, tmp, db, blobs)
            Toast.makeText(ctx, "Restore completed", Toast.LENGTH_LONG).show()
        }
    }
    val importFont = rememberOpenDocumentLauncher(arrayOf("font/ttf","font/otf","application/octet-stream")) { uri: Uri? ->
        if (uri != null) {
            // TODO: persist and register font URI
            Toast.makeText(ctx, "Font imported", Toast.LENGTH_LONG).show()
        }
    }
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Backup & Restore", style = MaterialTheme.typography.titleMedium)
        Button(onClick = { createBackup.launch("RohailNotebook-backup.zip") }) { Text("Create Local Backup (Choose location)") }
        Button(onClick = { openZip.launch(arrayOf("application/zip")) }) { Text("Restore from ZIP (Choose file)") }
        Divider()
        Text("Appearance", style = MaterialTheme.typography.titleMedium)
            var useAccent by remember { mutableStateOf(com.penandpaper.app.ui.theme.FolderTheme.useAccentForHighlights) }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Switch(checked = useAccent, onCheckedChange = { v -> useAccent = v; com.penandpaper.app.ui.theme.FolderTheme.useAccentForHighlights = v })
                Text("Use folder accent for highlights")
            }
            Divider()
            Text("Fonts", style = MaterialTheme.typography.titleMedium)
        Button(onClick = { importFont.launch(arrayOf("font/ttf","font/otf")) }) { Text("Import TTF/OTF") }
    }
}
