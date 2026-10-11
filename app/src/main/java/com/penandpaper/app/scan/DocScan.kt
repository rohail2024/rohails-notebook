package com.penandpaper.app.scan

import android.app.Activity
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.mlkit.vision.documentscanner.GmsDocumentScanner
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.penandpaper.app.ui.editor.EditorViewModel
import com.penandpaper.app.ui.theme.FolderTheme
import kotlinx.coroutines.launch

/**
 * Real on-device document scanner options (ML Kit via Google Play Services):
 * full scanner mode = edge detection, auto-capture, perspective correction,
 * color / grayscale / black-and-white filters and multipage capture.
 */
fun docScannerOptions(): GmsDocumentScannerOptions =
    GmsDocumentScannerOptions.Builder()
        .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
        .setGalleryImportAllowed(true)
        .setPageLimit(20)
        .setResultFormats(
            GmsDocumentScannerOptions.RESULT_FORMAT_JPEG,
            GmsDocumentScannerOptions.RESULT_FORMAT_PDF
        )
        .build()

/**
 * Remembers the ML Kit document-scanner flow. Returns a `launch()` function:
 * call it (e.g. from a button or on first composition) to open the scanner UI.
 * [onResult] receives the scanned page image URIs (empty if cancelled/failed).
 */
@Composable
fun rememberDocScanLauncher(onResult: (List<Uri>) -> Unit): () -> Unit {
    val ctx = LocalContext.current
    val latestOnResult by rememberUpdatedState(onResult)
    val launcher: ActivityResultLauncher<IntentSenderRequest> =
        rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            val data = result.data
            val uris = if (result.resultCode == Activity.RESULT_OK && data != null) {
                try {
                    GmsDocumentScanningResult.fromActivityResultIntent(data)
                        ?.pages?.mapNotNull { it.imageUri }.orEmpty()
                } catch (t: Throwable) {
                    Log.w("DocScan", "parseResult failed", t)
                    emptyList()
                }
            } else {
                emptyList()
            }
            latestOnResult(uris)
        }
    return {
        val activity = ctx as? Activity
        if (activity == null) {
            Log.w("DocScan", "No Activity context for scanner")
            latestOnResult(emptyList())
            return@let
        }
        try {
            val client = GmsDocumentScanner.getClient(docScannerOptions())
            client.getStartScanIntent(activity)
                .addOnSuccessListener { intentSender ->
                    launcher.launch(IntentSenderRequest.Builder(intentSender).build())
                }
                .addOnFailureListener { e ->
                    Log.w("DocScan", "getStartScanIntent failed", e)
                    latestOnResult(emptyList())
                }
        } catch (t: Throwable) {
            Log.w("DocScan", "Scanner unavailable", t)
            latestOnResult(emptyList())
        }
    }
}

/**
 * Full-screen route: opens the document scanner immediately, imports the
 * scanned pages into the note as SCAN blocks, then calls [onDone].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanImportScreen(
    noteId: Long,
    onDone: () -> Unit,
    vm: EditorViewModel = hiltViewModel()
) {
    val scope = rememberCoroutineScope()
    var started by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Opening scanner…") }

    val launchScan = rememberDocScanLauncher { uris ->
        if (uris.isEmpty()) {
            onDone()
        } else {
            status = "Importing ${uris.size} page(s)…"
            scope.launch {
                try {
                    vm.importScans(noteId, uris)
                } catch (t: Throwable) {
                    Log.w("DocScan", "import failed", t)
                }
                onDone()
            }
        }
    }

    LaunchedEffect(noteId) {
        vm.load(noteId)
        if (!started) {
            started = true
            launchScan()
        }
    }

    Scaffold(topBar = { TopAppBar(colors = FolderTheme.appBarColors(), title = { Text("Scan document") }) }) { pad ->
        Box(Modifier.padding(pad).fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CircularProgressIndicator()
                Text(status)
            }
        }
    }
}
