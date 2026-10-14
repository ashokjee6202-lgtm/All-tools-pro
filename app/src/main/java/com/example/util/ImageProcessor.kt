package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

enum class ImageOutputFormat(val extension: String, val mimeType: String) {
    JPEG("jpg", "image/jpeg"),
    PNG("png", "image/png"),
    WEBP("webp", "image/webp")
}

data class ImageProcessResult(
    val outputFile: File,
    val originalSizeBytes: Long,
    val finalSizeBytes: Long,
    val width: Int,
    val height: Int,
    val targetAchieved: Boolean = true,
    val statusMessage: String = ""
)

object ImageProcessor {

    fun loadBitmapFromUri(context: Context, uri: Uri, maxDimension: Int = 3000): Bitmap? {
        return try {
            val input: InputStream? = context.contentResolver.openInputStream(uri)
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(input, null, boundsOptions)
            input?.close()

            var sampleSize = 1
            var w = boundsOptions.outWidth
            var h = boundsOptions.outHeight
            while (w > maxDimension || h > maxDimension) {
                sampleSize *= 2
                w /= 2
                h /= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val stream: InputStream? = context.contentResolver.openInputStream(uri)
            val bmp = BitmapFactory.decodeStream(stream, null, decodeOptions)
            stream?.close()
            bmp
        } catch (_: Exception) {
            null
        }
    }

    suspend fun resizeByDimensions(
        context: Context,
        sourceBitmap: Bitmap,
        targetWidth: Int,
        targetHeight: Int,
        format: ImageOutputFormat,
        quality: Int = 90,
        outputPrefix: String = "resized_photo"
    ): ImageProcessResult = withContext(Dispatchers.IO) {
        val scaled = Bitmap.createScaledBitmap(sourceBitmap, targetWidth, targetHeight, true)
        val outFile = File(context.cacheDir, "${outputPrefix}_${System.currentTimeMillis()}.${format.extension}")
        
        val compressFormat = when (format) {
            ImageOutputFormat.JPEG -> Bitmap.CompressFormat.JPEG
            ImageOutputFormat.PNG -> Bitmap.CompressFormat.PNG
            ImageOutputFormat.WEBP -> Bitmap.CompressFormat.WEBP
        }

        val outStream = FileOutputStream(outFile)
        scaled.compress(compressFormat, quality.coerceIn(10, 100), outStream)
        outStream.flush()
        outStream.close()

        val origEst = (sourceBitmap.width * sourceBitmap.height * 4L)
        ImageProcessResult(
            outputFile = outFile,
            originalSizeBytes = origEst,
            finalSizeBytes = outFile.length(),
            width = targetWidth,
            height = targetHeight,
            targetAchieved = true,
            statusMessage = "Resized to ${targetWidth}x${targetHeight}"
        )
    }

    suspend fun compressToTargetSize(
        context: Context,
        sourceBitmap: Bitmap,
        targetSizeBytes: Long,
        format: ImageOutputFormat = ImageOutputFormat.JPEG,
        outputPrefix: String = "compressed_photo"
    ): ImageProcessResult = withContext(Dispatchers.IO) {
        var currentBitmap = sourceBitmap
        var quality = 95
        var bestBytes: ByteArray? = null
        val compressFormat = when (format) {
            ImageOutputFormat.JPEG -> Bitmap.CompressFormat.JPEG
            ImageOutputFormat.PNG -> Bitmap.CompressFormat.JPEG // PNG cannot be lossy compressed easily; use JPEG or downsample
            ImageOutputFormat.WEBP -> Bitmap.CompressFormat.WEBP
        }

        // Iterative search: first adjust quality down to 20
        var attempts = 0
        while (attempts < 15) {
            val stream = ByteArrayOutputStream()
            currentBitmap.compress(compressFormat, quality, stream)
            val bytes = stream.toByteArray()
            bestBytes = bytes

            if (bytes.size <= targetSizeBytes) {
                break
            }

            if (quality > 25) {
                quality -= 15
            } else {
                // Scale down bitmap dimensions if quality adjustment is not enough
                val newW = (currentBitmap.width * 0.82f).toInt()
                val newH = (currentBitmap.height * 0.82f).toInt()
                if (newW < 80 || newH < 80) break
                currentBitmap = Bitmap.createScaledBitmap(currentBitmap, newW, newH, true)
            }
            attempts++
        }

        val outFile = File(context.cacheDir, "${outputPrefix}_${System.currentTimeMillis()}.${format.extension}")
        bestBytes?.let {
            FileOutputStream(outFile).use { fos -> fos.write(it) }
        }

        val achieved = outFile.length() <= targetSizeBytes * 1.15
        val diffKb = (outFile.length() - targetSizeBytes) / 1024
        val msg = if (achieved) {
            "Target achieved (${formatBytes(outFile.length())})"
        } else {
            "Closest size reached: ${formatBytes(outFile.length())} (limited by image complexity)"
        }

        ImageProcessResult(
            outputFile = outFile,
            originalSizeBytes = (sourceBitmap.width * sourceBitmap.height * 3L) / 4,
            finalSizeBytes = outFile.length(),
            width = currentBitmap.width,
            height = currentBitmap.height,
            targetAchieved = achieved,
            statusMessage = msg
        )
    }

    suspend fun batchResize(
        context: Context,
        sourceUris: List<Uri>,
        targetWidth: Int,
        targetHeight: Int,
        targetSizeBytes: Long?,
        format: ImageOutputFormat,
        onProgress: (Int, Int) -> Unit
    ): List<File> = withContext(Dispatchers.IO) {
        val results = mutableListOf<File>()
        sourceUris.forEachIndexed { index, uri ->
            val bmp = loadBitmapFromUri(context, uri)
            if (bmp != null) {
                val res = if (targetSizeBytes != null && targetSizeBytes > 0) {
                    compressToTargetSize(context, bmp, targetSizeBytes, format, "batch_${index}")
                } else {
                    resizeByDimensions(context, bmp, targetWidth, targetHeight, format, 90, "batch_${index}")
                }
                results.add(res.outputFile)
            }
            onProgress(index + 1, sourceUris.size)
        }
        results
    }

    suspend fun createZipArchive(context: Context, files: List<File>, zipName: String = "AllToolsPro_Images"): File = withContext(Dispatchers.IO) {
        val zipFile = File(context.cacheDir, "${zipName}_${System.currentTimeMillis()}.zip")
        ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
            for (file in files) {
                if (file.exists()) {
                    val entry = ZipEntry(file.name)
                    zos.putNextEntry(entry)
                    file.inputStream().use { it.copyTo(zos) }
                    zos.closeEntry()
                }
            }
        }
        zipFile
    }

    fun applyScannerFilter(
        source: Bitmap,
        grayscale: Boolean = false,
        highContrast: Boolean = false,
        brightness: Float = 0f,
        contrast: Float = 1f
    ): Bitmap {
        val out = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val cm = ColorMatrix()
        if (grayscale) {
            cm.setSaturation(0f)
        }
        if (highContrast) {
            // Document B&W high contrast threshold matrix
            val contrastFactor = 1.8f
            val shift = -64f
            val bAndWMatrix = ColorMatrix(
                floatArrayOf(
                    contrastFactor, 0f, 0f, 0f, shift,
                    0f, contrastFactor, 0f, 0f, shift,
                    0f, 0f, contrastFactor, 0f, shift,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            cm.postConcat(bAndWMatrix)
        }

        if (brightness != 0f || contrast != 1f) {
            val scale = contrast
            val translate = brightness * 255f
            val bcMatrix = ColorMatrix(
                floatArrayOf(
                    scale, 0f, 0f, 0f, translate,
                    0f, scale, 0f, 0f, translate,
                    0f, 0f, scale, 0f, translate,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            cm.postConcat(bcMatrix)
        }

        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(source, 0f, 0f, paint)
        return out
    }

    fun rotateBitmap(source: Bitmap, degrees: Float): Bitmap {
        if (degrees % 360 == 0f) return source
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    fun flipBitmap(source: Bitmap, horizontal: Boolean): Bitmap {
        val matrix = Matrix().apply {
            if (horizontal) postScale(-1f, 1f) else postScale(1f, -1f)
        }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    fun createPassportPhoto(
        source: Bitmap,
        targetWidth: Int = 413, // 35mm @ 300dpi approx
        targetHeight: Int = 531, // 45mm @ 300dpi approx
        backgroundColor: Int = Color.WHITE
    ): Bitmap {
        val result = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawColor(backgroundColor)

        // Center crop and scale to 35x45mm ratio
        val srcRatio = source.width.toFloat() / source.height.toFloat()
        val targetRatio = targetWidth.toFloat() / targetHeight.toFloat()

        val srcRect: Rect
        if (srcRatio > targetRatio) {
            val cropW = (source.height * targetRatio).toInt()
            val left = (source.width - cropW) / 2
            srcRect = Rect(left, 0, left + cropW, source.height)
        } else {
            val cropH = (source.width / targetRatio).toInt()
            val top = (source.height - cropH) / 2
            srcRect = Rect(0, top, source.width, top + cropH)
        }

        val dstRect = Rect(0, 0, targetWidth, targetHeight)
        canvas.drawBitmap(source, srcRect, dstRect, Paint(Paint.FILTER_BITMAP_FLAG))
        return result
    }

    fun formatBytes(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${String.format("%.1f", bytes / 1024.0)} KB"
            else -> "${String.format("%.2f", bytes / (1024.0 * 1024.0))} MB"
        }
    }

    fun formatFileSize(bytes: Long): String = formatBytes(bytes)
}
