package com.penandpaper.app.export

import android.content.Context
import com.penandpaper.domain.model.BlockType
import com.penandpaper.domain.model.NoteBlock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Minimal but valid .docx writer: TEXT blocks become paragraphs.
 * (Images are noted as placeholders — full image embedding is out of scope.)
 */
object DocxExporter {
    suspend fun exportSimple(ctx: Context, title: String, blocks: List<NoteBlock>, out: File) =
        withContext(Dispatchers.IO) {
            fun esc(s: String): String = s
                .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;")

            val paras = StringBuilder()
            paras.append("<w:p><w:r><w:rPr><w:b/><w:sz w:val=\"32\"/></w:rPr><w:t>${esc(title)}</w:t></w:r></w:p>")
            for (b in blocks) {
                when (b.type) {
                    BlockType.TEXT -> {
                        val content = try {
                            JSONObject(b.metaJson ?: "{}").optString("content", "")
                        } catch (t: Throwable) { "" }
                        for (line in content.split("\n")) {
                            paras.append("<w:p><w:r><w:t xml:space=\"preserve\">${esc(line)}</w:t></w:r></w:p>")
                        }
                    }
                    BlockType.IMAGE, BlockType.SCAN ->
                        paras.append("<w:p><w:r><w:t>[Image attachment]</w:t></w:r></w:p>")
                }
            }
            val documentXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
<w:body>$paras<w:sectPr><w:pgSz w:w="11906" w:h="16838"/></w:sectPr></w:body></w:document>"""
            val contentTypes = """<?xml version="1.0" encoding="UTF-8"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
<Default Extension="xml" ContentType="application/xml"/>
<Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
</Types>"""
            val rels = """<?xml version="1.0" encoding="UTF-8"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>"""
            out.outputStream().use { fos ->
                ZipOutputStream(fos).use { zip ->
                    zip.putNextEntry(ZipEntry("[Content_Types].xml")); zip.write(contentTypes.toByteArray()); zip.closeEntry()
                    zip.putNextEntry(ZipEntry("_rels/.rels")); zip.write(rels.toByteArray()); zip.closeEntry()
                    zip.putNextEntry(ZipEntry("word/document.xml")); zip.write(documentXml.toByteArray()); zip.closeEntry()
                }
            }
        }
}
