package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.RecentFile
import com.example.model.RecentFileManager
import com.example.ui.components.AppTopBar
import com.example.ui.components.RecentFileItem
import com.example.ui.components.viewFile
import com.example.util.Strings
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentFilesScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var fileToDelete by remember { mutableStateOf<RecentFile?>(null) }
    var filterType by remember { mutableStateOf("all") } // all, pdf, img, zip

    val filteredList = remember(filterType, RecentFileManager.recentFiles.size) {
        when (filterType) {
            "pdf" -> RecentFileManager.recentFiles.filter { it.name.endsWith(".pdf", ignoreCase = true) }
            "img" -> RecentFileManager.recentFiles.filter { it.name.endsWith(".jpg", ignoreCase = true) || it.name.endsWith(".png", ignoreCase = true) }
            "zip" -> RecentFileManager.recentFiles.filter { it.name.endsWith(".zip", ignoreCase = true) }
            else -> RecentFileManager.recentFiles
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = Strings.get("recent_files"),
                canNavigateBack = true,
                onNavigateBack = onNavigateBack
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filterType == "all",
                    onClick = { filterType = "all" },
                    label = { Text("All (${RecentFileManager.recentFiles.size})") }
                )
                FilterChip(
                    selected = filterType == "pdf",
                    onClick = { filterType = "pdf" },
                    label = { Text("PDFs") }
                )
                FilterChip(
                    selected = filterType == "img",
                    onClick = { filterType = "img" },
                    label = { Text("Photos") }
                )
                FilterChip(
                    selected = filterType == "zip",
                    onClick = { filterType = "zip" },
                    label = { Text("ZIPs") }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = Strings.get("no_recent_files"),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredList, key = { it.id }) { item ->
                        RecentFileItem(
                            item = item,
                            onOpen = { viewFile(context, File(item.path), item.mimeType) },
                            onDelete = { fileToDelete = item }
                        )
                    }
                }
            }
        }

        fileToDelete?.let { item ->
            AlertDialog(
                onDismissRequest = { fileToDelete = null },
                title = { Text(Strings.get("delete")) },
                text = { Text("Delete ${item.name} from device?") },
                confirmButton = {
                    TextButton(onClick = {
                        RecentFileManager.deleteRecent(context, item)
                        fileToDelete = null
                    }) {
                        Text(Strings.get("delete"), color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { fileToDelete = null }) {
                        Text(Strings.get("cancel"))
                    }
                }
            )
        }
    }
}
