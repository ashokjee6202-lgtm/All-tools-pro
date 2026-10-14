package com.example.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.example.ui.components.ResultSummaryCard
import com.example.ui.components.shareFile
import com.example.ui.components.viewFile
import com.example.util.ImageProcessor
import com.example.util.LocaleManager
import com.example.util.PdfCompressResult
import com.example.util.PdfPagePreset
import com.example.util.PdfProcessor
import com.example.util.Strings
import kotlinx.coroutines.launch
import java.io.File

enum class PdfSubTool(val labelKey: String) {
    COMPRESS("tool_pdf_compress"),
    PAGE_RESIZE("tool_pdf_resize"),
    IMG_TO_PDF("tool_img_to_pdf"),
    PDF_TO_IMG("tool_pdf_to_img"),
    MERGE("tool_merge_pdf"),
    SPLIT("tool_split_pdf"),
    WATERMARK("tool_watermark_pdf"),
    BLANK_PDF("tool_blank_pdf")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfToolsScreen(
    initialTool: String? = null,
    onNavigateBack: () -> Unit,
    onNavigateHome: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var activeTool by remember {
        mutableStateOf(
            when (initialTool) {
                "pdf_resize" -> PdfSubTool.PAGE_RESIZE
                "img_to_pdf" -> PdfSubTool.IMG_TO_PDF
                "pdf_to_img" -> PdfSubTool.PDF_TO_IMG
                "merge_pdf" -> PdfSubTool.MERGE
                "split_pdf" -> PdfSubTool.SPLIT
                "watermark_pdf" -> PdfSubTool.WATERMARK
                "blank_pdf" -> PdfSubTool.BLANK_PDF
                else -> PdfSubTool.COMPRESS
            }
        )
    }

    // Shared PDF state
    var selectedPdfFile by remember { mutableStateOf<File?>(null) }
    var pdfPageCount by remember { mutableIntStateOf(0) }
    var renderedThumbnails by remember { mutableStateOf<List<Bitmap>>(emptyList()) }

    // Multi-PDF for Merge
    val mergePdfFiles = remember { mutableStateListOf<File>() }

    // Multi-Image for Images to PDF
    val imgToPdfBitmaps = remember { mutableStateListOf<Bitmap>() }

    // Processing & Output
    var isProcessing by remember { mutableStateOf(false) }
    var progressInfo by remember { mutableStateOf<String?>(null) }
    var outputPdfFile by remember { mutableStateOf<File?>(null) }
    var compressResult by remember { mutableStateOf<PdfCompressResult?>(null) }
    var extractedImageFiles by remember { mutableStateOf<List<File>>(emptyList()) }

    // Compression Mode controls
    var compressionPresetKb by remember { mutableLongStateOf(200L) }
    var isCustomCompressKb by remember { mutableStateOf(false) }
    var customCompressKbInput by remember { mutableStateOf("") }
    var compressionQualityPreset by remember { mutableStateOf("medium") } // low, medium, high

    // Page Resize Mode controls
    var targetPagePreset by remember { mutableStateOf(PdfPagePreset.A4) }
    var isLandscapePage by remember { mutableStateOf(false) }

    // Split controls
    var splitStartPage by remember { mutableStateOf("1") }
    var splitEndPage by remember { mutableStateOf("1") }

    // Watermark controls
    var watermarkTextInput by remember { mutableStateOf("CONFIDENTIAL") }
    var addPageNumbersCheck by remember { mutableStateOf(true) }

    // Blank PDF controls
    var blankPageCount by remember { mutableIntStateOf(3) }
    var blankPaperStyle by remember { mutableStateOf("lined") }

    // PDF Single File Picker
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            outputPdfFile = null
            compressResult = null
            coroutineScope.launch {
                val file = PdfProcessor.copyUriToTempFile(context, uri)
                if (file != null) {
                    selectedPdfFile = file
                    pdfPageCount = PdfProcessor.getPdfPageCount(file)
                    splitEndPage = pdfPageCount.coerceAtLeast(1).toString()
                    renderedThumbnails = PdfProcessor.renderPdfPagesToBitmaps(file, scaleFactor = 0.6f, maxPages = 6)
                }
            }
        }
    }

    // Multi PDF Picker for Merge
    val multiPdfPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        coroutineScope.launch {
            mergePdfFiles.clear()
            uris.forEach { u ->
                val f = PdfProcessor.copyUriToTempFile(context, u, "merge_doc")
                if (f != null) mergePdfFiles.add(f)
            }
        }
    }

    // Multi Image Picker for Img to PDF
    val multiImgPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        coroutineScope.launch {
            imgToPdfBitmaps.clear()
            uris.forEach { u ->
                val b = ImageProcessor.loadBitmapFromUri(context, u)
                if (b != null) imgToPdfBitmaps.add(b)
            }
        }
    }

    fun loadSamplePdf() {
        isProcessing = true
        coroutineScope.launch {
            val samplePdf = com.example.util.SampleDataProvider.getOrCreateSamplePdf(context)
            selectedPdfFile = samplePdf
            pdfPageCount = PdfProcessor.getPdfPageCount(samplePdf)
            splitEndPage = pdfPageCount.coerceAtLeast(1).toString()
            renderedThumbnails = PdfProcessor.renderPdfPagesToBitmaps(samplePdf, scaleFactor = 0.6f, maxPages = 6)
            compressResult = null
            outputPdfFile = null
            isProcessing = false
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = Strings.get("cat_pdf"),
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
            // Sub-tool switcher tabs
            ScrollableTabRow(
                selectedTabIndex = activeTool.ordinal,
                edgePadding = 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                PdfSubTool.values().forEach { tool ->
                    Tab(
                        selected = activeTool == tool,
                        onClick = {
                            activeTool = tool
                            outputPdfFile = null
                            compressResult = null
                            extractedImageFiles = emptyList()
                        },
                        text = { Text(Strings.get(tool.labelKey)) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (activeTool) {
                PdfSubTool.COMPRESS -> {
                    // MODE A: Compress PDF by file size
                    Text(
                        text = "Mode A: Compress PDF by File Size",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Reduce file size to meet email, upload, or portal requirements.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (selectedPdfFile == null) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { pdfPickerLauncher.launch("application/pdf") },
                                modifier = Modifier.weight(1f).testTag("select_pdf_btn"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.UploadFile, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(Strings.get("select_pdf"))
                            }
                            if (com.example.BuildConfig.DEBUG) {
                                OutlinedButton(
                                    onClick = { loadSamplePdf() },
                                    modifier = Modifier.weight(1f).testTag("load_sample_pdf_btn"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                    )
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("⚡ Sample PDF")
                                }
                            }
                        }
                    } else {
                        // PDF info card
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = selectedPdfFile!!.name,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = "$pdfPageCount pages • ${ImageProcessor.formatBytes(selectedPdfFile!!.length())}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    TextButton(onClick = { pdfPickerLauncher.launch("application/pdf") }) {
                                        Text("Change")
                                    }
                                }

                                if (renderedThumbnails.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        items(renderedThumbnails) { bmp ->
                                            Image(
                                                bitmap = bmp.asImageBitmap(),
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .height(70.dp)
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .border(1.dp, Color.LightGray, RoundedCornerShape(4.dp))
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Target Presets
                        Text(text = "Target Size Presets: ", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(100L, 200L, 500L, 1024L, 2048L).forEach { kb ->
                                val label = if (kb >= 1024L) "${kb / 1024} MB" else "${kb} KB"
                                FilterChip(
                                    selected = !isCustomCompressKb && compressionPresetKb == kb,
                                    onClick = {
                                        isCustomCompressKb = false
                                        compressionPresetKb = kb
                                    },
                                    label = { Text(label) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = customCompressKbInput,
                            onValueChange = {
                                customCompressKbInput = it
                                isCustomCompressKb = true
                            },
                            label = { Text("Custom Target (KB)") },
                            placeholder = { Text("e.g. 350") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(text = "Compression Profile: ", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("low" to "Maximum Compression", "medium" to "Balanced (Recommended)", "high" to "High Quality").forEach { (id, label) ->
                                FilterChip(
                                    selected = compressionQualityPreset == id,
                                    onClick = { compressionQualityPreset = id },
                                    label = { Text(label) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                isProcessing = true
                                coroutineScope.launch {
                                    val targetBytes = if (isCustomCompressKb && customCompressKbInput.toLongOrNull() != null) {
                                        customCompressKbInput.toLong() * 1024L
                                    } else {
                                        compressionPresetKb * 1024L
                                    }
                                    val result = PdfProcessor.compressPdf(
                                        context = context,
                                        sourcePdf = selectedPdfFile!!,
                                        targetSizeBytes = targetBytes,
                                        qualityPreset = compressionQualityPreset,
                                        onProgress = { cur, tot -> progressInfo = "Compressing page $cur of $tot..." }
                                    )
                                    compressResult = result
                                    outputPdfFile = result.outputFile
                                    RecentFileManager.addRecent(context, result.outputFile, "application/pdf", "pdf_compress")
                                    isProcessing = false
                                    progressInfo = null
                                }
                            },
                            enabled = !isProcessing,
                            modifier = Modifier.fillMaxWidth().testTag("execute_compress_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isProcessing) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(progressInfo ?: Strings.get("processing"))
                            } else {
                                Text("Compress PDF Now")
                            }
                        }
                    }

                    // Compress Result Card
                    compressResult?.let { res ->
                        Spacer(modifier = Modifier.height(16.dp))
                        ResultSummaryCard(
                            title = if (LocaleManager.isHindi()) "पीडीएफ संपीड़न पूरा हुआ!" else "PDF Compressed Successfully!",
                            originalSize = ImageProcessor.formatBytes(res.originalSizeBytes),
                            outputSize = ImageProcessor.formatBytes(res.finalSizeBytes),
                            reductionRatio = "${res.reductionPercent}%",
                            onView = { viewFile(context, res.outputFile, "application/pdf") },
                            onShare = { shareFile(context, res.outputFile, "application/pdf") }
                        )
                    }
                }

                PdfSubTool.PAGE_RESIZE -> {
                    // MODE B: Resize PDF page dimensions
                    Text(
                        text = "Mode B: Resize PDF Page Dimensions",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Convert existing PDF pages to standardized print formats (A4, Letter, A5 etc.) with content preservation.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (selectedPdfFile == null) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { pdfPickerLauncher.launch("application/pdf") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.UploadFile, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(Strings.get("select_pdf"))
                            }
                            OutlinedButton(
                                onClick = { loadSamplePdf() },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                )
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("⚡ Sample PDF")
                            }
                        }
                    } else {
                        Text(text = "Target Page Size Preset:", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                        Spacer(modifier = Modifier.height(6.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(
                                PdfPagePreset.A4,
                                PdfPagePreset.A5,
                                PdfPagePreset.A3,
                                PdfPagePreset.LETTER,
                                PdfPagePreset.LEGAL
                            ).forEach { preset ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    RadioButton(
                                        selected = targetPagePreset == preset,
                                        onClick = { targetPagePreset = preset }
                                    )
                                    Text(preset.title, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = isLandscapePage,
                                onCheckedChange = { isLandscapePage = it }
                            )
                            Text("Landscape Orientation", style = MaterialTheme.typography.bodyMedium)
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                isProcessing = true
                                coroutineScope.launch {
                                    val out = PdfProcessor.resizePdfDimensions(
                                        context = context,
                                        sourcePdf = selectedPdfFile!!,
                                        targetPreset = targetPagePreset,
                                        isLandscape = isLandscapePage,
                                        onProgress = { cur, tot -> progressInfo = "Resizing page $cur of $tot..." }
                                    )
                                    outputPdfFile = out
                                    RecentFileManager.addRecent(context, out, "application/pdf", "pdf_resize")
                                    isProcessing = false
                                    progressInfo = null
                                }
                            },
                            enabled = !isProcessing,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isProcessing) {
                                Text(progressInfo ?: Strings.get("processing"))
                            } else {
                                Text("Resize Pages to ${targetPagePreset.name}")
                            }
                        }
                    }
                }

                PdfSubTool.IMG_TO_PDF -> {
                    Text(
                        text = Strings.get("tool_img_to_pdf"),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { multiImgPicker.launch("image/*") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (imgToPdfBitmaps.isEmpty()) "Select Images" else "${imgToPdfBitmaps.size} Selected")
                        }
                        OutlinedButton(
                            onClick = {
                                imgToPdfBitmaps.clear()
                                imgToPdfBitmaps.add(com.example.util.SampleDataProvider.createSampleDocumentBitmap())
                                imgToPdfBitmaps.add(com.example.util.SampleDataProvider.createSamplePhotoBitmap())
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                            )
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("⚡ 2 Samples")
                        }
                    }

                    if (imgToPdfBitmaps.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            itemsIndexed(imgToPdfBitmaps) { idx, bmp ->
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                isProcessing = true
                                coroutineScope.launch {
                                    val pdf = PdfProcessor.imagesToPdf(
                                        context = context,
                                        images = imgToPdfBitmaps,
                                        preset = PdfPagePreset.A4,
                                        outputName = "Converted_Images_Doc"
                                    )
                                    outputPdfFile = pdf
                                    RecentFileManager.addRecent(context, pdf, "application/pdf", "img_to_pdf")
                                    isProcessing = false
                                }
                            },
                            enabled = !isProcessing,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isProcessing) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                            } else {
                                Text("Generate Single PDF (${imgToPdfBitmaps.size} pages)")
                            }
                        }
                    }
                }

                PdfSubTool.PDF_TO_IMG -> {
                    Text(
                        text = Strings.get("tool_pdf_to_img"),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (selectedPdfFile == null) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { pdfPickerLauncher.launch("application/pdf") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(Strings.get("select_pdf"))
                            }
                            OutlinedButton(
                                onClick = { loadSamplePdf() },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                )
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("⚡ Sample PDF")
                            }
                        }
                    } else {
                        Text(
                            text = "${selectedPdfFile!!.name} ($pdfPageCount pages)",
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                isProcessing = true
                                coroutineScope.launch {
                                    val bmps = PdfProcessor.renderPdfPagesToBitmaps(selectedPdfFile!!, scaleFactor = 2.0f)
                                    val outFiles = bmps.mapIndexed { idx, bmp ->
                                        ImageProcessor.resizeByDimensions(
                                            context = context,
                                            sourceBitmap = bmp,
                                            targetWidth = bmp.width,
                                            targetHeight = bmp.height,
                                            format = com.example.util.ImageOutputFormat.JPEG,
                                            outputPrefix = "page_${idx + 1}"
                                        ).outputFile
                                    }
                                    extractedImageFiles = outFiles
                                    val zip = ImageProcessor.createZipArchive(context, outFiles, "Extracted_PDF_Pages")
                                    outputPdfFile = zip
                                    RecentFileManager.addRecent(context, zip, "application/zip", "pdf_to_img")
                                    isProcessing = false
                                }
                            },
                            enabled = !isProcessing,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isProcessing) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                            } else {
                                Text("Extract All Pages as JPGs & ZIP")
                            }
                        }
                    }
                }

                PdfSubTool.MERGE -> {
                    Text(
                        text = Strings.get("tool_merge_pdf"),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { multiPdfPicker.launch("application/pdf") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.MergeType, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (mergePdfFiles.isEmpty()) "Select PDFs" else "${mergePdfFiles.size} Selected")
                        }
                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    val p1 = com.example.util.SampleDataProvider.getOrCreateSamplePdf(context)
                                    val p2 = PdfProcessor.createBlankPdf(context, 2, "lined")
                                    mergePdfFiles.clear()
                                    mergePdfFiles.add(p1)
                                    mergePdfFiles.add(p2)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                            )
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("⚡ 2 Samples")
                        }
                    }

                    if (mergePdfFiles.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        mergePdfFiles.forEachIndexed { i, f ->
                            Text(text = "${i + 1}. ${f.name}", style = MaterialTheme.typography.bodySmall)
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                isProcessing = true
                                coroutineScope.launch {
                                    val merged = PdfProcessor.mergePdfs(
                                        context = context,
                                        pdfFiles = mergePdfFiles,
                                        onProgress = { cur, tot -> progressInfo = "Merging document $cur of $tot..." }
                                    )
                                    outputPdfFile = merged
                                    RecentFileManager.addRecent(context, merged, "application/pdf", "merge_pdf")
                                    isProcessing = false
                                    progressInfo = null
                                }
                            },
                            enabled = !isProcessing && mergePdfFiles.size >= 2,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isProcessing) {
                                Text(progressInfo ?: Strings.get("processing"))
                            } else {
                                Text("Merge ${mergePdfFiles.size} PDFs into One")
                            }
                        }
                    }
                }

                PdfSubTool.SPLIT -> {
                    Text(
                        text = Strings.get("tool_split_pdf"),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (selectedPdfFile == null) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { pdfPickerLauncher.launch("application/pdf") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.UploadFile, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(Strings.get("select_pdf"))
                            }
                            OutlinedButton(
                                onClick = { loadSamplePdf() },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                )
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("⚡ Sample PDF")
                            }
                        }
                    } else {
                        Text(text = "${selectedPdfFile!!.name} ($pdfPageCount total pages)", fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = splitStartPage,
                                onValueChange = { splitStartPage = it },
                                label = { Text("From Page") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = splitEndPage,
                                onValueChange = { splitEndPage = it },
                                label = { Text("To Page") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                isProcessing = true
                                coroutineScope.launch {
                                    val start = splitStartPage.toIntOrNull() ?: 1
                                    val end = splitEndPage.toIntOrNull() ?: pdfPageCount
                                    val split = PdfProcessor.splitPdf(context, selectedPdfFile!!, start, end)
                                    outputPdfFile = split
                                    RecentFileManager.addRecent(context, split, "application/pdf", "split_pdf")
                                    isProcessing = false
                                }
                            },
                            enabled = !isProcessing,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isProcessing) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                            } else {
                                Text("Split / Extract Pages")
                            }
                        }
                    }
                }

                PdfSubTool.WATERMARK -> {
                    Text(
                        text = Strings.get("tool_watermark_pdf"),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (selectedPdfFile == null) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { pdfPickerLauncher.launch("application/pdf") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.UploadFile, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(Strings.get("select_pdf"))
                            }
                            OutlinedButton(
                                onClick = { loadSamplePdf() },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                )
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("⚡ Sample PDF")
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = watermarkTextInput,
                            onValueChange = { watermarkTextInput = it },
                            label = { Text("Watermark Text (e.g. DRAFT, CONFIDENTIAL)") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = addPageNumbersCheck,
                                onCheckedChange = { addPageNumbersCheck = it }
                            )
                            Text("Add 'Page X of Y' numbering at bottom", style = MaterialTheme.typography.bodyMedium)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                isProcessing = true
                                coroutineScope.launch {
                                    val watermarked = PdfProcessor.addWatermarkAndNumbers(
                                        context = context,
                                        sourcePdf = selectedPdfFile!!,
                                        watermarkText = watermarkTextInput,
                                        addPageNumbers = addPageNumbersCheck
                                    )
                                    outputPdfFile = watermarked
                                    RecentFileManager.addRecent(context, watermarked, "application/pdf", "watermark_pdf")
                                    isProcessing = false
                                }
                            },
                            enabled = !isProcessing,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isProcessing) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                            } else {
                                Text("Apply Watermark & Page Numbers")
                            }
                        }
                    }
                }

                PdfSubTool.BLANK_PDF -> {
                    Text(
                        text = Strings.get("tool_blank_pdf"),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(text = "Paper Template: ", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("lined" to "Ruled Notebook", "grid" to "Grid Graph", "plain" to "Plain Sheet").forEach { (style, label) ->
                            FilterChip(
                                selected = blankPaperStyle == style,
                                onClick = { blankPaperStyle = style },
                                label = { Text(label) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Number of Pages: $blankPageCount", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.width(16.dp))
                        IconButton(onClick = { if (blankPageCount > 1) blankPageCount-- }) {
                            Icon(Icons.Default.Remove, contentDescription = "Minus")
                        }
                        IconButton(onClick = { if (blankPageCount < 20) blankPageCount++ }) {
                            Icon(Icons.Default.Add, contentDescription = "Plus")
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            isProcessing = true
                            coroutineScope.launch {
                                val blank = PdfProcessor.createBlankPdf(context, blankPageCount, blankPaperStyle)
                                outputPdfFile = blank
                                RecentFileManager.addRecent(context, blank, "application/pdf", "blank_pdf")
                                isProcessing = false
                            }
                        },
                        enabled = !isProcessing,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                        } else {
                            Text("Create Blank Notebook PDF")
                        }
                    }
                }
            }

            // Generic Output Card
            outputPdfFile?.let { file ->
                Spacer(modifier = Modifier.height(18.dp))
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "File Ready: ${file.name} (${ImageProcessor.formatBytes(file.length())})",
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val mime = if (file.name.endsWith(".zip")) "application/zip" else "application/pdf"
                            Button(
                                onClick = { viewFile(context, file, mime) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(Strings.get("preview"))
                            }
                            OutlinedButton(
                                onClick = { shareFile(context, file, mime) },
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
