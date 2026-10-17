package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AppTopBar
import com.example.ui.components.shareFile
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.BrandPurple
import com.example.util.DownloadHelper
import com.example.util.LocaleManager
import com.example.util.Strings
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OcrResultScreen(
    initialText: String,
    onNavigateBack: () -> Unit,
    onNavigateHome: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var editableText by remember { mutableStateOf(initialText) }

    val wordCount = remember(editableText) {
        if (editableText.isBlank()) 0 else editableText.trim().split(Regex("\\s+")).count { it.isNotBlank() }
    }
    val lineCount = remember(editableText) {
        if (editableText.isBlank()) 0 else editableText.lines().size
    }

    fun copyToClipboard() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Scanned Document Text", editableText)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, Strings.get("text_copied"), Toast.LENGTH_SHORT).show()
    }

    fun shareText() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Extracted Document Text")
            putExtra(Intent.EXTRA_TEXT, editableText)
        }
        context.startActivity(Intent.createChooser(intent, Strings.get("share")))
    }

    fun exportAsTxt() {
        try {
            val file = File(context.cacheDir, "OCR_Text_${System.currentTimeMillis()}.txt")
            FileOutputStream(file).use { fos ->
                fos.write(editableText.toByteArray())
            }
            coroutineScope.launch {
                DownloadHelper.downloadFile(context, file, mimeType = "text/plain", toolId = "ocr_tool")
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Export error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = Strings.get("ocr_title"),
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
                        onClick = { copyToClipboard() },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("copy_ocr_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = Strings.get("copy_text"))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(Strings.get("copy_text"))
                    }

                    Button(
                        onClick = { shareText() },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("share_ocr_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = Strings.get("share"))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(Strings.get("share"))
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Stats banner (English & Hindi support badge, word count, line count)
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Translate,
                            contentDescription = null,
                            tint = BrandPurple,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (LocaleManager.isHindi()) "हिंदी व अंग्रेज़ी (ऑन-डिवाइस OCR)" else "English & Hindi (On-Device OCR)",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = BrandPurple
                        )
                    }

                    Text(
                        text = "${String.format(Strings.get("word_count"), wordCount)} | ${String.format(Strings.get("line_count"), lineCount)}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Editable Text Area
            OutlinedTextField(
                value = editableText,
                onValueChange = { editableText = it },
                label = { Text(Strings.get("recognized_text")) },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("ocr_text_field"),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Export TXT button
            TextButton(
                onClick = { exportAsTxt() },
                modifier = Modifier.align(Alignment.End)
            ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(Strings.get("export_txt"))
            }
        }
    }
}
