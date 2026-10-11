package com.penandpaper.app.ocr

import android.content.Context
import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * On-device OCR (ML Kit Latin text recognizer, served via Google Play
 * Services). Returns recognized text preserving block/line structure.
 */
object Ocr {
    suspend fun recognizeText(ctx: Context, bitmap: Bitmap): String =
        suspendCancellableCoroutine { cont ->
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT)
            recognizer.process(InputImage.fromBitmap(bitmap, 0))
                .addOnSuccessListener { visionText ->
                    val sb = StringBuilder()
                    for (block in visionText.textBlocks) {
                        for (line in block.lines) {
                            sb.appendLine(line.text)
                        }
                        sb.appendLine()
                    }
                    cont.resume(sb.toString().trim())
                }
                .addOnFailureListener { e -> cont.resumeWithException(e) }
        }
}
