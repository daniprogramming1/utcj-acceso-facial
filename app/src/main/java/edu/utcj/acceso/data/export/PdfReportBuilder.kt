package edu.utcj.acceso.data.export

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import edu.utcj.acceso.brand.BrandConfig
import edu.utcj.acceso.domain.model.AccessEvent
import edu.utcj.acceso.domain.model.AccessMethod
import edu.utcj.acceso.domain.model.AccessResult
import edu.utcj.acceso.util.TimeUtil
import java.io.File

/**
 * Reporte PDF de bitácora con encabezado de marca, resumen y tabla paginada.
 * Usa solo `android.graphics.pdf.PdfDocument` (sin dependencias externas).
 */
class PdfReportBuilder(
    private val title: String = "Bitácora de acceso",
    private val rangeLabel: String = "Todos los registros"
) {
    private val pageW = 595
    private val pageH = 842
    private val margin = 36f
    private val rowH = 20f
    private val headerH = 112f
    private val footerH = 36f

    private val brand = BrandConfig.palette
    private fun argb(c: androidx.compose.ui.graphics.Color): Int =
        Color.argb((c.alpha * 255).toInt(), (c.red * 255).toInt(), (c.green * 255).toInt(), (c.blue * 255).toInt())

    private val primary = argb(brand.primary)
    private val secondary = argb(brand.secondary)
    private val navy = argb(brand.navy)
    private val success = Color.rgb(21, 128, 61)
    private val danger = Color.rgb(198, 40, 40)
    private val info = Color.rgb(29, 78, 216)
    private val ink = Color.rgb(15, 23, 42)
    private val muted = Color.rgb(100, 116, 139)
    private val zebra = Color.rgb(246, 248, 251)
    private val line = Color.rgb(226, 232, 239)

    private val bold = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    private val regular = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)

    private data class Col(val title: String, val width: Float)

    private val cols = listOf(
        Col("Fecha y hora", 92f), Col("Matrícula", 66f), Col("Nombre", 128f),
        Col("Resultado", 64f), Col("Método", 64f), Col("Detalle", 0f) // resto
    )

    fun write(events: List<AccessEvent>, file: File, generatedAtMs: Long = System.currentTimeMillis()): File {
        val pdf = PdfDocument()
        val firstPageRows = ((pageH - headerH - 96f - footerH - margin - rowH) / rowH).toInt()
        val otherPageRows = ((pageH - margin - footerH - margin - rowH) / rowH).toInt()
        val totalPages = if (events.size <= firstPageRows) 1
        else 1 + Math.ceil((events.size - firstPageRows) / otherPageRows.toDouble()).toInt()

        var index = 0
        for (pageNum in 1..totalPages) {
            val page = pdf.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, pageNum).create())
            val c = page.canvas
            var y: Float
            if (pageNum == 1) {
                drawHeader(c, generatedAtMs)
                y = drawSummary(c, events, headerH + 20f)
            } else {
                y = margin
            }
            y = drawTableHeader(c, y)
            val rows = if (pageNum == 1) firstPageRows else otherPageRows
            var r = 0
            while (r < rows && index < events.size) {
                drawRow(c, events[index], y, r % 2 == 1)
                y += rowH
                r++
                index++
            }
            if (events.isEmpty() && pageNum == 1) {
                val p = paint(11f, muted)
                c.drawText("No hay registros para el filtro seleccionado.", margin, y + 24f, p)
            }
            drawFooter(c, pageNum, totalPages)
            pdf.finishPage(page)
        }
        file.outputStream().use { pdf.writeTo(it) }
        pdf.close()
        return file
    }

    private fun paint(size: Float, color: Int, isBold: Boolean = false) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size
        this.color = color
        typeface = if (isBold) bold else regular
    }

    private fun drawHeader(c: Canvas, generatedAtMs: Long) {
        val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, 0f, pageW.toFloat(), headerH, primary, secondary, Shader.TileMode.CLAMP)
        }
        c.drawRect(0f, 0f, pageW.toFloat(), headerH, bg)
        // Marca angular (misma geometría que ic_brand_logo)
        val s = 44f / 48f
        val ox = margin
        val oy = 28f
        val white = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        val tile = RectF(ox - 6f, oy - 6f, ox + 50f, oy + 50f)
        c.drawRoundRect(tile, 12f, 12f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(40, 255, 255, 255) })
        val p1 = Path().apply {
            moveTo(ox + 5 * s, oy + 41 * s); lineTo(ox + 21 * s, oy + 7 * s); lineTo(ox + 28 * s, oy + 7 * s)
            lineTo(ox + 13 * s, oy + 41 * s); close()
        }
        val p2 = Path().apply {
            moveTo(ox + 28 * s, oy + 7 * s); lineTo(ox + 44 * s, oy + 41 * s); lineTo(ox + 36 * s, oy + 41 * s)
            lineTo(ox + 24.5f * s, oy + 15 * s); close()
        }
        val p3 = Path().apply {
            moveTo(ox + 18 * s, oy + 41 * s); lineTo(ox + 24.5f * s, oy + 28 * s); lineTo(ox + 31 * s, oy + 41 * s); close()
        }
        c.drawPath(p1, white)
        c.drawPath(p2, Paint(white).apply { alpha = 210 })
        c.drawPath(p3, Paint(white).apply { alpha = 170 })

        val tx = margin + 64f
        c.drawText(title, tx, 50f, paint(20f, Color.WHITE, true))
        c.drawText("${BrandConfig.APP_NAME} · ${BrandConfig.INSTITUTION_NAME}", tx, 68f, paint(10f, Color.argb(230, 255, 255, 255)))
        c.drawText("Periodo: $rangeLabel", tx, 84f, paint(10f, Color.argb(230, 255, 255, 255)))
        val gen = "Generado: ${TimeUtil.formatDateTime(generatedAtMs)}"
        val gp = paint(9f, Color.argb(220, 255, 255, 255))
        c.drawText(gen, pageW - margin - gp.measureText(gen), 100f, gp)
    }

    private fun drawSummary(c: Canvas, events: List<AccessEvent>, top: Float): Float {
        val ok = events.count { it.result != AccessResult.DENIED }
        val denied = events.count { it.result == AccessResult.DENIED }
        val rate = if (events.isEmpty()) "—" else "${(ok * 100f / events.size).toInt()} %"
        val boxes = listOf(
            "Registros" to "${events.size}" to navy,
            "Permitidos" to "$ok" to success,
            "Denegados" to "$denied" to danger,
            "Tasa de éxito" to rate to primary
        )
        val gap = 10f
        val w = (pageW - margin * 2 - gap * 3) / 4
        val h = 56f
        boxes.forEachIndexed { i, (lv, color) ->
            val (label, value) = lv
            val x = margin + i * (w + gap)
            val rect = RectF(x, top, x + w, top + h)
            c.drawRoundRect(rect, 10f, 10f, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = zebra })
            c.drawRoundRect(RectF(x, top, x + 4f, top + h), 2f, 2f, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color })
            c.drawText(label, x + 14f, top + 20f, paint(9f, muted))
            c.drawText(value, x + 14f, top + 44f, paint(18f, ink, true))
        }
        return top + h + 20f
    }

    private fun colX(i: Int): Float = margin + cols.take(i).sumOf { it.width.toDouble() }.toFloat()
    private fun colW(i: Int): Float =
        if (cols[i].width > 0f) cols[i].width else (pageW - margin * 2) - cols.sumOf { it.width.toDouble() }.toFloat()

    private fun drawTableHeader(c: Canvas, top: Float): Float {
        c.drawRoundRect(RectF(margin, top, pageW - margin, top + rowH), 6f, 6f, Paint().apply { color = navy })
        val p = paint(8.5f, Color.WHITE, true)
        cols.forEachIndexed { i, col -> c.drawText(col.title.uppercase(), colX(i) + 6f, top + 13.5f, p) }
        return top + rowH
    }

    private fun ellipsize(text: String, p: Paint, max: Float): String {
        if (p.measureText(text) <= max) return text
        var t = text
        while (t.isNotEmpty() && p.measureText("$t…") > max) t = t.dropLast(1)
        return "$t…"
    }

    private fun resultLabel(r: AccessResult) = when (r) {
        AccessResult.ALLOWED -> "Permitido"; AccessResult.DENIED -> "Denegado"
        AccessResult.MANUAL -> "Manual"; AccessResult.QR -> "QR"
    }

    private fun methodLabel(m: AccessMethod) = when (m) {
        AccessMethod.FACE -> "Rostro"; AccessMethod.FINGERPRINT -> "Huella"; AccessMethod.QR -> "QR"
        AccessMethod.MANUAL -> "Manual"; AccessMethod.FALLBACK -> "Respaldo"
    }

    private fun drawRow(c: Canvas, e: AccessEvent, top: Float, striped: Boolean) {
        if (striped) c.drawRect(margin, top, pageW - margin, top + rowH, Paint().apply { color = zebra })
        c.drawLine(margin, top + rowH, pageW - margin, top + rowH, Paint().apply { color = line; strokeWidth = 0.5f })
        val p = paint(8.5f, ink)
        val base = top + 13.5f
        val detail = listOfNotNull(
            e.reason, e.authorizingGuard?.let { "Guardia: $it" },
            e.similarity?.let { "Sim. ${"%.2f".format(it)}" }
        ).joinToString(" · ")
        val values = listOf(
            TimeUtil.formatDateTime(e.datetimeMs), e.matricula, e.nombre, "", methodLabel(e.method), detail
        )
        values.forEachIndexed { i, v ->
            if (i == 3) return@forEachIndexed
            c.drawText(ellipsize(v, p, colW(i) - 10f), colX(i) + 6f, base, p)
        }
        val rc = when (e.result) {
            AccessResult.ALLOWED -> success; AccessResult.DENIED -> danger
            AccessResult.MANUAL -> info; AccessResult.QR -> primary
        }
        val label = resultLabel(e.result)
        val lp = paint(8f, rc, true)
        val x = colX(3) + 6f
        val w = lp.measureText(label) + 12f
        c.drawRoundRect(
            RectF(x, top + 4f, x + w, top + rowH - 4f), 6f, 6f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = rc; alpha = 28 }
        )
        c.drawText(label, x + 6f, base - 0.5f, lp)
    }

    private fun drawFooter(c: Canvas, page: Int, total: Int) {
        val y = pageH - 20f
        c.drawLine(margin, y - 14f, pageW - margin, y - 14f, Paint().apply { color = line })
        val p = paint(8.5f, muted)
        c.drawText("${BrandConfig.PRODUCT_NAME} · Documento de solo lectura · ${BrandConfig.SUPPORT_EMAIL}", margin, y, p)
        val pg = "Página $page de $total"
        c.drawText(pg, pageW - margin - p.measureText(pg), y, p)
    }
}
