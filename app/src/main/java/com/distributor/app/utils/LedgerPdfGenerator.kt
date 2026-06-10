package com.distributor.app.utils

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ── Data classes ──────────────────────────────────────────────────────────────

data class LedgerReportData(
    val periodLabel: String,
    val revenue: Double,
    val collected: Double,
    val outstanding: Double,
    val grossProfit: Double,
    val entries: List<LedgerReportEntry>
)

data class LedgerReportEntry(
    val timestampMillis: Long,
    val description: String,
    val typeLabel: String,  // "JUAL" | "BAYAR" | "RETUR"
    val amount: Double,
    val isCredit: Boolean   // false = retur (shown in red with minus)
)

// ── Generator ─────────────────────────────────────────────────────────────────

class LedgerPdfGenerator(private val context: Context) {

    private companion object {
        const val PAGE_WIDTH: Int      = 595
        const val PAGE_HEIGHT_MIN: Int = 842
        const val MARGIN: Float        = 40f
        const val CENTER_X: Float      = PAGE_WIDTH / 2f
        const val RIGHT_EDGE: Float    = PAGE_WIDTH - MARGIN

        // Entry table column anchors
        const val COL_DESC_LEFT: Float  = MARGIN + 78f
        const val COL_TYPE_RIGHT: Float = RIGHT_EDGE - 105f
        const val COL_AMT_RIGHT: Float  = RIGHT_EDGE
    }

    fun generate(data: LedgerReportData): File {
        val config    = BusinessConfigStore.get(context)
        val logoFile  = BusinessConfigStore.getLogoFile(context)
        val logoBitmap = if (logoFile.exists()) BitmapFactory.decodeFile(logoFile.absolutePath) else null

        val ref        = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault()).format(Date())
        val outputFile = File(File(context.cacheDir, "receipts").also { it.mkdirs() }, "LAP-$ref.pdf")
        val pageHeight = computePageHeight(data.entries.size)

        val document = PdfDocument()
        try {
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, pageHeight, 1).create()
            val page     = document.startPage(pageInfo)
            drawReport(page.canvas, data, config, logoBitmap)
            document.finishPage(page)
            FileOutputStream(outputFile).use { document.writeTo(it) }
        } finally {
            document.close()
        }
        return outputFile
    }

    private fun computePageHeight(entryCount: Int): Int {
        val height = 370 + entryCount * 22 + 60
        return maxOf(PAGE_HEIGHT_MIN, height)
    }

    // ─────────────────────────────────────────────────────────────────────────

    private fun drawReport(
        canvas: Canvas,
        data: LedgerReportData,
        config: BusinessConfig,
        logoBitmap: android.graphics.Bitmap?
    ) {
        val bold18  = makePaint(18f, Color.BLACK, bold = true)
        val bold14  = makePaint(14f, Color.BLACK, bold = true)
        val body    = makePaint(11f, Color.BLACK, bold = false)
        val caption = makePaint(9f,  Color.GRAY,  bold = false)
        val gray    = makePaint(10f, Color.DKGRAY, bold = false)

        val lineThin  = strokePaint(Color.LTGRAY, 0.8f)
        val lineThick = strokePaint(Color.BLACK,  1.5f)
        val fillLight = fillPaint(Color.parseColor("#F5F5F5"))
        val fillRow   = fillPaint(Color.parseColor("#FAFAFA"))

        var y = MARGIN + 24f

        // ── 1. Business header ────────────────────────────────────────────────
        val businessName = config.businessName.ifBlank { "DISTRIBUTOR" }.uppercase()
        if (logoBitmap != null) {
            val logoSize = 56f
            val logoTop  = y - 14f
            canvas.drawBitmap(logoBitmap, null, RectF(MARGIN, logoTop, MARGIN + logoSize, logoTop + logoSize), null)
            val tx = MARGIN + logoSize + 12f
            bold18.textAlign = Paint.Align.LEFT
            canvas.drawText(businessName, tx, y, bold18); y += 16f
            if (config.ownerPhone.isNotBlank()) { body.textAlign = Paint.Align.LEFT; canvas.drawText(config.ownerPhone, tx, y, body); y += 14f }
            if (config.address.isNotBlank())    { gray.textAlign = Paint.Align.LEFT; canvas.drawText(config.address,    tx, y, gray); y += 14f }
            y = maxOf(y, logoTop + logoSize + 8f)
        } else {
            bold18.textAlign = Paint.Align.CENTER
            canvas.drawText(businessName, CENTER_X, y, bold18); y += 16f
            if (config.ownerPhone.isNotBlank()) { body.textAlign = Paint.Align.CENTER; canvas.drawText(config.ownerPhone, CENTER_X, y, body); y += 14f }
            if (config.address.isNotBlank())    { gray.textAlign = Paint.Align.CENTER; canvas.drawText(config.address,    CENTER_X, y, gray); y += 14f }
        }

        // ── 2. Report title ───────────────────────────────────────────────────
        bold14.textAlign = Paint.Align.CENTER
        canvas.drawText("LAPORAN KEUANGAN", CENTER_X, y, bold14); y += 8f
        canvas.drawLine(MARGIN, y, RIGHT_EDGE, y, lineThick); y += 14f

        body.textAlign = Paint.Align.LEFT
        canvas.drawText("Periode : ${data.periodLabel}", MARGIN, y, body); y += 14f
        caption.textAlign = Paint.Align.LEFT
        canvas.drawText("Dicetak : ${formatDate(System.currentTimeMillis())}", MARGIN, y, caption); y += 10f
        canvas.drawLine(MARGIN, y, RIGHT_EDGE, y, lineThin); y += 16f

        // ── 3. Summary ────────────────────────────────────────────────────────
        val secHdr = makePaint(9f, Color.DKGRAY, bold = true).also { it.textAlign = Paint.Align.LEFT }
        canvas.drawText("RINGKASAN KEUANGAN", MARGIN, y, secHdr); y += 14f

        val halfX    = CENTER_X - 4f
        val sumLabel = makePaint(9f,  Color.DKGRAY, bold = false)
        val sumBold  = makePaint(13f, Color.BLACK,  bold = true)

        // Row 1: Omzet | Diterima
        sumLabel.textAlign = Paint.Align.LEFT
        canvas.drawText("Omzet",    MARGIN,       y, sumLabel)
        canvas.drawText("Diterima", halfX + 8f,   y, sumLabel); y += 14f
        canvas.drawText(formatAmount(data.revenue),   MARGIN,     y, sumBold)
        canvas.drawText(formatAmount(data.collected), halfX + 8f, y,
            makePaint(13f, Color.parseColor("#2E7D32"), bold = true)); y += 20f

        // Row 2: Piutang | Laba Kotor
        canvas.drawText("Piutang",    MARGIN,     y, sumLabel)
        canvas.drawText("Laba Kotor", halfX + 8f, y, sumLabel); y += 14f
        canvas.drawText(formatAmount(data.outstanding), MARGIN, y,
            makePaint(13f, if (data.outstanding > 0.001) Color.parseColor("#C62828") else Color.parseColor("#2E7D32"), bold = true))
        canvas.drawText(formatAmount(data.grossProfit), halfX + 8f, y,
            makePaint(13f, Color.parseColor("#1565C0"), bold = true)); y += 18f

        canvas.drawLine(MARGIN, y, RIGHT_EDGE, y, lineThin); y += 14f

        // ── 4. Entries section ────────────────────────────────────────────────
        canvas.drawText("RIWAYAT TRANSAKSI  (${data.entries.size} entri)", MARGIN, y, secHdr); y += 10f

        // Column headers
        canvas.drawRect(MARGIN, y - 12f, RIGHT_EDGE, y + 6f, fillLight)
        val ch  = makePaint(9f, Color.DKGRAY, bold = true)
        val chR = makePaint(9f, Color.DKGRAY, bold = true).also { it.textAlign = Paint.Align.RIGHT }
        canvas.drawText("TANGGAL",    MARGIN,          y, ch)
        canvas.drawText("KETERANGAN", COL_DESC_LEFT,   y, ch)
        canvas.drawText("JENIS",      COL_TYPE_RIGHT,  y, chR)
        canvas.drawText("JUMLAH",     COL_AMT_RIGHT,   y, chR)
        y += 7f
        canvas.drawLine(MARGIN, y, RIGHT_EDGE, y, lineThick); y += 14f

        if (data.entries.isEmpty()) {
            caption.textAlign = Paint.Align.CENTER
            canvas.drawText("Tidak ada transaksi untuk periode ini.", CENTER_X, y, gray)
            y += 20f
        } else {
            val bodyLocal  = makePaint(11f, Color.BLACK, bold = false)
            val dateFmtShort = SimpleDateFormat("dd MMM", Locale("in", "ID"))
            val maxDescWidth = COL_TYPE_RIGHT - COL_DESC_LEFT - 10f

            data.entries.forEachIndexed { i, entry ->
                if (i % 2 == 1) canvas.drawRect(MARGIN, y - 14f, RIGHT_EDGE, y + 6f, fillRow)

                bodyLocal.textAlign = Paint.Align.LEFT
                canvas.drawText(dateFmtShort.format(Date(entry.timestampMillis)), MARGIN, y, bodyLocal)
                canvas.drawText(truncate(entry.description, bodyLocal, maxDescWidth), COL_DESC_LEFT, y, bodyLocal)

                val typeColor = when (entry.typeLabel) {
                    "RETUR" -> Color.parseColor("#C62828")
                    "JUAL"  -> Color.parseColor("#1565C0")
                    else    -> Color.parseColor("#2E7D32")
                }
                canvas.drawText(entry.typeLabel, COL_TYPE_RIGHT, y,
                    makePaint(9f, typeColor, bold = true).also { it.textAlign = Paint.Align.RIGHT })

                val amtColor = if (entry.isCredit) Color.BLACK else Color.parseColor("#C62828")
                val amtText  = if (entry.isCredit) formatAmount(entry.amount) else "-${formatAmount(entry.amount)}"
                canvas.drawText(amtText, COL_AMT_RIGHT, y,
                    makePaint(11f, amtColor, bold = false).also { it.textAlign = Paint.Align.RIGHT })

                y += 22f
            }
        }

        canvas.drawLine(MARGIN, y, RIGHT_EDGE, y, lineThick); y += 14f

        // ── 5. Footer ─────────────────────────────────────────────────────────
        caption.textAlign = Paint.Align.CENTER
        canvas.drawText("Dicetak ${formatDate(System.currentTimeMillis())}  ·  Distributor App", CENTER_X, y, caption); y += 12f
        canvas.drawText("Dokumen elektronik — tidak memerlukan tanda tangan.", CENTER_X, y, caption)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun makePaint(textSize: Float, color: Int, bold: Boolean): Paint =
        Paint().apply {
            this.textSize  = textSize
            this.color     = color
            this.typeface  = Typeface.create(Typeface.DEFAULT, if (bold) Typeface.BOLD else Typeface.NORMAL)
            this.isAntiAlias = true
        }

    private fun strokePaint(color: Int, width: Float): Paint =
        Paint().apply { this.color = color; strokeWidth = width; style = Paint.Style.STROKE }

    private fun fillPaint(color: Int): Paint =
        Paint().apply { this.color = color; style = Paint.Style.FILL }

    private fun truncate(text: String, paint: Paint, maxPx: Float): String {
        if (paint.measureText(text) <= maxPx) return text
        val ellipsis = "…"
        var result = text
        while (result.isNotEmpty() && paint.measureText(result + ellipsis) > maxPx) {
            result = result.dropLast(1)
        }
        return result + ellipsis
    }

    private fun formatDate(millis: Long): String =
        SimpleDateFormat("dd MMMM yyyy", Locale("in", "ID")).format(Date(millis))

    private fun formatAmount(amount: Double): String = formatRupiah(amount)
}
