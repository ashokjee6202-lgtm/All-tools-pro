package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.RecentFileManager
import com.example.ui.components.AppTopBar
import com.example.ui.components.shareFile
import com.example.ui.components.viewFile
import com.example.util.Strings
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

data class PathPoint(val offset: Offset, val color: Color, val strokeWidth: Float)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignatureMakerScreen(
    onNavigateBack: () -> Unit,
    onNavigateHome: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val paths = remember { mutableStateListOf<List<PathPoint>>() }
    var currentPath = remember { mutableStateListOf<PathPoint>() }

    var selectedColor by remember { mutableStateOf(Color(0xFF0D47A1)) } // Default Pro Navy Blue
    var strokeWidth by remember { mutableFloatStateOf(6f) }
    var exportedFile by remember { mutableStateOf<File?>(null) }

    fun exportSignature(isPdf: Boolean) {
        if (paths.isEmpty()) return
        coroutineScope.launch {
            val width = 800
            val height = 400
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                style = Paint.Style.STROKE
            }

            for (path in paths) {
                if (path.size > 1) {
                    for (i in 0 until path.size - 1) {
                        val p1 = path[i]
                        val p2 = path[i + 1]
                        val r = (p1.color.red * 255).toInt()
                        val g = (p1.color.green * 255).toInt()
                        val b = (p1.color.blue * 255).toInt()
                        paint.color = android.graphics.Color.rgb(r, g, b)
                        paint.strokeWidth = p1.strokeWidth
                        canvas.drawLine(p1.offset.x, p1.offset.y, p2.offset.x, p2.offset.y, paint)
                    }
                }
            }

            if (isPdf) {
                val pdfDoc = PdfDocument()
                val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
                val page = pdfDoc.startPage(pageInfo)
                page.canvas.drawColor(android.graphics.Color.WHITE)
                page.canvas.drawBitmap(bitmap, null, android.graphics.Rect(100, 200, 500, 400), paint)
                pdfDoc.finishPage(page)

                val file = File(context.cacheDir, "Signature_${System.currentTimeMillis()}.pdf")
                FileOutputStream(file).use { pdfDoc.writeTo(it) }
                pdfDoc.close()
                exportedFile = file
                RecentFileManager.addRecent(context, file, "application/pdf", "sign_pdf")
            } else {
                val file = File(context.cacheDir, "Signature_${System.currentTimeMillis()}.png")
                FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                exportedFile = file
                RecentFileManager.addRecent(context, file, "image/png", "sign_pdf")
            }
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = Strings.get("tool_sign_pdf"),
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
                .padding(16.dp)
        ) {
            Text(
                text = "Draw your digital signature below:",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Signature Drawing Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White)
                    .border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(14.dp))
                    .testTag("signature_canvas")
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                currentPath = mutableStateListOf(PathPoint(offset, selectedColor, strokeWidth))
                                paths.add(currentPath)
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                currentPath.add(PathPoint(change.position, selectedColor, strokeWidth))
                            }
                        )
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    for (path in paths) {
                        if (path.size > 1) {
                            for (i in 0 until path.size - 1) {
                                drawLine(
                                    color = path[i].color,
                                    start = path[i].offset,
                                    end = path[i + 1].offset,
                                    strokeWidth = path[i].strokeWidth,
                                    cap = StrokeCap.Round
                                )
                            }
                        }
                    }
                }

                if (paths.isEmpty()) {
                    Text(
                        text = "Sign here with your finger or stylus",
                        color = Color.LightGray,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Color & Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        Color(0xFF0D47A1) to "Blue",
                        Color(0xFF212121) to "Black",
                        Color(0xFFC62828) to "Red"
                    ).forEach { (color, name) ->
                        FilterChip(
                            selected = selectedColor == color,
                            onClick = { selectedColor = color },
                            label = { Text(name) }
                        )
                    }
                }

                TextButton(
                    onClick = {
                        paths.clear()
                        exportedFile = null
                    }
                ) {
                    Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(Strings.get("reset"))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Export Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { exportSignature(isPdf = false) },
                    modifier = Modifier.weight(1f).testTag("save_sig_png_btn"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Transparent PNG")
                }

                OutlinedButton(
                    onClick = { exportSignature(isPdf = true) },
                    modifier = Modifier.weight(1f).testTag("save_sig_pdf_btn"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save as PDF")
                }
            }

            exportedFile?.let { file ->
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Saved Signature: ${file.name}",
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))
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
