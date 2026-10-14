package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

enum class PdfPagePreset(val title: String, val widthPt: Int, val heightPt: Int) {
    A4("A4 (210 x 297 mm)", 595, 842),
    A5("A5 (148 x 210 mm)", 420, 595),
    A3("A3 (297 x 420 mm)", 842, 1191),
    LETTER("Letter (8.5 x 11 in)", 612, 792),
    LEGAL("Legal (8.5 x 14 in)", 612, 1008),
    CUSTOM("Custom", 595, 842)
}

data class PdfCompressResult(
    val outputFile: File,
    val originalSizeBytes: Long,
    val finalSizeBytes: Long,
    val pageCount: Int,
    val reductionPercent: Int,
    val statusMessage: String
)

object PdfProcessor {

    fun copyUriToTempFile(context: Context, uri: Uri, prefix: String = "temp_doc", extension: String = "pdf"): File? {
        return try {
            val file = File(context.cacheDir, "${prefix}_${System.currentTimeMillis()}.$extension")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            file
        } catch (_: Exception) {
            null
        }
    }

    suspend fun getPdfPageCount(pdfFile: File): Int = withContext(Dispatchers.IO) {
        try {
            val pfd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            val count = renderer.pageCount
            renderer.close()
            pfd.close()
            count
        } catch (_: Exception) {
            0
        }
    }

    suspend fun renderPdfPagesToBitmaps(
        pdfFile: File,
        scaleFactor: Float = 2.0f,
        maxPages: Int = 50
    ): List<Bitmap> = withContext(Dispatchers.IO) {
        val bitmaps = mutableListOf<Bitmap>()
        try {
            val pfd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            val count = minOf(renderer.pageCount, maxPages)

            for (i in 0 until count) {
                val page = renderer.openPage(i)
                val width = (page.width * scaleFactor).toInt().coerceAtLeast(1)
                val height = (page.height * scaleFactor).toInt().coerceAtLeast(1)
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                bitmaps.add(bitmap)
            }

            renderer.close()
            pfd.close()
        } catch (_: Exception) {}
        bitmaps
    }

    suspend fun imagesToPdf(
        context: Context,
        images: List<Bitmap>,
        preset: PdfPagePreset = PdfPagePreset.A4,
        marginPt: Int = 20,
        outputName: String = "Images_Document"
    ): File = withContext(Dispatchers.IO) {
        val pdfDoc = PdfDocument()
        val paint = Paint(Paint.FILTER_BITMAP_FLAG)

        images.forEachIndexed { index, bitmap ->
            val pageInfo = PdfDocument.PageInfo.Builder(preset.widthPt, preset.heightPt, index + 1).create()
            val page = pdfDoc.startPage(pageInfo)
            val canvas = page.canvas

            canvas.drawColor(Color.WHITE)

            val availW = preset.widthPt - (marginPt * 2)
            val availH = preset.heightPt - (marginPt * 2)

            val bmpW = bitmap.width.toFloat()
            val bmpH = bitmap.height.toFloat()
            val scale = minOf(availW / bmpW, availH / bmpH)

            val drawW = bmpW * scale
            val drawH = bmpH * scale
            val left = marginPt + (availW - drawW) / 2f
            val top = marginPt + (availH - drawH) / 2f

            val dstRect = RectF(left, top, left + drawW, top + drawH)
            canvas.drawBitmap(bitmap, null, dstRect, paint)

            pdfDoc.finishPage(page)
        }

        val outFile = File(context.cacheDir, "${outputName}_${System.currentTimeMillis()}.pdf")
        FileOutputStream(outFile).use { fos ->
            pdfDoc.writeTo(fos)
        }
        pdfDoc.close()
        outFile
    }

    suspend fun createPdfFromBitmaps(
        context: Context,
        images: List<Bitmap>,
        preset: PdfPagePreset = PdfPagePreset.A4,
        marginPt: Int = 20,
        outputName: String = "Scanned_Document"
    ): File = imagesToPdf(context, images, preset, marginPt, outputName)

    suspend fun compressPdf(
        context: Context,
        sourcePdf: File,
        targetSizeBytes: Long? = null,
        qualityPreset: String = "medium", // low, medium, high
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): PdfCompressResult = withContext(Dispatchers.IO) {
        val origSize = sourcePdf.length()
        val pfd = ParcelFileDescriptor.open(sourcePdf, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(pfd)
        val pageCount = renderer.pageCount

        val (scale, jpegQuality) = when (qualityPreset.lowercase()) {
            "high" -> Pair(1.6f, 80) // High quality, low compression
            "low" -> Pair(1.0f, 40) // Strong compression
            else -> Pair(1.3f, 65) // Balanced
        }

        val targetScale = if (targetSizeBytes != null && targetSizeBytes < origSize / 3) {
            0.95f
        } else scale

        val targetQuality = if (targetSizeBytes != null && targetSizeBytes < origSize / 2) {
            45
        } else jpegQuality

        val pdfDoc = PdfDocument()
        val paint = Paint(Paint.FILTER_BITMAP_FLAG)

        for (i in 0 until pageCount) {
            val page = renderer.openPage(i)
            val pWidth = page.width
            val pHeight = page.height

            val bmpW = (pWidth * targetScale).toInt().coerceAtLeast(1)
            val bmpH = (pHeight * targetScale).toInt().coerceAtLeast(1)
            val bmp = Bitmap.createBitmap(bmpW, bmpH, Bitmap.Config.RGB_565)
            val c = Canvas(bmp)
            c.drawColor(Color.WHITE)
            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
            page.close()

            // Compress to JPEG stream to reduce size
            val stream = ByteArrayOutputStream()
            bmp.compress(Bitmap.CompressFormat.JPEG, targetQuality, stream)
            val compBmp = android.graphics.BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size())

            val pageInfo = PdfDocument.PageInfo.Builder(pWidth, pHeight, i + 1).create()
            val newPage = pdfDoc.startPage(pageInfo)
            val canvas = newPage.canvas
            canvas.drawBitmap(compBmp ?: bmp, null, Rect(0, 0, pWidth, pHeight), paint)
            pdfDoc.finishPage(newPage)

            bmp.recycle()
            compBmp?.recycle()

            onProgress(i + 1, pageCount)
        }

        renderer.close()
        pfd.close()

        val outFile = File(context.cacheDir, "compressed_${System.currentTimeMillis()}.pdf")
        FileOutputStream(outFile).use { fos ->
            pdfDoc.writeTo(fos)
        }
        pdfDoc.close()

        val finalSize = outFile.length()
        val reduction = if (origSize > 0) {
            (((origSize - finalSize).toDouble() / origSize) * 100).toInt().coerceAtLeast(0)
        } else 0

        val msg = if (reduction > 0) {
            "Compressed by $reduction% (Saved ${ImageProcessor.formatBytes(origSize - finalSize)})"
        } else {
            "Optimal compression achieved (${ImageProcessor.formatBytes(finalSize)})"
        }

        PdfCompressResult(
            outputFile = outFile,
            originalSizeBytes = origSize,
            finalSizeBytes = finalSize,
            pageCount = pageCount,
            reductionPercent = reduction,
            statusMessage = msg
        )
    }

    suspend fun resizePdfDimensions(
        context: Context,
        sourcePdf: File,
        targetPreset: PdfPagePreset,
        isLandscape: Boolean = false,
        marginPt: Int = 18,
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): File = withContext(Dispatchers.IO) {
        val pfd = ParcelFileDescriptor.open(sourcePdf, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(pfd)
        val pageCount = renderer.pageCount

        val targetW = if (isLandscape) targetPreset.heightPt else targetPreset.widthPt
        val targetH = if (isLandscape) targetPreset.widthPt else targetPreset.heightPt

        val pdfDoc = PdfDocument()
        val paint = Paint(Paint.FILTER_BITMAP_FLAG)

        for (i in 0 until pageCount) {
            val page = renderer.openPage(i)
            val srcW = page.width
            val srcH = page.height

            val renderBmp = Bitmap.createBitmap(srcW * 2, srcH * 2, Bitmap.Config.ARGB_8888)
            val c = Canvas(renderBmp)
            c.drawColor(Color.WHITE)
            page.render(renderBmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
            page.close()

            val pageInfo = PdfDocument.PageInfo.Builder(targetW, targetH, i + 1).create()
            val newPage = pdfDoc.startPage(pageInfo)
            val canvas = newPage.canvas
            canvas.drawColor(Color.WHITE)

            val availW = (targetW - marginPt * 2).toFloat()
            val availH = (targetH - marginPt * 2).toFloat()
            val scale = minOf(availW / renderBmp.width, availH / renderBmp.height)

            val drawW = renderBmp.width * scale
            val drawH = renderBmp.height * scale
            val left = marginPt + (availW - drawW) / 2f
            val top = marginPt + (availH - drawH) / 2f

            canvas.drawBitmap(renderBmp, null, RectF(left, top, left + drawW, top + drawH), paint)
            pdfDoc.finishPage(newPage)
            renderBmp.recycle()

            onProgress(i + 1, pageCount)
        }

        renderer.close()
        pfd.close()

        val outFile = File(context.cacheDir, "resized_page_${System.currentTimeMillis()}.pdf")
        FileOutputStream(outFile).use { fos ->
            pdfDoc.writeTo(fos)
        }
        pdfDoc.close()
        outFile
    }

    suspend fun mergePdfs(
        context: Context,
        pdfFiles: List<File>,
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): File = withContext(Dispatchers.IO) {
        val pdfDoc = PdfDocument()
        val paint = Paint(Paint.FILTER_BITMAP_FLAG)
        var globalPageIndex = 1

        pdfFiles.forEachIndexed { docIdx, file ->
            val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            for (p in 0 until renderer.pageCount) {
                val page = renderer.openPage(p)
                val w = page.width
                val h = page.height
                val bmp = Bitmap.createBitmap(w * 2, h * 2, Bitmap.Config.ARGB_8888)
                val c = Canvas(bmp)
                c.drawColor(Color.WHITE)
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                page.close()

                val pageInfo = PdfDocument.PageInfo.Builder(w, h, globalPageIndex++).create()
                val newPage = pdfDoc.startPage(pageInfo)
                newPage.canvas.drawBitmap(bmp, null, Rect(0, 0, w, h), paint)
                pdfDoc.finishPage(newPage)
                bmp.recycle()
            }
            renderer.close()
            pfd.close()
            onProgress(docIdx + 1, pdfFiles.size)
        }

        val outFile = File(context.cacheDir, "merged_${System.currentTimeMillis()}.pdf")
        FileOutputStream(outFile).use { fos ->
            pdfDoc.writeTo(fos)
        }
        pdfDoc.close()
        outFile
    }

    suspend fun splitPdf(
        context: Context,
        sourcePdf: File,
        startPage: Int, // 1-based
        endPage: Int // 1-based
    ): File = withContext(Dispatchers.IO) {
        val pfd = ParcelFileDescriptor.open(sourcePdf, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(pfd)
        val total = renderer.pageCount

        val s = (startPage - 1).coerceIn(0, total - 1)
        val e = (endPage - 1).coerceIn(s, total - 1)

        val pdfDoc = PdfDocument()
        val paint = Paint(Paint.FILTER_BITMAP_FLAG)
        var newIdx = 1

        for (i in s..e) {
            val page = renderer.openPage(i)
            val w = page.width
            val h = page.height
            val bmp = Bitmap.createBitmap(w * 2, h * 2, Bitmap.Config.ARGB_8888)
            val c = Canvas(bmp)
            c.drawColor(Color.WHITE)
            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
            page.close()

            val pageInfo = PdfDocument.PageInfo.Builder(w, h, newIdx++).create()
            val newPage = pdfDoc.startPage(pageInfo)
            newPage.canvas.drawBitmap(bmp, null, Rect(0, 0, w, h), paint)
            pdfDoc.finishPage(newPage)
            bmp.recycle()
        }

        renderer.close()
        pfd.close()

        val outFile = File(context.cacheDir, "split_pages_${startPage}_to_${endPage}_${System.currentTimeMillis()}.pdf")
        FileOutputStream(outFile).use { fos ->
            pdfDoc.writeTo(fos)
        }
        pdfDoc.close()
        outFile
    }

    suspend fun rotatePdfPages(
        context: Context,
        sourcePdf: File,
        rotationDegrees: Int = 90
    ): File = withContext(Dispatchers.IO) {
        val pfd = ParcelFileDescriptor.open(sourcePdf, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(pfd)
        val pageCount = renderer.pageCount

        val pdfDoc = PdfDocument()
        val paint = Paint(Paint.FILTER_BITMAP_FLAG)

        for (i in 0 until pageCount) {
            val page = renderer.openPage(i)
            val w = page.width
            val h = page.height
            val bmp = Bitmap.createBitmap(w * 2, h * 2, Bitmap.Config.ARGB_8888)
            val c = Canvas(bmp)
            c.drawColor(Color.WHITE)
            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
            page.close()

            val rotatedBmp = ImageProcessor.rotateBitmap(bmp, rotationDegrees.toFloat())
            val targetW = if (rotationDegrees % 180 != 0) h else w
            val targetH = if (rotationDegrees % 180 != 0) w else h

            val pageInfo = PdfDocument.PageInfo.Builder(targetW, targetH, i + 1).create()
            val newPage = pdfDoc.startPage(pageInfo)
            newPage.canvas.drawBitmap(rotatedBmp, null, Rect(0, 0, targetW, targetH), paint)
            pdfDoc.finishPage(newPage)

            bmp.recycle()
            if (rotatedBmp != bmp) rotatedBmp.recycle()
        }

        renderer.close()
        pfd.close()

        val outFile = File(context.cacheDir, "rotated_${System.currentTimeMillis()}.pdf")
        FileOutputStream(outFile).use { fos ->
            pdfDoc.writeTo(fos)
        }
        pdfDoc.close()
        outFile
    }

    suspend fun addWatermarkAndNumbers(
        context: Context,
        sourcePdf: File,
        watermarkText: String?,
        addPageNumbers: Boolean,
        signatureBitmap: Bitmap? = null
    ): File = withContext(Dispatchers.IO) {
        val pfd = ParcelFileDescriptor.open(sourcePdf, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(pfd)
        val pageCount = renderer.pageCount

        val pdfDoc = PdfDocument()
        val paint = Paint(Paint.FILTER_BITMAP_FLAG)

        val wmPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.GRAY
            alpha = 70
            textSize = 44f
            textAlign = Paint.Align.CENTER
        }

        val numPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.DKGRAY
            textSize = 14f
            textAlign = Paint.Align.CENTER
        }

        for (i in 0 until pageCount) {
            val page = renderer.openPage(i)
            val w = page.width
            val h = page.height
            val bmp = Bitmap.createBitmap(w * 2, h * 2, Bitmap.Config.ARGB_8888)
            val c = Canvas(bmp)
            c.drawColor(Color.WHITE)
            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
            page.close()

            val pageInfo = PdfDocument.PageInfo.Builder(w, h, i + 1).create()
            val newPage = pdfDoc.startPage(pageInfo)
            val canvas = newPage.canvas
            canvas.drawBitmap(bmp, null, Rect(0, 0, w, h), paint)

            // Draw watermark
            if (!watermarkText.isNullOrBlank()) {
                canvas.save()
                canvas.rotate(-40f, w / 2f, h / 2f)
                canvas.drawText(watermarkText, w / 2f, h / 2f, wmPaint)
                canvas.restore()
            }

            // Draw signature on bottom right if provided
            signatureBitmap?.let { sig ->
                val sigW = 120f
                val sigH = (sig.height.toFloat() / sig.width) * sigW
                val sigLeft = w - sigW - 30f
                val sigTop = h - sigH - 45f
                canvas.drawBitmap(sig, null, RectF(sigLeft, sigTop, sigLeft + sigW, sigTop + sigH), paint)
            }

            // Draw page numbers
            if (addPageNumbers) {
                canvas.drawText("Page ${i + 1} of $pageCount", w / 2f, h - 18f, numPaint)
            }

            pdfDoc.finishPage(newPage)
            bmp.recycle()
        }

        renderer.close()
        pfd.close()

        val outFile = File(context.cacheDir, "watermarked_${System.currentTimeMillis()}.pdf")
        FileOutputStream(outFile).use { fos ->
            pdfDoc.writeTo(fos)
        }
        pdfDoc.close()
        outFile
    }

    suspend fun createBlankPdf(
        context: Context,
        pageCount: Int = 1,
        style: String = "lined" // "plain", "lined", "grid"
    ): File = withContext(Dispatchers.IO) {
        val pdfDoc = PdfDocument()
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(200, 220, 245)
            strokeWidth = 1f
        }
        val marginPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(255, 160, 160)
            strokeWidth = 1.5f
        }

        for (p in 1..pageCount) {
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, p).create()
            val page = pdfDoc.startPage(pageInfo)
            val canvas = page.canvas
            canvas.drawColor(Color.WHITE)

            when (style) {
                "lined" -> {
                    // Margin line
                    canvas.drawLine(60f, 0f, 60f, 842f, marginPaint)
                    var y = 80f
                    while (y < 820f) {
                        canvas.drawLine(40f, y, 555f, y, linePaint)
                        y += 28f
                    }
                }
                "grid" -> {
                    var x = 40f
                    while (x < 560f) {
                        canvas.drawLine(x, 40f, x, 802f, linePaint)
                        x += 20f
                    }
                    var y = 40f
                    while (y < 802f) {
                        canvas.drawLine(40f, y, 555f, y, linePaint)
                        y += 20f
                    }
                }
            }

            pdfDoc.finishPage(page)
        }

        val outFile = File(context.cacheDir, "blank_${style}_${System.currentTimeMillis()}.pdf")
        FileOutputStream(outFile).use { fos ->
            pdfDoc.writeTo(fos)
        }
        pdfDoc.close()
        outFile
    }
}
