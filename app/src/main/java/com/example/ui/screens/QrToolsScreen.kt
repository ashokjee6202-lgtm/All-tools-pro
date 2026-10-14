package com.example.ui.screens

import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.RecentFileManager
import com.example.ui.components.AppTopBar
import com.example.ui.components.shareFile
import com.example.ui.components.viewFile
import com.example.util.ImageProcessor
import com.example.util.QrCodeGenerator
import com.example.util.Strings
import kotlinx.coroutines.launch
import java.io.File

enum class QrType(val label: String) {
    UPI_PAYMENT("UPI Payment"),
    URL("Website URL"),
    TEXT("Plain Text"),
    WIFI("Wi-Fi Network"),
    BARCODE("Barcode 128")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrToolsScreen(
    initialScanMode: Boolean = false,
    onNavigateBack: () -> Unit,
    onNavigateHome: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isScannerMode by remember { mutableStateOf(initialScanMode) }
    var selectedQrType by remember { mutableStateOf(QrType.UPI_PAYMENT) }

    // UPI Fields
    var upiId by remember { mutableStateOf("") }
    var payeeName by remember { mutableStateOf("") }
    var upiAmount by remember { mutableStateOf("") }
    var upiNote by remember { mutableStateOf("") }

    // URL & Text Fields
    var urlText by remember { mutableStateOf("https://") }
    var plainText by remember { mutableStateOf("") }

    // Wi-Fi Fields
    var wifiSsid by remember { mutableStateOf("") }
    var wifiPassword by remember { mutableStateOf("") }
    var wifiSecurity by remember { mutableStateOf("WPA") }

    // Barcode Field
    var barcodeText by remember { mutableStateOf("123456789012") }

    // Customization
    var selectedColorPreset by remember { mutableStateOf(android.graphics.Color.BLACK) }

    // Generated QR / Barcode
    var generatedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var generatedPayload by remember { mutableStateOf("") }
    var exportedFile by remember { mutableStateOf<File?>(null) }

    // Scanner state
    var scannedResultText by remember { mutableStateOf<String?>(null) }

    val scanImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val bmp = ImageProcessor.loadBitmapFromUri(context, uri)
            if (bmp != null) {
                val scanned = QrCodeGenerator.scanQrFromBitmap(bmp)
                scannedResultText = scanned ?: "No QR or barcode detected in image"
            }
        }
    }

    fun generate() {
        val payload: String
        val bmp: Bitmap?

        when (selectedQrType) {
            QrType.UPI_PAYMENT -> {
                if (upiId.isBlank()) {
                    Toast.makeText(context, "Please enter a valid UPI ID", Toast.LENGTH_SHORT).show()
                    return
                }
                val name = payeeName.ifBlank { "Merchant" }
                payload = QrCodeGenerator.buildUpiUri(upiId, name, upiAmount, upiNote)
                bmp = QrCodeGenerator.generateQrBitmap(payload, 512, selectedColorPreset, android.graphics.Color.WHITE)
            }
            QrType.URL -> {
                payload = urlText.trim()
                bmp = QrCodeGenerator.generateQrBitmap(payload, 512, selectedColorPreset, android.graphics.Color.WHITE)
            }
            QrType.TEXT -> {
                payload = plainText.trim()
                bmp = QrCodeGenerator.generateQrBitmap(payload, 512, selectedColorPreset, android.graphics.Color.WHITE)
            }
            QrType.WIFI -> {
                payload = QrCodeGenerator.buildWifiConfig(wifiSsid, wifiPassword, wifiSecurity)
                bmp = QrCodeGenerator.generateQrBitmap(payload, 512, selectedColorPreset, android.graphics.Color.WHITE)
            }
            QrType.BARCODE -> {
                payload = barcodeText.trim()
                bmp = QrCodeGenerator.generateBarcodeBitmap(payload, 600, 240)
            }
        }

        generatedPayload = payload
        generatedBitmap = bmp
        exportedFile = null
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = Strings.get("cat_qr"),
                canNavigateBack = true,
                onNavigateBack = onNavigateBack,
                onNavigateHome = onNavigateHome
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
            // Mode Toggle (Generator vs Scanner)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = !isScannerMode,
                    onClick = { isScannerMode = false },
                    leadingIcon = { Icon(Icons.Default.QrCode, contentDescription = null) },
                    label = { Text("Generate QR / Barcode") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = isScannerMode,
                    onClick = { isScannerMode = true },
                    leadingIcon = { Icon(Icons.Default.QrCodeScanner, contentDescription = null) },
                    label = { Text("Scan QR / Barcode") },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isScannerMode) {
                // SCANNER MODE
                Card(
                    onClick = { scanImagePicker.launch("image/*") },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth().testTag("scan_from_gallery_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Scan QR / Barcode from Image",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Select any image from gallery to decode QR, UPI or Barcodes instantly.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                scannedResultText?.let { result ->
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(text = "Scanned Content:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = result, style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    val clip = android.content.ClipData.newPlainText("Scanned QR", result)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Copied to clipboard!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copy Scanned Result")
                            }
                        }
                    }
                }
            } else {
                // GENERATOR MODE
                Text(text = "Choose QR Type:", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                Spacer(modifier = Modifier.height(6.dp))

                ScrollableTabRow(
                    selectedTabIndex = selectedQrType.ordinal,
                    edgePadding = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    QrType.values().forEach { type ->
                        Tab(
                            selected = selectedQrType == type,
                            onClick = {
                                selectedQrType = type
                                generatedBitmap = null
                            },
                            text = { Text(type.label) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                when (selectedQrType) {
                    QrType.UPI_PAYMENT -> {
                        OutlinedTextField(
                            value = upiId,
                            onValueChange = { upiId = it },
                            label = { Text("UPI ID (e.g. name@okhdfcbank, mobile@paytm)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = payeeName,
                            onValueChange = { payeeName = it },
                            label = { Text("Payee / Store Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = upiAmount,
                            onValueChange = { upiAmount = it },
                            label = { Text("Preset Amount in ₹ (Optional)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = upiNote,
                            onValueChange = { upiNote = it },
                            label = { Text("Payment Note / Purpose (Optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                    QrType.URL -> {
                        OutlinedTextField(
                            value = urlText,
                            onValueChange = { urlText = it },
                            label = { Text("Website URL") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                    QrType.TEXT -> {
                        OutlinedTextField(
                            value = plainText,
                            onValueChange = { plainText = it },
                            label = { Text("Plain Text / Message") },
                            modifier = Modifier.fillMaxWidth().height(100.dp)
                        )
                    }
                    QrType.WIFI -> {
                        OutlinedTextField(
                            value = wifiSsid,
                            onValueChange = { wifiSsid = it },
                            label = { Text("Wi-Fi Network Name (SSID)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = wifiPassword,
                            onValueChange = { wifiPassword = it },
                            label = { Text("Wi-Fi Password") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                    QrType.BARCODE -> {
                        OutlinedTextField(
                            value = barcodeText,
                            onValueChange = { barcodeText = it },
                            label = { Text("Barcode Number / Code 128 Data") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Color Customization
                if (selectedQrType != QrType.BARCODE) {
                    Text(text = "QR Color: ", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            android.graphics.Color.BLACK to "Classic Black",
                            android.graphics.Color.rgb(21, 101, 192) to "Pro Blue",
                            android.graphics.Color.rgb(46, 125, 50) to "Forest Green",
                            android.graphics.Color.rgb(106, 27, 154) to "Royal Purple"
                        ).forEach { (col, label) ->
                            FilterChip(
                                selected = selectedColorPreset == col,
                                onClick = {
                                    selectedColorPreset = col
                                    generate()
                                },
                                label = { Text(label) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                Button(
                    onClick = { generate() },
                    modifier = Modifier.fillMaxWidth().testTag("generate_qr_btn"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.QrCode, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generate Now")
                }

                generatedBitmap?.let { bmp ->
                    Spacer(modifier = Modifier.height(18.dp))
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(240.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White)
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = "Generated QR",
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = generatedPayload,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            val png = QrCodeGenerator.saveQrAsPng(context, bmp, "QR_Code")
                                            exportedFile = png
                                            RecentFileManager.addRecent(context, png, "image/png", "qr_generator")
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Save PNG")
                                }

                                OutlinedButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            val pdf = QrCodeGenerator.saveQrAsPdf(context, bmp, "QR_Code_${selectedQrType.name}")
                                            exportedFile = pdf
                                            RecentFileManager.addRecent(context, pdf, "application/pdf", "qr_generator")
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Save PDF")
                                }
                            }
                        }
                    }

                    exportedFile?.let { file ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val mime = if (file.name.endsWith(".pdf")) "application/pdf" else "image/png"
                            Button(onClick = { viewFile(context, file, mime) }, modifier = Modifier.weight(1f)) {
                                Text("Preview")
                            }
                            OutlinedButton(onClick = { shareFile(context, file, mime) }, modifier = Modifier.weight(1f)) {
                                Text("Share")
                            }
                        }
                    }
                }
            }
        }
    }
}
