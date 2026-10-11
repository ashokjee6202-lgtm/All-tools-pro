package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.model.RecentFileManager
import java.io.File
import java.io.FileOutputStream

object SampleDataProvider {

    fun createSampleDocumentBitmap(): Bitmap {
        val width = 1200
        val height = 1600
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Clean white document paper
        canvas.drawColor(Color.WHITE)

        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(21, 101, 192)
            textSize = 54f
            isFakeBoldText = true
        }

        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(100, 116, 139)
            textSize = 28f
        }

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 41, 59)
            textSize = 32f
        }

        val boldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            textSize = 34f
            isFakeBoldText = true
        }

        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(203, 213, 225)
            strokeWidth = 3f
        }

        // Draw header
        canvas.drawText("TAX INVOICE / बिल", 80f, 130f, headerPaint)
        canvas.drawText("Invoice #: ATP-2026-9041   •   Date: 12 Oct 2026", 80f, 180f, subPaint)
        canvas.drawLine(80f, 220f, 1120f, 220f, linePaint)

        // Customer details
        canvas.drawText("Billed To:", 80f, 280f, boldPaint)
        canvas.drawText("Ashok Kumar / ऑलटूल्स प्रो ग्राहक", 80f, 330f, textPaint)
        canvas.drawText("Email: ashokjee@example.com", 80f, 380f, textPaint)
        canvas.drawText("Account ID: ATP-PREMIUM-USER", 80f, 430f, textPaint)

        canvas.drawLine(80f, 490f, 1120f, 490f, linePaint)

        // Items table header
        val tableHeadPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(241, 245, 249)
        }
        canvas.drawRect(80f, 520f, 1120f, 580f, tableHeadPaint)
        canvas.drawText("ITEM DESCRIPTION", 100f, 562f, boldPaint)
        canvas.drawText("QTY", 700f, 562f, boldPaint)
        canvas.drawText("AMOUNT", 940f, 562f, boldPaint)

        // Items
        val items = listOf(
            Triple("1. Document Scanning & OCR Pro", "1", "₹ 499.00"),
            Triple("2. Unlimited PDF Resizer & Compressor", "1", "₹ 799.00"),
            Triple("3. Handwriting Generator Module", "1", "₹ 299.00"),
            Triple("4. High-Speed UPI QR Suite", "1", "₹ 199.00"),
            Triple("5. 100% Offline Privacy Engine", "1", "FREE")
        )

        var y = 640f
        for (item in items) {
            canvas.drawText(item.first, 100f, y, textPaint)
            canvas.drawText(item.second, 720f, y, textPaint)
            canvas.drawText(item.third, 940f, y, textPaint)
            canvas.drawLine(80f, y + 25f, 1120f, y + 25f, linePaint)
            y += 80f
        }

        // Total
        canvas.drawText("Subtotal:  ₹ 1,796.00", 720f, y + 40f, textPaint)
        canvas.drawText("Total Payable:  ₹ 1,796.00", 680f, y + 100f, boldPaint)

        // Stamp badge
        val stampPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(46, 125, 50)
            style = Paint.Style.STROKE
            strokeWidth = 6f
        }
        val stampFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(46, 125, 50)
            textSize = 36f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawRoundRect(140f, 1220f, 440f, 1340f, 20f, 20f, stampPaint)
        canvas.drawText("PAID / सत्यापित", 290f, 1295f, stampFill)

        // Footer note
        canvas.drawText("Thank you for using AllToolsPro (100% On-Device Safe)", 80f, 1500f, subPaint)

        return bitmap
    }

    fun createSamplePhotoBitmap(): Bitmap {
        val width = 1200
        val height = 1200
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Radiant sky gradient
        val bgPaint = Paint().apply {
            color = Color.rgb(187, 222, 251) // Sky blue
        }
        canvas.drawRect(0f, 0f, 1200f, 800f, bgPaint)

        // Ground/landscape
        val groundPaint = Paint().apply {
            color = Color.rgb(76, 175, 80) // Green hill
        }
        canvas.drawRect(0f, 800f, 1200f, 1200f, groundPaint)

        // Sun
        val sunPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(255, 193, 7)
        }
        canvas.drawCircle(300f, 300f, 140f, sunPaint)

        // Mountains
        val mountainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(90, 110, 140)
        }
        val path = android.graphics.Path().apply {
            moveTo(100f, 800f)
            lineTo(450f, 400f)
            lineTo(800f, 800f)
            close()
        }
        canvas.drawPath(path, mountainPaint)

        val mountainPaint2 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(65, 85, 115)
        }
        val path2 = android.graphics.Path().apply {
            moveTo(500f, 800f)
            lineTo(850f, 450f)
            lineTo(1200f, 800f)
            close()
        }
        canvas.drawPath(path2, mountainPaint2)

        // Text banner
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 58f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
            setShadowLayer(8f, 0f, 4f, Color.DKGRAY)
        }
        canvas.drawText("AllToolsPro High-Res Sample Photo", 600f, 960f, textPaint)
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(230, 255, 230)
            textSize = 34f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("1200 x 1200 px  •  Ready for Resize & Compression", 600f, 1030f, subPaint)

        return bitmap
    }

    suspend fun getOrCreateSamplePdf(context: Context): File = withContext(Dispatchers.IO) {
        val file = File(context.cacheDir, "Sample_Report_AllToolsPro.pdf")
        if (file.exists() && file.length() > 0) return@withContext file

        val pdfDoc = PdfDocument()

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(21, 101, 192)
            textSize = 26f
            isFakeBoldText = true
        }

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 41, 59)
            textSize = 14f
        }

        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.LTGRAY
            strokeWidth = 1.5f
        }

        // 3 pages
        for (p in 1..3) {
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, p).create()
            val page = pdfDoc.startPage(pageInfo)
            val canvas = page.canvas
            canvas.drawColor(Color.WHITE)

            canvas.drawText("AllToolsPro Official Document (Page $p)", 50f, 70f, titlePaint)
            canvas.drawLine(50f, 85f, 545f, 85f, linePaint)

            var y = 120f
            val paras = when (p) {
                1 -> listOf(
                    "Chapter 1: Document Processing Architecture",
                    "AllToolsPro processes all files on-device using Android native graphics and PDF renderer pipeline.",
                    "Privacy is preserved with zero external cloud transmission.",
                    "You can test resizing, compression, splitting, and merging on this sample document."
                )
                2 -> listOf(
                    "Chapter 2: Performance & Metrics",
                    "High throughput document parsing allows compressing PDFs to target sizes (100KB, 200KB, 500KB).",
                    "Page dimension presets allow transforming A4, Letter, and Legal formats effortlessly.",
                    "Digital watermarking and page numbering can be stamped cleanly across all pages."
                )
                else -> listOf(
                    "Chapter 3: Verification & Output",
                    "This 3-page document demonstrates splitting, rotation, watermark stamps, and image extraction.",
                    "All operations execute in memory without altering the source file.",
                    "Thank you for evaluating AllToolsPro."
                )
            }

            for (para in paras) {
                canvas.drawText(para, 50f, y, textPaint)
                y += 45f
            }

            // Page footer
            canvas.drawText("AllToolsPro 100% Offline  •  Page $p of 3", 200f, 800f, textPaint)

            pdfDoc.finishPage(page)
        }

        FileOutputStream(file).use { fos ->
            pdfDoc.writeTo(fos)
        }
        pdfDoc.close()
        file
    }

    fun initRecentSampleFiles(context: Context) {
        if (RecentFileManager.recentFiles.isEmpty()) {
            val sampleBmp = createSampleDocumentBitmap()
            val imgFile = File(context.cacheDir, "Sample_Scanned_Invoice.jpg")
            if (!imgFile.exists()) {
                FileOutputStream(imgFile).use { sampleBmp.compress(Bitmap.CompressFormat.JPEG, 90, it) }
            }
            RecentFileManager.addRecent(context, imgFile, "image/jpeg", "doc_scanner")
        }
    }
}
