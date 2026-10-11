package com.example.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.model.RecentFileManager
import com.example.ui.components.AppTopBar
import com.example.ui.components.shareFile
import com.example.ui.components.viewFile
import com.example.util.ImageProcessor
import com.example.util.PdfPagePreset
import com.example.util.PdfProcessor
import com.example.util.Strings
import kotlinx.coroutines.launch
import java.io.File

data class ScannedPage(
    val rawBitmap: Bitmap,
    var filteredBitmap: Bitmap,
    var rotationDegrees: Float = 0f
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentScannerScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val scannedPages = remember { mutableStateListOf<ScannedPage>() }
    var selectedPageIndex by remember { mutableIntStateOf(0) }

    // Filters state
    var isGrayscale by remember { mutableStateOf(false) }
    var isHighContrast by remember { mutableStateOf(false) }
    var brightness by remember { mutableFloatStateOf(0f) }
    var contrast by remember { mutableFloatStateOf(1f) }
    var selectedPreset by remember { mutableStateOf(PdfPagePreset.A4) }

    var isProcessing by remember { mutableStateOf(false) }
    var exportedFile by remember { mutableStateOf<File?>(null) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    var tempCameraFile by remember { mutableStateOf<File?>(null) }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempCameraFile != null) {
            val bmp = ImageProcessor.loadBitmapFromUri(context, Uri.fromFile(tempCameraFile))
            if (bmp != null) {
                val filtered = ImageProcessor.applyScannerFilter(bmp, isGrayscale, isHighContrast, brightness, contrast)
                scannedPages.add(ScannedPage(bmp, filtered))
                selectedPageIndex = scannedPages.size - 1
            }
        }
    }

    // Gallery launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        uris.forEach { uri ->
            val bmp = ImageProcessor.loadBitmapFromUri(context, uri)
            if (bmp != null) {
                val filtered = ImageProcessor.applyScannerFilter(bmp, isGrayscale, isHighContrast, brightness, contrast)
                scannedPages.add(ScannedPage(bmp, filtered))
            }
        }
        if (scannedPages.isNotEmpty() && selectedPageIndex >= scannedPages.size) {
            selectedPageIndex = scannedPages.size - 1
        }
    }

    fun launchCamera() {
        try {
            val file = File(context.cacheDir, "camera_scan_${System.currentTimeMillis()}.jpg")
            tempCameraFile = file
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            tempCameraUri = uri
            cameraLauncher.launch(uri)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open camera: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    fun updateCurrentPageFilter() {
        if (scannedPages.isNotEmpty() && selectedPageIndex in scannedPages.indices) {
            val page = scannedPages[selectedPageIndex]
            val rotated = ImageProcessor.rotateBitmap(page.rawBitmap, page.rotationDegrees)
            page.filteredBitmap = ImageProcessor.applyScannerFilter(rotated, isGrayscale, isHighContrast, brightness, contrast)
            // trigger recomposition
            scannedPages[selectedPageIndex] = page.copy(filteredBitmap = page.filteredBitmap)
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = Strings.get("cat_scanner"),
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
            // Capture Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { launchCamera() },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("scan_camera_btn"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(Strings.get("camera"))
                }

                OutlinedButton(
                    onClick = { galleryLauncher.launch("image/*") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("scan_gallery_btn"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(Strings.get("gallery"))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Sample Button for instant testing
            OutlinedButton(
                onClick = {
                    val sampleBmp = com.example.util.SampleDataProvider.createSampleDocumentBitmap()
                    val filtered = ImageProcessor.applyScannerFilter(sampleBmp, isGrayscale, isHighContrast, brightness, contrast)
                    scannedPages.add(ScannedPage(sampleBmp, filtered))
                    selectedPageIndex = scannedPages.size - 1
                },
                modifier = Modifier.fillMaxWidth().testTag("scan_sample_doc_btn"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                )
            ) {
                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (com.example.util.LocaleManager.isHindi()) "⚡ नमूना दस्तावेज़ लोड करें (तुरंत टेस्ट करें)" else "⚡ Load Sample Invoice (Test Scanner)",
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (scannedPages.isEmpty()) {
                // Empty state card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.DocumentScanner,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = Strings.get("tool_doc_scanner"),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = Strings.get("tool_doc_scanner_desc"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // Multi-page thumbnail strip
                Text(
                    text = "Scanned Pages (${scannedPages.size})",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    itemsIndexed(scannedPages) { index, page ->
                        val isSelected = index == selectedPageIndex
                        Box(
                            modifier = Modifier
                                .size(75.dp, 100.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedPageIndex = index }
                        ) {
                            Image(
                                bitmap = page.filteredBitmap.asImageBitmap(),
                                contentDescription = "Page ${index + 1}",
                                modifier = Modifier.fillMaxSize()
                            )
                            // Page number badge
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Current Page Preview & Page Controls
                if (selectedPageIndex in scannedPages.indices) {
                    val currentPage = scannedPages[selectedPageIndex]

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(280.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    bitmap = currentPage.filteredBitmap.asImageBitmap(),
                                    contentDescription = "Current Page Preview",
                                    modifier = Modifier.fillMaxHeight()
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Page Action Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                IconButton(onClick = {
                                    currentPage.rotationDegrees = (currentPage.rotationDegrees + 90f) % 360f
                                    updateCurrentPageFilter()
                                }) {
                                    Icon(imageVector = Icons.Default.RotateRight, contentDescription = "Rotate")
                                }

                                IconButton(
                                    onClick = {
                                        if (selectedPageIndex > 0) {
                                            val temp = scannedPages[selectedPageIndex]
                                            scannedPages[selectedPageIndex] = scannedPages[selectedPageIndex - 1]
                                            scannedPages[selectedPageIndex - 1] = temp
                                            selectedPageIndex--
                                        }
                                    },
                                    enabled = selectedPageIndex > 0
                                ) {
                                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Move Left")
                                }

                                IconButton(
                                    onClick = {
                                        if (selectedPageIndex < scannedPages.size - 1) {
                                            val temp = scannedPages[selectedPageIndex]
                                            scannedPages[selectedPageIndex] = scannedPages[selectedPageIndex + 1]
                                            scannedPages[selectedPageIndex + 1] = temp
                                            selectedPageIndex++
                                        }
                                    },
                                    enabled = selectedPageIndex < scannedPages.size - 1
                                ) {
                                    Icon(imageVector = Icons.Default.ArrowForward, contentDescription = "Move Right")
                                }

                                IconButton(
                                    onClick = {
                                        scannedPages.removeAt(selectedPageIndex)
                                        if (selectedPageIndex >= scannedPages.size && scannedPages.isNotEmpty()) {
                                            selectedPageIndex = scannedPages.size - 1
                                        }
                                    }
                                ) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Page", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Scanner Filter Options
                    Text(
                        text = "Document Filters & Enhancements",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = !isGrayscale && !isHighContrast,
                            onClick = {
                                isGrayscale = false
                                isHighContrast = false
                                updateCurrentPageFilter()
                            },
                            label = { Text("Color") }
                        )
                        FilterChip(
                            selected = isGrayscale && !isHighContrast,
                            onClick = {
                                isGrayscale = true
                                isHighContrast = false
                                updateCurrentPageFilter()
                            },
                            label = { Text("Grayscale") }
                        )
                        FilterChip(
                            selected = isHighContrast,
                            onClick = {
                                isGrayscale = true
                                isHighContrast = true
                                updateCurrentPageFilter()
                            },
                            label = { Text("B&W Clean") }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Brightness slider
                    Text(
                        text = "Brightness (${(brightness * 100).toInt()})",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Slider(
                        value = brightness,
                        onValueChange = {
                            brightness = it
                            updateCurrentPageFilter()
                        },
                        valueRange = -0.3f..0.3f
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Page size preset
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Page Format: ",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        listOf(PdfPagePreset.A4, PdfPagePreset.LETTER).forEach { preset ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clickable { selectedPreset = preset }
                                    .padding(end = 12.dp)
                            ) {
                                RadioButton(
                                    selected = selectedPreset == preset,
                                    onClick = { selectedPreset = preset }
                                )
                                Text(preset.name, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Export Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                isProcessing = true
                                coroutineScope.launch {
                                    val bitmaps = scannedPages.map { it.filteredBitmap }
                                    val pdf = PdfProcessor.imagesToPdf(
                                        context = context,
                                        images = bitmaps,
                                        preset = selectedPreset,
                                        marginPt = 16,
                                        outputName = "Scanned_Doc_${scannedPages.size}_pages"
                                    )
                                    exportedFile = pdf
                                    RecentFileManager.addRecent(context, pdf, "application/pdf", "doc_scanner")
                                    isProcessing = false
                                }
                            },
                            enabled = !isProcessing,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_pdf_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isProcessing) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                            } else {
                                Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Save as PDF")
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                isProcessing = true
                                coroutineScope.launch {
                                    val currentPageBmp = scannedPages[selectedPageIndex].filteredBitmap
                                    val jpgFile = ImageProcessor.resizeByDimensions(
                                        context = context,
                                        sourceBitmap = currentPageBmp,
                                        targetWidth = currentPageBmp.width,
                                        targetHeight = currentPageBmp.height,
                                        format = com.example.util.ImageOutputFormat.JPEG,
                                        outputPrefix = "scan_page_${selectedPageIndex + 1}"
                                    ).outputFile
                                    exportedFile = jpgFile
                                    RecentFileManager.addRecent(context, jpgFile, "image/jpeg", "doc_scanner")
                                    isProcessing = false
                                }
                            },
                            enabled = !isProcessing,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_jpg_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Image, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save as JPG")
                        }
                    }

                    // Result card
                    AnimatedVisibility(visible = exportedFile != null) {
                        exportedFile?.let { file ->
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF2E7D32)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Ready: ${file.name} (${ImageProcessor.formatBytes(file.length())})",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                val mime = if (file.name.endsWith(".pdf")) "application/pdf" else "image/jpeg"
                                                viewFile(context, file, mime)
                                            },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(Strings.get("preview"))
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                val mime = if (file.name.endsWith(".pdf")) "application/pdf" else "image/jpeg"
                                                shareFile(context, file, mime)
                                            },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(Strings.get("share"))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
