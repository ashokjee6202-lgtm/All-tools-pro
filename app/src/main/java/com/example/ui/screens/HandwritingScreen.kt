package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
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
import com.example.model.RecentFileManager
import com.example.ui.components.AppTopBar
import com.example.ui.components.shareFile
import com.example.ui.components.viewFile
import com.example.util.*
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HandwritingScreen(
    onNavigateBack: () -> Unit,
    onNavigateHome: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var inputText by remember {
        mutableStateOf(
            if (LocaleManager.isHindi()) {
                "आज का गृहकार्य (Assignment 1)\n\nहस्तलेखन असाइनमेंट तैयार करने के लिए ऑलटूल्स प्रो का उपयोग करें। यह टूल हिंदी और अंग्रेजी दोनों भाषाओं का समर्थन करता है और हस्तलिखित पृष्ठ तैयार करता है।"
            } else {
                "Assignment Chapter 4: Data Processing Systems\n\nAllToolsPro generates realistic handwritten notebook pages from plain typed text. It supports ruled paper, ink modulation, margins, and multi-page documents seamlessly."
            }
        )
    }

    var selectedPaper by remember { mutableStateOf(PaperType.RULED) }
    var selectedInk by remember { mutableStateOf(InkColor.BLUE) }
    var fontStyle by remember { mutableStateOf("Cursive / Casual") }
    var fontSizePt by remember { mutableFloatStateOf(20f) }
    var lineSpacingPt by remember { mutableFloatStateOf(34f) }
    var humanJitter by remember { mutableStateOf(true) }

    var generatedPages by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var previewPageIndex by remember { mutableIntStateOf(0) }
    var isGenerating by remember { mutableStateOf(false) }
    var outputPdfFile by remember { mutableStateOf<File?>(null) }
    var outputJpgFile by remember { mutableStateOf<File?>(null) }

    fun generatePages() {
        if (inputText.isBlank()) return
        isGenerating = true
        coroutineScope.launch {
            val options = HandwritingOptions(
                paperType = selectedPaper,
                inkColor = selectedInk,
                fontSizePt = fontSizePt,
                lineSpacingPt = lineSpacingPt,
                humanJitter = humanJitter,
                fontStyle = fontStyle
            )
            val pages = HandwritingGenerator.generateHandwritingPages(inputText, options)
            generatedPages = pages
            previewPageIndex = 0
            isGenerating = false
        }
    }

    LaunchedEffect(Unit) {
        generatePages()
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = Strings.get("cat_handwriting"),
                canNavigateBack = true,
                onNavigateBack = onNavigateBack,
                onNavigateHome = onNavigateHome
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(top = innerPadding.calculateTopPadding())
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            // Text Input Box
            Text(
                text = "Type or Paste Your Text (English / हिन्दी):",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .testTag("handwriting_text_input"),
                placeholder = { Text("Enter assignment, notes or essay text...") }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Options: Paper Type
            Text(text = "Paper Style: ", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PaperType.values().forEach { paper ->
                    FilterChip(
                        selected = selectedPaper == paper,
                        onClick = {
                            selectedPaper = paper
                            generatePages()
                        },
                        label = { Text(paper.titleKey) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Options: Ink Color
            Text(text = "Ink Color: ", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InkColor.values().forEach { ink ->
                    FilterChip(
                        selected = selectedInk == ink,
                        onClick = {
                            selectedInk = ink
                            generatePages()
                        },
                        label = { Text(ink.title) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Options: Font Style
            Text(text = "Handwriting Style: ", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Cursive / Casual", "Natural Serif", "Clean Print").forEach { style ->
                    FilterChip(
                        selected = fontStyle == style,
                        onClick = {
                            fontStyle = style
                            generatePages()
                        },
                        label = { Text(style) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Jitter & Human realism
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Organic Human Slant & Jitter",
                    style = MaterialTheme.typography.bodyMedium
                )
                Switch(
                    checked = humanJitter,
                    onCheckedChange = {
                        humanJitter = it
                        generatePages()
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Font size slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Font Size (${fontSizePt.toInt()}pt)", style = MaterialTheme.typography.bodySmall)
                Text(text = "Line Spacing (${lineSpacingPt.toInt()}pt)", style = MaterialTheme.typography.bodySmall)
            }
            Slider(
                value = fontSizePt,
                onValueChange = {
                    fontSizePt = it
                    lineSpacingPt = it * 1.7f
                    generatePages()
                },
                valueRange = 16f..28f
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { generatePages() },
                modifier = Modifier.fillMaxWidth().testTag("regenerate_handwriting_btn"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Refresh Handwriting")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Preview Section
            if (generatedPages.isNotEmpty()) {
                Text(
                    text = "Page Preview (${previewPageIndex + 1} of ${generatedPages.size} pages)",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (generatedPages.size > 1) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        itemsIndexed(generatedPages) { idx, page ->
                            Box(
                                modifier = Modifier
                                    .size(60.dp, 85.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .border(
                                        width = if (idx == previewPageIndex) 2.dp else 1.dp,
                                        color = if (idx == previewPageIndex) MaterialTheme.colorScheme.primary else Color.LightGray,
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .clickable { previewPageIndex = idx }
                            ) {
                                Image(
                                    bitmap = page.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Main Page Preview Card
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(340.dp)
                            .background(Color(0xFFE0E0E0)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (previewPageIndex in generatedPages.indices) {
                            Image(
                                bitmap = generatedPages[previewPageIndex].asImageBitmap(),
                                contentDescription = "Page Preview",
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .padding(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Export buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                val pdf = HandwritingGenerator.exportHandwritingAsPdf(context, generatedPages)
                                outputPdfFile = pdf
                                DownloadHelper.downloadFile(context, pdf, mimeType = "application/pdf", toolId = "handwriting")
                            }
                        },
                        modifier = Modifier.weight(1f).testTag("save_hw_pdf_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export as PDF")
                    }

                    OutlinedButton(
                        onClick = {
                            coroutineScope.launch {
                                val jpg = HandwritingGenerator.exportHandwritingAsJpg(
                                    context,
                                    generatedPages[previewPageIndex],
                                    "Handwritten_Page_${previewPageIndex + 1}"
                                )
                                outputJpgFile = jpg
                                DownloadHelper.downloadFile(context, jpg, mimeType = "image/jpeg", toolId = "handwriting")
                            }
                        },
                        modifier = Modifier.weight(1f).testTag("save_hw_jpg_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Page as JPG")
                    }
                }

                // Download / Share exported file card
                (outputPdfFile ?: outputJpgFile)?.let { file ->
                    Spacer(modifier = Modifier.height(14.dp))
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Exported: ${file.name} (${ImageProcessor.formatBytes(file.length())})",
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val mime = if (file.name.endsWith(".pdf")) "application/pdf" else "image/jpeg"
                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            DownloadHelper.downloadFile(context, file, mimeType = mime, toolId = "handwriting")
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(Strings.get("download"))
                                }
                                FilledTonalButton(
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
}
