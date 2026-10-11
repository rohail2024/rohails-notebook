
package com.penandpaper.app.richtext
import org.json.JSONArray
import org.json.JSONObject

data class Run(val text: String, val bold: Boolean=false, val italic: Boolean=false, val underline: Boolean=false)
data class Paragraph(val runs: List<Run> = listOf(Run("")), val checklist: Boolean = false, val checked: Boolean = false, val ordered: Boolean = false)
data class RichDoc(val paragraphs: List<Paragraph> = listOf(Paragraph()))

fun RichDoc.toJson(): String = JSONObject().apply {
    put("paragraphs", JSONArray().apply {
        paragraphs.forEach { p ->
            put(JSONObject().apply {
                put("checklist", p.checklist); put("checked", p.checked); put("ordered", p.ordered)
                put("runs", JSONArray().apply {
                    p.runs.forEach { r ->
                        put(JSONObject().apply { put("t", r.text); put("b", r.bold); put("i", r.italic); put("u", r.underline) })
                    }
                })
            })
        }
    })
}.toString()

fun richFromJson(json: String?): RichDoc = try {
    val obj = JSONObject(json ?: "{}")
    val arr = obj.optJSONArray("paragraphs") ?: JSONArray()
    val paras = (0 until arr.length()).map { idx ->
        val po = arr.getJSONObject(idx)
        val runsArr = po.optJSONArray("runs") ?: JSONArray()
        val runs = (0 until runsArr.length()).map { ridx ->
            val ro = runsArr.getJSONObject(ridx)
            Run(ro.optString("t",""), ro.optBoolean("b"), ro.optBoolean("i"), ro.optBoolean("u"))
        }
        Paragraph(runs, po.optBoolean("checklist"), po.optBoolean("checked"), po.optBoolean("ordered"))
    }
    RichDoc(paras.ifEmpty { listOf(Paragraph()) })
} catch (t: Throwable) { RichDoc() }
