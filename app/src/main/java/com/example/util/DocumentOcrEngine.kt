package com.example.util

import android.content.Context
import android.graphics.Bitmap
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

data class OcrResult(
    val fullText: String,
    val lineCount: Int,
    val wordCount: Int,
    val detectedLanguage: String = "English / Devanagari (Hindi)"
)

object DocumentOcrEngine {

    /**
     * Extracts text from an image using ML Kit Devanagari + Latin text recognizer on-device.
     * Fully works offline, without internet access or server calls.
     */
    suspend fun recognizeText(bitmap: Bitmap): OcrResult = withContext(Dispatchers.Default) {
        val image = InputImage.fromBitmap(bitmap, 0)
        // DevanagariTextRecognizerOptions supports both Devanagari (Hindi, Marathi, Sanskrit, etc.) and Latin (English) scripts!
        val recognizer = TextRecognition.getClient(
            DevanagariTextRecognizerOptions.Builder().build()
        )

        suspendCancellableCoroutine { continuation ->
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val rawText = visionText.text
                    val lines = if (rawText.isBlank()) 0 else rawText.lines().size
                    val words = if (rawText.isBlank()) 0 else rawText.split(Regex("\\s+")).count { it.isNotBlank() }

                    continuation.resume(
                        OcrResult(
                            fullText = rawText.ifBlank { "No readable text detected in this document." },
                            lineCount = lines,
                            wordCount = words
                        )
                    )
                }
                .addOnFailureListener { exception ->
                    continuation.resume(
                        OcrResult(
                            fullText = "OCR Processing error: ${exception.localizedMessage ?: "Unknown error"}",
                            lineCount = 0,
                            wordCount = 0
                        )
                    )
                }
        }
    }

    /**
     * Scans QR Code / Barcode from a bitmap using Google ML Kit BarcodeScanning.
     */
    suspend fun scanBarcode(bitmap: Bitmap): String? = withContext(Dispatchers.Default) {
        val image = InputImage.fromBitmap(bitmap, 0)
        val scanner = BarcodeScanning.getClient()

        suspendCancellableCoroutine { continuation ->
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    val first = barcodes.firstOrNull()
                    if (first != null) {
                        val displayValue = first.displayValue ?: first.rawValue
                        continuation.resume(displayValue)
                    } else {
                        continuation.resume(null)
                    }
                }
                .addOnFailureListener {
                    continuation.resume(null)
                }
        }
    }
}
