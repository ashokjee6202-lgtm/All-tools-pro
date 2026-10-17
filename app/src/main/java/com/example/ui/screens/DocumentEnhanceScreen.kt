package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AppTopBar
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.BrandPurple
import com.example.util.DocumentImageTransformer
import com.example.util.LocaleManager
import com.example.util.ScannerFilterType
import com.example.util.Strings

data class EnhancedPage(
    val originalCroppedBitmap: Bitmap,
    var filteredBitmap: Bitmap,
    var activeFilter: ScannerFilterType = ScannerFilterType.MAGIC_COLOR,
    var filterIntensity: Float = 0.9f
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentEnhanceScreen(
    pages: MutableList<EnhancedPage>,
    onAddMorePages: () -> Unit,
    onProceedToPdfEditor: (List<Bitmap>) -> Unit,
    onExtractOcr: (Bitmap) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateHome: () -> Unit = {}
) {
    var selectedPageIndex by remember { mutableIntStateOf(0) }
    var isHoldingOriginal by remember { mutableStateOf(false) }

    val currentPage = pages.getOrNull(selectedPageIndex)

    // Current page filter and intensity
    var currentFilter by remember(selectedPageIndex) {
        mutableStateOf(currentPage?.activeFilter ?: ScannerFilterType.MAGIC_COLOR)
    }
    var currentIntensity by remember(selectedPageIndex) {
        mutableFloatStateOf(currentPage?.filterIntensity ?: 0.9f)
    }

    fun updateFilter(filter: ScannerFilterType, intensity: Float = currentIntensity) {
        currentFilter = filter
        currentIntensity = intensity
        currentPage?.let { page ->
            page.activeFilter = filter
            page.filterIntensity = intensity
            page.filteredBitmap = DocumentImageTransformer.applyEnhancementFilter(
                source = page.originalCroppedBitmap,
                filterType = filter,
                intensity = intensity
            )
        }
    }

    fun deleteCurrentPage() {
        if (pages.size > 1) {
            pages.removeAt(selectedPageIndex)
            if (selectedPageIndex >= pages.size) {
                selectedPageIndex = pages.size - 1
            }
        }
    }

    fun movePageLeft() {
        if (selectedPageIndex > 0) {
            val item = pages.removeAt(selectedPageIndex)
            pages.add(selectedPageIndex - 1, item)
            selectedPageIndex--
        }
    }

    fun movePageRight() {
        if (selectedPageIndex < pages.size - 1) {
            val item = pages.removeAt(selectedPageIndex)
            pages.add(selectedPageIndex + 1, item)
            selectedPageIndex++
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = Strings.get("enhance_title"),
                canNavigateBack = true,
                onNavigateBack = onNavigateBack,
                onNavigateHome = onNavigateHome
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            Surface(
                tonalElevation = 6.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            currentPage?.filteredBitmap?.let { onExtractOcr(it) }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("ocr_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.TextFields, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (LocaleManager.isHindi()) "OCR टेक्स्ट" else "Extract Text", fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            val finalBitmaps = pages.map { it.filteredBitmap }
                            onProceedToPdfEditor(finalBitmaps)
                        },
                        modifier = Modifier
                            .weight(1.3f)
                            .height(50.dp)
                            .testTag("save_pdf_editor_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (LocaleManager.isHindi()) "पीडीएफ बनाएं (${pages.size})" else "Create PDF (${pages.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Main Document Preview Box with "Hold for Original"
            currentPage?.let { page ->
                val displayBitmap = if (isHoldingOriginal) page.originalCroppedBitmap else page.filteredBitmap

                Card(
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Image(
                            bitmap = displayBitmap.asImageBitmap(),
                            contentDescription = "Document Page",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp)
                        )

                        // "Hold for Original" pill button overlay
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isHoldingOriginal) BrandPurple else Color.Black.copy(alpha = 0.65f),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 12.dp)
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onPress = {
                                            isHoldingOriginal = true
                                            tryAwaitRelease()
                                            isHoldingOriginal = false
                                        }
                                    )
                                }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = Strings.get("hold_for_original"),
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Page badge
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(10.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black.copy(alpha = 0.6f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${selectedPageIndex + 1} / ${pages.size}",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Page Carousel & Page Management Row
                Text(
                    text = "${if (LocaleManager.isHindi()) "पेज प्रबंधित करें" else "Pages"} (${pages.size})",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        itemsIndexed(pages) { idx, p ->
                            val isSelected = idx == selectedPageIndex
                            Box(
                                modifier = Modifier
                                    .size(60.dp, 80.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(
                                        width = if (isSelected) 2.5.dp else 1.dp,
                                        color = if (isSelected) BrandBlue else Color.LightGray,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { selectedPageIndex = idx }
                            ) {
                                Image(
                                    bitmap = p.filteredBitmap.asImageBitmap(),
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
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        item {
                            // Add Page button
                            OutlinedButton(
                                onClick = onAddMorePages,
                                modifier = Modifier.size(60.dp, 80.dp),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.AddAPhoto, contentDescription = Strings.get("add_page"), tint = BrandBlue)
                                    Text(Strings.get("add_page"), fontSize = 9.sp, color = BrandBlue)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Page reorder / delete toolbar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { movePageLeft() },
                        enabled = selectedPageIndex > 0
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Move Left")
                    }
                    IconButton(
                        onClick = { movePageRight() },
                        enabled = selectedPageIndex < pages.size - 1
                    ) {
                        Icon(Icons.Default.ArrowForward, contentDescription = "Move Right")
                    }
                    if (pages.size > 1) {
                        IconButton(onClick = { deleteCurrentPage() }) {
                            Icon(Icons.Default.Delete, contentDescription = Strings.get("delete"), tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 6 Enhancement Filters Chips
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = if (LocaleManager.isHindi()) "फ़िल्टर चुनें" else "Enhancement Filters",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        val allFilters = listOf(
                            ScannerFilterType.ORIGINAL,
                            ScannerFilterType.LIGHTEN,
                            ScannerFilterType.MAGIC_COLOR,
                            ScannerFilterType.GRAYSCALE,
                            ScannerFilterType.BLACK_AND_WHITE,
                            ScannerFilterType.SHADOW_REDUCTION
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            itemsIndexed(allFilters) { _, filter ->
                                val isSelected = currentFilter == filter
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { updateFilter(filter) },
                                    label = {
                                        Text(
                                            text = if (LocaleManager.isHindi()) filter.displayNameHi else filter.displayNameEn,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = BrandBlue,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }

                        // Filter Intensity Slider (if not original)
                        if (currentFilter != ScannerFilterType.ORIGINAL) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = Strings.get("filter_intensity"),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${(currentIntensity * 100).toInt()}%",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandBlue
                                )
                            }
                            Slider(
                                value = currentIntensity,
                                onValueChange = { updateFilter(currentFilter, it) },
                                valueRange = 0.1f..1.0f,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}
