package com.bolpik.studio.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.bolpik.studio.engine.ImageProcessor
import com.bolpik.studio.i18n.LocalBolpikStrings
import com.bolpik.studio.model.CollageTemplate
import com.bolpik.studio.ui.components.ColorSwatchRow
import com.bolpik.studio.ui.theme.BolpikGreen
import com.bolpik.studio.ui.theme.BolpikLime
import com.bolpik.studio.viewmodel.BolpikViewModel
import com.bolpik.studio.viewmodel.CollageUiState
import kotlinx.coroutines.launch
import kotlin.math.min
import kotlin.math.roundToInt

private val collageAspectOptions = listOf(
    "1:1" to 1f,
    "4:5" to (4f / 5f),
    "16:9" to (16f / 9f),
    "9:16" to (9f / 16f)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollageScreen(
    state: CollageUiState,
    viewModel: BolpikViewModel,
    onOpenInEditor: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalBolpikStrings.current
    val isEn = strings.navHome == "Home"
    var activeSlotIndex by remember { mutableIntStateOf(0) }
    var slotMenuIndex by remember { mutableStateOf<Int?>(null) }
    var selectedTab by remember { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val singleSlotPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.setCollageSlotUri(activeSlotIndex, uri)
        }
    }

    val multiPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 6)
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.setCollageMultipleUris(uris)
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val isWideSplit = maxWidth >= 650.dp

        val canvasSection: @Composable (Modifier) -> Unit = { mod ->
            Box(
                modifier = mod.padding(horizontal = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(state.aspectRatio.coerceIn(0.55f, 1.8f))
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(state.bgColorArgb))
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                            RoundedCornerShape(18.dp)
                        )
                ) {
                    val density = LocalDensity.current
                    val wPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
                    val hPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)
                    val rects = remember(state.template, wPx, hPx, state.spacingNorm) {
                        ImageProcessor.computeCollageRects(state.template, wPx, hPx, state.spacingNorm)
                    }
                    val cornerDp = with(density) { (min(wPx, hPx) * state.cornerRadiusNorm).toDp() }

                    rects.forEachIndexed { idx, rect ->
                        val slot = state.slots.getOrNull(idx)
                        val bmp = slot?.bitmap
                        Box(
                            modifier = Modifier
                                .offset { IntOffset(rect.left.roundToInt(), rect.top.roundToInt()) }
                                .size(
                                    width = with(density) { rect.width().toDp() },
                                    height = with(density) { rect.height().toDp() }
                                )
                                .clip(RoundedCornerShape(cornerDp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                .clickable {
                                    activeSlotIndex = idx
                                    slotMenuIndex = idx
                                }
                                .pointerInput(idx) {
                                    detectDragGestures { change, drag ->
                                        change.consume()
                                        viewModel.panCollageSlot(
                                            idx,
                                            drag.x / rect.width().coerceAtLeast(1f),
                                            drag.y / rect.height().coerceAtLeast(1f)
                                        )
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (bmp != null) {
                                Image(
                                    bitmap = remember(bmp) { bmp.asImageBitmap() },
                                    contentDescription = "Celda ${idx + 1}",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.AddPhotoAlternate,
                                        contentDescription = "Añadir foto",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = if (isEn) "Cell ${idx + 1}" else "Celda ${idx + 1}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Context Popup Menu for this Slot
                            DropdownMenu(
                                expanded = slotMenuIndex == idx,
                                onDismissRequest = { slotMenuIndex = null }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(strings.chooseFromGallery) },
                                    onClick = {
                                        slotMenuIndex = null
                                        activeSlotIndex = idx
                                        singleSlotPicker.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (isEn) "Center frame" else "Centrar encuadre") },
                                    onClick = {
                                        slotMenuIndex = null
                                        viewModel.panCollageSlot(idx, -(slot?.panX ?: 0f), -(slot?.panY ?: 0f))
                                    }
                                )
                            }
                        }
                    }

                    if (state.isBusy) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = BolpikGreen)
                        }
                    }
                }
            }
        }

        val inspectorSection: @Composable (Modifier) -> Unit = { mod ->
            Column(
                modifier = mod,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Tab Bar for Collage Controls
                PrimaryTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    listOf(strings.tabDesign, strings.tabStyle, strings.tabCells).forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title, style = MaterialTheme.typography.labelLarge) }
                        )
                    }
                }

                when (selectedTab) {
                    0 -> {
                        // Templates & Aspect Ratio
                        Column(
                            modifier = Modifier.padding(horizontal = 18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(CollageTemplate.entries) { tpl ->
                                    FilterChip(
                                        selected = state.template == tpl,
                                        onClick = { viewModel.setCollageTemplate(tpl) },
                                        label = { Text(tpl.label) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                        ),
                                        modifier = Modifier.testTag("collage_template_${tpl.name.lowercase()}")
                                    )
                                }
                            }
                            Text(strings.proportion, style = MaterialTheme.typography.labelLarge)
                            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                collageAspectOptions.forEachIndexed { idx, (label, ratio) ->
                                    SegmentedButton(
                                        selected = kotlin.math.abs(state.aspectRatio - ratio) < 0.02f,
                                        onClick = { viewModel.setCollageAspectRatio(ratio) },
                                        shape = SegmentedButtonDefaults.itemShape(idx, collageAspectOptions.size),
                                        label = { Text(label) }
                                    )
                                }
                            }
                        }
                    }
                    1 -> {
                        // Style: Spacing, Corners, Background
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(strings.spacing, style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(64.dp))
                                    Slider(
                                        value = state.spacingNorm,
                                        onValueChange = viewModel::setCollageSpacing,
                                        valueRange = 0f..0.06f,
                                        colors = SliderDefaults.colors(
                                            thumbColor = MaterialTheme.colorScheme.primary,
                                            activeTrackColor = MaterialTheme.colorScheme.primary
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(strings.borders, style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(64.dp))
                                    Slider(
                                        value = state.cornerRadiusNorm,
                                        onValueChange = viewModel::setCollageCornerRadius,
                                        valueRange = 0f..0.08f,
                                        colors = SliderDefaults.colors(thumbColor = BolpikLime, activeTrackColor = BolpikLime),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Text(strings.canvasBg, style = MaterialTheme.typography.bodySmall)
                                ColorSwatchRow(
                                    selectedColorArgb = state.bgColorArgb,
                                    colors = listOf(0xFF101814L, 0xFFF2F7F4L, 0xFF000000L, 0xFF10E88AL, 0xFFB8FF4DL, 0xFF2DD4BFL, 0xFF202E26L),
                                    onSelectColor = viewModel::setCollageBackgroundColor
                                )
                            }
                        }
                    }
                    2 -> {
                        // Cells Picker Row
                        Column(
                            modifier = Modifier.padding(horizontal = 18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    multiPicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("collage_multi_pick_button")
                            ) {
                                Icon(Icons.Filled.Collections, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    if (isEn) "Select up to ${state.template.slotCount} photos"
                                    else "Seleccionar hasta ${state.template.slotCount} fotos juntas"
                                )
                            }
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(state.template.slotCount) { idx ->
                                    FilterChip(
                                        selected = activeSlotIndex == idx,
                                        onClick = {
                                            activeSlotIndex = idx
                                            singleSlotPicker.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        },
                                        label = {
                                            Text(if (isEn) "Change Cell ${idx + 1}" else "Cambiar Celda ${idx + 1}")
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Floating Action Toolbar for Collage
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = CircleShape,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                viewModel.saveCollageToGallery { saved ->
                                    scope.launch {
                                        val msg = if (saved != null) strings.savedToGalleryBanner else "Error"
                                        snackbarHostState.showSnackbar(msg)
                                    }
                                }
                            },
                            enabled = !state.isBusy,
                            shape = CircleShape,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("collage_save_button")
                        ) {
                            Icon(Icons.Filled.SaveAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(strings.save)
                        }

                        Button(
                            onClick = { viewModel.renderCollageAndOpenInEditor(onOpenInEditor) },
                            enabled = !state.isBusy,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = CircleShape,
                            modifier = Modifier
                                .weight(1.25f)
                                .height(46.dp)
                                .testTag("collage_edit_studio_button")
                        ) {
                            Icon(Icons.Filled.AutoFixHigh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(strings.editInStudio, color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }
            }
        }

        if (isWideSplit) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                canvasSection(Modifier.weight(1f).fillMaxHeight())
                inspectorSection(
                    Modifier
                        .width(380.dp)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 104.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = strings.collageTitle,
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Text(
                            text = strings.collageSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                canvasSection(Modifier.fillMaxWidth())
                inspectorSection(Modifier.fillMaxWidth())
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 86.dp)
        )
    }
}
