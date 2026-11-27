package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FilterNone
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoFilter
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.i18n.BolpikStrings
import com.example.i18n.LocalBolpikStrings
import com.example.model.EditorToolTab
import com.example.ui.components.AdjustmentsDeck
import com.example.ui.components.CropTransformDeck
import com.example.ui.components.DrawDeck
import com.example.ui.components.EditorCanvas
import com.example.ui.components.FiltersDeck
import com.example.ui.components.FramesDeck
import com.example.ui.components.StickersDeck
import com.example.ui.components.TextDeck
import com.example.ui.theme.BolpikGreen
import com.example.ui.theme.BolpikTeal
import com.example.viewmodel.BolpikViewModel
import com.example.viewmodel.EditorUiState

@Composable
fun EditorScreen(
    state: EditorUiState,
    viewModel: BolpikViewModel,
    onBack: () -> Unit,
    onExportClick: () -> Unit
) {
    BackHandler { onBack() }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.flushAutoSaveNow()
        }
    }

    val strings = LocalBolpikStrings.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showSavePresetDialog by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var overflowExpanded by remember { mutableStateOf(false) }
    var panelCollapsed by remember { mutableStateOf(false) }

    LaunchedEffect(state.statusMessage) {
        state.statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    Scaffold(
        containerColor = Color(0xFF070C09),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val isWideSplit = maxWidth >= 650.dp

            if (isWideSplit) {
                // Split-Screen Side Panel Layout for Tablets / Landscape
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        EditorCanvasStage(
                            state = state,
                            viewModel = viewModel,
                            onBack = onBack,
                            onOpenOverflow = { overflowExpanded = true },
                            overflowExpanded = overflowExpanded,
                            onDismissOverflow = { overflowExpanded = false },
                            onSavePresetClick = { showSavePresetDialog = true },
                            onResetClick = { showResetConfirmDialog = true },
                            onInfoClick = { showInfoDialog = true },
                            onExportClick = onExportClick
                        )
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .width(340.dp)
                            .fillMaxHeight()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .statusBarsPadding()
                                .navigationBarsPadding()
                                .verticalScroll(rememberScrollState())
                                .padding(vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            EditorToolTabStrip(
                                activeTab = state.activeToolTab,
                                onSelectTab = viewModel::selectToolTab
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                            ActiveToolDeckContent(state = state, viewModel = viewModel)
                        }
                    }
                }
            } else {
                // Phone Layout: Full Canvas + Floating Top Toolbar + Collapsible Floating Control Panel
                Box(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            EditorCanvasStage(
                                state = state,
                                viewModel = viewModel,
                                onBack = onBack,
                                onOpenOverflow = { overflowExpanded = true },
                                overflowExpanded = overflowExpanded,
                                onDismissOverflow = { overflowExpanded = false },
                                onSavePresetClick = { showSavePresetDialog = true },
                                onResetClick = { showResetConfirmDialog = true },
                                onInfoClick = { showInfoDialog = true },
                                onExportClick = onExportClick
                            )
                        }

                        // Collapsible Floating Studio Panel
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .navigationBarsPadding()
                                    .padding(top = 4.dp, bottom = 8.dp)
                            ) {
                                // Collapse / Expand Handle Bar
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { panelCollapsed = !panelCollapsed }
                                        .padding(horizontal = 16.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = localizedToolLabel(state.activeToolTab, strings).uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Icon(
                                        imageVector = if (panelCollapsed) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                AnimatedVisibility(visible = !panelCollapsed) {
                                    ActiveToolDeckContent(state = state, viewModel = viewModel)
                                }

                                EditorToolTabStrip(
                                    activeTab = state.activeToolTab,
                                    onSelectTab = {
                                        panelCollapsed = false
                                        viewModel.selectToolTab(it)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSavePresetDialog) {
        var presetName by remember { mutableStateOf("") }
        val isEn = strings.navHome == "Home"
        AlertDialog(
            onDismissRequest = { showSavePresetDialog = false },
            title = { Text(strings.saveAsPreset) },
            text = {
                OutlinedTextField(
                    value = presetName,
                    onValueChange = { presetName = it },
                    label = { Text(if (isEn) "Preset name" else "Nombre del preset") },
                    placeholder = { Text(if (isEn) "E.g.: Emerald Night" else "Ej: Esmeralda Nocturna") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveCurrentLookAsPreset(presetName)
                        showSavePresetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(strings.save, color = MaterialTheme.colorScheme.onPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSavePresetDialog = false }) { Text(strings.cancel) }
            }
        )
    }

    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = { Text(strings.resetConfirmTitle) },
            text = { Text(strings.resetConfirmBody) },
            confirmButton = {
                Button(
                    onClick = {
                        showResetConfirmDialog = false
                        viewModel.resetAllEdits()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(strings.resetAll, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) { Text(strings.cancel) }
            }
        )
    }

    if (showInfoDialog) {
        val bmp = state.workingBitmap
        val isEn = strings.navHome == "Home"
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = { Text(strings.canvasInfo) },
            text = {
                Text(
                    if (isEn) {
                        "Project: ${state.projectTitle}\n" +
                            "Current resolution: ${bmp?.width ?: 0} × ${bmp?.height ?: 0} px\n" +
                            "Active filter: ${state.activeFilterId}\n" +
                            "Text layers: ${state.textLayers.size}\n" +
                            "Stickers: ${state.stickerLayers.size}\n" +
                            "Brush strokes: ${state.strokes.size}\n" +
                            "Status: ${strings.autoSavedLabel}"
                    } else {
                        "Proyecto: ${state.projectTitle}\n" +
                            "Resolución actual: ${bmp?.width ?: 0} × ${bmp?.height ?: 0} px\n" +
                            "Filtro activo: ${state.activeFilterId}\n" +
                            "Capas de texto: ${state.textLayers.size}\n" +
                            "Stickers: ${state.stickerLayers.size}\n" +
                            "Trazos de pincel: ${state.strokes.size}\n" +
                            "Estado: ${strings.autoSavedLabel}"
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) { Text(strings.close) }
            }
        )
    }
}

@Composable
private fun EditorCanvasStage(
    state: EditorUiState,
    viewModel: BolpikViewModel,
    onBack: () -> Unit,
    onOpenOverflow: () -> Unit,
    overflowExpanded: Boolean,
    onDismissOverflow: () -> Unit,
    onSavePresetClick: () -> Unit,
    onResetClick: () -> Unit,
    onInfoClick: () -> Unit,
    onExportClick: () -> Unit
) {
    val strings = LocalBolpikStrings.current
    Box(modifier = Modifier.fillMaxSize()) {
        if (state.workingBitmap != null) {
            EditorCanvas(
                state = state,
                onSplitPositionChange = viewModel::updateSplitPosition,
                onCropRectChange = viewModel::updateCropRect,
                onStrokeCompleted = viewModel::addDrawStroke,
                onSelectText = viewModel::selectTextLayer,
                onTransformText = viewModel::updateTextLayerTransform,
                onEndTransformGesture = viewModel::commitContinuousEdit,
                onSelectSticker = viewModel::selectStickerLayer,
                onTransformSticker = viewModel::updateStickerTransform
            )
        }

        // Floating Translucent Top Studio Bar
        Surface(
            color = Color(0xCC101814),
            shape = CircleShape,
            shadowElevation = 6.dp,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("editor_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                    IconButton(
                        onClick = viewModel::undo,
                        enabled = state.canUndo,
                        modifier = Modifier.testTag("editor_undo_button")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Deshacer",
                            tint = if (state.canUndo) Color.White else Color.White.copy(alpha = 0.28f)
                        )
                    }
                    IconButton(
                        onClick = viewModel::redo,
                        enabled = state.canRedo,
                        modifier = Modifier.testTag("editor_redo_button")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Redo,
                            contentDescription = "Rehacer",
                            tint = if (state.canRedo) Color.White else Color.White.copy(alpha = 0.28f)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(
                        onClick = viewModel::toggleCompareMode,
                        modifier = Modifier.testTag("editor_compare_button")
                    ) {
                        Icon(
                            Icons.Filled.Compare,
                            contentDescription = "Comparar antes y después",
                            tint = if (state.isComparing) BolpikTeal else Color.White
                        )
                    }

                    Box {
                        IconButton(onClick = onOpenOverflow) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "Más opciones", tint = Color.White)
                        }
                        DropdownMenu(
                            expanded = overflowExpanded,
                            onDismissRequest = onDismissOverflow
                        ) {
                            DropdownMenuItem(
                                text = { Text(strings.saveAsPreset) },
                                leadingIcon = { Icon(Icons.Filled.BookmarkAdd, contentDescription = null) },
                                onClick = {
                                    onDismissOverflow()
                                    onSavePresetClick()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(strings.canvasInfo) },
                                leadingIcon = { Icon(Icons.Filled.Info, contentDescription = null) },
                                onClick = {
                                    onDismissOverflow()
                                    onInfoClick()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(strings.resetAll) },
                                leadingIcon = { Icon(Icons.Filled.RestartAlt, contentDescription = null) },
                                onClick = {
                                    onDismissOverflow()
                                    onResetClick()
                                }
                            )
                        }
                    }

                    Button(
                        onClick = onExportClick,
                        enabled = state.workingBitmap != null && !state.isBusy,
                        colors = ButtonDefaults.buttonColors(containerColor = BolpikGreen),
                        shape = CircleShape,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        modifier = Modifier
                            .height(38.dp)
                            .testTag("editor_export_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.FileDownload,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFF041F12)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            strings.exportAction,
                            color = Color(0xFF041F12),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }

        if (state.activeProjectId != null) {
            Surface(
                color = Color(0xAA101814),
                shape = CircleShape,
                border = BorderStroke(1.dp, BolpikGreen.copy(alpha = 0.3f)),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(BolpikGreen)
                    )
                    Text(
                        text = "${state.projectTitle} • ${strings.autoSavedLabel}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
        }

        if (state.isBusy) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = BolpikGreen)
            }
        }
    }
}

@Composable
private fun ActiveToolDeckContent(
    state: EditorUiState,
    viewModel: BolpikViewModel
) {
    when (state.activeToolTab) {
        EditorToolTab.ADJUST -> AdjustmentsDeck(
            state = state,
            onSelectParam = viewModel::selectAdjustmentParam,
            onValueChange = viewModel::updateAdjustmentValue,
            onValueChangeFinished = viewModel::commitContinuousEdit,
            onResetParam = viewModel::resetCurrentAdjustmentParam
        )
        EditorToolTab.FILTERS -> FiltersDeck(
            state = state,
            onSelectFilter = viewModel::selectFilter,
            onIntensityChange = viewModel::updateFilterIntensity,
            onIntensityChangeFinished = viewModel::commitContinuousEdit
        )
        EditorToolTab.CROP -> CropTransformDeck(
            state = state,
            onSelectAspect = viewModel::selectCropAspect,
            onRotate90 = viewModel::rotate90,
            onStraightenChange = viewModel::updateStraighten,
            onFlipH = viewModel::toggleFlipHorizontal,
            onFlipV = viewModel::toggleFlipVertical,
            onResetCrop = viewModel::resetCropAndTransform,
            onApplyCrop = viewModel::applyCropAndTransform
        )
        EditorToolTab.DRAW -> DrawDeck(
            state = state,
            onBrushTypeChange = viewModel::setBrushType,
            onColorChange = viewModel::setBrushColor,
            onSizeChange = viewModel::setBrushSizeNorm,
            onOpacityChange = viewModel::setBrushOpacity,
            onClearStrokes = viewModel::clearAllStrokes
        )
        EditorToolTab.TEXT -> TextDeck(
            state = state,
            onAddText = viewModel::addTextLayer,
            onEditText = viewModel::updateTextLayerStyle,
            onSelectText = viewModel::selectTextLayer,
            onDeleteText = viewModel::deleteTextLayer
        )
        EditorToolTab.STICKERS -> StickersDeck(
            state = state,
            onAddSticker = viewModel::addStickerLayer,
            onDeleteSelected = viewModel::deleteStickerLayer
        )
        EditorToolTab.FRAME -> FramesDeck(
            state = state,
            onSelectFrame = viewModel::selectFrameStyle,
            onThicknessChange = viewModel::updateFrameThickness,
            onThicknessChangeFinished = viewModel::commitContinuousEdit,
            onColorChange = viewModel::updateFrameColor
        )
    }
}

@Composable
private fun EditorToolTabStrip(
    activeTab: EditorToolTab,
    onSelectTab: (EditorToolTab) -> Unit
) {
    val strings = LocalBolpikStrings.current
    LazyRow(
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(EditorToolTab.entries) { tab ->
            val selected = activeTab == tab
            val icon = iconForToolTab(tab)
            val label = localizedToolLabel(tab, strings)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (selected) MaterialTheme.colorScheme.primaryContainer
                        else Color.Transparent
                    )
                    .clickable { onSelectTab(tab) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .testTag("editor_tab_${tab.name.lowercase()}")
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun localizedToolLabel(tab: EditorToolTab, strings: BolpikStrings): String = when (tab) {
    EditorToolTab.ADJUST -> strings.toolAdjust
    EditorToolTab.FILTERS -> strings.toolFilters
    EditorToolTab.CROP -> strings.toolCrop
    EditorToolTab.DRAW -> strings.toolDraw
    EditorToolTab.TEXT -> strings.toolText
    EditorToolTab.STICKERS -> strings.toolStickers
    EditorToolTab.FRAME -> strings.toolFrame
}

private fun iconForToolTab(tab: EditorToolTab): ImageVector = when (tab) {
    EditorToolTab.ADJUST -> Icons.Filled.Tune
    EditorToolTab.FILTERS -> Icons.Filled.PhotoFilter
    EditorToolTab.CROP -> Icons.Filled.Crop
    EditorToolTab.DRAW -> Icons.Filled.Brush
    EditorToolTab.TEXT -> Icons.Filled.TextFields
    EditorToolTab.STICKERS -> Icons.Filled.EmojiEmotions
    EditorToolTab.FRAME -> Icons.Filled.FilterNone
}
