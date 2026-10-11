package com.example.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import com.example.util.ImageOutputFormat
import com.example.util.ImageProcessResult
import com.example.util.ImageProcessor
import com.example.util.Strings
import kotlinx.coroutines.launch
import java.io.File

enum class PhotoResizeTab(val titleKey: String) {
    BY_FILE_SIZE("By File Size"),
    BY_DIMENSIONS("By Dimensions"),
    BATCH_RESIZE("Batch Mode"),
    PASSPORT("Passport Photo")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoToolsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedTab by remember { mutableStateOf(PhotoResizeTab.BY_FILE_SIZE) }

    // Selected single image
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var loadedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var originalFileSizeBytes by remember { mutableLongStateOf(0L) }

    // Batch images
    val batchUris = remember { mutableStateListOf<Uri>() }
    var batchProgress by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var batchZipFile by remember { mutableStateOf<File?>(null) }

    // File size mode state
    var selectedPresetKb by remember { mutableLongStateOf(100L) }
    var customKbInput by remember { mutableStateOf("") }
    var isCustomKb by remember { mutableStateOf(false) }

    // Dimensions mode state
    var widthInput by remember { mutableStateOf("") }
    var heightInput by remember { mutableStateOf("") }
    var lockAspectRatio by remember { mutableStateOf(true) }
    var aspectRatio by remember { mutableFloatStateOf(1f) }

    // Format & Quality
    var selectedFormat by remember { mutableStateOf(ImageOutputFormat.JPEG) }
    var qualitySlider by remember { mutableFloatStateOf(90f) }

    // Results & Loading
    var isProcessing by remember { mutableStateOf(false) }
    var processResult by remember { mutableStateOf<ImageProcessResult?>(null) }

    // Single image picker
    val singlePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            selectedUri = uri
            processResult = null
            coroutineScope.launch {
                val bmp = ImageProcessor.loadBitmapFromUri(context, uri)
                loadedBitmap = bmp
                bmp?.let {
                    aspectRatio = it.width.toFloat() / it.height.toFloat()
                    widthInput = it.width.toString()
                    heightInput = it.height.toString()
                    val pfd = context.contentResolver.openFileDescriptor(uri, "r")
                    originalFileSizeBytes = pfd?.statSize ?: (it.width * it.height * 3L / 4)
                    pfd?.close()
                }
            }
        }
    }

    // Batch image picker
    val batchPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        batchUris.clear()
        batchUris.addAll(uris)
        batchZipFile = null
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = Strings.get("cat_photo"),
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
            // Mode Tabs
            ScrollableTabRow(
                selectedTabIndex = selectedTab.ordinal,
                edgePadding = 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                PhotoResizeTab.values().forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = {
                            selectedTab = tab
                            processResult = null
                        },
                        text = { Text(tab.titleKey) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (selectedTab == PhotoResizeTab.BATCH_RESIZE) {
                // Batch Resize View
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { batchPicker.launch("image/*") },
                        modifier = Modifier.weight(1f).testTag("select_batch_images_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Collections, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (batchUris.isEmpty()) "Select Images" else "${batchUris.size} Images")
                    }
                    OutlinedButton(
                        onClick = {
                            batchUris.clear()
                            val f1 = File(context.cacheDir, "sample_batch_1.jpg")
                            val f2 = File(context.cacheDir, "sample_batch_2.jpg")
                            val bmp1 = com.example.util.SampleDataProvider.createSamplePhotoBitmap()
                            val bmp2 = com.example.util.SampleDataProvider.createSampleDocumentBitmap()
                            f1.outputStream().use { bmp1.compress(Bitmap.CompressFormat.JPEG, 90, it) }
                            f2.outputStream().use { bmp2.compress(Bitmap.CompressFormat.JPEG, 90, it) }
                            batchUris.add(Uri.fromFile(f1))
                            batchUris.add(Uri.fromFile(f2))
                        },
                        modifier = Modifier.weight(1f).testTag("load_sample_batch_btn"),
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

                if (batchUris.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Batch Settings",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Preset KB buttons for batch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(50L, 100L, 200L, 500L).forEach { kb ->
                            FilterChip(
                                selected = selectedPresetKb == kb,
                                onClick = { selectedPresetKb = kb },
                                label = { Text("${kb} KB") }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            isProcessing = true
                            coroutineScope.launch {
                                val outFiles = ImageProcessor.batchResize(
                                    context = context,
                                    sourceUris = batchUris,
                                    targetWidth = 1080,
                                    targetHeight = 1080,
                                    targetSizeBytes = selectedPresetKb * 1024L,
                                    format = selectedFormat,
                                    onProgress = { current, total ->
                                        batchProgress = Pair(current, total)
                                    }
                                )
                                val zip = ImageProcessor.createZipArchive(context, outFiles, "Batch_Resized_Photos")
                                batchZipFile = zip
                                RecentFileManager.addRecent(context, zip, "application/zip", "photo_resize")
                                isProcessing = false
                            }
                        },
                        enabled = !isProcessing,
                        modifier = Modifier.fillMaxWidth().testTag("process_batch_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isProcessing) {
                            Text("Processing ${batchProgress?.first ?: 0}/${batchProgress?.second ?: batchUris.size}...")
                        } else {
                            Text("Process All & Create ZIP")
                        }
                    }

                    batchZipFile?.let { zip ->
                        Spacer(modifier = Modifier.height(14.dp))
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Batch Complete! ${zip.name} (${ImageProcessor.formatBytes(zip.length())})",
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { shareFile(context, zip, "application/zip") },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(imageVector = Icons.Default.Share, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Share ZIP Archive")
                                }
                            }
                        }
                    }
                }
            } else {
                // Single Image Workflows
                if (loadedBitmap == null) {
                    Card(
                        onClick = { singlePicker.launch("image/*") },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp)
                            .testTag("pick_single_photo_card")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = Strings.get("select_images"),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "JPEG, PNG, WebP supported",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Sample Photo Button
                    OutlinedButton(
                        onClick = {
                            val sampleBmp = com.example.util.SampleDataProvider.createSamplePhotoBitmap()
                            loadedBitmap = sampleBmp
                            aspectRatio = sampleBmp.width.toFloat() / sampleBmp.height.toFloat()
                            widthInput = sampleBmp.width.toString()
                            heightInput = sampleBmp.height.toString()
                            originalFileSizeBytes = (sampleBmp.width * sampleBmp.height * 3L) / 4
                            processResult = null
                        },
                        modifier = Modifier.fillMaxWidth().testTag("load_sample_photo_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        )
                    ) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (com.example.util.LocaleManager.isHindi()) "⚡ नमूना फोटो लोड करें (तुरंत टेस्ट करें)" else "⚡ Load Sample Photo (Instant Test)",
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    // Image selected - Show preview & tools
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Original: ${loadedBitmap!!.width} x ${loadedBitmap!!.height} px (${ImageProcessor.formatBytes(originalFileSizeBytes)})",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                                )
                                TextButton(onClick = { singlePicker.launch("image/*") }) {
                                    Text("Change")
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    bitmap = loadedBitmap!!.asImageBitmap(),
                                    contentDescription = "Photo Preview",
                                    modifier = Modifier.fillMaxHeight()
                                )
                            }

                            // Quick Rotate / Flip controls
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                TextButton(onClick = {
                                    loadedBitmap = ImageProcessor.rotateBitmap(loadedBitmap!!, 90f)
                                }) {
                                    Icon(Icons.Default.RotateRight, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Rotate 90°")
                                }
                                TextButton(onClick = {
                                    loadedBitmap = ImageProcessor.flipBitmap(loadedBitmap!!, horizontal = true)
                                }) {
                                    Icon(Icons.Default.Flip, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Flip H")
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Mode Specific Options
                    when (selectedTab) {
                        PhotoResizeTab.BY_FILE_SIZE -> {
                            Text(
                                text = "Target File Size Presets",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(20L, 50L, 100L, 200L, 500L, 1024L).forEach { kb ->
                                    val isSelected = !isCustomKb && selectedPresetKb == kb
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            isCustomKb = false
                                            selectedPresetKb = kb
                                        },
                                        label = { Text(if (kb == 1024L) "1 MB" else "${kb} KB") }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = customKbInput,
                                onValueChange = {
                                    customKbInput = it
                                    isCustomKb = true
                                },
                                label = { Text("Or Custom Target (KB)") },
                                placeholder = { Text("e.g. 75") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        PhotoResizeTab.BY_DIMENSIONS -> {
                            Text(
                                text = "Resize by Dimensions (Pixels)",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            // Presets
                            Text(text = "Presets: ", style = MaterialTheme.typography.bodySmall)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(listOf(
                                    Pair("Passport 413x531", Pair(413, 531)),
                                    Pair("Instagram 1080x1080", Pair(1080, 1080)),
                                    Pair("HD 1280x720", Pair(1280, 720)),
                                    Pair("Full HD 1920x1080", Pair(1920, 1080)),
                                    Pair("Thumb 150x150", Pair(150, 150))
                                )) { (label, dims) ->
                                    ElevatedFilterChip(
                                        selected = widthInput == dims.first.toString() && heightInput == dims.second.toString(),
                                        onClick = {
                                            widthInput = dims.first.toString()
                                            heightInput = dims.second.toString()
                                        },
                                        label = { Text(label) }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = widthInput,
                                    onValueChange = {
                                        widthInput = it
                                        if (lockAspectRatio && it.toIntOrNull() != null && aspectRatio > 0) {
                                            heightInput = (it.toInt() / aspectRatio).toInt().toString()
                                        }
                                    },
                                    label = { Text("Width (px)") },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )

                                IconButton(onClick = { lockAspectRatio = !lockAspectRatio }) {
                                    Icon(
                                        imageVector = if (lockAspectRatio) Icons.Default.Lock else Icons.Default.LockOpen,
                                        contentDescription = "Lock Ratio",
                                        tint = if (lockAspectRatio) MaterialTheme.colorScheme.primary else Color.Gray
                                    )
                                }

                                OutlinedTextField(
                                    value = heightInput,
                                    onValueChange = {
                                        heightInput = it
                                        if (lockAspectRatio && it.toIntOrNull() != null && aspectRatio > 0) {
                                            widthInput = (it.toInt() * aspectRatio).toInt().toString()
                                        }
                                    },
                                    label = { Text("Height (px)") },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                            }
                        }

                        PhotoResizeTab.PASSPORT -> {
                            Text(
                                text = "Passport Photo Maker (Standard 35 x 45 mm)",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Automatically center-crops to passport specs (413 x 531 px) with white background.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        else -> {}
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Format selector
                    Text(text = "Output Format: ", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ImageOutputFormat.values().forEach { fmt ->
                            FilterChip(
                                selected = selectedFormat == fmt,
                                onClick = { selectedFormat = fmt },
                                label = { Text(fmt.name) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Execute Button
                    Button(
                        onClick = {
                            isProcessing = true
                            coroutineScope.launch {
                                val bmp = loadedBitmap ?: return@launch
                                val res = when (selectedTab) {
                                    PhotoResizeTab.BY_FILE_SIZE -> {
                                        val targetKb = if (isCustomKb && customKbInput.toLongOrNull() != null) {
                                            customKbInput.toLong()
                                        } else {
                                            selectedPresetKb
                                        }
                                        ImageProcessor.compressToTargetSize(
                                            context = context,
                                            sourceBitmap = bmp,
                                            targetSizeBytes = targetKb * 1024L,
                                            format = selectedFormat
                                        )
                                    }
                                    PhotoResizeTab.BY_DIMENSIONS -> {
                                        val w = widthInput.toIntOrNull() ?: bmp.width
                                        val h = heightInput.toIntOrNull() ?: bmp.height
                                        ImageProcessor.resizeByDimensions(
                                            context = context,
                                            sourceBitmap = bmp,
                                            targetWidth = w,
                                            targetHeight = h,
                                            format = selectedFormat,
                                            quality = qualitySlider.toInt()
                                        )
                                    }
                                    PhotoResizeTab.PASSPORT -> {
                                        val passBmp = ImageProcessor.createPassportPhoto(bmp)
                                        ImageProcessor.resizeByDimensions(
                                            context = context,
                                            sourceBitmap = passBmp,
                                            targetWidth = 413,
                                            targetHeight = 531,
                                            format = selectedFormat,
                                            quality = 95,
                                            outputPrefix = "passport_photo"
                                        )
                                    }
                                    else -> null
                                }

                                processResult = res
                                res?.let {
                                    RecentFileManager.addRecent(context, it.outputFile, selectedFormat.mimeType, "photo_resize")
                                }
                                isProcessing = false
                            }
                        },
                        enabled = !isProcessing,
                        modifier = Modifier.fillMaxWidth().testTag("process_photo_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(Strings.get("processing"))
                        } else {
                            Icon(imageVector = Icons.Default.Done, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Process & Save Image")
                        }
                    }

                    // Processed Result
                    processResult?.let { res ->
                        Spacer(modifier = Modifier.height(16.dp))
                        ResultSummaryCard(
                            originalSize = originalFileSizeBytes,
                            finalSize = res.finalSizeBytes,
                            reductionPercent = if (originalFileSizeBytes > 0) {
                                (((originalFileSizeBytes - res.finalSizeBytes).toDouble() / originalFileSizeBytes) * 100).toInt().coerceAtLeast(0)
                            } else null,
                            statusMessage = res.statusMessage
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { viewFile(context, res.outputFile, selectedFormat.mimeType) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(imageVector = Icons.Default.Visibility, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(Strings.get("preview"))
                            }

                            OutlinedButton(
                                onClick = { shareFile(context, res.outputFile, selectedFormat.mimeType) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(imageVector = Icons.Default.Share, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(Strings.get("share"))
                            }
                        }
                    }
                }
            }
        }
    }
}
