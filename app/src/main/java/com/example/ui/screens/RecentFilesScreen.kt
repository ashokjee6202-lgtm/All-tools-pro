package com.example.ui.screens

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DocumentManager
import com.example.model.ScannedDocItem
import com.example.ui.components.AppTopBar
import com.example.ui.components.shareFile
import com.example.ui.components.viewFile
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.BrandPurple
import com.example.util.ImageProcessor
import com.example.util.LocaleManager
import com.example.util.Strings
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentFilesScreen(
    onNavigateBack: () -> Unit,
    onOpenTool: () -> Unit = {},
    onScanDocument: () -> Unit = {}
) {
    val context = LocalContext.current

    var searchQuery by remember { mutableStateOf("") }
    var selectedFolder by remember { mutableStateOf("All") }
    var isGridView by remember { mutableStateOf(true) }
    var sortBy by remember { mutableStateOf("date") } // "date", "name", "size"

    // Multi-selection state
    var isSelectionMode by remember { mutableStateOf(false) }
    val selectedItemIds = remember { mutableStateListOf<String>() }

    // Dialogs
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var newFolderNameInput by remember { mutableStateOf("") }

    var docToRename by remember { mutableStateOf<ScannedDocItem?>(null) }
    var renameInput by remember { mutableStateOf("") }

    var docToDelete by remember { mutableStateOf<ScannedDocItem?>(null) }
    var showBatchDeleteDialog by remember { mutableStateOf(false) }

    var docToMove by remember { mutableStateOf<ScannedDocItem?>(null) }
    var showBatchMoveDialog by remember { mutableStateOf(false) }

    // Filter and Sort documents
    val filteredList = remember(
        searchQuery,
        selectedFolder,
        sortBy,
        DocumentManager.documents.size,
        DocumentManager.documents.toList()
    ) {
        var list = DocumentManager.documents.toList()

        // Folder filter
        if (selectedFolder != "All") {
            list = list.filter { it.folderName.equals(selectedFolder, ignoreCase = true) }
        }

        // Search query
        if (searchQuery.isNotBlank()) {
            list = list.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                it.folderName.contains(searchQuery, ignoreCase = true)
            }
        }

        // Sorting
        when (sortBy) {
            "name" -> list.sortedBy { it.title.lowercase() }
            "size" -> list.sortedByDescending { it.fileSizeBytes }
            else -> list.sortedByDescending { it.timestamp }
        }
    }

    val totalDocsCount = DocumentManager.documents.size

    Scaffold(
        topBar = {
            AppTopBar(
                title = "${Strings.get("docs_title")} ($totalDocsCount)",
                canNavigateBack = false
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(top = innerPadding.calculateTopPadding())
                .fillMaxSize()
        ) {
            // Search Bar & Multi-Select Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(Strings.get("search_docs"), fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )

                IconButton(
                    onClick = {
                        isSelectionMode = !isSelectionMode
                        if (!isSelectionMode) selectedItemIds.clear()
                    },
                    modifier = Modifier.background(
                        if (isSelectionMode) BrandBlue else MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(12.dp)
                    )
                ) {
                    Icon(
                        Icons.Default.Checklist,
                        contentDescription = Strings.get("select_items"),
                        tint = if (isSelectionMode) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Folder Filter Chips Carousel + "+ New Folder"
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item {
                    AssistChip(
                        onClick = { showNewFolderDialog = true },
                        label = { Text(Strings.get("new_folder"), fontWeight = FontWeight.Bold, color = BrandPurple) },
                        leadingIcon = { Icon(Icons.Default.CreateNewFolder, contentDescription = null, tint = BrandPurple, modifier = Modifier.size(16.dp)) },
                        colors = AssistChipDefaults.assistChipColors(containerColor = BrandPurple.copy(alpha = 0.1f))
                    )
                }

                items(DocumentManager.getAllFolders()) { folder ->
                    val isSelected = selectedFolder == folder
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFolder = folder },
                        label = { Text(folder) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BrandBlue,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            // Sub-bar: Document count, Sort dropdown, Grid/List view toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = String.format(Strings.get("total_docs_count"), filteredList.size),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Sort Menu
                    var sortExpanded by remember { mutableStateOf(false) }
                    Box {
                        TextButton(onClick = { sortExpanded = true }) {
                            Icon(Icons.Default.Sort, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                when (sortBy) {
                                    "name" -> Strings.get("sort_name")
                                    "size" -> Strings.get("sort_size")
                                    else -> Strings.get("sort_date")
                                },
                                fontSize = 12.sp
                            )
                        }
                        DropdownMenu(
                            expanded = sortExpanded,
                            onDismissRequest = { sortExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(Strings.get("sort_date")) },
                                onClick = { sortBy = "date"; sortExpanded = false }
                            )
                            DropdownMenuItem(
                                text = { Text(Strings.get("sort_name")) },
                                onClick = { sortBy = "name"; sortExpanded = false }
                            )
                            DropdownMenuItem(
                                text = { Text(Strings.get("sort_size")) },
                                onClick = { sortBy = "size"; sortExpanded = false }
                            )
                        }
                    }

                    // Grid / List Toggle
                    IconButton(onClick = { isGridView = !isGridView }) {
                        Icon(
                            if (isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                            contentDescription = "Toggle View",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Main Document List / Grid
            if (filteredList.isEmpty()) {
                // Empty state inspired by Oken Scanner / CamScanner
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = BrandBlue.copy(alpha = 0.12f),
                            modifier = Modifier.size(90.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.DocumentScanner,
                                    contentDescription = null,
                                    tint = BrandBlue,
                                    modifier = Modifier.size(46.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = Strings.get("no_docs_found"),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = Strings.get("files_empty_helper"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = onScanDocument,
                            colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(Strings.get("scan_new_docs"), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                if (isGridView) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredList, key = { it.id }) { doc ->
                            DocGridCard(
                                item = doc,
                                isSelectionMode = isSelectionMode,
                                isSelected = selectedItemIds.contains(doc.id),
                                onToggleSelect = {
                                    if (selectedItemIds.contains(doc.id)) selectedItemIds.remove(doc.id)
                                    else selectedItemIds.add(doc.id)
                                },
                                onOpen = {
                                    viewFile(context, File(doc.pdfPath), "application/pdf")
                                },
                                onShare = {
                                    shareFile(context, File(doc.pdfPath), "application/pdf")
                                },
                                onRename = {
                                    docToRename = doc
                                    renameInput = doc.title
                                },
                                onMove = {
                                    docToMove = doc
                                },
                                onDelete = {
                                    docToDelete = doc
                                }
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredList, key = { it.id }) { doc ->
                            DocListItem(
                                item = doc,
                                isSelectionMode = isSelectionMode,
                                isSelected = selectedItemIds.contains(doc.id),
                                onToggleSelect = {
                                    if (selectedItemIds.contains(doc.id)) selectedItemIds.remove(doc.id)
                                    else selectedItemIds.add(doc.id)
                                },
                                onOpen = {
                                    viewFile(context, File(doc.pdfPath), "application/pdf")
                                },
                                onShare = {
                                    shareFile(context, File(doc.pdfPath), "application/pdf")
                                },
                                onRename = {
                                    docToRename = doc
                                    renameInput = doc.title
                                },
                                onMove = {
                                    docToMove = doc
                                },
                                onDelete = {
                                    docToDelete = doc
                                }
                            )
                        }
                    }
                }
            }

            // Multi-Selection Bottom Bar
            AnimatedVisibility(visible = isSelectionMode && selectedItemIds.isNotEmpty()) {
                Surface(
                    color = BrandBlue,
                    tonalElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = String.format(Strings.get("selected_count"), selectedItemIds.size),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Move to folder batch
                            IconButton(onClick = { showBatchMoveDialog = true }) {
                                Icon(Icons.Default.DriveFileMove, contentDescription = Strings.get("move_to_folder"), tint = Color.White)
                            }
                            // Delete batch
                            IconButton(onClick = { showBatchDeleteDialog = true }) {
                                Icon(Icons.Default.Delete, contentDescription = Strings.get("delete_selected"), tint = Color.White)
                            }
                        }
                    }
                }
            }
        }

        // New Folder Dialog
        if (showNewFolderDialog) {
            AlertDialog(
                onDismissRequest = { showNewFolderDialog = false },
                title = { Text(Strings.get("create_folder")) },
                text = {
                    OutlinedTextField(
                        value = newFolderNameInput,
                        onValueChange = { newFolderNameInput = it },
                        label = { Text(Strings.get("folder_name")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newFolderNameInput.isNotBlank()) {
                                DocumentManager.addFolder(context, newFolderNameInput)
                                selectedFolder = newFolderNameInput.trim()
                                newFolderNameInput = ""
                            }
                            showNewFolderDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
                    ) {
                        Text(Strings.get("confirm"))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showNewFolderDialog = false }) {
                        Text(Strings.get("cancel"))
                    }
                }
            )
        }

        // Rename Dialog
        docToRename?.let { item ->
            AlertDialog(
                onDismissRequest = { docToRename = null },
                title = { Text(Strings.get("rename_document")) },
                text = {
                    OutlinedTextField(
                        value = renameInput,
                        onValueChange = { renameInput = it },
                        label = { Text(Strings.get("rename_document")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (renameInput.isNotBlank()) {
                                DocumentManager.renameDocument(context, item, renameInput.trim())
                            }
                            docToRename = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
                    ) {
                        Text(Strings.get("save"))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { docToRename = null }) {
                        Text(Strings.get("cancel"))
                    }
                }
            )
        }

        // Move to Folder Dialog
        docToMove?.let { item ->
            AlertDialog(
                onDismissRequest = { docToMove = null },
                title = { Text(Strings.get("move_to_folder")) },
                text = {
                    Column {
                        DocumentManager.getAllFolders().forEach { folder ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        DocumentManager.moveToFolder(context, item, folder)
                                        docToMove = null
                                    }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Folder, contentDescription = null, tint = BrandBlue)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(folder, fontSize = 15.sp)
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { docToMove = null }) {
                        Text(Strings.get("cancel"))
                    }
                }
            )
        }

        // Batch Move Dialog
        if (showBatchMoveDialog) {
            AlertDialog(
                onDismissRequest = { showBatchMoveDialog = false },
                title = { Text(Strings.get("move_to_folder")) },
                text = {
                    Column {
                        DocumentManager.getAllFolders().forEach { folder ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val items = DocumentManager.documents.filter { selectedItemIds.contains(it.id) }
                                        DocumentManager.moveMultipleToFolder(context, items, folder)
                                        selectedItemIds.clear()
                                        isSelectionMode = false
                                        showBatchMoveDialog = false
                                    }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Folder, contentDescription = null, tint = BrandBlue)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(folder, fontSize = 15.sp)
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showBatchMoveDialog = false }) {
                        Text(Strings.get("cancel"))
                    }
                }
            )
        }

        // Delete Single Dialog
        docToDelete?.let { item ->
            AlertDialog(
                onDismissRequest = { docToDelete = null },
                title = { Text(Strings.get("delete")) },
                text = { Text(if (LocaleManager.isHindi()) "क्या आप वाकई '${item.title}' हटाना चाहते हैं?" else "Delete '${item.title}' permanently?") },
                confirmButton = {
                    Button(
                        onClick = {
                            DocumentManager.deleteDocument(context, item)
                            docToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(Strings.get("delete"))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { docToDelete = null }) {
                        Text(Strings.get("cancel"))
                    }
                }
            )
        }

        // Batch Delete Dialog
        if (showBatchDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showBatchDeleteDialog = false },
                title = { Text(Strings.get("delete_selected")) },
                text = { Text(if (LocaleManager.isHindi()) "क्या आप चयनित ${selectedItemIds.size} दस्तावेज़ हटाना चाहते हैं?" else "Delete ${selectedItemIds.size} selected documents permanently?") },
                confirmButton = {
                    Button(
                        onClick = {
                            val items = DocumentManager.documents.filter { selectedItemIds.contains(it.id) }
                            DocumentManager.deleteMultiple(context, items)
                            selectedItemIds.clear()
                            isSelectionMode = false
                            showBatchDeleteDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(Strings.get("delete"))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showBatchDeleteDialog = false }) {
                        Text(Strings.get("cancel"))
                    }
                }
            )
        }
    }
}

@Composable
fun DocGridCard(
    item: ScannedDocItem,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onRename: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val dateStr = remember(item.timestamp) {
        SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(item.timestamp))
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (isSelectionMode) onToggleSelect() else onOpen()
            }
    ) {
        Column {
            // Thumbnail container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(Color(0xFFF1F5F9)),
                contentAlignment = Alignment.Center
            ) {
                // If thumbnail image exists, display it; otherwise display PDF vector
                val thumbBitmap = remember(item.thumbnailPath) {
                    item.thumbnailPath?.let { path ->
                        try { BitmapFactory.decodeFile(path) } catch (_: Exception) { null }
                    }
                }

                if (thumbBitmap != null) {
                    Image(
                        bitmap = thumbBitmap.asImageBitmap(),
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        tint = BrandBlue,
                        modifier = Modifier.size(54.dp)
                    )
                }

                // Page count badge
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.65f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${item.pageCount}P",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Multi-select Checkbox
                if (isSelectionMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onToggleSelect() },
                        modifier = Modifier.align(Alignment.TopStart).padding(4.dp)
                    )
                }
            }

            // Info row
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Menu", modifier = Modifier.size(16.dp))
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(Strings.get("open_pdf")) },
                                leadingIcon = { Icon(Icons.Default.Visibility, contentDescription = null) },
                                onClick = { menuExpanded = false; onOpen() }
                            )
                            DropdownMenuItem(
                                text = { Text(Strings.get("share")) },
                                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                                onClick = { menuExpanded = false; onShare() }
                            )
                            DropdownMenuItem(
                                text = { Text(Strings.get("rename_document")) },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                onClick = { menuExpanded = false; onRename() }
                            )
                            DropdownMenuItem(
                                text = { Text(Strings.get("move_to_folder")) },
                                leadingIcon = { Icon(Icons.Default.DriveFileMove, contentDescription = null) },
                                onClick = { menuExpanded = false; onMove() }
                            )
                            DropdownMenuItem(
                                text = { Text(Strings.get("delete"), color = MaterialTheme.colorScheme.error) },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                onClick = { menuExpanded = false; onDelete() }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = dateStr,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = ImageProcessor.formatFileSize(item.fileSizeBytes),
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun DocListItem(
    item: ScannedDocItem,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onRename: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val dateStr = remember(item.timestamp) {
        SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(item.timestamp))
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (isSelectionMode) onToggleSelect() else onOpen()
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelectionMode) {
                Checkbox(checked = isSelected, onCheckedChange = { onToggleSelect() })
                Spacer(modifier = Modifier.width(6.dp))
            }

            // Thumbnail / Icon
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = BrandBlue.copy(alpha = 0.1f),
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        tint = BrandBlue,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "${item.pageCount} ${if (item.pageCount == 1) "page" else "pages"}",
                        fontSize = 11.sp,
                        color = BrandPurple,
                        fontWeight = FontWeight.Medium
                    )
                    Text("•", fontSize = 11.sp, color = Color.Gray)
                    Text(dateStr, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("•", fontSize = 11.sp, color = Color.Gray)
                    Text(ImageProcessor.formatFileSize(item.fileSizeBytes), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(Strings.get("open_pdf")) },
                        leadingIcon = { Icon(Icons.Default.Visibility, contentDescription = null) },
                        onClick = { menuExpanded = false; onOpen() }
                    )
                    DropdownMenuItem(
                        text = { Text(Strings.get("share")) },
                        leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                        onClick = { menuExpanded = false; onShare() }
                    )
                    DropdownMenuItem(
                        text = { Text(Strings.get("rename_document")) },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        onClick = { menuExpanded = false; onRename() }
                    )
                    DropdownMenuItem(
                        text = { Text(Strings.get("move_to_folder")) },
                        leadingIcon = { Icon(Icons.Default.DriveFileMove, contentDescription = null) },
                        onClick = { menuExpanded = false; onMove() }
                    )
                    DropdownMenuItem(
                        text = { Text(Strings.get("delete"), color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        onClick = { menuExpanded = false; onDelete() }
                    )
                }
            }
        }
    }
}
