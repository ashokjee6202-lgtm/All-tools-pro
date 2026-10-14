package com.example.ui.components

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.RecentFile
import com.example.model.RecentFileManager
import com.example.model.ToolItem
import com.example.ui.theme.BlueContainer
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.BrandPurple
import com.example.ui.theme.OnBlueContainer
import com.example.util.ImageProcessor
import com.example.util.LocaleManager
import com.example.util.Strings
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    canNavigateBack: Boolean = false,
    onNavigateBack: () -> Unit = {},
    onNavigateHome: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // If canNavigateBack is false (i.e. top-level screen), show the logo next to the title
                if (!canNavigateBack) {
                    Image(
                        painter = painterResource(id = R.drawable.alltoolspro_logo),
                        contentDescription = "AllToolsPro Logo",
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(9.dp))
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
        },
        navigationIcon = {
            if (canNavigateBack) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("top_bar_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = Strings.get("cd_back"),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        },
        actions = {
            // Inner tool screens show a Home icon button to jump straight to Home tab
            if (canNavigateBack && onNavigateHome != null) {
                IconButton(
                    onClick = onNavigateHome,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("top_bar_home_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = Strings.get("cd_home"),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Language switch button (हिन्दी / English)
            TextButton(
                onClick = { LocaleManager.toggleLanguage() },
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("language_switch_button")
            ) {
                Text(
                    text = if (LocaleManager.isHindi()) "English" else "हिन्दी",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
            }
            actions()
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
fun ToolCard(
    tool: ToolItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onFavoriteToggled: ((Boolean) -> Unit)? = null
) {
    val context = LocalContext.current
    val isFav = RecentFileManager.isFavorite(tool.id)

    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .testTag("tool_card_${tool.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(listOf(BrandBlue.copy(alpha = 0.15f), BrandPurple.copy(alpha = 0.15f)))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = tool.icon,
                    contentDescription = Strings.get(tool.titleKey),
                    tint = BrandBlue,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = Strings.get(tool.titleKey),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = Strings.get(tool.descriptionKey),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = {
                    val wasFav = RecentFileManager.isFavorite(tool.id)
                    RecentFileManager.toggleFavorite(context, tool.id)
                    onFavoriteToggled?.invoke(!wasFav)
                },
                modifier = Modifier
                    .size(48.dp)
                    .testTag("fav_btn_${tool.id}")
            ) {
                Icon(
                    imageVector = if (isFav) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = if (isFav) Strings.get("removed_from_favorites") else Strings.get("added_to_favorites"),
                    tint = if (isFav) Color(0xFFFFB300) else MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
fun ToolGridCard(
    tool: ToolItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onFavoriteToggled: ((Boolean) -> Unit)? = null
) {
    val context = LocalContext.current
    val isFav = RecentFileManager.isFavorite(tool.id)

    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .testTag("tool_card_${tool.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(listOf(BrandBlue.copy(alpha = 0.15f), BrandPurple.copy(alpha = 0.15f)))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = tool.icon,
                        contentDescription = Strings.get(tool.titleKey),
                        tint = BrandBlue,
                        modifier = Modifier.size(24.dp)
                    )
                }

                IconButton(
                    onClick = {
                        val wasFav = RecentFileManager.isFavorite(tool.id)
                        RecentFileManager.toggleFavorite(context, tool.id)
                        onFavoriteToggled?.invoke(!wasFav)
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("fav_btn_${tool.id}")
                ) {
                    Icon(
                        imageVector = if (isFav) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = if (isFav) Strings.get("removed_from_favorites") else Strings.get("added_to_favorites"),
                        tint = if (isFav) Color(0xFFFFB300) else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = Strings.get(tool.titleKey),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = Strings.get(tool.descriptionKey),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontSize = 11.5.sp,
                lineHeight = 15.sp
            )
        }
    }
}

@Composable
fun PopularToolChip(
    tool: ToolItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .width(140.dp)
            .testTag("popular_chip_${tool.id}")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(listOf(BrandBlue.copy(alpha = 0.15f), BrandPurple.copy(alpha = 0.15f)))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = tool.icon,
                    contentDescription = Strings.get(tool.titleKey),
                    tint = BrandBlue,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = Strings.get(tool.titleKey),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 15.sp
            )
        }
    }
}

@Composable
fun RecentFileItem(
    item: RecentFile,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val file = File(item.path)
    val exists = file.exists()

    val fileDate = remember(item.timestamp) {
        val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
        sdf.format(Date(item.timestamp))
    }

    val icon = when {
        item.name.endsWith(".pdf", ignoreCase = true) -> Icons.Default.PictureAsPdf
        item.name.endsWith(".jpg", ignoreCase = true) || item.name.endsWith(".png", ignoreCase = true) -> Icons.Default.Image
        item.name.endsWith(".zip", ignoreCase = true) -> Icons.Default.FolderZip
        else -> Icons.Default.InsertDriveFile
    }

    val iconTint = when {
        item.name.endsWith(".pdf", ignoreCase = true) -> Color(0xFFE53935)
        item.name.endsWith(".jpg", ignoreCase = true) || item.name.endsWith(".png", ignoreCase = true) -> BrandBlue
        item.name.endsWith(".zip", ignoreCase = true) -> BrandPurple
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = exists, onClick = onOpen)
            .testTag("recent_file_${item.id}")
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconTint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${ImageProcessor.formatBytes(item.sizeBytes)} • $fileDate",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = {
                    shareFile(context, file, item.mimeType)
                },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = Strings.get("share"),
                    tint = MaterialTheme.colorScheme.outline
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = Strings.get("delete"),
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun ResultSummaryCard(
    title: String,
    originalSize: String,
    outputSize: String,
    reductionRatio: String? = null,
    onView: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BlueContainer),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = OnBlueContainer
                )
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF2E7D32)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = Strings.get("original_size"), style = MaterialTheme.typography.bodySmall, color = OnBlueContainer.copy(alpha = 0.7f))
                    Text(text = originalSize, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = OnBlueContainer)
                }
                Column {
                    Text(text = Strings.get("output_size"), style = MaterialTheme.typography.bodySmall, color = OnBlueContainer.copy(alpha = 0.7f))
                    Text(text = outputSize, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = Color(0xFF2E7D32))
                }
                if (reductionRatio != null) {
                    Column {
                        Text(text = Strings.get("compression_ratio"), style = MaterialTheme.typography.bodySmall, color = OnBlueContainer.copy(alpha = 0.7f))
                        Text(text = reductionRatio, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = BrandPurple)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onView,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(Strings.get("preview"))
                }

                FilledTonalButton(
                    onClick = onShare,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(Strings.get("share"))
                }
            }
        }
    }
}

fun viewFile(context: Context, file: File, mimeType: String) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Open file with"))
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "Could not open file: ${e.localizedMessage}", android.widget.Toast.LENGTH_SHORT).show()
    }
}

fun shareFile(context: Context, file: File, mimeType: String) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share file"))
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "Could not share file: ${e.localizedMessage}", android.widget.Toast.LENGTH_SHORT).show()
    }
}
