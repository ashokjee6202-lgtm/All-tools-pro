package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.R
import com.example.ui.components.AppTopBar
import com.example.ui.theme.BlueContainer
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.BrandPurple
import com.example.ui.theme.OnBlueContainer
import com.example.util.AppLanguage
import com.example.util.ImageProcessor
import com.example.util.LocaleManager
import com.example.util.Strings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class AppThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

object ThemePreferences {
    var themeMode by mutableStateOf(AppThemeMode.SYSTEM)

    fun init(context: Context) {
        val sp = context.getSharedPreferences("app_theme_prefs", Context.MODE_PRIVATE)
        val saved = sp.getString("theme_mode", AppThemeMode.SYSTEM.name) ?: AppThemeMode.SYSTEM.name
        themeMode = try {
            AppThemeMode.valueOf(saved)
        } catch (_: Exception) {
            AppThemeMode.SYSTEM
        }
    }

    fun setTheme(context: Context, mode: AppThemeMode) {
        themeMode = mode
        val sp = context.getSharedPreferences("app_theme_prefs", Context.MODE_PRIVATE)
        sp.edit().putString("theme_mode", mode.name).apply()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showClearCacheConfirm by remember { mutableStateOf(false) }
    var cacheSizeBytes by remember { mutableLongStateOf(0L) }
    var isCalculatingCache by remember { mutableStateOf(true) }

    fun calculateCacheSize() {
        coroutineScope.launch(Dispatchers.IO) {
            var total = 0L
            fun getDirSize(dir: File?): Long {
                var size = 0L
                dir?.listFiles()?.forEach { file ->
                    size += if (file.isDirectory) getDirSize(file) else file.length()
                }
                return size
            }
            total += getDirSize(context.cacheDir)
            total += getDirSize(context.externalCacheDir)
            withContext(Dispatchers.Main) {
                cacheSizeBytes = total
                isCalculatingCache = false
            }
        }
    }

    LaunchedEffect(Unit) {
        calculateCacheSize()
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = Strings.get("nav_settings"),
                canNavigateBack = false
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // App Header Card with new logo and dynamic version
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, BrandBlue.copy(alpha = 0.15f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.foundation.Image(
                        painter = painterResource(id = R.drawable.alltoolspro_logo),
                        contentDescription = "AllToolsPro Logo",
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(14.dp))
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = Strings.get("app_name"),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = BrandBlue)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = Strings.get("app_tagline"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Language Selector Section
            Text(
                text = if (LocaleManager.isHindi()) "भाषा (Language)" else "Language / भाषा",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { LocaleManager.setLanguage(AppLanguage.ENGLISH) }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = LocaleManager.currentLanguage == AppLanguage.ENGLISH,
                            onClick = { LocaleManager.setLanguage(AppLanguage.ENGLISH) }
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = "English (Default)", style = MaterialTheme.typography.bodyLarge)
                    }
                    HorizontalDivider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { LocaleManager.setLanguage(AppLanguage.HINDI) }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = LocaleManager.currentLanguage == AppLanguage.HINDI,
                            onClick = { LocaleManager.setLanguage(AppLanguage.HINDI) }
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = "हिन्दी (Hindi)", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Theme Selector Section
            Text(
                text = Strings.get("theme"),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { ThemePreferences.setTheme(context, AppThemeMode.SYSTEM) }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = ThemePreferences.themeMode == AppThemeMode.SYSTEM,
                            onClick = { ThemePreferences.setTheme(context, AppThemeMode.SYSTEM) }
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = Strings.get("theme_system"), style = MaterialTheme.typography.bodyLarge)
                    }
                    HorizontalDivider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { ThemePreferences.setTheme(context, AppThemeMode.LIGHT) }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = ThemePreferences.themeMode == AppThemeMode.LIGHT,
                            onClick = { ThemePreferences.setTheme(context, AppThemeMode.LIGHT) }
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = Strings.get("theme_light"), style = MaterialTheme.typography.bodyLarge)
                    }
                    HorizontalDivider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { ThemePreferences.setTheme(context, AppThemeMode.DARK) }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = ThemePreferences.themeMode == AppThemeMode.DARK,
                            onClick = { ThemePreferences.setTheme(context, AppThemeMode.DARK) }
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = Strings.get("theme_dark"), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Storage & Cache Section
            Text(
                text = if (LocaleManager.isHindi()) "स्टोरेज और डेटा" else "Storage & Cache",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                onClick = { showClearCacheConfirm = true },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.CleaningServices, contentDescription = null, tint = BrandBlue)
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = Strings.get("clear_cache"), fontWeight = FontWeight.SemiBold)
                        Text(
                            text = if (isCalculatingCache) Strings.get("calculating") else "${ImageProcessor.formatFileSize(cacheSizeBytes)} temporary files",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Actions (Rate, Share, Feedback, Privacy, About)
            Text(
                text = if (LocaleManager.isHindi()) "सपोर्ट और जानकारी" else "Support & About",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    // Official Website
                    SettingsRowItem(
                        icon = Icons.Default.Language,
                        title = Strings.get("official_website"),
                        subtitle = "https://www.alltoolspro.in/",
                        onClick = {
                            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.alltoolspro.in/")).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            try {
                                context.startActivity(browserIntent)
                            } catch (_: Exception) {
                                Toast.makeText(context, "Could not open browser", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                    HorizontalDivider()

                    // WhatsApp Support
                    SettingsRowItem(
                        icon = Icons.Default.Chat,
                        title = Strings.get("whatsapp_support"),
                        subtitle = "WhatsApp: +91 6202292319",
                        onClick = {
                            val waIntent = Intent(Intent.ACTION_VIEW).apply {
                                data = Uri.parse("https://api.whatsapp.com/send?phone=916202292319&text=Hello%20AllToolsMaster%20Support")
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            try {
                                context.startActivity(waIntent)
                            } catch (_: Exception) {
                                val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:6202292319")).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(dialIntent)
                            }
                        }
                    )
                    HorizontalDivider()

                    // Rate App
                    SettingsRowItem(
                        icon = Icons.Default.StarRate,
                        title = Strings.get("rate_app"),
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${context.packageName}")).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            try {
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}")))
                            }
                        }
                    )
                    HorizontalDivider()

                    // Share App
                    SettingsRowItem(
                        icon = Icons.Default.Share,
                        title = Strings.get("share_app"),
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "AllToolsMaster - All-in-One Utility App")
                                putExtra(Intent.EXTRA_TEXT, "Download AllToolsMaster for high quality document scanning, PDF tools, photo resizing and utilities: https://www.alltoolspro.in/")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share AllToolsMaster"))
                        }
                    )
                    HorizontalDivider()

                    // Send Feedback
                    SettingsRowItem(
                        icon = Icons.Default.MailOutline,
                        title = Strings.get("send_feedback"),
                        onClick = {
                            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:ashokjee6202@gmail.com")
                                putExtra(Intent.EXTRA_SUBJECT, "Feedback for AllToolsMaster v${BuildConfig.VERSION_NAME}")
                            }
                            try {
                                context.startActivity(Intent.createChooser(emailIntent, "Send Feedback"))
                            } catch (_: Exception) {
                                Toast.makeText(context, "No email app found", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                    HorizontalDivider()

                    // Privacy Policy
                    SettingsRowItem(
                        icon = Icons.Default.Shield,
                        title = Strings.get("privacy_policy"),
                        onClick = { showPrivacyDialog = true }
                    )
                    HorizontalDivider()

                    // About App
                    SettingsRowItem(
                        icon = Icons.Default.Info,
                        title = Strings.get("about"),
                        subtitle = "Developer: AllToolsMaster Team • 100% On-Device",
                        onClick = { showPrivacyDialog = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }

        // Cache confirmation dialog
        if (showClearCacheConfirm) {
            val formattedSize = ImageProcessor.formatFileSize(cacheSizeBytes)
            AlertDialog(
                onDismissRequest = { showClearCacheConfirm = false },
                title = { Text(Strings.get("clear_cache"), fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        text = java.lang.String.format(Strings.get("confirm_clear_cache"), formattedSize)
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showClearCacheConfirm = false
                            val freed = cacheSizeBytes
                            try {
                                context.cacheDir.deleteRecursively()
                                context.cacheDir.mkdirs()
                                context.externalCacheDir?.deleteRecursively()
                                context.externalCacheDir?.mkdirs()
                            } catch (_: Exception) {}
                            cacheSizeBytes = 0L
                            val freedStr = ImageProcessor.formatFileSize(freed)
                            Toast.makeText(
                                context,
                                java.lang.String.format(Strings.get("cache_cleared"), freedStr),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    ) {
                        Text(Strings.get("confirm"), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearCacheConfirm = false }) {
                        Text(Strings.get("cancel"))
                    }
                }
            )
        }

        // Privacy Dialog
        if (showPrivacyDialog) {
            AlertDialog(
                onDismissRequest = { showPrivacyDialog = false },
                title = { Text(if (LocaleManager.isHindi()) "गोपनीयता प्रतिबद्धता" else "AllToolsMaster Privacy Commitment", fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        if (LocaleManager.isHindi()) {
                            "AllToolsMaster 100% गोपनीयता-केंद्रित ऐप है:\n\n" +
                                    "1. 100% ऑन-डिवाइस निष्पादन: दस्तावेज़ स्कैनिंग, पीडीएफ संपीड़न, फोटो रिसाइज़िंग और क्यूआर जेनरेशन पूरी तरह से आपके डिवाइस पर होता है।\n\n" +
                                    "2. शून्य अपलोड: कोई भी दस्तावेज़, फोटो या डेटा किसी भी सर्वर या क्लाउड पर अपलोड नहीं होता है।\n\n" +
                                    "3. कोई इंटरनेट की आवश्यकता नहीं: सभी टूल्स बिना इंटरनेट के सुरक्षित रूप से काम करते हैं।"
                        } else {
                            "AllToolsMaster is built with strict privacy-by-design principles:\n\n" +
                                    "1. 100% Complete Local Execution: All document scanning, PDF compression, image resizing, and handwriting generation occur strictly on your device.\n\n" +
                                    "2. Zero Uploads: None of your photos, documents, or data are ever transmitted to any external server or cloud service.\n\n" +
                                    "3. No Internet Required: All core utility features operate offline without external APIs or ad trackers."
                        }
                    )
                },
                confirmButton = {
                    TextButton(onClick = { showPrivacyDialog = false }) {
                        Text(Strings.get("confirm"))
                    }
                }
            )
        }
    }
}

@Composable
fun SettingsRowItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium))
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
    }
}
