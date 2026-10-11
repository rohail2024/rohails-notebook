package com.penandpaper.app.camera

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.penandpaper.app.ui.editor.EditorViewModel
import com.penandpaper.app.ui.theme.FolderTheme
import kotlinx.coroutines.launch
import java.io.File

/**
 * Self-contained CameraX capture screen: live preview, tap-to-focus,
 * pinch-to-zoom, flash cycle, front/back switch, JPEG capture.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    onCaptured: (File) -> Unit,
    onClose: () -> Unit
) {
    val ctx = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mainExecutor = remember { ContextCompat.getMainExecutor(ctx) }

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }

    if (!hasPermission) {
        Scaffold(topBar = { TopAppBar(colors = FolderTheme.appBarColors(), title = { Text("Camera") }) }) { pad ->
            Column(
                Modifier.padding(pad).fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Camera permission is needed to take photos.", style = MaterialTheme.typography.bodyLarge)
                Button(onClick = { permLauncher.launch(Manifest.permission.CAMERA) }) {
                    Text("Grant camera permission")
                }
                FilledTonalButton(onClick = onClose) { Text("Cancel") }
            }
        }
        return
    }

    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    var flashMode by remember { mutableStateOf(ImageCapture.FLASH_MODE_OFF) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var binding by remember { mutableStateOf(false) }

    val providerFuture = remember { ProcessCameraProvider.getInstance(ctx) }

    // Bind / rebind camera when lens or flash changes.
    LaunchedEffect(lensFacing, flashMode, previewView) {
        val pv = previewView ?: return@LaunchedEffect
        binding = true
        providerFuture.addListener({
            try {
                val provider = providerFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(pv.surfaceProvider)
                }
                val ic = ImageCapture.Builder()
                    .setFlashMode(flashMode)
                    .build()
                imageCapture = ic
                provider.unbindAll()
                camera = provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.Builder().requireLensFacing(lensFacing).build(),
                    preview,
                    ic
                )
            } catch (t: Throwable) {
                Log.e("CameraScreen", "bind failed", t)
                Toast.makeText(ctx, "Camera unavailable", Toast.LENGTH_SHORT).show()
            } finally {
                binding = false
            }
        }, mainExecutor)
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                if (providerFuture.isDone) providerFuture.get().unbindAll()
            } catch (t: Throwable) {
                Log.w("CameraScreen", "unbind failed", t)
            }
        }
    }

    fun takePhoto() {
        val ic = imageCapture ?: return
        val outFile = File(ctx.cacheDir, "capture_${System.currentTimeMillis()}.jpg")
        val opts = ImageCapture.OutputFileOptions.Builder(outFile).build()
        ic.takePicture(opts, mainExecutor, object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                onCaptured(outFile)
            }

            override fun onError(exc: ImageCaptureException) {
                Log.e("CameraScreen", "capture failed", exc)
                Toast.makeText(ctx, "Capture failed", Toast.LENGTH_SHORT).show()
            }
        })
    }

    val flashLabel = when (flashMode) {
        ImageCapture.FLASH_MODE_ON -> "Flash: On"
        ImageCapture.FLASH_MODE_AUTO -> "Flash: Auto"
        else -> "Flash: Off"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = FolderTheme.appBarColors(),
                title = { Text("Camera") },
                actions = {
                    FilledTonalButton(onClick = {
                        flashMode = when (flashMode) {
                            ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_ON
                            ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_AUTO
                            else -> ImageCapture.FLASH_MODE_OFF
                        }
                    }) { Text(flashLabel) }
                }
            )
        }
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            AndroidView(
                factory = { c ->
                    PreviewView(c).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                        val scaleDetector = ScaleGestureDetector(
                            c,
                            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                                override fun onScale(detector: ScaleGestureDetector): Boolean {
                                    val cam = camera ?: return false
                                    val zs = cam.cameraInfo.zoomState.value ?: return false
                                    val newRatio = (zs.zoomRatio * detector.scaleFactor)
                                        .coerceIn(zs.minZoomRatio, zs.maxZoomRatio)
                                    cam.cameraControl.setZoomRatio(newRatio)
                                    return true
                                }
                            }
                        )
                        setOnTouchListener { v, e ->
                            scaleDetector.onTouchEvent(e)
                            if (e.action == MotionEvent.ACTION_UP && !scaleDetector.isInProgress) {
                                val cam = camera
                                if (cam != null) {
                                    val point = meteringPointFactory.createPoint(e.x, e.y)
                                    val action = FocusMeteringAction.Builder(point).build()
                                    cam.cameraControl.startFocusAndMetering(action)
                                }
                                v.performClick()
                            }
                            true
                        }
                        previewView = this
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            if (binding) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            }
            Row(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(onClick = {
                    lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK)
                        CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
                }) { Text("Flip") }
                Button(onClick = { takePhoto() }) { Text("Capture") }
                FilledTonalButton(onClick = onClose) { Text("Cancel") }
            }
        }
    }
}

/** Navigation route: captures a photo and imports it into the note as an IMAGE block. */
@Composable
fun CameraRoute(
    noteId: Long,
    onDone: () -> Unit,
    vm: EditorViewModel = hiltViewModel()
) {
    val scope = rememberCoroutineScope()
    LaunchedEffect(noteId) { vm.load(noteId) }
    CameraScreen(
        onCaptured = { file ->
            scope.launch {
                try {
                    vm.importPhoto(noteId, file)
                } catch (t: Throwable) {
                    Log.w("CameraRoute", "import failed", t)
                } finally {
                    onDone()
                }
            }
        },
        onClose = onDone
    )
}
