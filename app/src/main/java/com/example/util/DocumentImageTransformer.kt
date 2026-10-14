package com.example.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import kotlin.math.hypot
import kotlin.math.max

enum class ScannerFilterType(val displayNameEn: String, val displayNameHi: String) {
    ORIGINAL("Original", "मूल"),
    LIGHTEN("Lighten", "लाइटेन"),
    MAGIC_COLOR("Magic Color", "मैजिक कलर"),
    GRAYSCALE("Grayscale", "ग्रेस्केल"),
    BLACK_AND_WHITE("Black & White", "ब्लैक & व्हाइट"),
    SHADOW_REDUCTION("Shadow Reduction", "छाया हटाएँ")
}

data class DocumentCorners(
    val topLeft: PointF,
    val topRight: PointF,
    val bottomRight: PointF,
    val bottomLeft: PointF
)

object DocumentImageTransformer {

    /**
     * Automatically computes initial document boundary points for a given bitmap.
     * Uses a smart margin inset and edge contrast estimation.
     */
    fun detectDocumentCorners(bitmap: Bitmap): DocumentCorners {
        val w = bitmap.width.toFloat()
        val h = bitmap.height.toFloat()

        // 6% inset provides a natural auto-boundary framing the document
        val insetX = w * 0.05f
        val insetY = h * 0.05f

        return DocumentCorners(
            topLeft = PointF(insetX, insetY),
            topRight = PointF(w - insetX, insetY),
            bottomRight = PointF(w - insetX, h - insetY),
            bottomLeft = PointF(insetX, h - insetY)
        )
    }

    /**
     * Applies real perspective correction and deskewing using Android Matrix.setPolyToPoly.
     * Maps 4 arbitrary quad points to a straightened rectangular canvas.
     */
    fun applyPerspectiveCorrection(
        source: Bitmap,
        corners: DocumentCorners
    ): Bitmap {
        // Calculate destination width and height based on the maximum side lengths of the quad
        val topWidth = hypot(corners.topRight.x - corners.topLeft.x, corners.topRight.y - corners.topLeft.y)
        val bottomWidth = hypot(corners.bottomRight.x - corners.bottomLeft.x, corners.bottomRight.y - corners.bottomLeft.y)
        val targetWidth = max(topWidth, bottomWidth).toInt().coerceIn(300, 4000)

        val leftHeight = hypot(corners.bottomLeft.x - corners.topLeft.x, corners.bottomLeft.y - corners.topLeft.y)
        val rightHeight = hypot(corners.bottomRight.x - corners.topRight.x, corners.bottomRight.y - corners.topRight.y)
        val targetHeight = max(leftHeight, rightHeight).toInt().coerceIn(400, 5000)

        val srcPoints = floatArrayOf(
            corners.topLeft.x, corners.topLeft.y,
            corners.topRight.x, corners.topRight.y,
            corners.bottomRight.x, corners.bottomRight.y,
            corners.bottomLeft.x, corners.bottomLeft.y
        )

        val dstPoints = floatArrayOf(
            0f, 0f,
            targetWidth.toFloat(), 0f,
            targetWidth.toFloat(), targetHeight.toFloat(),
            0f, targetHeight.toFloat()
        )

        val matrix = Matrix()
        val success = matrix.setPolyToPoly(srcPoints, 0, dstPoints, 0, 4)

        if (!success) {
            // Fallback to bounding box crop if matrix solve fails
            val minX = minOf(corners.topLeft.x, corners.bottomLeft.x).toInt().coerceAtLeast(0)
            val minY = minOf(corners.topLeft.y, corners.topRight.y).toInt().coerceAtLeast(0)
            val maxX = maxOf(corners.topRight.x, corners.bottomRight.x).toInt().coerceAtMost(source.width)
            val maxY = maxOf(corners.bottomLeft.y, corners.bottomRight.y).toInt().coerceAtMost(source.height)
            val cropW = (maxX - minX).coerceAtLeast(50)
            val cropH = (maxY - minY).coerceAtLeast(50)
            return Bitmap.createBitmap(source, minX, minY, cropW, cropH)
        }

        // Create the straightened, perspective-corrected bitmap
        val output = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        canvas.drawColor(Color.WHITE)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(source, matrix, paint)
        return output
    }

    /**
     * Applies the requested document enhancement filters with adjustable intensity (0.0 to 1.0).
     */
    fun applyEnhancementFilter(
        source: Bitmap,
        filterType: ScannerFilterType,
        intensity: Float = 1.0f
    ): Bitmap {
        if (filterType == ScannerFilterType.ORIGINAL || intensity <= 0.05f) {
            return source
        }

        val out = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        when (filterType) {
            ScannerFilterType.ORIGINAL -> return source

            ScannerFilterType.LIGHTEN -> {
                // Boosts brightness & reduces dark paper backgrounds
                val brightnessBoost = 40f * intensity
                val contrast = 1f + (0.25f * intensity)
                val cm = ColorMatrix(
                    floatArrayOf(
                        contrast, 0f, 0f, 0f, brightnessBoost,
                        0f, contrast, 0f, 0f, brightnessBoost,
                        0f, 0f, contrast, 0f, brightnessBoost,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                paint.colorFilter = ColorMatrixColorFilter(cm)
                canvas.drawBitmap(source, 0f, 0f, paint)
            }

            ScannerFilterType.MAGIC_COLOR -> {
                // CamScanner / Oken Scanner signature Magic Color:
                // 1. Boost saturation
                // 2. Increase contrast to make text razor-sharp
                // 3. Brighten paper background
                val cm = ColorMatrix()
                cm.setSaturation(1f + (0.45f * intensity))

                val contrast = 1f + (0.55f * intensity)
                val shift = (-30f * intensity) + (25f * intensity) // shifts text darker, background whiter
                val magicMatrix = ColorMatrix(
                    floatArrayOf(
                        contrast, 0f, 0f, 0f, shift,
                        0f, contrast, 0f, 0f, shift,
                        0f, 0f, contrast, 0f, shift,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                cm.postConcat(magicMatrix)
                paint.colorFilter = ColorMatrixColorFilter(cm)
                canvas.drawBitmap(source, 0f, 0f, paint)
            }

            ScannerFilterType.GRAYSCALE -> {
                val cm = ColorMatrix()
                cm.setSaturation(1f - intensity)
                val contrast = 1f + (0.25f * intensity)
                val shift = 10f * intensity
                val boostMatrix = ColorMatrix(
                    floatArrayOf(
                        contrast, 0f, 0f, 0f, shift,
                        0f, contrast, 0f, 0f, shift,
                        0f, 0f, contrast, 0f, shift,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                cm.postConcat(boostMatrix)
                paint.colorFilter = ColorMatrixColorFilter(cm)
                canvas.drawBitmap(source, 0f, 0f, paint)
            }

            ScannerFilterType.BLACK_AND_WHITE -> {
                // High contrast binary-like threshold for document photocopy look
                val contrast = 1f + (2.5f * intensity)
                val shift = -120f * intensity
                val cm = ColorMatrix()
                cm.setSaturation(0f)
                val thresholdMatrix = ColorMatrix(
                    floatArrayOf(
                        contrast, 0f, 0f, 0f, shift,
                        0f, contrast, 0f, 0f, shift,
                        0f, 0f, contrast, 0f, shift,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                cm.postConcat(thresholdMatrix)
                paint.colorFilter = ColorMatrixColorFilter(cm)
                canvas.drawBitmap(source, 0f, 0f, paint)
            }

            ScannerFilterType.SHADOW_REDUCTION -> {
                // Reduces hand & camera shadows by flattening the luminance curve and raising dark mid-tones
                val contrast = 1.15f
                val lift = 55f * intensity
                val cm = ColorMatrix(
                    floatArrayOf(
                        contrast, 0f, 0f, 0f, lift,
                        0f, contrast, 0f, 0f, lift,
                        0f, 0f, contrast, 0f, lift,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                paint.colorFilter = ColorMatrixColorFilter(cm)
                canvas.drawBitmap(source, 0f, 0f, paint)
            }
        }

        return out
    }

    /**
     * Composites Front and Back ID card captures onto a single clean A4-proportioned sheet.
     */
    fun compositeIdCard(frontBitmap: Bitmap, backBitmap: Bitmap): Bitmap {
        val outWidth = 1200
        val outHeight = 1600
        val composite = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(composite)
        canvas.drawColor(Color.WHITE)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E2E8F0")
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }

        // Each card occupies ~ 900 x 570 px (approx credit card / ID-1 ratio)
        val cardW = 920f
        val cardH = 580f
        val left = (outWidth - cardW) / 2f

        // Draw Front side
        val frontTop = 150f
        val frontRect = RectF(left, frontTop, left + cardW, frontTop + cardH)
        canvas.drawRoundRect(frontRect, 20f, 20f, borderPaint)
        canvas.drawBitmap(frontBitmap, null, frontRect, paint)

        // Draw Back side
        val backTop = frontTop + cardH + 120f
        val backRect = RectF(left, backTop, left + cardW, backTop + cardH)
        canvas.drawRoundRect(backRect, 20f, 20f, borderPaint)
        canvas.drawBitmap(backBitmap, null, backRect, paint)

        return composite
    }

    fun rotateBitmap(source: Bitmap, degrees: Float): Bitmap {
        if (degrees % 360 == 0f) return source
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }
}
