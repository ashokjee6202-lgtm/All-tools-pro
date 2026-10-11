package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.random.Random

enum class PaperType(val titleKey: String) {
    RULED("Ruled Notebook"),
    PLAIN("Plain Paper"),
    GRID("Grid Paper")
}

enum class InkColor(val title: String, val colorInt: Int) {
    BLUE("Blue Ink", Color.rgb(18, 52, 130)),
    BLACK("Black Ink", Color.rgb(35, 35, 38)),
    RED("Red Ink", Color.rgb(180, 25, 25))
}

data class HandwritingOptions(
    val paperType: PaperType = PaperType.RULED,
    val inkColor: InkColor = InkColor.BLUE,
    val fontSizePt: Float = 20f,
    val lineSpacingPt: Float = 34f,
    val leftMarginPt: Float = 70f,
    val topMarginPt: Float = 110f,
    val rightMarginPt: Float = 45f,
    val bottomMarginPt: Float = 60f,
    val humanJitter: Boolean = true,
    val fontStyle: String = "Cursive / Casual" // "Cursive / Casual", "Natural Serif", "Clean Print"
)

object HandwritingGenerator {

    // A4 dimensions at 72dpi: 595 x 842. For sharp rendering we generate at 2x: 1190 x 1684
    const val PAGE_WIDTH = 1190
    const val PAGE_HEIGHT = 1684

    suspend fun generateHandwritingPages(
        text: String,
        options: HandwritingOptions
    ): List<Bitmap> = withContext(Dispatchers.Default) {
        val pages = mutableListOf<Bitmap>()
        val scale = 2.0f

        val leftMargin = options.leftMarginPt * scale
        val rightMargin = PAGE_WIDTH - (options.rightMarginPt * scale)
        val topMargin = options.topMarginPt * scale
        val bottomMargin = PAGE_HEIGHT - (options.bottomMarginPt * scale)
        val lineHeight = options.lineSpacingPt * scale
        val textSize = options.fontSizePt * scale

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = options.inkColor.colorInt
            this.textSize = textSize
            typeface = when (options.fontStyle) {
                "Natural Serif" -> Typeface.SERIF
                "Cursive / Casual" -> try {
                    Typeface.create("cursive", Typeface.NORMAL)
                } catch (_: Exception) {
                    Typeface.SERIF
                }
                else -> Typeface.DEFAULT
            }
        }

        val maxTextWidth = rightMargin - leftMargin
        val lines = wrapText(text, textPaint, maxTextWidth)

        val linesPerPage = ((bottomMargin - topMargin) / lineHeight).toInt().coerceAtLeast(1)
        val totalPages = (lines.size + linesPerPage - 1) / linesPerPage

        val rnd = Random(42)

        for (pageIdx in 0 until maxOf(1, totalPages)) {
            val bitmap = Bitmap.createBitmap(PAGE_WIDTH, PAGE_HEIGHT, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            drawPaperBackground(canvas, options.paperType, scale, leftMargin, topMargin, bottomMargin, lineHeight)

            val startLine = pageIdx * linesPerPage
            val endLine = minOf(startLine + linesPerPage, lines.size)

            var y = topMargin + lineHeight * 0.85f

            for (i in startLine until endLine) {
                val lineText = lines[i]
                var x = leftMargin

                // Human writing: word by word with subtle natural jitter
                if (options.humanJitter) {
                    val words = lineText.split(" ")
                    for (wIdx in words.indices) {
                        val word = words[wIdx] + if (wIdx < words.size - 1) " " else ""
                        val jitterY = (rnd.nextFloat() - 0.5f) * (scale * 2.2f)
                        val slant = (rnd.nextFloat() - 0.5f) * 0.05f

                        canvas.save()
                        canvas.skew(slant, 0f)
                        canvas.drawText(word, x, y + jitterY, textPaint)
                        canvas.restore()

                        x += textPaint.measureText(word) + (rnd.nextFloat() * scale * 1.5f)
                    }
                } else {
                    canvas.drawText(lineText, x, y, textPaint)
                }

                y += lineHeight
            }

            pages.add(bitmap)
        }

        pages
    }

    private fun drawPaperBackground(
        canvas: Canvas,
        paperType: PaperType,
        scale: Float,
        leftMargin: Float,
        topMargin: Float,
        bottomMargin: Float,
        lineHeight: Float
    ) {
        // Warm paper background
        canvas.drawColor(Color.rgb(253, 251, 247))

        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(205, 222, 240)
            strokeWidth = 1.2f * scale
        }

        val redMarginPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(240, 140, 140)
            strokeWidth = 1.8f * scale
        }

        when (paperType) {
            PaperType.RULED -> {
                // Vertical margin line
                canvas.drawLine(leftMargin - (15f * scale), 0f, leftMargin - (15f * scale), PAGE_HEIGHT.toFloat(), redMarginPaint)
                // Top header double line
                canvas.drawLine(0f, topMargin - (12f * scale), PAGE_WIDTH.toFloat(), topMargin - (12f * scale), redMarginPaint)

                // Ruled lines
                var y = topMargin
                while (y <= bottomMargin + lineHeight) {
                    canvas.drawLine(0f, y, PAGE_WIDTH.toFloat(), y, linePaint)
                    y += lineHeight
                }
            }
            PaperType.GRID -> {
                val gridStep = 20f * scale
                var x = 0f
                while (x <= PAGE_WIDTH) {
                    canvas.drawLine(x, 0f, x, PAGE_HEIGHT.toFloat(), linePaint)
                    x += gridStep
                }
                var y = 0f
                while (y <= PAGE_HEIGHT) {
                    canvas.drawLine(0f, y, PAGE_WIDTH.toFloat(), y, linePaint)
                    y += gridStep
                }
            }
            PaperType.PLAIN -> {
                // Subtle warm grain / clean sheet
            }
        }
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        val result = mutableListOf<String>()
        val paragraphs = text.split("\n")

        for (paragraph in paragraphs) {
            if (paragraph.isEmpty()) {
                result.add("")
                continue
            }

            val words = paragraph.split(" ")
            var currentLine = StringBuilder()

            for (word in words) {
                val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
                if (paint.measureText(testLine) <= maxWidth) {
                    currentLine = StringBuilder(testLine)
                } else {
                    if (currentLine.isNotEmpty()) {
                        result.add(currentLine.toString())
                    }
                    currentLine = StringBuilder(word)
                }
            }
            if (currentLine.isNotEmpty()) {
                result.add(currentLine.toString())
            }
        }

        return result
    }

    suspend fun exportHandwritingAsPdf(
        context: Context,
        pages: List<Bitmap>,
        outputName: String = "Handwritten_Notes"
    ): File = withContext(Dispatchers.IO) {
        val pdfDoc = PdfDocument()
        val paint = Paint(Paint.FILTER_BITMAP_FLAG)

        pages.forEachIndexed { index, bitmap ->
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, index + 1).create() // A4
            val page = pdfDoc.startPage(pageInfo)
            page.canvas.drawBitmap(bitmap, null, android.graphics.Rect(0, 0, 595, 842), paint)
            pdfDoc.finishPage(page)
        }

        val outFile = File(context.cacheDir, "${outputName}_${System.currentTimeMillis()}.pdf")
        FileOutputStream(outFile).use { fos ->
            pdfDoc.writeTo(fos)
        }
        pdfDoc.close()
        outFile
    }

    suspend fun exportHandwritingAsJpg(
        context: Context,
        bitmap: Bitmap,
        outputName: String = "Handwritten_Page"
    ): File = withContext(Dispatchers.IO) {
        val outFile = File(context.cacheDir, "${outputName}_${System.currentTimeMillis()}.jpg")
        FileOutputStream(outFile).use { fos ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, fos)
        }
        outFile
    }
}
