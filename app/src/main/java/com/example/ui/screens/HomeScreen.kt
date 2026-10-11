package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.RecentFile
import com.example.model.RecentFileManager
import com.example.model.ToolCategory
import com.example.model.ToolItem
import com.example.model.ToolRegistry
import com.example.ui.components.AppTopBar
import com.example.ui.components.PopularToolChip
import com.example.ui.components.RecentFileItem
import com.example.ui.components.SectionHeader
import com.example.ui.components.ToolCard
import com.example.ui.components.viewFile
import com.example.ui.theme.BlueContainer
import com.example.ui.theme.OnBlueContainer
import com.example.util.Strings
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToTool: (String) -> Unit,
    onNavigateToCategory: (ToolCategory) -> Unit,
    onViewAllRecent: () -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf<ToolCategory?>(null) }
    var fileToDelete by remember { mutableStateOf<RecentFile?>(null) }

    val filteredTools = remember(searchQuery, selectedCategoryFilter) {
        ToolRegistry.allTools.filter { tool ->
            val matchesCat = selectedCategoryFilter == null || tool.category == selectedCategoryFilter
            val matchesQuery = searchQuery.isBlank() ||
                    Strings.get(tool.titleKey).contains(searchQuery, ignoreCase = true) ||
                    Strings.get(tool.descriptionKey).contains(searchQuery, ignoreCase = true)
            matchesCat && matchesQuery
        }
    }

    val popularTools = remember { ToolRegistry.allTools.filter { it.isPopular } }
    val favoriteTools = remember(RecentFileManager.favoriteToolIds.size) {
        ToolRegistry.allTools.filter { RecentFileManager.isFavorite(it.id) }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = Strings.get("app_name"),
                canNavigateBack = false
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Brand Logo Hero Header
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.alltoolspro_logo),
                        contentDescription = "AllToolsPro Logo",
                        modifier = Modifier
                            .size(68.dp)
                            .clip(RoundedCornerShape(16.dp))
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = Strings.get("app_name"),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 22.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFFF9100))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "PRO",
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 10.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = Strings.get("app_tagline"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(Strings.get("search_tools_placeholder"), fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("home_search_bar")
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Privacy Assurance Card
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = BlueContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = OnBlueContainer,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = Strings.get("privacy_notice"),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = OnBlueContainer,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Category Filter Chips
            Text(
                text = Strings.get("categories"),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = selectedCategoryFilter == null,
                        onClick = { selectedCategoryFilter = null },
                        label = { Text("All") }
                    )
                }
                items(ToolCategory.values().filter { it != ToolCategory.POPULAR }) { cat ->
                    FilterChip(
                        selected = selectedCategoryFilter == cat,
                        onClick = {
                            selectedCategoryFilter = if (selectedCategoryFilter == cat) null else cat
                        },
                        label = { Text(Strings.get(cat.titleKey)) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (searchQuery.isNotEmpty() || selectedCategoryFilter != null) {
                // Search or Filter results view
                SectionHeader(
                    title = "Tools (${filteredTools.size})"
                )
                ToolGridList(tools = filteredTools, onNavigateToTool = onNavigateToTool)
            } else {
                // Standard Home Layout

                // Popular Tools Horizontal Carousel
                SectionHeader(title = Strings.get("popular_tools"))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(popularTools) { tool ->
                        PopularToolChip(tool = tool, onClick = { onNavigateToTool(tool.id) })
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Favorites Section (if present)
                if (favoriteTools.isNotEmpty()) {
                    SectionHeader(title = Strings.get("favorites"))
                    ToolGridList(tools = favoriteTools.take(4), onNavigateToTool = onNavigateToTool)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Recent Files Section
                SectionHeader(
                    title = Strings.get("recent_files"),
                    actionText = if (RecentFileManager.recentFiles.isNotEmpty()) "View All" else null,
                    onActionClick = onViewAllRecent
                )

                if (RecentFileManager.recentFiles.isEmpty()) {
                    Text(
                        text = Strings.get("no_recent_files"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        RecentFileManager.recentFiles.take(3).forEach { fileItem ->
                            RecentFileItem(
                                item = fileItem,
                                onOpen = { viewFile(context, File(fileItem.path), fileItem.mimeType) },
                                onDelete = { fileToDelete = fileItem }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // CATEGORIZED GRID SECTIONS

                // 1. Document Scanner
                val scannerTools = remember { ToolRegistry.allTools.filter { it.category == ToolCategory.SCANNER } }
                SectionHeader(title = Strings.get("cat_scanner"))
                ToolGridList(tools = scannerTools, onNavigateToTool = onNavigateToTool)
                Spacer(modifier = Modifier.height(14.dp))

                // 2. PDF Tools
                val pdfTools = remember { ToolRegistry.allTools.filter { it.category == ToolCategory.PDF } }
                SectionHeader(title = Strings.get("cat_pdf"))
                ToolGridList(tools = pdfTools, onNavigateToTool = onNavigateToTool)
                Spacer(modifier = Modifier.height(14.dp))

                // 3. Photo Tools
                val photoTools = remember { ToolRegistry.allTools.filter { it.category == ToolCategory.PHOTO } }
                SectionHeader(title = Strings.get("cat_photo"))
                ToolGridList(tools = photoTools, onNavigateToTool = onNavigateToTool)
                Spacer(modifier = Modifier.height(14.dp))

                // 4. Text to Handwriting
                val hwTools = remember { ToolRegistry.allTools.filter { it.category == ToolCategory.HANDWRITING } }
                SectionHeader(title = Strings.get("cat_handwriting"))
                ToolGridList(tools = hwTools, onNavigateToTool = onNavigateToTool)
                Spacer(modifier = Modifier.height(14.dp))

                // 5. QR & Barcode Tools
                val qrTools = remember { ToolRegistry.allTools.filter { it.category == ToolCategory.QR } }
                SectionHeader(title = Strings.get("cat_qr"))
                ToolGridList(tools = qrTools, onNavigateToTool = onNavigateToTool)
                Spacer(modifier = Modifier.height(14.dp))

                // 6. Utility Tools
                val utilTools = remember { ToolRegistry.allTools.filter { it.category == ToolCategory.UTILITIES } }
                SectionHeader(title = Strings.get("cat_utilities"))
                ToolGridList(tools = utilTools, onNavigateToTool = onNavigateToTool)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Delete Confirmation Dialog
        fileToDelete?.let { item ->
            AlertDialog(
                onDismissRequest = { fileToDelete = null },
                title = { Text(Strings.get("delete")) },
                text = { Text("Are you sure you want to delete ${item.name}? This file will be removed from local storage.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            RecentFileManager.deleteRecent(context, item)
                            fileToDelete = null
                        }
                    ) {
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

@Composable
fun ToolGridList(
    tools: List<ToolItem>,
    onNavigateToTool: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        for (i in tools.indices step 2) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    com.example.ui.components.ToolGridCard(
                        tool = tools[i],
                        onClick = { onNavigateToTool(tools[i].id) }
                    )
                }
                if (i + 1 < tools.size) {
                    Box(modifier = Modifier.weight(1f)) {
                        com.example.ui.components.ToolGridCard(
                            tool = tools[i + 1],
                            onClick = { onNavigateToTool(tools[i + 1].id) }
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
