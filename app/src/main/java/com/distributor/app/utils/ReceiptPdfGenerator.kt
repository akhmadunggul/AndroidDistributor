package com.distributor.app.utils

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.distributor.app.data.entity.TransactionEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ── Data classes ──────────────────────────────────────────────────────────

data class PaymentReceiptData(
    val resellerName: String,
    val resellerPhone: String,
    val resellerAddress: String,
    val resellerEmail: String = "",
    val paymentAmount: Double,
    val paymentMethod: String,
    val notes: String,
    val paymentDate: Long,
    val allocations: List<PaymentAllocationLine>
)

data class PaymentAllocationLine(
    val invoiceNumber: String,
    val invoiceTotal: Double,
    val amountApplied: Double,
    val balanceAfter: Double
)

// ── Immutable receipt snapshot passed by the ViewModel ─────────────────────

data class ReceiptData(
    val transaction: TransactionEntity,
    val resellerName: String,
    val resellerPhone: String,
    val resellerAddress: String,
    val resellerEmail: String = "",
    val lineItems: List<ReceiptLineItem>
) {
    val balanceDue: Double get() = transaction.totalAmount - transaction.amountPaid
}

data class ReceiptLineItem(
    val productName: String,
    val unit: String,
    val quantity: Double,
    val unitPrice: Double,
    val subtotal: Double
)

// ── Generator ───────────────────────────────────────────────────────────────

class ReceiptPdfGenerator(private val context: Context) {

    private companion object {
        const val PAGE_WIDTH: Int = 595           // A4 portrait — PDF points (1pt = 1/72 inch)
        const val PAGE_HEIGHT_MIN: Int = 842      // A4 minimum
        const val MARGIN: Float = 40f
        const val CENTER_X: Float = PAGE_WIDTH / 2f
        const val RIGHT_EDGE: Float = PAGE_WIDTH - MARGIN

        // Table column right-edge anchors (text drawn right-aligned to these)
        const val COL_QTY_RIGHT: Float = RIGHT_EDGE - 175f
        const val COL_PRICE_RIGHT: Float = RIGHT_EDGE - 80f
        const val COL_SUBTOTAL_RIGHT: Float = RIGHT_EDGE

        // Maximum pixel width before product name is truncated with "…"
        const val COL_PRODUCT_MAX_PX: Float = COL_QTY_RIGHT - MARGIN - 12f
    }

    /**
     * Generates a PDF receipt for [data] and persists it to the app's scoped
     * cache directory. Returns the resulting [File].
     *
     * All streams are closed inside `.use {}` blocks. PdfDocument is closed
     * in a finally block to guarantee release even if writeTo() throws.
     */
    fun generate(data: ReceiptData): File {
        val config = BusinessConfigStore.get(context)
        val logoFile = BusinessConfigStore.getLogoFile(context)
        val logoBitmap = if (logoFile.exists()) BitmapFactory.decodeFile(logoFile.absolutePath) else null

        val outputFile = prepareOutputFile(data.transaction.invoiceNumber)
        val pageHeight = computePageHeight(data.lineItems.size)

        val document = PdfDocument()
        try {
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, pageHeight, 1).create()
            val page = document.startPage(pageInfo)
            drawReceipt(page.canvas, data, config, logoBitmap)
            document.finishPage(page)

            FileOutputStream(outputFile).use { stream ->
                document.writeTo(stream)
            }
        } finally {
            document.close()
        }

        return outputFile
    }

    // ── Layout ────────────────────────────────────────────────────────────────

    private fun drawReceipt(canvas: Canvas, data: ReceiptData, config: BusinessConfig, logoBitmap: android.graphics.Bitmap?) {
        // Shared paint instances (reused; textAlign reset per section)
        val paintBoldLg  = makePaint(18f, Color.BLACK,  bold = true)
        val paintBoldMd  = makePaint(13f, Color.BLACK,  bold = true)
        val paintBoldSm  = makePaint(11f, Color.BLACK,  bold = true)
        val paintBody    = makePaint(11f, Color.BLACK,  bold = false)
        val paintCaption = makePaint(9f,  Color.GRAY,   bold = false)
        val paintGray    = makePaint(10f, Color.DKGRAY, bold = false)

        val lineThin  = strokePaint(Color.LTGRAY, 0.8f)
        val lineThick = strokePaint(Color.BLACK,  1.5f)
        val fillLight = fillPaint(Color.parseColor("#F5F5F5"))
        val fillRow   = fillPaint(Color.parseColor("#FAFAFA"))

        var y = MARGIN + 24f

        // ── 1. App Header ────────────────────────────────────────────────────
        val businessName = config.businessName.ifBlank { "DISTRIBUTOR" }.uppercase()

        if (logoBitmap != null) {
            val logoSize = 60f
            val logoTop  = y - 14f
            val logoRect = RectF(MARGIN, logoTop, MARGIN + logoSize, logoTop + logoSize)
            canvas.drawBitmap(logoBitmap, null, logoRect, null)

            val textX = MARGIN + logoSize + 12f
            paintBoldLg.textAlign = Paint.Align.LEFT
            canvas.drawText(businessName, textX, y, paintBoldLg)
            y += 16f

            if (config.ownerPhone.isNotBlank()) {
                paintBody.textAlign = Paint.Align.LEFT
                canvas.drawText(config.ownerPhone, textX, y, paintBody)
                y += 14f
            }
            if (config.address.isNotBlank()) {
                paintGray.textAlign = Paint.Align.LEFT
                canvas.drawText(config.address, textX, y, paintGray)
                y += 14f
            }
            y = maxOf(y, logoTop + logoSize + 8f)
        } else {
            paintBoldLg.textAlign = Paint.Align.CENTER
            canvas.drawText(businessName, CENTER_X, y, paintBoldLg)
            y += 16f

            if (config.ownerPhone.isNotBlank()) {
                paintBody.textAlign = Paint.Align.CENTER
                canvas.drawText(config.ownerPhone, CENTER_X, y, paintBody)
                y += 14f
            }
            if (config.address.isNotBlank()) {
                paintGray.textAlign = Paint.Align.CENTER
                canvas.drawText(config.address, CENTER_X, y, paintGray)
                y += 14f
            }
        }

        paintCaption.textAlign = Paint.Align.CENTER
        canvas.drawText("STRUK INVOICE", CENTER_X, y, paintCaption)
        y += 14f

        canvas.drawLine(MARGIN, y, RIGHT_EDGE, y, lineThick)
        y += 16f

        // ── 2. Invoice Meta ──────────────────────────────────────────────────
        val metaLabel = makePaint(10f, Color.DKGRAY, bold = true)
        val metaValue = makePaint(11f, Color.BLACK,  bold = false)
        val metaRight = makePaint(10f, Color.DKGRAY, bold = false).also {
            it.textAlign = Paint.Align.RIGHT
        }

        canvas.drawText("No. Invoice", MARGIN, y, metaLabel)
        canvas.drawText(data.transaction.invoiceNumber, MARGIN + 72f, y, metaValue)
        canvas.drawText(formatDate(data.transaction.createdAt), RIGHT_EDGE, y, metaRight)
        y += 18f

        canvas.drawText("Status", MARGIN, y, metaLabel)
        val statusColor = when (data.transaction.status) {
            TransactionEntity.STATUS_PAID    -> Color.parseColor("#2E7D32")
            TransactionEntity.STATUS_PARTIAL -> Color.parseColor("#E65100")
            else                             -> Color.parseColor("#C62828")
        }
        val statusLabel = when (data.transaction.status) {
            TransactionEntity.STATUS_PAID    -> "LUNAS"
            TransactionEntity.STATUS_PARTIAL -> "CICILAN"
            else                             -> "BELUM LUNAS"
        }
        canvas.drawText(
            statusLabel,
            MARGIN + 72f, y,
            makePaint(11f, statusColor, bold = true)
        )
        y += 6f

        canvas.drawLine(MARGIN, y, RIGHT_EDGE, y, lineThin)
        y += 16f

        // ── 3. Bill To ───────────────────────────────────────────────────────
        paintCaption.textAlign = Paint.Align.LEFT
        canvas.drawText("TAGIHAN KEPADA", MARGIN, y, paintCaption)
        y += 15f

        canvas.drawText(data.resellerName, MARGIN, y, paintBoldMd)
        y += 16f

        if (data.resellerPhone.isNotBlank()) {
            canvas.drawText(data.resellerPhone, MARGIN, y, paintBody)
            y += 14f
        }
        if (data.resellerAddress.isNotBlank()) {
            canvas.drawText(data.resellerAddress, MARGIN, y, paintGray)
            y += 14f
        }
        y += 8f

        canvas.drawLine(MARGIN, y, RIGHT_EDGE, y, lineThin)
        y += 16f

        // ── 4. Table Header ──────────────────────────────────────────────────
        canvas.drawRect(MARGIN, y - 13f, RIGHT_EDGE, y + 5f, fillLight)

        val colHdrPaint = makePaint(9f, Color.DKGRAY, bold = true)
        val colHdrRight = makePaint(9f, Color.DKGRAY, bold = true).also {
            it.textAlign = Paint.Align.RIGHT
        }

        canvas.drawText("PRODUK", MARGIN, y, colHdrPaint)
        canvas.drawText("JML",          COL_QTY_RIGHT,      y, colHdrRight)
        canvas.drawText("HARGA SATUAN", COL_PRICE_RIGHT,    y, colHdrRight)
        canvas.drawText("SUBTOTAL",     COL_SUBTOTAL_RIGHT, y, colHdrRight)
        y += 7f

        canvas.drawLine(MARGIN, y, RIGHT_EDGE, y, lineThick)
        y += 15f

        // ── 5. Line Items ────────────────────────────────────────────────────
        val rowValueRight = makePaint(11f, Color.BLACK, bold = false).also {
            it.textAlign = Paint.Align.RIGHT
        }

        data.lineItems.forEachIndexed { i, item ->
            if (i % 2 == 1) {
                canvas.drawRect(MARGIN, y - 13f, RIGHT_EDGE, y + 5f, fillRow)
            }
            val nameLabel = truncate(
                "${item.productName} (${item.unit})",
                paintBody,
                COL_PRODUCT_MAX_PX
            )
            canvas.drawText(nameLabel,                  MARGIN,              y, paintBody)
            canvas.drawText(formatQty(item.quantity),   COL_QTY_RIGHT,       y, rowValueRight)
            canvas.drawText(formatAmount(item.unitPrice), COL_PRICE_RIGHT,   y, rowValueRight)
            canvas.drawText(formatAmount(item.subtotal), COL_SUBTOTAL_RIGHT, y, rowValueRight)
            y += 20f
        }

        canvas.drawLine(MARGIN, y, RIGHT_EDGE, y, lineThick)
        y += 22f

        // ── 6. Totals ────────────────────────────────────────────────────────
        val totLabelX = COL_SUBTOTAL_RIGHT - 130f
        val totValueX = COL_SUBTOTAL_RIGHT

        val totLabel = makePaint(11f, Color.DKGRAY, bold = false).also {
            it.textAlign = Paint.Align.RIGHT
        }
        val totValue = makePaint(11f, Color.BLACK, bold = false).also {
            it.textAlign = Paint.Align.RIGHT
        }

        canvas.drawText("Total", totLabelX, y, totLabel)
        canvas.drawText(formatAmount(data.transaction.totalAmount), totValueX, y, totValue)
        y += 18f

        val paidColor = if (data.transaction.amountPaid > 0.0)
            Color.parseColor("#2E7D32") else Color.DKGRAY
        canvas.drawText("Terbayar", totLabelX, y, totLabel)
        canvas.drawText(
            formatAmount(data.transaction.amountPaid), totValueX, y,
            makePaint(11f, paidColor, bold = false).also { it.textAlign = Paint.Align.RIGHT }
        )
        y += 6f

        canvas.drawLine(totLabelX - 8f, y, totValueX, y, lineThin)
        y += 14f

        val balanceColor = if (data.balanceDue > 0.001)
            Color.parseColor("#C62828") else Color.parseColor("#2E7D32")
        canvas.drawText("Sisa Tagihan",
            totLabelX, y,
            makePaint(13f, balanceColor, bold = true).also { it.textAlign = Paint.Align.RIGHT }
        )
        canvas.drawText(
            formatAmount(data.balanceDue), totValueX, y,
            makePaint(14f, balanceColor, bold = true).also { it.textAlign = Paint.Align.RIGHT }
        )
        y += 30f

        // ── 7. Footer ────────────────────────────────────────────────────────
        canvas.drawLine(MARGIN, y, RIGHT_EDGE, y, lineThin)
        y += 14f

        paintCaption.textAlign = Paint.Align.CENTER
        canvas.drawText(
            "Dibuat ${formatDate(System.currentTimeMillis())}  ·  Distributor App",
            CENTER_X, y, paintCaption
        )
        y += 12f
        canvas.drawText(
            "Dokumen elektronik — tidak memerlukan tanda tangan.",
            CENTER_X, y, paintCaption
        )
    }

    // ── Payment receipt ───────────────────────────────────────────────────────

    fun generatePaymentReceipt(data: PaymentReceiptData): File {
        val config = BusinessConfigStore.get(context)
        val logoFile = BusinessConfigStore.getLogoFile(context)
        val logoBitmap = if (logoFile.exists()) BitmapFactory.decodeFile(logoFile.absolutePath) else null

        val ref = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault()).format(Date(data.paymentDate))
        val outputFile = File(File(context.cacheDir, "receipts").also { it.mkdirs() }, "PAY-$ref.pdf")
        val pageHeight = maxOf(PAGE_HEIGHT_MIN, 360 + data.allocations.size * 22 + 80)

        val document = PdfDocument()
        try {
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, pageHeight, 1).create()
            val page = document.startPage(pageInfo)
            drawPaymentReceipt(page.canvas, data, config, logoBitmap, ref)
            document.finishPage(page)
            FileOutputStream(outputFile).use { document.writeTo(it) }
        } finally {
            document.close()
        }
        return outputFile
    }

    private fun drawPaymentReceipt(
        canvas: Canvas,
        data: PaymentReceiptData,
        config: BusinessConfig,
        logoBitmap: android.graphics.Bitmap?,
        ref: String
    ) {
        val paintBoldLg  = makePaint(18f, Color.BLACK,  bold = true)
        val paintBoldMd  = makePaint(13f, Color.BLACK,  bold = true)
        val paintBody    = makePaint(11f, Color.BLACK,  bold = false)
        val paintCaption = makePaint(9f,  Color.GRAY,   bold = false)
        val paintGray    = makePaint(10f, Color.DKGRAY, bold = false)

        val lineThin  = strokePaint(Color.LTGRAY, 0.8f)
        val lineThick = strokePaint(Color.BLACK,  1.5f)
        val fillLight = fillPaint(Color.parseColor("#F5F5F5"))
        val fillRow   = fillPaint(Color.parseColor("#FAFAFA"))

        // Column anchors for the allocation table
        val colTotalRight   = 275f
        val colAppliedRight = 405f
        val colBalanceRight = RIGHT_EDGE

        var y = MARGIN + 24f

        // ── 1. Business header (reuse same logic as invoice) ──────────────────
        val businessName = config.businessName.ifBlank { "DISTRIBUTOR" }.uppercase()
        if (logoBitmap != null) {
            val logoSize = 60f
            val logoTop  = y - 14f
            val logoRect = RectF(MARGIN, logoTop, MARGIN + logoSize, logoTop + logoSize)
            canvas.drawBitmap(logoBitmap, null, logoRect, null)
            val textX = MARGIN + logoSize + 12f
            paintBoldLg.textAlign = Paint.Align.LEFT
            canvas.drawText(businessName, textX, y, paintBoldLg); y += 16f
            if (config.ownerPhone.isNotBlank()) { paintBody.textAlign = Paint.Align.LEFT; canvas.drawText(config.ownerPhone, textX, y, paintBody); y += 14f }
            if (config.address.isNotBlank()) { paintGray.textAlign = Paint.Align.LEFT; canvas.drawText(config.address, textX, y, paintGray); y += 14f }
            y = maxOf(y, logoTop + logoSize + 8f)
        } else {
            paintBoldLg.textAlign = Paint.Align.CENTER
            canvas.drawText(businessName, CENTER_X, y, paintBoldLg); y += 16f
            if (config.ownerPhone.isNotBlank()) { paintBody.textAlign = Paint.Align.CENTER; canvas.drawText(config.ownerPhone, CENTER_X, y, paintBody); y += 14f }
            if (config.address.isNotBlank()) { paintGray.textAlign = Paint.Align.CENTER; canvas.drawText(config.address, CENTER_X, y, paintGray); y += 14f }
        }

        paintCaption.textAlign = Paint.Align.CENTER
        canvas.drawText("KUITANSI PEMBAYARAN", CENTER_X, y, paintCaption); y += 14f
        canvas.drawLine(MARGIN, y, RIGHT_EDGE, y, lineThick); y += 16f

        // ── 2. Payment meta ──────────────────────────────────────────────────
        val metaLabel = makePaint(10f, Color.DKGRAY, bold = true)
        val metaValue = makePaint(11f, Color.BLACK,  bold = false)
        val metaRight = makePaint(10f, Color.DKGRAY, bold = false).also { it.textAlign = Paint.Align.RIGHT }

        canvas.drawText("Ref",    MARGIN, y, metaLabel)
        canvas.drawText("PAY-$ref", MARGIN + 36f, y, metaValue)
        canvas.drawText(formatDate(data.paymentDate), RIGHT_EDGE, y, metaRight); y += 18f

        val methodLabel = when (data.paymentMethod) {
            "CASH"     -> "Tunai"
            "TRANSFER" -> "Transfer Bank"
            else       -> "Lainnya"
        }
        canvas.drawText("Metode", MARGIN, y, metaLabel)
        canvas.drawText(methodLabel, MARGIN + 52f, y, metaValue); y += 14f

        if (data.notes.isNotBlank()) {
            canvas.drawText("Catatan", MARGIN, y, metaLabel)
            canvas.drawText(data.notes, MARGIN + 52f, y, metaValue); y += 14f
        }
        y += 4f
        canvas.drawLine(MARGIN, y, RIGHT_EDGE, y, lineThin); y += 14f

        // ── 3. Received from ─────────────────────────────────────────────────
        paintCaption.textAlign = Paint.Align.LEFT
        canvas.drawText("DITERIMA DARI", MARGIN, y, paintCaption); y += 14f
        canvas.drawText(data.resellerName, MARGIN, y, paintBoldMd); y += 16f
        if (data.resellerPhone.isNotBlank()) { canvas.drawText(data.resellerPhone, MARGIN, y, paintBody); y += 14f }
        if (data.resellerAddress.isNotBlank()) { canvas.drawText(data.resellerAddress, MARGIN, y, paintGray); y += 14f }
        y += 8f
        canvas.drawLine(MARGIN, y, RIGHT_EDGE, y, lineThin); y += 14f

        // ── 4. Allocation table header ───────────────────────────────────────
        canvas.drawRect(MARGIN, y - 13f, RIGHT_EDGE, y + 5f, fillLight)
        val colHdr = makePaint(9f, Color.DKGRAY, bold = true)
        val colHdrR = makePaint(9f, Color.DKGRAY, bold = true).also { it.textAlign = Paint.Align.RIGHT }
        canvas.drawText("INVOICE",   MARGIN,          y, colHdr)
        canvas.drawText("JUMLAH",    colTotalRight,   y, colHdrR)
        canvas.drawText("DIBAYAR",   colAppliedRight, y, colHdrR)
        canvas.drawText("SISA",      colBalanceRight, y, colHdrR); y += 7f
        canvas.drawLine(MARGIN, y, RIGHT_EDGE, y, lineThick); y += 15f

        // ── 5. Allocation rows ───────────────────────────────────────────────
        val rowRight = makePaint(11f, Color.BLACK, bold = false).also { it.textAlign = Paint.Align.RIGHT }
        data.allocations.forEachIndexed { i, alloc ->
            if (i % 2 == 1) canvas.drawRect(MARGIN, y - 13f, RIGHT_EDGE, y + 5f, fillRow)
            val nameLabel = truncate(alloc.invoiceNumber, paintBody, colTotalRight - MARGIN - 12f)
            canvas.drawText(nameLabel, MARGIN, y, paintBody)
            canvas.drawText(formatAmount(alloc.invoiceTotal),  colTotalRight,   y, rowRight)
            canvas.drawText(formatAmount(alloc.amountApplied), colAppliedRight, y, rowRight)
            val balanceText = if (alloc.balanceAfter <= 0.001) "LUNAS"
                              else formatAmount(alloc.balanceAfter)
            val balancePaint = if (alloc.balanceAfter <= 0.001)
                makePaint(11f, Color.parseColor("#2E7D32"), bold = true).also { it.textAlign = Paint.Align.RIGHT }
                else rowRight
            canvas.drawText(balanceText, colBalanceRight, y, balancePaint)
            y += 20f
        }
        canvas.drawLine(MARGIN, y, RIGHT_EDGE, y, lineThick); y += 22f

        // ── 6. Total paid ────────────────────────────────────────────────────
        val totLabelX = colBalanceRight - 130f
        val totLabel = makePaint(11f, Color.DKGRAY, bold = false).also { it.textAlign = Paint.Align.RIGHT }
        val totValue = makePaint(14f, Color.BLACK,  bold = true).also { it.textAlign = Paint.Align.RIGHT }
        canvas.drawText("TOTAL DIBAYAR", totLabelX, y, totLabel)
        canvas.drawText(formatAmount(data.paymentAmount), colBalanceRight, y, totValue); y += 30f

        // ── 7. Footer ────────────────────────────────────────────────────────
        canvas.drawLine(MARGIN, y, RIGHT_EDGE, y, lineThin); y += 14f
        paintCaption.textAlign = Paint.Align.CENTER
        canvas.drawText("Dibuat ${formatDate(System.currentTimeMillis())}  ·  Distributor App", CENTER_X, y, paintCaption); y += 12f
        canvas.drawText("Dokumen elektronik — tidak memerlukan tanda tangan.", CENTER_X, y, paintCaption)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun prepareOutputFile(invoiceNumber: String): File {
        val dir = File(context.cacheDir, "receipts")
        dir.mkdirs()
        return File(dir, "$invoiceNumber.pdf")
    }

    private fun computePageHeight(lineItemCount: Int): Int {
        // Fixed sections: ~380 px; each item row: 20 px; minimum: A4
        val dynamicHeight = 380 + lineItemCount * 20 + 120
        return maxOf(PAGE_HEIGHT_MIN, dynamicHeight)
    }

    private fun makePaint(textSize: Float, color: Int, bold: Boolean): Paint =
        Paint().apply {
            this.textSize = textSize
            this.color = color
            this.typeface = Typeface.create(
                Typeface.DEFAULT,
                if (bold) Typeface.BOLD else Typeface.NORMAL
            )
            this.isAntiAlias = true
        }

    private fun strokePaint(color: Int, width: Float): Paint =
        Paint().apply {
            this.color = color
            this.strokeWidth = width
            this.style = Paint.Style.STROKE
        }

    private fun fillPaint(color: Int): Paint =
        Paint().apply {
            this.color = color
            this.style = Paint.Style.FILL
        }

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

    private fun formatQty(qty: Double): String =
        if (qty == qty.toLong().toDouble()) qty.toLong().toString()
        else String.format(Locale.getDefault(), "%.2f", qty)
}
