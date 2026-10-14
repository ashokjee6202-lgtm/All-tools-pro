package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DocumentManager
import com.example.ui.components.AppTopBar
import com.example.ui.components.shareFile
import com.example.ui.components.viewFile
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.BrandPurple
import com.example.util.LocaleManager
import com.example.util.PdfPagePreset
import com.example.util.Strings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

data class AnnotationText(
    var text: String,
    var normOffset: Offset,
    var color: Int = android.graphics.Color.BLACK,
    var textSizeSp: Float = 16f
)

data class SignatureOverlay(
    val bitmap: Bitmap,
    var normOffset: Offset,
    var scale: Float = 1f
)

data class DrawPoint(val offset: Offset, val color: Color, val strokeWidth: Float)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfEditorScreen(
    pageBitmaps: List<Bitmap>,
    initialDocTitle: String = "Scan_${System.currentTimeMillis() / 1000}",
    onNavigateBack: () -> Unit,
    onNavigateHome: () -> Unit = {},
    onDocumentSaved: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val editablePages = remember { mutableStateListOf<Bitmap>().apply { addAll(pageBitmaps) } }
    var selectedPageIndex by remember { mutableIntStateOf(0) }

    var docTitle by remember { mutableStateOf(initialDocTitle) }
    var selectedFolder by remember { mutableStateOf("All") }
    var selectedPreset by remember { mutableStateOf(PdfPagePreset.A4) }

    // Annotations & Markups on current page
    val textAnnotations = remember { mutableStateListOf<AnnotationText>() }
    val freehandDrawings = remember { mutableStateListOf<DrawPoint>() }
    var activeSignature by remember { mutableStateOf<SignatureOverlay?>(null) }

    // Tools state: none, text, pen, signature
    var activeEditorTool by remember { mutableStateOf("none") }
    var penColor by remember { mutableStateOf(Color.Red) }
    var penStrokeWidth by remember { mutableFloatStateOf(6f) }

    // Dialog states
    var showAddTextDialog by remember { mutableStateOf(false) }
    var tempTextInput by remember { mutableStateOf("") }
    var showSignatureDialog by remember { mutableStateOf(false) }
    var isSavingPdf by remember { mutableStateOf(false) }
    var savedPdfFile by remember { mutableStateOf<File?>(null) }

    fun addTextAnnotation(text: String) {
        if (text.isNotBlank()) {
            textAnnotations.add(
                AnnotationText(
                    text = text,
                    normOffset = Offset(0.2f, 0.4f),
                    color = android.graphics.Color.BLACK,
                    textSizeSp = 18f
                )
            )
        }
    }

    fun rotateCurrentPage() {
        if (editablePages.isNotEmpty() && selectedPageIndex in editablePages.indices) {
            val rotated = com.example.util.DocumentImageTransformer.rotateBitmap(editablePages[selectedPageIndex], 90f)
            editablePages[selectedPageIndex] = rotated
        }
    }

    fun deleteCurrentPage() {
        if (editablePages.size > 1) {
            editablePages.removeAt(selectedPageIndex)
            if (selectedPageIndex >= editablePages.size) {
                selectedPageIndex = editablePages.size - 1
            }
        }
    }

    fun exportAndSavePdf() {
        isSavingPdf = true
        coroutineScope.launch {
            try {
                val pdfDoc = PdfDocument()
                val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)

                editablePages.forEachIndexed { index, bmp ->
                    val pageInfo = PdfDocument.PageInfo.Builder(selectedPreset.widthPt, selectedPreset.heightPt, index + 1).create()
                    val page = pdfDoc.startPage(pageInfo)
                    val canvas = page.canvas

                    canvas.drawColor(android.graphics.Color.WHITE)

                    // Draw image fitted on page with 20pt margin
                    val margin = 20f
                    val availW = selectedPreset.widthPt - (margin * 2)
                    val availH = selectedPreset.heightPt - (margin * 2)

                    val bmpScale = minOf(availW / bmp.width, availH / bmp.height)
                    val drawW = bmp.width * bmpScale
                    val drawH = bmp.height * bmpScale
                    val left = margin + (availW - drawW) / 2f
                    val top = margin + (availH - drawH) / 2f

                    val dstRect = RectF(left, top, left + drawW, top + drawH)
                    canvas.drawBitmap(bmp, null, dstRect, paint)

                    // If it's the annotated page, burn text annotations & signatures onto the canvas
                    if (index == selectedPageIndex) {
                        // Text annotations
                        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = android.graphics.Color.BLACK
                            textSize = 18f
                            typeface = android.graphics.Typeface.DEFAULT_BOLD
                        }
                        textAnnotations.forEach { ann ->
                            val tx = left + (ann.normOffset.x * drawW)
                            val ty = top + (ann.normOffset.y * drawH)
                            canvas.drawText(ann.text, tx, ty, textPaint)
                        }

                        // Signature
                        activeSignature?.let { sig ->
                            val sx = left + (sig.normOffset.x * drawW)
                            val sy = top + (sig.normOffset.y * drawH)
                            val sW = (sig.bitmap.width * 0.35f * sig.scale).coerceAtLeast(80f)
                            val sH = (sig.bitmap.height * 0.35f * sig.scale).coerceAtLeast(40f)
                            canvas.drawBitmap(sig.bitmap, null, RectF(sx, sy, sx + sW, sy + sH), paint)
                        }
                    }

                    pdfDoc.finishPage(page)
                }

                // Write to temp file
                val tempFile = File(context.cacheDir, "${docTitle}.pdf")
                withContext(Dispatchers.IO) {
                    FileOutputStream(tempFile).use { fos ->
                        pdfDoc.writeTo(fos)
                    }
                    pdfDoc.close()
                }

                // Create thumbnail
                val thumbFile = File(context.cacheDir, "thumb_${System.currentTimeMillis()}.jpg")
                withContext(Dispatchers.IO) {
                    val firstBmp = editablePages.firstOrNull()
                    if (firstBmp != null) {
                        val thumb = Bitmap.createScaledBitmap(firstBmp, 200, 260, true)
                        FileOutputStream(thumbFile).use { fos ->
                            thumb.compress(Bitmap.CompressFormat.JPEG, 80, fos)
                        }
                    }
                }

                // Add to DocumentManager (Persistent storage!)
                DocumentManager.addDocument(
                    context = context,
                    title = docTitle,
                    folderName = selectedFolder,
                    pdfFile = tempFile,
                    thumbnailFile = thumbFile,
                    pageCount = editablePages.size
                )

                savedPdfFile = tempFile
                Toast.makeText(context, Strings.get("document_saved"), Toast.LENGTH_SHORT).show()
                onDocumentSaved()
            } catch (e: Exception) {
                Toast.makeText(context, "Error saving PDF: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            } finally {
                isSavingPdf = false
            }
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = Strings.get("pdf_editor_title"),
                canNavigateBack = true,
                onNavigateBack = onNavigateBack,
                onNavigateHome = onNavigateHome
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 6.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    // Document Name Field
                    OutlinedTextField(
                        value = docTitle,
                        onValueChange = { docTitle = it },
                        label = { Text(if (LocaleManager.isHindi()) "दस्तावेज़ का नाम" else "Document Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        savedPdfFile?.let { pdf ->
                            OutlinedButton(
                                onClick = { shareFile(context, pdf, "application/pdf") },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = Strings.get("share"))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(Strings.get("share"))
                            }
                        }

                        Button(
                            onClick = { exportAndSavePdf() },
                            enabled = !isSavingPdf && editablePages.isNotEmpty(),
                            modifier = Modifier
                                .weight(1.5f)
                                .height(50.dp)
                                .testTag("save_to_docs_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isSavingPdf) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(Strings.get("processing"))
                            } else {
                                Icon(Icons.Default.Save, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = Strings.get("save_to_docs"),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            val currentBmp = editablePages.getOrNull(selectedPageIndex)

            // Current Page Card with Canvas for annotations & freehand drawing
            currentBmp?.let { bmp ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(activeEditorTool) {
                                if (activeEditorTool == "pen") {
                                    detectDragGestures { change, _ ->
                                        change.consume()
                                        freehandDrawings.add(
                                            DrawPoint(change.position, penColor, penStrokeWidth)
                                        )
                                    }
                                }
                            }
                    ) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Page Preview",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize().padding(8.dp)
                        )

                        // Draw Freehand Lines Overlay
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            for (i in 0 until freehandDrawings.size - 1) {
                                val p1 = freehandDrawings[i]
                                val p2 = freehandDrawings[i + 1]
                                drawLine(
                                    color = p1.color,
                                    start = p1.offset,
                                    end = p2.offset,
                                    strokeWidth = p1.strokeWidth
                                )
                            }
                        }

                        // Text Annotations overlay (draggable)
                        textAnnotations.forEachIndexed { idx, ann ->
                            Box(
                                modifier = Modifier
                                    .offset {
                                        IntOffset(
                                            (ann.normOffset.x * 600f).roundToInt(),
                                            (ann.normOffset.y * 700f).roundToInt()
                                        )
                                    }
                                    .background(Color.White.copy(alpha = 0.85f), RoundedCornerShape(4.dp))
                                    .border(1.dp, BrandBlue, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                    .pointerInput(idx) {
                                        detectDragGestures { change, dragAmount ->
                                            change.consume()
                                            ann.normOffset = Offset(
                                                (ann.normOffset.x + dragAmount.x / 600f).coerceIn(0f, 0.8f),
                                                (ann.normOffset.y + dragAmount.y / 700f).coerceIn(0f, 0.8f)
                                            )
                                        }
                                    }
                            ) {
                                Text(
                                    text = ann.text,
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        // Signature overlay (draggable)
                        activeSignature?.let { sig ->
                            Box(
                                modifier = Modifier
                                    .offset {
                                        IntOffset(
                                            (sig.normOffset.x * 600f).roundToInt(),
                                            (sig.normOffset.y * 700f).roundToInt()
                                        )
                                    }
                                    .background(Color.White.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                    .border(1.dp, BrandPurple, RoundedCornerShape(4.dp))
                                    .padding(4.dp)
                                    .pointerInput(Unit) {
                                        detectDragGestures { change, dragAmount ->
                                            change.consume()
                                            sig.normOffset = Offset(
                                                (sig.normOffset.x + dragAmount.x / 600f).coerceIn(0f, 0.8f),
                                                (sig.normOffset.y + dragAmount.y / 700f).coerceIn(0f, 0.8f)
                                            )
                                        }
                                    }
                            ) {
                                Image(
                                    bitmap = sig.bitmap.asImageBitmap(),
                                    contentDescription = "Signature Overlay",
                                    modifier = Modifier.size(100.dp, 50.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Tool buttons: Text, Draw, Signature, Rotate, Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { showAddTextDialog = true }) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.TextFields, contentDescription = Strings.get("add_text"), tint = BrandBlue)
                        Text(Strings.get("add_text"), fontSize = 10.sp)
                    }
                }

                IconButton(
                    onClick = {
                        activeEditorTool = if (activeEditorTool == "pen") "none" else "pen"
                    }
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Draw,
                            contentDescription = Strings.get("markup_draw"),
                            tint = if (activeEditorTool == "pen") BrandPurple else MaterialTheme.colorScheme.onSurface
                        )
                        Text(Strings.get("markup_draw"), fontSize = 10.sp)
                    }
                }

                IconButton(onClick = { showSignatureDialog = true }) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Gesture, contentDescription = Strings.get("add_signature"), tint = BrandBlue)
                        Text(Strings.get("add_signature"), fontSize = 10.sp)
                    }
                }

                IconButton(onClick = { rotateCurrentPage() }) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.RotateRight, contentDescription = "Rotate")
                        Text("Rotate", fontSize = 10.sp)
                    }
                }

                if (editablePages.size > 1) {
                    IconButton(onClick = { deleteCurrentPage() }) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            Text("Delete", fontSize = 10.sp, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            // Pen Tool Palette (visible when pen tool is active)
            if (activeEditorTool == "pen") {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(Color.Red, Color.Blue, Color.Black, Color(0xFFF59E0B)).forEach { c ->
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(c)
                                        .border(
                                            width = if (penColor == c) 2.5.dp else 0.dp,
                                            color = Color.White,
                                            shape = CircleShape
                                        )
                                        .clickable { penColor = c }
                                )
                            }
                        }
                        TextButton(onClick = { freehandDrawings.clear() }) {
                            Text(Strings.get("clear_all"), fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Page Thumbnails row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                itemsIndexed(editablePages) { idx, p ->
                    val isSelected = idx == selectedPageIndex
                    Box(
                        modifier = Modifier
                            .size(55.dp, 75.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) BrandBlue else Color.LightGray,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { selectedPageIndex = idx }
                    ) {
                        Image(
                            bitmap = p.asImageBitmap(),
                            contentDescription = "Page ${idx + 1}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .background(Color.Black.copy(alpha = 0.65f))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "${idx + 1}",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Folder Assignment Chips
            Text(
                text = if (LocaleManager.isHindi()) "फ़ोल्डर चुनें" else "Assign to Folder",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(DocumentManager.getAllFolders().size) { idx ->
                    val folder = DocumentManager.getAllFolders()[idx]
                    val isSelected = selectedFolder == folder
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFolder = folder },
                        label = { Text(folder) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BrandPurple,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        // Add Text Dialog
        if (showAddTextDialog) {
            AlertDialog(
                onDismissRequest = { showAddTextDialog = false },
                title = { Text(Strings.get("add_text")) },
                text = {
                    OutlinedTextField(
                        value = tempTextInput,
                        onValueChange = { tempTextInput = it },
                        placeholder = { Text(if (LocaleManager.isHindi()) "यहाँ टेक्स्ट लिखें..." else "Enter annotation text...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            addTextAnnotation(tempTextInput)
                            tempTextInput = ""
                            showAddTextDialog = false
                        }
                    ) {
                        Text(Strings.get("confirm"))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddTextDialog = false }) {
                        Text(Strings.get("cancel"))
                    }
                }
            )
        }

        // Signature Dialog
        if (showSignatureDialog) {
            SignatureDrawingDialog(
                onDismiss = { showSignatureDialog = false },
                onSignatureCreated = { sigBitmap ->
                    activeSignature = SignatureOverlay(
                        bitmap = sigBitmap,
                        normOffset = Offset(0.3f, 0.6f)
                    )
                    showSignatureDialog = false
                }
            )
        }
    }
}

@Composable
fun SignatureDrawingDialog(
    onDismiss: () -> Unit,
    onSignatureCreated: (Bitmap) -> Unit
) {
    val points = remember { mutableStateListOf<Offset>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Strings.get("draw_signature")) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (LocaleManager.isHindi()) "सफ़ेद बॉक्स में उंगली से हस्ताक्षर करें" else "Draw your signature with finger below",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White)
                        .border(1.5.dp, Color.LightGray, RoundedCornerShape(10.dp))
                        .pointerInput(Unit) {
                            detectDragGestures { change, _ ->
                                change.consume()
                                points.add(change.position)
                            }
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        for (i in 0 until points.size - 1) {
                            drawLine(
                                color = Color.Blue,
                                start = points[i],
                                end = points[i + 1],
                                strokeWidth = 5f
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = { points.clear() }) {
                    Text(Strings.get("clear_all"), color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // Create bitmap from points
                    val bmp = Bitmap.createBitmap(400, 200, Bitmap.Config.ARGB_8888)
                    val canvas = android.graphics.Canvas(bmp)
                    canvas.drawColor(android.graphics.Color.TRANSPARENT)
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = android.graphics.Color.BLUE
                        strokeWidth = 6f
                        style = Paint.Style.STROKE
                        strokeCap = Paint.Cap.ROUND
                    }
                    for (i in 0 until points.size - 1) {
                        canvas.drawLine(points[i].x, points[i].y, points[i + 1].x, points[i + 1].y, paint)
                    }
                    onSignatureCreated(bmp)
                },
                colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
            ) {
                Text(Strings.get("insert_signature"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(Strings.get("cancel"))
            }
        }
    )
}
