package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.PointF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AppTopBar
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.BrandPurple
import com.example.util.DocumentCorners
import com.example.util.DocumentImageTransformer
import com.example.util.LocaleManager
import com.example.util.Strings
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CropPerspectiveScreen(
    sourceBitmap: Bitmap,
    pageIndex: Int = 0,
    totalPages: Int = 1,
    onCropConfirmed: (Bitmap) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateHome: () -> Unit = {}
) {
    var currentBitmap by remember(sourceBitmap) { mutableStateOf(sourceBitmap) }

    // Display coordinates state for the 4 corners
    var containerWidthPx by remember { mutableFloatStateOf(0f) }
    var containerHeightPx by remember { mutableFloatStateOf(0f) }

    // Normalized coordinates (0.0 to 1.0) for resolution-independent rotation and scaling
    var normTopLeft by remember { mutableStateOf(Offset(0.06f, 0.06f)) }
    var normTopRight by remember { mutableStateOf(Offset(0.94f, 0.06f)) }
    var normBottomRight by remember { mutableStateOf(Offset(0.94f, 0.94f)) }
    var normBottomLeft by remember { mutableStateOf(Offset(0.06f, 0.94f)) }

    var activeHandleIndex by remember { mutableIntStateOf(-1) } // 0: TL, 1: TR, 2: BR, 3: BL

    fun resetToAuto() {
        normTopLeft = Offset(0.06f, 0.06f)
        normTopRight = Offset(0.94f, 0.06f)
        normBottomRight = Offset(0.94f, 0.94f)
        normBottomLeft = Offset(0.06f, 0.94f)
    }

    fun setFullPage() {
        normTopLeft = Offset(0f, 0f)
        normTopRight = Offset(1f, 0f)
        normBottomRight = Offset(1f, 1f)
        normBottomLeft = Offset(0f, 1f)
    }

    fun rotate(degrees: Float) {
        currentBitmap = DocumentImageTransformer.rotateBitmap(currentBitmap, degrees)
        resetToAuto()
    }

    fun confirmCrop() {
        val bmpW = currentBitmap.width.toFloat()
        val bmpH = currentBitmap.height.toFloat()

        val corners = DocumentCorners(
            topLeft = PointF(normTopLeft.x * bmpW, normTopLeft.y * bmpH),
            topRight = PointF(normTopRight.x * bmpW, normTopRight.y * bmpH),
            bottomRight = PointF(normBottomRight.x * bmpW, normBottomRight.y * bmpH),
            bottomLeft = PointF(normBottomLeft.x * bmpW, normBottomLeft.y * bmpH)
        )

        val cropped = DocumentImageTransformer.applyPerspectiveCorrection(currentBitmap, corners)
        onCropConfirmed(cropped)
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "${Strings.get("crop_title")} (${pageIndex + 1}/$totalPages)",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack,
                onNavigateHome = onNavigateHome
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    // Quick Transform Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { rotate(-90f) }) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.RotateLeft, contentDescription = Strings.get("rotate_left"), tint = MaterialTheme.colorScheme.onSurface)
                                Text(Strings.get("rotate_left"), fontSize = 10.sp)
                            }
                        }

                        IconButton(onClick = { resetToAuto() }) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Crop, contentDescription = Strings.get("auto_detect"), tint = BrandBlue)
                                Text(Strings.get("auto_detect"), fontSize = 10.sp, color = BrandBlue)
                            }
                        }

                        IconButton(onClick = { setFullPage() }) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Fullscreen, contentDescription = Strings.get("full_page"), tint = MaterialTheme.colorScheme.onSurface)
                                Text(Strings.get("full_page"), fontSize = 10.sp)
                            }
                        }

                        IconButton(onClick = { rotate(90f) }) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.RotateRight, contentDescription = Strings.get("rotate_right"), tint = MaterialTheme.colorScheme.onSurface)
                                Text(Strings.get("rotate_right"), fontSize = 10.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Next / Confirm Button
                    Button(
                        onClick = { confirmCrop() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("confirm_crop_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            Strings.get("next_step"),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.92f))
        ) {
            // Hint banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E293B))
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.TouchApp,
                        contentDescription = null,
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = Strings.get("drag_corners_hint"),
                        color = Color(0xFFE2E8F0),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Interactive Cropping Canvas
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                val boxWidthPx = constraints.maxWidth.toFloat()
                val boxHeightPx = constraints.maxHeight.toFloat()

                // Calculate image fitted rect
                val bmpW = currentBitmap.width.toFloat()
                val bmpH = currentBitmap.height.toFloat()
                val scale = minOf(boxWidthPx / bmpW, boxHeightPx / bmpH)
                val fitW = bmpW * scale
                val fitH = bmpH * scale
                val offsetX = (boxWidthPx - fitW) / 2f
                val offsetY = (boxHeightPx - fitH) / 2f

                containerWidthPx = fitW
                containerHeightPx = fitH

                val imageBitmap = remember(currentBitmap) { currentBitmap.asImageBitmap() }

                // Pointer Drag Gesture Detector
                Box(
                    modifier = Modifier
                        .size(with(LocalDensity.current) { fitW.toDp() }, with(LocalDensity.current) { fitH.toDp() })
                        .pointerInput(fitW, fitH) {
                            detectDragGestures(
                                onDragStart = { startOffset ->
                                    val touchNorm = Offset(
                                        (startOffset.x / fitW).coerceIn(0f, 1f),
                                        (startOffset.y / fitH).coerceIn(0f, 1f)
                                    )
                                    val dTL = (touchNorm - normTopLeft).getDistance()
                                    val dTR = (touchNorm - normTopRight).getDistance()
                                    val dBR = (touchNorm - normBottomRight).getDistance()
                                    val dBL = (touchNorm - normBottomLeft).getDistance()

                                    val minDist = minOf(dTL, dTR, dBR, dBL)
                                    activeHandleIndex = when (minDist) {
                                        dTL -> 0
                                        dTR -> 1
                                        dBR -> 2
                                        else -> 3
                                    }
                                },
                                onDragEnd = { activeHandleIndex = -1 },
                                onDragCancel = { activeHandleIndex = -1 },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val deltaNorm = Offset(dragAmount.x / fitW, dragAmount.y / fitH)
                                    when (activeHandleIndex) {
                                        0 -> normTopLeft = Offset((normTopLeft.x + deltaNorm.x).coerceIn(0f, normTopRight.x - 0.05f), (normTopLeft.y + deltaNorm.y).coerceIn(0f, normBottomLeft.y - 0.05f))
                                        1 -> normTopRight = Offset((normTopRight.x + deltaNorm.x).coerceIn(normTopLeft.x + 0.05f, 1f), (normTopRight.y + deltaNorm.y).coerceIn(0f, normBottomRight.y - 0.05f))
                                        2 -> normBottomRight = Offset((normBottomRight.x + deltaNorm.x).coerceIn(normBottomLeft.x + 0.05f, 1f), (normBottomRight.y + deltaNorm.y).coerceIn(normTopRight.y + 0.05f, 1f))
                                        3 -> normBottomLeft = Offset((normBottomLeft.x + deltaNorm.x).coerceIn(0f, normBottomRight.x - 0.05f), (normBottomLeft.y + deltaNorm.y).coerceIn(normTopLeft.y + 0.05f, 1f))
                                    }
                                }
                            )
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        // 1. Draw image fitted
                        drawImage(
                            image = imageBitmap,
                            dstSize = IntSize(fitW.roundToInt(), fitH.roundToInt())
                        )

                        // 2. Draw dim overlay outside quad
                        val ptTL = Offset(normTopLeft.x * fitW, normTopLeft.y * fitH)
                        val ptTR = Offset(normTopRight.x * fitW, normTopRight.y * fitH)
                        val ptBR = Offset(normBottomRight.x * fitW, normBottomRight.y * fitH)
                        val ptBL = Offset(normBottomLeft.x * fitW, normBottomLeft.y * fitH)

                        val quadPath = Path().apply {
                            moveTo(ptTL.x, ptTL.y)
                            lineTo(ptTR.x, ptTR.y)
                            lineTo(ptBR.x, ptBR.y)
                            lineTo(ptBL.x, ptBL.y)
                            close()
                        }

                        // Outline quad
                        drawPath(
                            path = quadPath,
                            color = Color(0xFF38BDF8),
                            style = Stroke(width = 3.5.dp.toPx())
                        )

                        // Draw corner handles
                        val handleRadius = 14.dp.toPx()
                        val points = listOf(ptTL, ptTR, ptBR, ptBL)
                        points.forEachIndexed { idx, pt ->
                            val isActive = idx == activeHandleIndex
                            // Outer circle
                            drawCircle(
                                color = if (isActive) Color(0xFF7C3AED) else Color(0xFF2563EB),
                                radius = if (isActive) handleRadius * 1.3f else handleRadius,
                                center = pt
                            )
                            // Inner white dot
                            drawCircle(
                                color = Color.White,
                                radius = handleRadius * 0.45f,
                                center = pt
                            )
                        }
                    }
                }
            }
        }
    }
}
