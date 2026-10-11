package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.model.RecentFileManager
import com.example.model.ToolCategory
import com.example.ui.screens.*
import com.example.ui.theme.BlueContainer
import com.example.ui.theme.BluePrimaryVariant
import com.example.ui.theme.MyApplicationTheme
import com.example.util.Strings

sealed class NavItem(val route: String, val labelKey: String, val icon: ImageVector) {
    object Home : NavItem("home", "nav_home", Icons.Default.Home)
    object Tools : NavItem("tools", "nav_tools", Icons.Default.Apps)
    object Files : NavItem("files", "nav_files", Icons.Default.Folder)
    object Favorites : NavItem("favorites", "nav_favorites", Icons.Default.Star)
    object Settings : NavItem("settings", "nav_settings", Icons.Default.Settings)
}

object ToolRoutes {
    const val DOC_SCANNER = "tool_doc_scanner"
    const val PHOTO_RESIZE = "tool_photo_resize"
    const val PASSPORT_PHOTO = "tool_passport_photo"
    const val PDF_TOOLS = "tool_pdf_tools"
    const val HANDWRITING = "tool_handwriting"
    const val QR_GENERATOR = "tool_qr_generator"
    const val QR_SCANNER = "tool_qr_scanner"
    const val SIGN_PDF = "tool_sign_pdf"
    const val OCR_TOOL = "tool_ocr_tool"

    fun routeForToolId(toolId: String): String {
        return when (toolId) {
            "doc_scanner" -> DOC_SCANNER
            "photo_resize" -> PHOTO_RESIZE
            "passport_photo" -> PASSPORT_PHOTO
            "pdf_compress", "pdf_resize", "img_to_pdf", "pdf_to_img", "merge_pdf", "split_pdf", "watermark_pdf", "blank_pdf" -> {
                "$PDF_TOOLS?tool=$toolId"
            }
            "handwriting" -> HANDWRITING
            "qr_generator" -> QR_GENERATOR
            "qr_scanner" -> QR_SCANNER
            "sign_pdf" -> SIGN_PDF
            "ocr_tool" -> OCR_TOOL
            else -> DOC_SCANNER
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        RecentFileManager.init(applicationContext)
        com.example.util.SampleDataProvider.initRecentSampleFiles(applicationContext)

        setContent {
            MyApplicationTheme {
                MainAppContainer()
            }
        }
    }
}

@Composable
fun MainAppContainer() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route

    val bottomNavItems = listOf(
        NavItem.Home,
        NavItem.Tools,
        NavItem.Files,
        NavItem.Favorites,
        NavItem.Settings
    )

    // Bottom navigation is shown on the primary 5 tabs
    val isBottomBarVisible = bottomNavItems.any { item ->
        currentRoute?.startsWith(item.route) == true
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (isBottomBarVisible) {
                NavigationBar(
                    modifier = Modifier.testTag("app_bottom_nav"),
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    val navItemColors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BluePrimaryVariant,
                        selectedTextColor = BluePrimaryVariant,
                        indicatorColor = BlueContainer,
                        unselectedIconColor = androidx.compose.ui.graphics.Color(0xFF64748B),
                        unselectedTextColor = androidx.compose.ui.graphics.Color(0xFF64748B)
                    )

                    bottomNavItems.forEach { item ->
                        val isSelected = currentRoute?.startsWith(item.route) == true
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = Strings.get(item.labelKey)) },
                            label = { Text(Strings.get(item.labelKey)) },
                            colors = navItemColors
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            NavHost(
                navController = navController,
                startDestination = NavItem.Home.route,
                modifier = Modifier.fillMaxSize()
            ) {
                composable(NavItem.Home.route) {
                    HomeScreen(
                        onNavigateToTool = { toolId ->
                            navController.navigate(ToolRoutes.routeForToolId(toolId))
                        },
                        onNavigateToCategory = { category ->
                            navController.navigate("${NavItem.Tools.route}?category=${category.name}")
                        },
                        onViewAllRecent = {
                            navController.navigate(NavItem.Files.route)
                        }
                    )
                }

                composable(
                    route = "${NavItem.Tools.route}?category={category}",
                    arguments = listOf(
                        navArgument("category") {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        }
                    )
                ) { backStackEntry ->
                    val categoryParam = backStackEntry.arguments?.getString("category")?.let { catName ->
                        try { ToolCategory.valueOf(catName) } catch (e: Exception) { null }
                    }
                    ToolsCatalogScreen(
                        initialCategory = categoryParam,
                        onNavigateToTool = { toolId ->
                            navController.navigate(ToolRoutes.routeForToolId(toolId))
                        }
                    )
                }

                composable(NavItem.Files.route) {
                    RecentFilesScreen(
                        onNavigateBack = {
                            navController.popBackStack()
                        }
                    )
                }

                composable(NavItem.Favorites.route) {
                    ToolsCatalogScreen(
                        initialCategory = null,
                        showFavoritesOnly = true,
                        onNavigateToTool = { toolId ->
                            navController.navigate(ToolRoutes.routeForToolId(toolId))
                        }
                    )
                }

                composable(NavItem.Settings.route) {
                    SettingsScreen(
                        onNavigateBack = {
                            navController.popBackStack()
                        }
                    )
                }

                // Dedicated Tool Routes
                composable(ToolRoutes.DOC_SCANNER) {
                    DocumentScannerScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(ToolRoutes.PHOTO_RESIZE) {
                    PhotoToolsScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(ToolRoutes.PASSPORT_PHOTO) {
                    PhotoToolsScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(
                    route = "${ToolRoutes.PDF_TOOLS}?tool={tool}",
                    arguments = listOf(
                        navArgument("tool") {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        }
                    )
                ) { backStackEntry ->
                    val toolParam = backStackEntry.arguments?.getString("tool")
                    PdfToolsScreen(
                        initialTool = toolParam,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(ToolRoutes.HANDWRITING) {
                    HandwritingScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(ToolRoutes.QR_GENERATOR) {
                    QrToolsScreen(
                        initialScanMode = false,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(ToolRoutes.QR_SCANNER) {
                    QrToolsScreen(
                        initialScanMode = true,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(ToolRoutes.SIGN_PDF) {
                    SignatureMakerScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(ToolRoutes.OCR_TOOL) {
                    UtilitiesScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
