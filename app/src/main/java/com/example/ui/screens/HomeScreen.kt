package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import com.example.ui.components.ToolGridCard
import com.example.ui.components.viewFile
import com.example.ui.theme.BlueContainer
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.BrandPurple
import com.example.ui.theme.OnBlueContainer
import com.example.util.Strings
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToTool: (String) -> Unit,
    onNavigateToCategory: (ToolCategory) -> Unit,
    onViewAllRecent: () -> Unit,
    onFavoriteToggled: ((Boolean) -> Unit)? = null
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf<ToolCategory?>(null) }
    var fileToDelete by remember { mutableStateOf<RecentFile?>(null) }

    val popularTools: List<ToolItem> = remember { ToolRegistry.getPopularTools() }
    val favoriteTools: List<ToolItem> = remember(RecentFileManager.favoriteIds.size) {
        ToolRegistry.allTools.filter { RecentFileManager.isFavorite(it.id) }
    }

    val filteredTools = remember(searchQuery, selectedCategoryFilter) {
        ToolRegistry.allTools.filter { tool ->
            val matchesCategory = selectedCategoryFilter == null || tool.category == selectedCategoryFilter
            val title = Strings.get(tool.titleKey)
            val desc = Strings.get(tool.descriptionKey)
            val matchesSearch = searchQuery.isBlank() ||
                    title.contains(searchQuery, ignoreCase = true) ||
                    desc.contains(searchQuery, ignoreCase = true) ||
                    tool.id.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }
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
            // Brand Logo Hero Header (No "PRO" badge)
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, BrandBlue.copy(alpha = 0.15f)),
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
                            .size(64.dp)
                            .clip(RoundedCornerShape(16.dp))
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = Strings.get("app_name"),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp,
                                color = BrandBlue
                            )
                        )
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
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = BrandBlue) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandBlue,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                ),
                singleLine = true,
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
                        tint = BrandBlue,
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

            // Category Filter Chips (with generous contentPadding so last item is not clipped)
            Text(
                text = Strings.get("categories"),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(end = 24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = selectedCategoryFilter == null,
                        onClick = { selectedCategoryFilter = null },
                        label = { Text(if (com.example.util.LocaleManager.isHindi()) "सभी" else "All") }
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
                ToolGridList(tools = filteredTools, onNavigateToTool = onNavigateToTool, onFavoriteToggled = onFavoriteToggled)
            } else {
                // Standard Home Layout

                // Popular Tools Horizontal Carousel (with generous contentPadding)
                SectionHeader(title = Strings.get("popular_tools"))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(end = 24.dp),
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
                    ToolGridList(tools = favoriteTools.take(4), onNavigateToTool = onNavigateToTool, onFavoriteToggled = onFavoriteToggled)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Recent Files Section
                SectionHeader(
                    title = Strings.get("recent_files"),
                    actionText = if (RecentFileManager.recentFiles.isNotEmpty()) (if (com.example.util.LocaleManager.isHindi()) "सभी देखें" else "View All") else null,
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
                val scannerTools = remember { ToolRegistry.allTools.filter { it.category == ToolCategory.SCANNER } }
                SectionHeader(title = Strings.get("cat_scanner"))
                ToolGridList(tools = scannerTools, onNavigateToTool = onNavigateToTool, onFavoriteToggled = onFavoriteToggled)
                Spacer(modifier = Modifier.height(14.dp))

                val pdfTools = remember { ToolRegistry.allTools.filter { it.category == ToolCategory.PDF } }
                SectionHeader(title = Strings.get("cat_pdf"))
                ToolGridList(tools = pdfTools, onNavigateToTool = onNavigateToTool, onFavoriteToggled = onFavoriteToggled)
                Spacer(modifier = Modifier.height(14.dp))

                val photoTools = remember { ToolRegistry.allTools.filter { it.category == ToolCategory.PHOTO } }
                SectionHeader(title = Strings.get("cat_photo"))
                ToolGridList(tools = photoTools, onNavigateToTool = onNavigateToTool, onFavoriteToggled = onFavoriteToggled)
                Spacer(modifier = Modifier.height(14.dp))

                val hwTools = remember { ToolRegistry.allTools.filter { it.category == ToolCategory.HANDWRITING } }
                SectionHeader(title = Strings.get("cat_handwriting"))
                ToolGridList(tools = hwTools, onNavigateToTool = onNavigateToTool, onFavoriteToggled = onFavoriteToggled)
                Spacer(modifier = Modifier.height(14.dp))

                val qrTools = remember { ToolRegistry.allTools.filter { it.category == ToolCategory.QR } }
                SectionHeader(title = Strings.get("cat_qr"))
                ToolGridList(tools = qrTools, onNavigateToTool = onNavigateToTool, onFavoriteToggled = onFavoriteToggled)
                Spacer(modifier = Modifier.height(14.dp))

                val utilTools = remember { ToolRegistry.allTools.filter { it.category == ToolCategory.UTILITIES } }
                SectionHeader(title = Strings.get("cat_utilities"))
                ToolGridList(tools = utilTools, onNavigateToTool = onNavigateToTool, onFavoriteToggled = onFavoriteToggled)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Website & WhatsApp Help Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = if (com.example.util.LocaleManager.isHindi()) "जुड़े रहें और सहायता प्राप्त करें" else "Official Website & Support",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.alltoolspro.in/")).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                try {
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(16.dp), tint = BrandBlue)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Website", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    data = Uri.parse("https://api.whatsapp.com/send?phone=916202292319&text=Hello%20AllToolsMaster%20Support")
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                try {
                                    context.startActivity(intent)
                                } catch (_: Exception) {
                                    val dial = Intent(Intent.ACTION_DIAL, Uri.parse("tel:6202292319")).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(dial)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("WhatsApp", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // Generous bottom spacer so content scrolls completely above bottom nav bar
            Spacer(modifier = Modifier.height(48.dp))
        }

        // Delete Confirmation Dialog
        fileToDelete?.let { item ->
            AlertDialog(
                onDismissRequest = { fileToDelete = null },
                title = { Text(Strings.get("delete")) },
                text = { Text(if (com.example.util.LocaleManager.isHindi()) "क्या आप वाकई ${item.name} हटाना चाहते हैं?" else "Are you sure you want to delete ${item.name}? This file will be removed from local storage.") },
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
fun SectionHeader(
    title: String,
    actionText: String? = null,
    onActionClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
        if (actionText != null) {
            TextButton(onClick = onActionClick) {
                Text(
                    text = actionText,
                    style = MaterialTheme.typography.labelLarge.copy(color = BrandBlue, fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

@Composable
fun ToolGridList(
    tools: List<ToolItem>,
    onNavigateToTool: (String) -> Unit,
    onFavoriteToggled: ((Boolean) -> Unit)? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        for (i in tools.indices step 2) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    ToolGridCard(
                        tool = tools[i],
                        onClick = { onNavigateToTool(tools[i].id) },
                        onFavoriteToggled = onFavoriteToggled
                    )
                }
                if (i + 1 < tools.size) {
                    Box(modifier = Modifier.weight(1f)) {
                        ToolGridCard(
                            tool = tools[i + 1],
                            onClick = { onNavigateToTool(tools[i + 1].id) },
                            onFavoriteToggled = onFavoriteToggled
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
