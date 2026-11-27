package com.example.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FilterVintage
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.outlined.FilterVintage
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.i18n.BolpikStrings
import com.example.i18n.EnglishStrings
import com.example.i18n.LocalBolpikStrings
import com.example.i18n.SpanishStrings
import com.example.model.EditorToolTab
import com.example.ui.screens.CollageScreen
import com.example.ui.screens.EditorScreen
import com.example.ui.screens.ExportStudioScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PresetsScreen
import com.example.ui.screens.ProjectsScreen
import com.example.ui.theme.BolpikGreen
import com.example.ui.theme.BolpikGreenDark
import com.example.ui.theme.BolpikLime
import com.example.viewmodel.BolpikViewModel
import kotlinx.coroutines.launch

object BolpikRoutes {
    const val HOME = "home"
    const val PROJECTS = "projects"
    const val PRESETS = "presets"
    const val COLLAGE = "collage"
    const val EDITOR = "editor"
    const val EXPORT_STUDIO = "export_studio"
}

private data class TopLevelDestination(
    val route: String,
    val labelSelector: (BolpikStrings) -> String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

// Ordered: Inicio (casita) -> Proyectos -> Presets -> Collage
private val topLevelDestinations = listOf(
    TopLevelDestination(BolpikRoutes.HOME, { it.navHome }, Icons.Filled.Home, Icons.Outlined.Home),
    TopLevelDestination(BolpikRoutes.PROJECTS, { it.navProjects }, Icons.Filled.FolderOpen, Icons.Outlined.FolderOpen),
    TopLevelDestination(BolpikRoutes.PRESETS, { it.navPresets }, Icons.Filled.FilterVintage, Icons.Outlined.FilterVintage),
    TopLevelDestination(BolpikRoutes.COLLAGE, { it.navCollage }, Icons.Filled.GridView, Icons.Outlined.GridView)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BolpikApp(
    viewModel: BolpikViewModel,
    darkroomMode: Boolean,
    appLanguage: String
) {
    val strings = if (appLanguage == "en") EnglishStrings else SpanishStrings

    CompositionLocalProvider(LocalBolpikStrings provides strings) {
        val navController = rememberNavController()
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = backStackEntry?.destination
        val currentRoute = currentDestination?.route

        val editorState by viewModel.editorState.collectAsStateWithLifecycle()
        val exportState by viewModel.exportState.collectAsStateWithLifecycle()
        val collageState by viewModel.collageState.collectAsStateWithLifecycle()
        val presetsState by viewModel.presetsState.collectAsStateWithLifecycle()
        val customPresets by viewModel.customPresets.collectAsStateWithLifecycle()
        val projects by viewModel.projects.collectAsStateWithLifecycle()

        val showChrome = currentRoute in topLevelDestinations.map { it.route }
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val scope = rememberCoroutineScope()
        var showAboutDialog by remember { mutableStateOf(false) }

        val quickImportPicker = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia()
        ) { uri ->
            if (uri != null) {
                viewModel.openUriInEditor(uri, initialTab = EditorToolTab.ADJUST) {
                    navController.navigate(BolpikRoutes.EDITOR)
                }
            }
        }

        fun navigateTopLevel(route: String) {
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }

        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = showChrome,
            drawerContent = {
                ModalDrawerSheet(
                    drawerContainerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.width(300.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 22.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Brush.linearGradient(listOf(BolpikGreenDark, BolpikLime))),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "B",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = Color(0xFF041F12),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column {
                                Text("Bolpik Studio", style = MaterialTheme.typography.titleLarge)
                                Text(
                                    "com.bolpik.studio",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BolpikGreen
                                )
                            }
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    Spacer(Modifier.height(10.dp))

                    topLevelDestinations.forEach { dest ->
                        val selected = currentDestination?.hierarchy?.any { it.route == dest.route } == true
                        val label = dest.labelSelector(strings)
                        NavigationDrawerItem(
                            selected = selected,
                            onClick = {
                                scope.launch { drawerState.close() }
                                navigateTopLevel(dest.route)
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                    contentDescription = label
                                )
                            },
                            label = { Text(label) },
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    Spacer(Modifier.height(12.dp))

                    // Language Selector (Spanish & English only)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Language,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(strings.languageLabel, style = MaterialTheme.typography.titleSmall)
                        }
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            SegmentedButton(
                                selected = appLanguage == "es",
                                onClick = { viewModel.setAppLanguage("es") },
                                shape = SegmentedButtonDefaults.itemShape(0, 2),
                                label = { Text("Español") }
                            )
                            SegmentedButton(
                                selected = appLanguage == "en",
                                onClick = { viewModel.setAppLanguage("en") },
                                shape = SegmentedButtonDefaults.itemShape(1, 2),
                                label = { Text("English") }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = if (darkroomMode) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(strings.darkroomMode, style = MaterialTheme.typography.titleSmall)
                        }
                        Switch(
                            checked = darkroomMode,
                            onCheckedChange = { viewModel.toggleDarkroomMode() },
                            colors = SwitchDefaults.colors(checkedTrackColor = BolpikGreenDark)
                        )
                    }

                    NavigationDrawerItem(
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            showAboutDialog = true
                        },
                        icon = { Icon(Icons.Filled.Info, contentDescription = null) },
                        label = { Text(strings.aboutBolpik) },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
            }
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val useNavRail = maxWidth >= 600.dp

                Scaffold(
                    containerColor = MaterialTheme.colorScheme.background,
                    contentWindowInsets = if (showChrome) WindowInsets.statusBars else WindowInsets(0, 0, 0, 0)
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        Row(modifier = Modifier.fillMaxSize()) {
                            if (showChrome && useNavRail) {
                                NavigationRail(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ) {
                                    topLevelDestinations.forEach { dest ->
                                        val selected = currentDestination?.hierarchy?.any { it.route == dest.route } == true
                                        val label = dest.labelSelector(strings)
                                        NavigationRailItem(
                                            selected = selected,
                                            onClick = { navigateTopLevel(dest.route) },
                                            icon = {
                                                Icon(
                                                    imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                                    contentDescription = label
                                                )
                                            },
                                            label = { Text(label) },
                                            colors = NavigationRailItemDefaults.colors(
                                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                            )
                                        )
                                    }
                                }
                            }

                            NavHost(
                                navController = navController,
                                startDestination = BolpikRoutes.HOME,
                                modifier = Modifier.weight(1f)
                            ) {
                                composable(BolpikRoutes.HOME) {
                                    HomeScreen(
                                        darkroomMode = darkroomMode,
                                        appLanguage = appLanguage,
                                        projects = projects,
                                        onOpenDrawer = { scope.launch { drawerState.open() } },
                                        onToggleTheme = viewModel::toggleDarkroomMode,
                                        onToggleLanguage = viewModel::toggleLanguage,
                                        onOpenUriInEditor = { uri, tab ->
                                            viewModel.openUriInEditor(uri, initialTab = tab) {
                                                navController.navigate(BolpikRoutes.EDITOR)
                                            }
                                        },
                                        onOpenProjectInEditor = { project, tab ->
                                            viewModel.openProjectInEditor(project, tab) {
                                                navController.navigate(BolpikRoutes.EDITOR)
                                            }
                                        },
                                        onNavigateToProjects = { navigateTopLevel(BolpikRoutes.PROJECTS) },
                                        onNavigateToCollage = { navigateTopLevel(BolpikRoutes.COLLAGE) },
                                        onNavigateToPresets = { navigateTopLevel(BolpikRoutes.PRESETS) }
                                    )
                                }

                                composable(BolpikRoutes.PROJECTS) {
                                    ProjectsScreen(
                                        projects = projects,
                                        viewModel = viewModel,
                                        onOpenProject = { project ->
                                            viewModel.openProjectInEditor(project) {
                                                navController.navigate(BolpikRoutes.EDITOR)
                                            }
                                        },
                                        onExportProject = { project ->
                                            viewModel.prepareExportForProject(project) {
                                                navController.navigate(BolpikRoutes.EXPORT_STUDIO)
                                            }
                                        },
                                        onImportNewPhoto = {
                                            quickImportPicker.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        }
                                    )
                                }

                                composable(BolpikRoutes.PRESETS) {
                                    PresetsScreen(
                                        state = presetsState,
                                        customPresets = customPresets,
                                        viewModel = viewModel,
                                        onApplyInEditor = { recipe ->
                                            viewModel.openPresetsPreviewInEditor(recipe) {
                                                navController.navigate(BolpikRoutes.EDITOR)
                                            }
                                        }
                                    )
                                }

                                composable(BolpikRoutes.COLLAGE) {
                                    CollageScreen(
                                        state = collageState,
                                        viewModel = viewModel,
                                        onOpenInEditor = {
                                            navController.navigate(BolpikRoutes.EDITOR)
                                        }
                                    )
                                }

                                composable(BolpikRoutes.EDITOR) {
                                    EditorScreen(
                                        state = editorState,
                                        viewModel = viewModel,
                                        onBack = {
                                            viewModel.flushAutoSaveNow()
                                            navController.popBackStack()
                                        },
                                        onExportClick = {
                                            viewModel.flushAutoSaveNow()
                                            viewModel.prepareExportPreview()
                                            navController.navigate(BolpikRoutes.EXPORT_STUDIO)
                                        }
                                    )
                                }

                                composable(BolpikRoutes.EXPORT_STUDIO) {
                                    ExportStudioScreen(
                                        state = exportState,
                                        viewModel = viewModel,
                                        onBack = { navController.popBackStack() },
                                        onDoneToGallery = {
                                            navController.popBackStack(BolpikRoutes.HOME, inclusive = false)
                                            navigateTopLevel(BolpikRoutes.PROJECTS)
                                        }
                                    )
                                }
                            }
                        }

                        // Floating Capsule Navigation Dock on Compact Screens
                        if (showChrome && !useNavRail) {
                            FloatingBolpikNavDock(
                                destinations = topLevelDestinations,
                                strings = strings,
                                currentDestination = currentDestination,
                                onNavigate = ::navigateTopLevel,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .navigationBarsPadding()
                                    .padding(horizontal = 14.dp, vertical = 12.dp)
                            )
                        }
                    }
                }
            }
        }

        if (showAboutDialog) {
            AlertDialog(
                onDismissRequest = { showAboutDialog = false },
                title = { Text(strings.aboutBolpik) },
                text = { Text(strings.aboutBody) },
                confirmButton = {
                    TextButton(onClick = { showAboutDialog = false }) {
                        Text(strings.close)
                    }
                }
            )
        }
    }
}

@Composable
private fun FloatingBolpikNavDock(
    destinations: List<TopLevelDestination>,
    strings: BolpikStrings,
    currentDestination: androidx.navigation.NavDestination?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        shape = CircleShape,
        shadowElevation = 10.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            destinations.forEach { dest ->
                val selected = currentDestination?.hierarchy?.any { it.route == dest.route } == true
                val label = dest.labelSelector(strings)
                Surface(
                    color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    shape = CircleShape,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { onNavigate(dest.route) }
                        .testTag("nav_${dest.route}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = if (selected) 13.dp else 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                            contentDescription = label,
                            tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        AnimatedVisibility(visible = selected) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}
