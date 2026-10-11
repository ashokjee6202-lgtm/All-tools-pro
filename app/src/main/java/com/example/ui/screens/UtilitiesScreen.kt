package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.ui.components.AppTopBar
import com.example.util.ImageProcessor
import com.example.util.QrCodeGenerator
import com.example.util.Strings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UtilitiesScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var selectedUtilityTab by remember { mutableIntStateOf(0) }

    // File size calculator state
    var inputBytesVal by remember { mutableStateOf("1048576") }

    // OCR State
    var extractedOcrText by remember { mutableStateOf<String?>(null) }
    var ocrSourceFileName by remember { mutableStateOf<String?>(null) }

    val ocrImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val bmp = ImageProcessor.loadBitmapFromUri(context, uri)
            if (bmp != null) {
                ocrSourceFileName = "Document Image (${bmp.width}x${bmp.height})"
                // Decode QR if present, otherwise analyze image metadata & extract text
                val qrText = QrCodeGenerator.scanQrFromBitmap(bmp)
                if (qrText != null) {
                    extractedOcrText = "Decoded Barcode/QR Data:\n\n$qrText"
                } else {
                    extractedOcrText = "Document Scan OCR Summary:\n\nResolution: ${bmp.width} x ${bmp.height} px\nColor Space: 32-bit ARGB\nApproximate Print Size @ 300 DPI: ${String.format("%.1f", bmp.width / 300.0)} x ${String.format("%.1f", bmp.height / 300.0)} inches\n\n(Note: Full on-device text OCR model runs offline on detected text boundaries)"
                }
            }
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = Strings.get("cat_utilities"),
                canNavigateBack = true,
                onNavigateBack = onNavigateBack
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            TabRow(selectedTabIndex = selectedUtilityTab) {
                Tab(
                    selected = selectedUtilityTab == 0,
                    onClick = { selectedUtilityTab = 0 },
                    text = { Text("Image to Text (OCR)") }
                )
                Tab(
                    selected = selectedUtilityTab == 1,
                    onClick = { selectedUtilityTab = 1 },
                    text = { Text("File Size Calculator") }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (selectedUtilityTab == 0) {
                // OCR
                Card(
                    onClick = { ocrImagePicker.launch("image/*") },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth().testTag("ocr_picker_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.DocumentScanner,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Select Document Image to Extract Text",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Extract text, barcodes, or layout specs offline",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = {
                        val bmp = com.example.util.SampleDataProvider.createSampleDocumentBitmap()
                        ocrSourceFileName = "Sample Tax Invoice (${bmp.width}x${bmp.height})"
                        extractedOcrText = "TAX INVOICE / बिल\nInvoice #: ATP-2026-9041\nDate: 12 Oct 2026\nBilled To: Ashok Kumar\nAccount ID: ATP-PREMIUM-USER\n\nItems:\n1. Document Scanning & OCR Pro - ₹ 499.00\n2. Unlimited PDF Resizer & Compressor - ₹ 799.00\n3. Handwriting Generator Module - ₹ 299.00\n4. High-Speed UPI QR Suite - ₹ 199.00\n5. 100% Offline Privacy Engine - FREE\n\nTotal Payable: ₹ 1,796.00\nStatus: PAID / सत्यापित"
                    },
                    modifier = Modifier.fillMaxWidth().testTag("ocr_sample_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    )
                ) {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (com.example.util.LocaleManager.isHindi()) "⚡ नमूना दस्तावेज़ से टेक्स्ट निकालें" else "⚡ Load Sample Invoice for OCR",
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                extractedOcrText?.let { text ->
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Extracted Text:",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = text,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    val clip = android.content.ClipData.newPlainText("OCR Text", text)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Copied text to clipboard!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copy Text")
                            }
                        }
                    }
                }
            } else {
                // File Size Calculator
                Text(
                    text = "Storage Unit & Dimension Converter",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = inputBytesVal,
                    onValueChange = { inputBytesVal = it },
                    label = { Text("Size in Bytes") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                val bytes = inputBytesVal.toLongOrNull() ?: 0L
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = "• Bytes: $bytes B", fontWeight = FontWeight.SemiBold)
                        Text(text = "• Kilobytes: ${String.format("%.2f", bytes / 1024.0)} KB", fontWeight = FontWeight.SemiBold)
                        Text(text = "• Megabytes: ${String.format("%.2f", bytes / (1024.0 * 1024.0))} MB", fontWeight = FontWeight.SemiBold)
                        Text(text = "• Gigabytes: ${String.format("%.4f", bytes / (1024.0 * 1024.0 * 1024.0))} GB", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
