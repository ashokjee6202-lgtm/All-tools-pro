package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.model.RecentFileManager
import com.example.model.ToolCategory
import com.example.model.ToolRegistry
import com.example.ui.components.AppTopBar
import com.example.ui.components.ToolCard
import com.example.util.Strings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsCatalogScreen(
    initialCategory: ToolCategory? = null,
    showFavoritesOnly: Boolean = false,
    onNavigateToTool: (String) -> Unit
) {
    var selectedCategory by remember { mutableStateOf<ToolCategory?>(initialCategory) }

    val filteredTools = remember(selectedCategory, showFavoritesOnly, RecentFileManager.favoriteToolIds.size) {
        val baseList = if (showFavoritesOnly) {
            ToolRegistry.allTools.filter { RecentFileManager.isFavorite(it.id) }
        } else {
            ToolRegistry.allTools
        }

        if (selectedCategory == null) baseList
        else baseList.filter { it.category == selectedCategory }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = Strings.get(if (showFavoritesOnly) "nav_favorites" else "nav_tools"),
                canNavigateBack = false
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            if (!showFavoritesOnly) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { selectedCategory = null },
                            label = { Text("All Tools") }
                        )
                    }
                    items(ToolCategory.values().filter { it != ToolCategory.POPULAR }) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = {
                                selectedCategory = if (selectedCategory == cat) null else cat
                            },
                            label = { Text(Strings.get(cat.titleKey)) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
            }

            if (filteredTools.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.StarBorder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (showFavoritesOnly) Strings.get("no_favorites") else "No tools found in this category",
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
                    val chunkedTools = filteredTools.chunked(2)
                    items(chunkedTools) { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                com.example.ui.components.ToolGridCard(
                                    tool = pair[0],
                                    onClick = { onNavigateToTool(pair[0].id) }
                                )
                            }
                            if (pair.size > 1) {
                                Box(modifier = Modifier.weight(1f)) {
                                    com.example.ui.components.ToolGridCard(
                                        tool = pair[1],
                                        onClick = { onNavigateToTool(pair[1].id) }
                                    )
                                }
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}
