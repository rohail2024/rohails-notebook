package com.penandpaper.app.export

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.penandpaper.app.storage.AttachmentStorage
import com.penandpaper.domain.model.BlockType
import com.penandpaper.domain.model.NoteBlock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

/**
 * Minimal but real PDF export: title page header, TEXT blocks as wrapped text,
 * IMAGE/SCAN blocks rendered as scaled bitmaps decoded from AttachmentStorage.
 */
object PdfExporter {
    suspend fun exportSimple(ctx: Context, title: String, blocks: List<NoteBlock>, out: File) =
        withContext(Dispatchers.IO) {
            val doc = PdfDocument()
            try {
                val pageW = 595
                val pageH = 842
                val margin = 48f
                val titlePaint = Paint().apply { textSize = 22f; isFakeBoldText = true }
                val bodyPaint = Paint().apply { textSize = 12f }
                var page = doc.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, 1).create())
                var y = margin + 24f
                var pageNo = 1

                fun newPageIfNeeded(need: Float) {
                    if (y + need > pageH - margin) {
                        doc.finishPage(page)
                        pageNo++
                        page = doc.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, pageNo).create())
                        y = margin
                    }
                }

                val canvas0 = page.canvas
                canvas0.drawText(title, margin, y, titlePaint)
                y += 28f

                for (b in blocks) {
                    when (b.type) {
                        BlockType.TEXT -> {
                            val content = try {
                                JSONObject(b.metaJson ?: "{}").optString("content", "")
                            } catch (t: Throwable) { "" }
                            if (content.isBlank()) continue
                            for (para in content.split("\n")) {
                                var rest = para
                                while (rest.isNotEmpty()) {
                                    val n = bodyPaint.breakText(rest, true, pageW - 2 * margin, null)
                                    newPageIfNeeded(18f)
                                    page.canvas.drawText(rest.substring(0, n), margin, y, bodyPaint)
                                    y += 16f
                                    rest = rest.substring(n)
                                }
                                y += 6f
                            }
                        }
                        BlockType.IMAGE, BlockType.SCAN -> {
                            val bmp = try {
                                b.payloadRef?.let { AttachmentStorage.readBytes(ctx, it) }
                                    ?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
                            } catch (t: Throwable) { null }
                            if (bmp != null) {
                                val maxW = pageW - 2 * margin
                                val s = (maxW / bmp.width).coerceAtMost(1f)
                                val dw = bmp.width * s
                                val dh = bmp.height * s
                                newPageIfNeeded(dh + 12f)
                                val dst = android.graphics.RectF(margin, y, margin + dw, y + dh)
                                page.canvas.drawBitmap(bmp, null, dst, null)
                                y += dh + 12f
                                bmp.recycle()
                            }
                        }
                    }
                }
                doc.finishPage(page)
                FileOutputStream(out).use { doc.writeTo(it) }
            } finally {
                doc.close()
            }
        }
}
