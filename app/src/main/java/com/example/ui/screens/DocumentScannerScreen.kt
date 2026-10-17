package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.Settings as AndroidSettings
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.BuildConfig
import com.example.ui.components.AppTopBar
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.BrandPurple
import com.example.util.DocumentImageTransformer
import com.example.util.DocumentOcrEngine
import com.example.util.ImageProcessor
import com.example.util.LocaleManager
import com.example.util.ScannerFilterType
import com.example.util.Strings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.Executors

enum class ScannerMode(val key: String) {
    ID_CARD("mode_id_card"),
    DOCS("mode_docs"),
    BOOK("mode_book"),
    TO_TEXT("mode_to_text"),
    QR_CODE("mode_qr_code")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentScannerScreen(
    onNavigateBack: () -> Unit,
    onNavigateHome: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    // Scanning Workflow Steps:
    // "camera" -> "crop" -> "enhance" -> "pdf_editor" | "ocr_result"
    var currentWorkflowStep by remember { mutableStateOf("camera") }

    // Scanner Mode
    var currentMode by remember { mutableStateOf(ScannerMode.DOCS) }

    // Camera settings
    var isFlashOn by remember { mutableStateOf(false) }
    var isGridVisible by remember { mutableStateOf(true) }
    var isHdMode by remember { mutableStateOf(true) }
    var isAutoCapture by remember { mutableStateOf(false) }

    // ID Card multi-step state
    var idCardFrontBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var idCardStage by remember { mutableStateOf("front") } // "front" or "back"

    // Multi-page scans list
    val scannedPages = remember { mutableStateListOf<EnhancedPage>() }
    var pageToCropIndex by remember { mutableIntStateOf(0) }
    var rawBitmapToCrop by remember { mutableStateOf<Bitmap?>(null) }

    // OCR extracted result text
    var ocrResultText by remember { mutableStateOf("") }
    var detectedQrCode by remember { mutableStateOf<String?>(null) }

    var isCapturing by remember { mutableStateOf(false) }
    var cameraHardwareAvailable by remember { mutableStateOf(true) }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    var showPermissionDialog by remember { mutableStateOf(false) }
    var isPermanentlyDenied by remember { mutableStateOf(false) }

    // CameraX instance holders
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var activeCamera: Camera? by remember { mutableStateOf(null) }

    // Fallback file for TakePicture intent
    var tempCameraFile by remember { mutableStateOf<File?>(null) }

    fun processCapturedImage(rawBitmap: Bitmap) {
        when (currentMode) {
            ScannerMode.ID_CARD -> {
                if (idCardStage == "front") {
                    idCardFrontBitmap = rawBitmap
                    idCardStage = "back"
                    Toast.makeText(context, Strings.get("scan_back_side"), Toast.LENGTH_SHORT).show()
                } else {
                    // Both front and back ready -> composite them!
                    val front = idCardFrontBitmap ?: rawBitmap
                    val composited = DocumentImageTransformer.compositeIdCard(front, rawBitmap)
                    idCardFrontBitmap = null
                    idCardStage = "front"
                    rawBitmapToCrop = composited
                    currentWorkflowStep = "crop"
                }
            }
            ScannerMode.TO_TEXT -> {
                // Direct on-device OCR
                coroutineScope.launch {
                    isCapturing = true
                    try {
                        val res = DocumentOcrEngine.recognizeText(rawBitmap)
                        ocrResultText = res.fullText
                        currentWorkflowStep = "ocr_result"
                    } catch (e: Exception) {
                        Toast.makeText(context, "OCR failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    } finally {
                        isCapturing = false
                    }
                }
            }
            ScannerMode.QR_CODE -> {
                coroutineScope.launch {
                    val code = DocumentOcrEngine.scanBarcode(rawBitmap)
                    if (code != null) {
                        detectedQrCode = code
                    } else {
                        Toast.makeText(context, "No QR Code found in image", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            ScannerMode.DOCS, ScannerMode.BOOK -> {
                rawBitmapToCrop = rawBitmap
                currentWorkflowStep = "crop"
            }
        }
    }

    // Permission Launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            val activity = context as? Activity
            val showRationale = activity != null && ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.CAMERA
            )
            isPermanentlyDenied = !showRationale
            showPermissionDialog = true
        }
    }

    // Camera Intent Fallback Launcher
    val fallbackCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempCameraFile != null && tempCameraFile!!.exists()) {
            val bmp = ImageProcessor.loadBitmapFromUri(context, Uri.fromFile(tempCameraFile))
            if (bmp != null) {
                processCapturedImage(bmp)
            }
        }
    }

    fun startFallbackCamera() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            return
        }
        try {
            val imageDir = File(context.cacheDir, "camera_scans").apply { mkdirs() }
            val file = File(imageDir, "scan_${System.currentTimeMillis()}.jpg")
            tempCameraFile = file
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            fallbackCameraLauncher.launch(uri)
        } catch (_: Exception) {
            Toast.makeText(context, Strings.get("camera_open_failed"), Toast.LENGTH_SHORT).show()
        }
    }

    fun triggerShutterCapture() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            return
        }
        val capture = imageCapture
        if (capture != null && cameraHardwareAvailable) {
            isCapturing = true
            val photoFile = File(context.cacheDir, "cap_${System.currentTimeMillis()}.jpg")
            val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
            val executor = ContextCompat.getMainExecutor(context)

            capture.takePicture(
                outputOptions,
                executor,
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        isCapturing = false
                        val bmp = ImageProcessor.loadBitmapFromUri(context, Uri.fromFile(photoFile))
                        if (bmp != null) {
                            processCapturedImage(bmp)
                        } else {
                            startFallbackCamera()
                        }
                    }

                    override fun onError(exception: ImageCaptureException) {
                        isCapturing = false
                        // CameraX device error -> use fallback camera intent
                        startFallbackCamera()
                    }
                }
            )
        } else {
            startFallbackCamera()
        }
    }

    // Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        uris.forEach { uri ->
            val bmp = ImageProcessor.loadBitmapFromUri(context, uri)
            if (bmp != null) {
                val enhanced = DocumentImageTransformer.applyEnhancementFilter(bmp, ScannerFilterType.MAGIC_COLOR, 0.9f)
                scannedPages.add(EnhancedPage(bmp, enhanced))
            }
        }
        if (scannedPages.isNotEmpty()) {
            currentWorkflowStep = "enhance"
        }
    }

    // File Import (PDF / Images)
    val fileImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        uris.forEach { uri ->
            val bmp = ImageProcessor.loadBitmapFromUri(context, uri)
            if (bmp != null) {
                val enhanced = DocumentImageTransformer.applyEnhancementFilter(bmp, ScannerFilterType.MAGIC_COLOR, 0.9f)
                scannedPages.add(EnhancedPage(bmp, enhanced))
            }
        }
        if (scannedPages.isNotEmpty()) {
            currentWorkflowStep = "enhance"
        }
    }

    // Auto-capture timer when Auto-Capture is turned on
    LaunchedEffect(isAutoCapture, currentWorkflowStep) {
        if (isAutoCapture && currentWorkflowStep == "camera") {
            delay(2800)
            if (isAutoCapture && currentWorkflowStep == "camera" && !isCapturing) {
                triggerShutterCapture()
            }
        }
    }

    // Render Sub-workflow screens
    when (currentWorkflowStep) {
        "crop" -> {
            rawBitmapToCrop?.let { bmp ->
                CropPerspectiveScreen(
                    sourceBitmap = bmp,
                    pageIndex = pageToCropIndex,
                    totalPages = scannedPages.size + 1,
                    onCropConfirmed = { croppedBmp ->
                        val enhancedBmp = DocumentImageTransformer.applyEnhancementFilter(
                            source = croppedBmp,
                            filterType = ScannerFilterType.MAGIC_COLOR,
                            intensity = 0.9f
                        )
                        scannedPages.add(
                            EnhancedPage(
                                originalCroppedBitmap = croppedBmp,
                                filteredBitmap = enhancedBmp,
                                activeFilter = ScannerFilterType.MAGIC_COLOR,
                                filterIntensity = 0.9f
                            )
                        )
                        currentWorkflowStep = "enhance"
                    },
                    onNavigateBack = { currentWorkflowStep = "camera" },
                    onNavigateHome = onNavigateHome
                )
                return
            }
        }

        "enhance" -> {
            DocumentEnhanceScreen(
                pages = scannedPages,
                onAddMorePages = { currentWorkflowStep = "camera" },
                onProceedToPdfEditor = {
                    currentWorkflowStep = "pdf_editor"
                },
                onExtractOcr = { bmp ->
                    coroutineScope.launch {
                        val res = DocumentOcrEngine.recognizeText(bmp)
                        ocrResultText = res.fullText
                        currentWorkflowStep = "ocr_result"
                    }
                },
                onNavigateBack = {
                    if (scannedPages.isEmpty()) {
                        currentWorkflowStep = "camera"
                    } else {
                        onNavigateBack()
                    }
                },
                onNavigateHome = onNavigateHome
            )
            return
        }

        "pdf_editor" -> {
            val bitmaps = scannedPages.map { it.filteredBitmap }
            PdfEditorScreen(
                pageBitmaps = bitmaps,
                onNavigateBack = { currentWorkflowStep = "enhance" },
                onNavigateHome = onNavigateHome,
                onDocumentSaved = {
                    onNavigateHome()
                }
            )
            return
        }

        "ocr_result" -> {
            OcrResultScreen(
                initialText = ocrResultText,
                onNavigateBack = { currentWorkflowStep = "camera" },
                onNavigateHome = onNavigateHome
            )
            return
        }
    }

    // Default "camera" Screen View
    Scaffold(
        topBar = {
            AppTopBar(
                title = Strings.get("cat_scanner"),
                canNavigateBack = true,
                onNavigateBack = onNavigateBack,
                onNavigateHome = onNavigateHome
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
                .background(Color(0xFF0F172A))
        ) {
            // 1. Top controls row (flash, HD, grid, Manual/Auto)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Flash toggle
                IconButton(
                    onClick = {
                        isFlashOn = !isFlashOn
                        activeCamera?.cameraControl?.enableTorch(isFlashOn)
                    },
                    modifier = Modifier
                        .size(42.dp)
                        .background(Color.White.copy(alpha = 0.15f), CircleShape)
                ) {
                    Icon(
                        if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = "Flash",
                        tint = if (isFlashOn) Color.Yellow else Color.White
                    )
                }

                // HD toggle
                FilterChip(
                    selected = isHdMode,
                    onClick = { isHdMode = !isHdMode },
                    label = { Text("HD", fontWeight = FontWeight.Bold, maxLines = 1) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = BrandBlue,
                        selectedLabelColor = Color.White,
                        containerColor = Color.White.copy(alpha = 0.15f),
                        labelColor = Color.White
                    )
                )

                // Grid toggle
                IconButton(
                    onClick = { isGridVisible = !isGridVisible },
                    modifier = Modifier
                        .size(42.dp)
                        .background(Color.White.copy(alpha = 0.15f), CircleShape)
                ) {
                    Icon(
                        Icons.Default.GridOn,
                        contentDescription = "Grid",
                        tint = if (isGridVisible) BrandBlue else Color.White
                    )
                }

                // Auto / Manual Capture toggle
                FilterChip(
                    selected = isAutoCapture,
                    onClick = { isAutoCapture = !isAutoCapture },
                    label = { Text(if (isAutoCapture) Strings.get("auto_capture") else Strings.get("manual_capture"), maxLines = 1) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = BrandPurple,
                        selectedLabelColor = Color.White,
                        containerColor = Color.White.copy(alpha = 0.15f),
                        labelColor = Color.White
                    )
                )
            }

            // 2. Camera preview area with Modifier.weight(1f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clipToBounds(),
                contentAlignment = Alignment.Center
            ) {
                if (!hasCameraPermission) {
                    // Permission Request Box
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    tint = BrandBlue,
                                    modifier = Modifier.size(54.dp)
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = Strings.get("camera_permission_needed"),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(18.dp))
                                Button(
                                    onClick = {
                                        if (isPermanentlyDenied) {
                                            val intent = Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                                data = Uri.fromParts("package", context.packageName, null)
                                            }
                                            context.startActivity(intent)
                                        } else {
                                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(if (isPermanentlyDenied) Strings.get("open_settings") else Strings.get("confirm"))
                                }
                            }
                        }
                    }
                } else {
                    // CameraX preview - COMPATIBLE mode prevents black preview on emulators and overlays
                    AndroidView(
                        factory = { ctx ->
                            val previewView = PreviewView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                                scaleType = PreviewView.ScaleType.FILL_CENTER
                            }

                            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                            cameraProviderFuture.addListener({
                                try {
                                    val cameraProvider = cameraProviderFuture.get()
                                    val preview = Preview.Builder().build().also {
                                        it.setSurfaceProvider(previewView.surfaceProvider)
                                    }

                                    val capture = ImageCapture.Builder()
                                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                                        .build()
                                    imageCapture = capture

                                    val cameraSelector = if (cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
                                        CameraSelector.DEFAULT_BACK_CAMERA
                                    } else if (cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                                        CameraSelector.DEFAULT_FRONT_CAMERA
                                    } else {
                                        CameraSelector.DEFAULT_BACK_CAMERA
                                    }

                                    cameraProvider.unbindAll()
                                    val cam = cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        cameraSelector,
                                        preview,
                                        capture
                                    )
                                    activeCamera = cam
                                } catch (_: Exception) {
                                    cameraHardwareAvailable = false
                                }
                            }, ContextCompat.getMainExecutor(ctx))

                            previewView
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Grid lines overlay (if enabled)
                    if (isGridVisible) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height

                            drawLine(Color.White.copy(alpha = 0.25f), Offset(w / 3f, 0f), Offset(w / 3f, h), strokeWidth = 1.5f)
                            drawLine(Color.White.copy(alpha = 0.25f), Offset(w * 2f / 3f, 0f), Offset(w * 2f / 3f, h), strokeWidth = 1.5f)
                            drawLine(Color.White.copy(alpha = 0.25f), Offset(0f, h / 3f), Offset(w, h / 3f), strokeWidth = 1.5f)
                            drawLine(Color.White.copy(alpha = 0.25f), Offset(0f, h * 2f / 3f), Offset(w, h * 2f / 3f), strokeWidth = 1.5f)
                        }
                    }

                    // 4. Draw the scan frame inside the preview area with equal margins, using aspectRatio(1f / 1.414f) (A4 ratio), scaling with screen
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        when (currentMode) {
                            ScannerMode.DOCS -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight(0.92f)
                                        .aspectRatio(1f / 1.414f)
                                        .border(2.5.dp, Color(0xFF38BDF8), RoundedCornerShape(14.dp))
                                )
                            }
                            ScannerMode.ID_CARD -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.88f)
                                        .aspectRatio(1.586f)
                                        .border(2.5.dp, BrandPurple, RoundedCornerShape(16.dp)),
                                    contentAlignment = Alignment.TopCenter
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = BrandPurple.copy(alpha = 0.85f),
                                        modifier = Modifier.padding(top = 10.dp)
                                    ) {
                                        Text(
                                            text = if (idCardStage == "front") Strings.get("scan_front_side") else Strings.get("scan_back_side"),
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                            ScannerMode.BOOK -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight(0.92f)
                                        .aspectRatio(1.33f)
                                        .border(2.5.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .width(2.dp)
                                            .background(Color(0xFF38BDF8))
                                            .align(Alignment.Center)
                                    )
                                }
                            }
                            ScannerMode.TO_TEXT -> {
                                val infiniteTransition = rememberInfiniteTransition(label = "scan_bar")
                                val scanProgress by infiniteTransition.animateFloat(
                                    initialValue = 0.08f,
                                    targetValue = 0.92f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(1600),
                                        repeatMode = RepeatMode.Reverse
                                    ),
                                    label = "scan_y"
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight(0.92f)
                                        .aspectRatio(1f / 1.414f)
                                        .border(2.5.dp, Color(0xFF10B981), RoundedCornerShape(14.dp))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(3.dp)
                                            .background(Color(0xFF34D399))
                                            .align(Alignment.TopCenter)
                                            .offset(y = (scanProgress * 260).dp)
                                    )
                                }
                            }
                            ScannerMode.QR_CODE -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight(0.6f)
                                        .aspectRatio(1f)
                                        .border(3.dp, Color(0xFFF59E0B), RoundedCornerShape(16.dp))
                                )
                            }
                        }
                    }

                    // 6. Debug Sample Loader (Test Scanner button) - ONLY IN DEBUG BUILDS (BuildConfig.DEBUG)
                    if (BuildConfig.DEBUG && scannedPages.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 10.dp)
                        ) {
                            Surface(
                                onClick = {
                                    val bmp = com.example.util.SampleDataProvider.createSampleDocumentBitmap()
                                    processCapturedImage(bmp)
                                },
                                shape = RoundedCornerShape(20.dp),
                                color = Color.Black.copy(alpha = 0.65f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.35f))
                            ) {
                                Text(
                                    text = if (LocaleManager.isHindi()) "⚡ नमूना दस्तावेज़ (Test)" else "⚡ Load Sample Invoice (Test)",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 3. Mode tabs row (Hindi-safe with maxLines and Ellipsis, equal spacing)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.85f))
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ScannerMode.values().forEach { mode ->
                    val isSelected = currentMode == mode
                    Text(
                        text = Strings.get(mode.key),
                        color = if (isSelected) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = if (isSelected) 14.sp else 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { currentMode = mode }
                            .padding(horizontal = 6.dp, vertical = 6.dp)
                    )
                }
            }

            // 4. Bottom row (gallery button, shutter button, import button) with navigationBarsPadding()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black)
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Gallery / Import
                IconButton(
                    onClick = {
                        if (ActivityResultContracts.PickVisualMedia.isPhotoPickerAvailable(context)) {
                            photoPickerLauncher.launch(
                                androidx.activity.result.PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        } else {
                            fileImportLauncher.launch("image/*")
                        }
                    },
                    modifier = Modifier
                        .size(50.dp)
                        .background(Color.White.copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(
                        Icons.Default.PhotoLibrary,
                        contentDescription = Strings.get("gallery"),
                        tint = Color.White
                    )
                }

                // Center: Large Shutter Capture Button
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .border(4.dp, Color.White, CircleShape)
                        .padding(5.dp)
                        .clip(CircleShape)
                        .background(if (isCapturing) Color.Gray else Color.White)
                        .clickable { triggerShutterCapture() }
                        .testTag("camera_shutter_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCapturing) {
                        CircularProgressIndicator(
                            color = BrandBlue,
                            modifier = Modifier.size(28.dp),
                            strokeWidth = 3.dp
                        )
                    }
                }

                // Right: Scanned Pages Counter Badge / Import Files
                if (scannedPages.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(BrandBlue)
                            .clickable { currentWorkflowStep = "enhance" },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${scannedPages.size}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                } else {
                    IconButton(
                        onClick = { fileImportLauncher.launch("*/*") },
                        modifier = Modifier
                            .size(50.dp)
                            .background(Color.White.copy(alpha = 0.2f), CircleShape)
                    ) {
                        Icon(
                            Icons.Default.UploadFile,
                            contentDescription = Strings.get("import_files"),
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }

    // QR Result Dialog
    detectedQrCode?.let { qrText ->
        AlertDialog(
            onDismissRequest = { detectedQrCode = null },
            title = { Text("QR Code Detected") },
            text = {
                Text(
                    text = qrText,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val clip = android.content.ClipData.newPlainText("QR Code", qrText)
                        (context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager).setPrimaryClip(clip)
                        Toast.makeText(context, Strings.get("text_copied"), Toast.LENGTH_SHORT).show()
                        detectedQrCode = null
                    }
                ) {
                    Text(Strings.get("copy_text"))
                }
            },
            dismissButton = {
                TextButton(onClick = { detectedQrCode = null }) {
                    Text(Strings.get("cancel"))
                }
            }
        )
    }

    // Camera Permission Error Dialog
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text(Strings.get("camera")) },
            text = { Text(Strings.get("camera_permission_needed")) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPermissionDialog = false
                        val intent = Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    }
                ) {
                    Text(Strings.get("open_settings"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text(Strings.get("cancel"))
                }
            }
        )
    }
}
