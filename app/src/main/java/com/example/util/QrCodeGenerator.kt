package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URLEncoder

object QrCodeGenerator {

    fun generateQrBitmap(
        content: String,
        size: Int = 512,
        fgColor: Int = Color.BLACK,
        bgColor: Int = Color.WHITE
    ): Bitmap? {
        return try {
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size)
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            for (x in 0 until size) {
                for (y in 0 until size) {
                    bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) fgColor else bgColor)
                }
            }
            bitmap
        } catch (_: Exception) {
            null
        }
    }

    fun generateBarcodeBitmap(
        content: String,
        width: Int = 600,
        height: Int = 250
    ): Bitmap? {
        return try {
            val writer = com.google.zxing.oned.Code128Writer()
            val bitMatrix = writer.encode(content, BarcodeFormat.CODE_128, width, height)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
                }
            }
            bitmap
        } catch (_: Exception) {
            null
        }
    }

    fun buildUpiUri(
        upiId: String,
        payeeName: String,
        amount: String? = null,
        note: String? = null
    ): String {
        val cleanUpi = upiId.trim()
        val cleanName = URLEncoder.encode(payeeName.trim(), "UTF-8")
        val builder = StringBuilder("upi://pay?pa=$cleanUpi&pn=$cleanName&cu=INR")
        if (!amount.isNullOrBlank() && amount.toDoubleOrNull() != null) {
            builder.append("&am=${amount.trim()}")
        }
        if (!note.isNullOrBlank()) {
            builder.append("&tn=${URLEncoder.encode(note.trim(), "UTF-8")}")
        }
        return builder.toString()
    }

    fun buildWifiConfig(
        ssid: String,
        password: String,
        encryptionType: String = "WPA" // WPA, WEP, nopass
    ): String {
        return "WIFI:T:$encryptionType;S:$ssid;P:$password;;"
    }

    suspend fun saveQrAsPng(context: Context, bitmap: Bitmap, name: String = "QRCode"): File = withContext(Dispatchers.IO) {
        val file = File(context.cacheDir, "${name}_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { fos ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
        }
        file
    }

    suspend fun saveQrAsPdf(context: Context, bitmap: Bitmap, title: String = "QR Code"): File = withContext(Dispatchers.IO) {
        val pdfDoc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4
        val page = pdfDoc.startPage(pageInfo)
        val canvas = page.canvas

        canvas.drawColor(Color.WHITE)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(21, 101, 192)
            textSize = 24f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
        canvas.drawText(title, 297f, 120f, titlePaint)

        val qrSize = 340f
        val left = (595f - qrSize) / 2f
        val top = 180f
        canvas.drawBitmap(bitmap, null, android.graphics.RectF(left, top, left + qrSize, top + qrSize), Paint(Paint.FILTER_BITMAP_FLAG))

        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.GRAY
            textSize = 12f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Generated with AllToolsPro (100% Offline & Private)", 297f, top + qrSize + 50f, footerPaint)

        pdfDoc.finishPage(page)

        val file = File(context.cacheDir, "${title.replace(" ", "_")}_${System.currentTimeMillis()}.pdf")
        FileOutputStream(file).use { fos ->
            pdfDoc.writeTo(fos)
        }
        pdfDoc.close()
        file
    }

    fun scanQrFromBitmap(bitmap: Bitmap): String? {
        return try {
            val width = bitmap.width
            val height = bitmap.height
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
            val source = RGBLuminanceSource(width, height, pixels)
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
            val reader = MultiFormatReader()
            val result = reader.decode(binaryBitmap)
            result.text
        } catch (_: Exception) {
            null
        }
    }
}
