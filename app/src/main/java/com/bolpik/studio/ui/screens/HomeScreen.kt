package com.bolpik.studio.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FilterVintage
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PhotoFilter
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.bolpik.studio.R
import com.bolpik.studio.i18n.LocalBolpikStrings
import com.bolpik.studio.model.BolpikProject
import com.bolpik.studio.model.EditorToolTab
import com.bolpik.studio.ui.theme.BolpikGreen
import com.bolpik.studio.ui.theme.BolpikLime
import com.bolpik.studio.ui.theme.BolpikTeal
import java.io.File

private data class QuickToolCard(
    val title: String,
    val subtitle: String,
    val tab: EditorToolTab,
    val icon: ImageVector,
    val accent: Color
)

@Composable
fun HomeScreen(
    darkroomMode: Boolean,
    appLanguage: String,
    projects: List<BolpikProject>,
    onOpenDrawer: () -> Unit,
    onToggleTheme: () -> Unit,
    onToggleLanguage: () -> Unit,
    onOpenUriInEditor: (Uri, EditorToolTab) -> Unit,
    onOpenProjectInEditor: (BolpikProject, EditorToolTab) -> Unit,
    onNavigateToProjects: () -> Unit,
    onNavigateToCollage: () -> Unit,
    onNavigateToPresets: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalBolpikStrings.current
    val context = LocalContext.current

    var pendingToolTab by remember { mutableStateOf<EditorToolTab?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var fabExpanded by remember { mutableStateOf(false) }

    val galleryPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        val tab = pendingToolTab ?: EditorToolTab.ADJUST
        pendingToolTab = null
        if (uri != null) {
            onOpenUriInEditor(uri, tab)
        }
    }

    val quickTools = remember(strings) {
        listOf(
            QuickToolCard(strings.toolAdjust, "Luz / Light", EditorToolTab.ADJUST, Icons.Filled.Tune, BolpikGreen),
            QuickToolCard(strings.toolFilters, "12 LUTs", EditorToolTab.FILTERS, Icons.Filled.PhotoFilter, BolpikLime),
            QuickToolCard(strings.toolCrop, "Ratio & 90°", EditorToolTab.CROP, Icons.Filled.Crop, BolpikTeal),
            QuickToolCard(strings.toolDraw, "Neón / Glow", EditorToolTab.DRAW, Icons.Filled.Brush, Color(0xFF4ADE80)),
            QuickToolCard(strings.toolText, "Studio Type", EditorToolTab.TEXT, Icons.Filled.TextFields, Color(0xFF34D399)),
            QuickToolCard(strings.toolFrame, "Polaroid", EditorToolTab.FRAME, Icons.Filled.Style, Color(0xFFA3E635))
        )
    }

    val filteredTools = remember(searchQuery, quickTools) {
        if (searchQuery.isBlank()) quickTools
        else quickTools.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                it.subtitle.contains(searchQuery, ignoreCase = true)
        }
    }
    val filteredRecentProjects = remember(searchQuery, projects) {
        val base = if (searchQuery.isBlank()) projects else projects.filter {
            it.name.contains(searchQuery, ignoreCase = true)
        }
        base.take(5)
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        val useNavRail = maxWidth >= 600.dp
        val isWideBento = maxWidth >= 680.dp
        val bottomListPad = if (useNavRail) 28.dp else 110.dp
        val fabBottomPad = if (useNavRail) 20.dp else 86.dp

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 900.dp),
            contentPadding = PaddingValues(bottom = bottomListPad),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Top Brand, Language (ES/EN), and Theme Bar
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        IconButton(
                            onClick = onOpenDrawer,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .testTag("open_drawer_button")
                        ) {
                            Icon(Icons.Filled.Menu, contentDescription = "Menu")
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.appName,
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = strings.studioSubtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // English / Spanish Language Switcher Button
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = CircleShape,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable(onClick = onToggleLanguage)
                                .testTag("language_toggle_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Language,
                                    contentDescription = strings.languageLabel,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = if (appLanguage == "es") "ES" else "EN",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        IconButton(
                            onClick = onToggleTheme,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .testTag("theme_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (darkroomMode) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                                contentDescription = strings.darkroomMode,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Floating Search Bar
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = CircleShape,
                    shadowElevation = 4.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                strings.searchPlaceholder,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Filled.Close, contentDescription = strings.close)
                                }
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Asymmetric Bento Grid Hub (3-column on Tablets, 2-column on Phones)
            item {
                val heroCard: @Composable (Modifier) -> Unit = { cardMod ->
                    Box(
                        modifier = cardMod
                            .clip(RoundedCornerShape(24.dp))
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                                RoundedCornerShape(24.dp)
                            )
                            .clickable {
                                pendingToolTab = EditorToolTab.ADJUST
                                galleryPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                            .testTag("home_open_gallery_button")
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_hero_studio),
                            contentDescription = strings.openGalleryPhoto,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.matchParentSize()
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color(0x22090F0C),
                                            Color(0xD9090F0C),
                                            Color(0xF5090F0C)
                                        )
                                    )
                                )
                        )
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                color = BolpikGreen.copy(alpha = 0.22f),
                                shape = CircleShape,
                                border = BorderStroke(1.dp, BolpikGreen.copy(alpha = 0.7f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.AutoAwesome,
                                        contentDescription = null,
                                        tint = BolpikLime,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = strings.newProjectBadge,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Text(
                                text = strings.openGalleryPhoto,
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Filled.AddPhotoAlternate,
                                    contentDescription = null,
                                    tint = BolpikGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = strings.browseFiles,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = BolpikGreen,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                val projectsCard: @Composable (Modifier) -> Unit = { cardMod ->
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(22.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                        modifier = cardMod
                            .clip(RoundedCornerShape(22.dp))
                            .clickable(onClick = onNavigateToProjects)
                            .testTag("home_open_projects_button")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.FolderOpen,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Column {
                                Text(
                                    strings.projectsTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    "${projects.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }

                val collageCard: @Composable (Modifier) -> Unit = { cardMod ->
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(22.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)),
                        modifier = cardMod
                            .clip(RoundedCornerShape(22.dp))
                            .clickable(onClick = onNavigateToCollage)
                            .testTag("home_open_collage_button")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.GridView,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            }
                            Column {
                                Text(
                                    strings.collageCardTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    strings.collageCardSubtitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                if (isWideBento) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp)
                            .height(IntrinsicSize.Min)
                            .heightIn(min = 196.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        heroCard(Modifier.weight(1.4f).fillMaxHeight())
                        projectsCard(Modifier.weight(0.8f).fillMaxHeight())
                        collageCard(Modifier.weight(0.8f).fillMaxHeight())
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp)
                            .height(IntrinsicSize.Min)
                            .heightIn(min = 210.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        heroCard(Modifier.weight(1.22f).fillMaxHeight())
                        Column(
                            modifier = Modifier
                                .weight(0.95f)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            projectsCard(Modifier.weight(1f).fillMaxWidth())
                            collageCard(Modifier.weight(1f).fillMaxWidth())
                        }
                    }
                }
            }

            // Horizontal Carousel of Quick Tools
            if (filteredTools.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = strings.editingModules,
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = "${strings.navPresets} →",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable(onClick = onNavigateToPresets)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 18.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filteredTools) { tool ->
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(18.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                                    modifier = Modifier
                                        .width(146.dp)
                                        .clip(RoundedCornerShape(18.dp))
                                        .clickable { pendingToolTab = tool.tab }
                                        .testTag("quick_tool_${tool.tab.name.lowercase()}")
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(RoundedCornerShape(11.dp))
                                                .background(tool.accent.copy(alpha = 0.16f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = tool.icon,
                                                contentDescription = tool.title,
                                                tint = tool.accent,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Text(
                                            text = tool.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = tool.subtitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Recent Auto-Saved Projects Preview Strip (if user has projects)
            if (filteredRecentProjects.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = strings.projectsTitle,
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = "→",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable(onClick = onNavigateToProjects)
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 18.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filteredRecentProjects, key = { it.id }) { proj ->
                                val req = remember(proj.id, proj.updatedAtMillis) {
                                    ImageRequest.Builder(context)
                                        .data(File(proj.previewPath).takeIf { it.exists() } ?: File(proj.originalPath))
                                        .memoryCacheKey("home_${proj.id}_${proj.updatedAtMillis}")
                                        .build()
                                }
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                                    modifier = Modifier
                                        .width(136.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .clickable { onOpenProjectInEditor(proj, EditorToolTab.ADJUST) }
                                ) {
                                    Column {
                                        AsyncImage(
                                            model = req,
                                            contentDescription = proj.name,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(110.dp)
                                        )
                                        Text(
                                            text = proj.name,
                                            style = MaterialTheme.typography.labelLarge,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(10.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Expandable Speed-Dial FAB
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 18.dp, bottom = fabBottomPad),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AnimatedVisibility(
                visible = fabExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SpeedDialRow(
                        label = strings.speedDialGallery,
                        icon = Icons.Filled.AddPhotoAlternate,
                        onClick = {
                            fabExpanded = false
                            pendingToolTab = EditorToolTab.ADJUST
                            galleryPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    )
                    SpeedDialRow(
                        label = strings.speedDialCollage,
                        icon = Icons.Filled.GridView,
                        onClick = {
                            fabExpanded = false
                            onNavigateToCollage()
                        }
                    )
                    SpeedDialRow(
                        label = strings.presetsCardTitle,
                        icon = Icons.Filled.FilterVintage,
                        onClick = {
                            fabExpanded = false
                            onNavigateToPresets()
                        }
                    )
                }
            }

            FloatingActionButton(
                onClick = { fabExpanded = !fabExpanded },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.testTag("home_expandable_fab")
            ) {
                Icon(
                    imageVector = if (fabExpanded) Icons.Filled.Close else Icons.Filled.Add,
                    contentDescription = strings.newProjectBadge
                )
            }
        }
    }

    // Centered Quick-Pick Dialog (Gallery or Recent Project, without Studio Samples)
    val chosenTab = pendingToolTab
    if (chosenTab != null) {
        val localizedTabName = when (chosenTab) {
            EditorToolTab.ADJUST -> strings.toolAdjust
            EditorToolTab.FILTERS -> strings.toolFilters
            EditorToolTab.CROP -> strings.toolCrop
            EditorToolTab.DRAW -> strings.toolDraw
            EditorToolTab.TEXT -> strings.toolText
            EditorToolTab.STICKERS -> strings.toolStickers
            EditorToolTab.FRAME -> strings.toolFrame
        }
        AlertDialog(
            onDismissRequest = { pendingToolTab = null },
            title = { Text("${strings.quickPickTitlePrefix} $localizedTabName") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            galleryPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(strings.chooseFromGallery, color = MaterialTheme.colorScheme.onPrimary)
                    }

                    if (projects.isNotEmpty()) {
                        Text(
                            text = strings.chooseRecentProject,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        projects.take(3).forEach { proj ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        val tab = chosenTab
                                        pendingToolTab = null
                                        onOpenProjectInEditor(proj, tab)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    AsyncImage(
                                        model = File(proj.previewPath).takeIf { it.exists() } ?: File(proj.originalPath),
                                        contentDescription = proj.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                    Text(
                                        text = proj.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            text = strings.noRecentProjectsHint,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { pendingToolTab = null }) { Text(strings.close) }
            }
        )
    }
}

@Composable
private fun SpeedDialRow(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(10.dp),
            shadowElevation = 4.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
        SmallFloatingActionButton(
            onClick = onClick,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(icon, contentDescription = label)
        }
    }
}
