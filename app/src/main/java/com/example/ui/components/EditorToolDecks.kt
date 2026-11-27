package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.PresetsCatalog
import com.example.i18n.LocalBolpikStrings
import com.example.model.AdjustmentParam
import com.example.model.AdjustmentsState
import com.example.model.BrushType
import com.example.model.CropAspectRatio
import com.example.model.FrameStyle
import com.example.model.StickerKind
import com.example.model.TextFontStyle
import com.example.model.TextLayer
import com.example.ui.theme.BolpikGreen
import com.example.ui.theme.BolpikGreenDark
import com.example.ui.theme.BolpikLime
import com.example.ui.theme.BolpikTeal
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.PlusJakartaSansFamily
import com.example.ui.theme.SpaceGroteskFamily
import com.example.viewmodel.EditorUiState
import kotlin.math.roundToInt

private fun adjLabel(param: AdjustmentParam, isEn: Boolean): String = if (!isEn) param.label else when (param) {
    AdjustmentParam.EXPOSURE -> "Exposure"
    AdjustmentParam.BRIGHTNESS -> "Brightness"
    AdjustmentParam.CONTRAST -> "Contrast"
    AdjustmentParam.SATURATION -> "Saturation"
    AdjustmentParam.WARMTH -> "Warmth"
    AdjustmentParam.TINT -> "Tint"
    AdjustmentParam.FADE -> "Fade"
    AdjustmentParam.VIGNETTE -> "Vignette"
}

private fun brushLabel(type: BrushType, isEn: Boolean): String = if (!isEn) type.label else when (type) {
    BrushType.NORMAL -> "Brush"
    BrushType.NEON -> "Neon"
    BrushType.HIGHLIGHTER -> "Marker"
}

private fun frameLabel(style: FrameStyle, isEn: Boolean): String = if (!isEn) style.label else when (style) {
    FrameStyle.NONE -> "None"
    FrameStyle.MINIMAL -> "Gallery"
    FrameStyle.POLAROID -> "Polaroid"
    FrameStyle.FILM_STRIP -> "Film"
    FrameStyle.ROUNDED -> "Card"
}

private val studioColors = listOf(
    0xFFFFFFFFL,
    0xFF090F0CL,
    0xFF10E88AL,
    0xFFB8FF4DL,
    0xFF2DD4BFL,
    0xFFFF5A36L,
    0xFFFFB84DL,
    0xFF4CC9F0L,
    0xFFC855FFL
)

@Composable
fun AdjustmentsDeck(
    state: EditorUiState,
    onSelectParam: (AdjustmentParam) -> Unit,
    onValueChange: (AdjustmentParam, Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    onResetParam: () -> Unit
) {
    val isEn = LocalBolpikStrings.current.navHome == "Home"
    val param = state.selectedAdjParam
    val paramLabel = adjLabel(param, isEn)
    val currentValue = adjustmentValueFor(state.adjustments, param)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = paramLabel,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            // Precision Stepper + Numeric Readout + Reset
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                IconButton(
                    onClick = {
                        onValueChange(param, (currentValue - 2f).coerceIn(param.min, param.max))
                        onValueChangeFinished()
                    },
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Icon(Icons.Filled.Remove, contentDescription = "Reducir ${param.label}", modifier = Modifier.size(16.dp))
                }

                val formatted = if (currentValue > 0.5f) "+${currentValue.roundToInt()}" else "${currentValue.roundToInt()}"
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = formatted,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (currentValue != 0f) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                IconButton(
                    onClick = {
                        onValueChange(param, (currentValue + 2f).coerceIn(param.min, param.max))
                        onValueChangeFinished()
                    },
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Aumentar ${param.label}", modifier = Modifier.size(16.dp))
                }

                if (currentValue != 0f) {
                    IconButton(
                        onClick = onResetParam,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Restablecer ${param.label}",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        Slider(
            value = currentValue,
            onValueChange = { onValueChange(param, it) },
            onValueChangeFinished = onValueChangeFinished,
            valueRange = param.min..param.max,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.outline
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("adjustment_slider")
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 2.dp)
        ) {
            items(AdjustmentParam.entries) { item ->
                val active = item == param
                val modified = adjustmentValueFor(state.adjustments, item) != 0f
                FilterChip(
                    selected = active,
                    onClick = { onSelectParam(item) },
                    label = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text(adjLabel(item, isEn), style = MaterialTheme.typography.labelLarge)
                            if (modified) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary)
                                )
                            }
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }
    }
}

private fun adjustmentValueFor(adj: AdjustmentsState, param: AdjustmentParam): Float = when (param) {
    AdjustmentParam.EXPOSURE -> adj.exposure
    AdjustmentParam.BRIGHTNESS -> adj.brightness
    AdjustmentParam.CONTRAST -> adj.contrast
    AdjustmentParam.SATURATION -> adj.saturation
    AdjustmentParam.WARMTH -> adj.warmth
    AdjustmentParam.TINT -> adj.tint
    AdjustmentParam.FADE -> adj.fade
    AdjustmentParam.VIGNETTE -> adj.vignette
}

@Composable
fun FiltersDeck(
    state: EditorUiState,
    onSelectFilter: (String) -> Unit,
    onIntensityChange: (Float) -> Unit,
    onIntensityChangeFinished: () -> Unit
) {
    val isEn = LocalBolpikStrings.current.navHome == "Home"
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (state.activeFilterId != "original") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (isEn) "Intensity" else "Intensidad",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Slider(
                    value = state.filterIntensity,
                    onValueChange = onIntensityChange,
                    onValueChangeFinished = onIntensityChangeFinished,
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = BolpikLime,
                        activeTrackColor = BolpikLime
                    ),
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${(state.filterIntensity * 100).roundToInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(PresetsCatalog.filters, key = { it.id }) { filter ->
                val selected = state.activeFilterId == filter.id
                val thumb = state.filterThumbnails[filter.id]
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier
                        .width(70.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onSelectFilter(filter.id) }
                        .testTag("filter_item_${filter.id}")
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .border(
                                width = if (selected) 2.5.dp else 1.dp,
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .background(Color(filter.accentColorHex).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (thumb != null) {
                            Image(
                                bitmap = remember(thumb) { thumb.asImageBitmap() },
                                contentDescription = filter.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.matchParentSize()
                            )
                        }
                    }
                    Text(
                        text = filter.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun CropTransformDeck(
    state: EditorUiState,
    onSelectAspect: (CropAspectRatio) -> Unit,
    onRotate90: (Boolean) -> Unit,
    onStraightenChange: (Float) -> Unit,
    onFlipH: () -> Unit,
    onFlipV: () -> Unit,
    onResetCrop: () -> Unit,
    onApplyCrop: () -> Unit
) {
    val isEn = LocalBolpikStrings.current.navHome == "Home"
    val hasPending = !state.cropRect.isFull ||
        state.transform.rotationQuarterTurns != 0 ||
        state.transform.straightenDegrees != 0f ||
        state.transform.flipHorizontal ||
        state.transform.flipVertical

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(CropAspectRatio.entries) { aspect ->
                val selected = state.cropAspect == aspect
                val lbl = if (isEn && aspect == CropAspectRatio.FREE) "Free" else aspect.label
                FilterChip(
                    selected = selected,
                    onClick = { onSelectAspect(aspect) },
                    label = { Text(lbl, style = MaterialTheme.typography.labelLarge) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = if (isEn) "Straighten" else "Enderezar",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Slider(
                value = state.transform.straightenDegrees,
                onValueChange = onStraightenChange,
                valueRange = -45f..45f,
                colors = SliderDefaults.colors(
                    thumbColor = BolpikTeal,
                    activeTrackColor = BolpikTeal
                ),
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${state.transform.straightenDegrees.roundToInt()}°",
                style = MaterialTheme.typography.labelMedium,
                color = BolpikTeal
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { onRotate90(false) },
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Icon(Icons.AutoMirrored.Filled.RotateLeft, contentDescription = null)
            }
            IconButton(
                onClick = { onRotate90(true) },
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Icon(Icons.AutoMirrored.Filled.RotateRight, contentDescription = null)
            }
            IconButton(
                onClick = onFlipH,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (state.transform.flipHorizontal) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceContainerHigh
                    )
            ) {
                Icon(Icons.Filled.Flip, contentDescription = null)
            }
            OutlinedButton(
                onClick = onResetCrop,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(42.dp)
            ) {
                Text(if (isEn) "Reset" else "Limpiar")
            }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = onApplyCrop,
                enabled = hasPending && !state.isBusy,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .height(42.dp)
                    .testTag("apply_crop_button")
            ) {
                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(5.dp))
                Text(if (isEn) "Apply" else "Aplicar", color = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawDeck(
    state: EditorUiState,
    onBrushTypeChange: (BrushType) -> Unit,
    onColorChange: (Long) -> Unit,
    onSizeChange: (Float) -> Unit,
    onOpacityChange: (Float) -> Unit,
    onClearStrokes: () -> Unit
) {
    val isEn = LocalBolpikStrings.current.navHome == "Home"
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.weight(1f)) {
                BrushType.entries.forEachIndexed { idx, type ->
                    SegmentedButton(
                        selected = state.activeBrushType == type,
                        onClick = { onBrushTypeChange(type) },
                        shape = SegmentedButtonDefaults.itemShape(idx, BrushType.entries.size),
                        label = { Text(brushLabel(type, isEn), maxLines = 1, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
            if (state.strokes.isNotEmpty()) {
                IconButton(
                    onClick = onClearStrokes,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.16f))
                ) {
                    Icon(
                        Icons.Filled.DeleteOutline,
                        contentDescription = if (isEn) "Clear strokes" else "Limpiar trazos",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(if (isEn) "Size" else "Grosor", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Slider(
                value = state.activeBrushSizeNorm,
                onValueChange = onSizeChange,
                valueRange = 0.006f..0.055f,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.weight(1f)
            )
            Text(if (isEn) "Opacity" else "Opacidad", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Slider(
                value = state.activeBrushOpacity,
                onValueChange = onOpacityChange,
                valueRange = 0.2f..1f,
                colors = SliderDefaults.colors(thumbColor = BolpikTeal, activeTrackColor = BolpikTeal),
                modifier = Modifier.weight(1f)
            )
        }

        ColorSwatchRow(
            selectedColorArgb = state.activeBrushColor,
            onSelectColor = onColorChange
        )
    }
}

@Composable
fun TextDeck(
    state: EditorUiState,
    onAddText: (String, Long, Long?, TextFontStyle) -> Unit,
    onEditText: (String, String, Long, Long?, TextFontStyle) -> Unit,
    onSelectText: (String) -> Unit,
    onDeleteText: (String) -> Unit
) {
    val isEn = LocalBolpikStrings.current.navHome == "Home"
    var showFullScreenComposer by remember { mutableStateOf(false) }
    var editingLayer by remember { mutableStateOf<TextLayer?>(null) }

    val selectedLayer = state.textLayers.firstOrNull { it.id == state.selectedTextId }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    editingLayer = null
                    showFullScreenComposer = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .height(44.dp)
                    .testTag("add_text_layer_button")
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (isEn) "New text" else "Nuevo texto", color = MaterialTheme.colorScheme.onPrimary)
            }

            if (selectedLayer != null) {
                OutlinedButton(
                    onClick = {
                        editingLayer = selectedLayer
                        showFullScreenComposer = true
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(44.dp)
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (isEn) "Edit" else "Editar")
                }
                IconButton(
                    onClick = { onDeleteText(selectedLayer.id) },
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.16f))
                ) {
                    Icon(
                        Icons.Filled.DeleteOutline,
                        contentDescription = if (isEn) "Delete text" else "Eliminar texto",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        if (state.textLayers.isEmpty()) {
            Text(
                text = if (isEn) "Open the text composer and drag or rotate labels on the canvas." else "Abre el compositor tipográfico y mueve o gira los rótulos en el lienzo.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.textLayers, key = { it.id }) { layer ->
                    FilterChip(
                        selected = layer.id == state.selectedTextId,
                        onClick = { onSelectText(layer.id) },
                        label = {
                            Text(
                                text = layer.text,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    )
                }
            }
        }
    }

    if (showFullScreenComposer) {
        TextLayerFullScreenDialog(
            initialLayer = editingLayer,
            onDismiss = { showFullScreenComposer = false },
            onConfirm = { txt, col, bg, font ->
                val target = editingLayer
                if (target == null) {
                    onAddText(txt, col, bg, font)
                } else {
                    onEditText(target.id, txt, col, bg, font)
                }
                showFullScreenComposer = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TextLayerFullScreenDialog(
    initialLayer: TextLayer?,
    onDismiss: () -> Unit,
    onConfirm: (String, Long, Long?, TextFontStyle) -> Unit
) {
    val isEn = LocalBolpikStrings.current.navHome == "Home"
    var text by remember { mutableStateOf(initialLayer?.text ?: "") }
    var colorArgb by remember { mutableLongStateOf(initialLayer?.colorArgb ?: 0xFFFFFFFFL) }
    var usePillBg by remember { mutableStateOf(initialLayer?.backgroundArgb != null) }
    var bgArgb by remember { mutableLongStateOf(initialLayer?.backgroundArgb ?: 0xCC090F0CL) }
    var fontStyle by remember { mutableStateOf(initialLayer?.fontStyle ?: TextFontStyle.GROTESK) }

    val previewFamily: FontFamily = when (fontStyle) {
        TextFontStyle.GROTESK -> SpaceGroteskFamily
        TextFontStyle.SANS -> PlusJakartaSansFamily
        TextFontStyle.MONO -> JetBrainsMonoFamily
        TextFontStyle.SERIF -> FontFamily.Serif
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = null)
                    }
                    Text(
                        text = if (initialLayer == null) {
                            if (isEn) "Text Composer" else "Compositor de Texto"
                        } else {
                            if (isEn) "Edit Label" else "Editar Rótulo"
                        },
                        style = MaterialTheme.typography.titleLarge
                    )
                    Button(
                        onClick = {
                            onConfirm(text, colorArgb, if (usePillBg) bgArgb else null, fontStyle)
                        },
                        enabled = text.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = CircleShape
                    ) {
                        Text(if (isEn) "Apply" else "Aplicar", color = MaterialTheme.colorScheme.onPrimary)
                    }
                }

                // Live Preview Stage
                Surface(
                    color = Color(0xFF0A120E),
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        Box(
                            modifier = if (usePillBg) {
                                Modifier
                                    .background(Color(bgArgb), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 16.dp, vertical = 10.dp)
                            } else Modifier
                        ) {
                            Text(
                                text = text.ifBlank { if (isEn) "Write your text here" else "Escribe tu texto aquí" },
                                color = if (text.isBlank()) Color.White.copy(alpha = 0.35f) else Color(colorArgb),
                                fontFamily = previewFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 26.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text(if (isEn) "Label text" else "Contenido del rótulo") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(if (isEn) "Typography style" else "Estilo tipográfico", style = MaterialTheme.typography.labelLarge)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    TextFontStyle.entries.forEachIndexed { idx, style ->
                        SegmentedButton(
                            selected = fontStyle == style,
                            onClick = { fontStyle = style },
                            shape = SegmentedButtonDefaults.itemShape(idx, TextFontStyle.entries.size),
                            label = { Text(style.label, maxLines = 1, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Text(if (isEn) "Text color" else "Color de letra", style = MaterialTheme.typography.labelLarge)
                ColorSwatchRow(selectedColorArgb = colorArgb, onSelectColor = { colorArgb = it })

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Checkbox(checked = usePillBg, onCheckedChange = { usePillBg = it })
                    Text(if (isEn) "Label background" else "Fondo de etiqueta", style = MaterialTheme.typography.bodyMedium)
                }

                if (usePillBg) {
                    ColorSwatchRow(
                        selectedColorArgb = bgArgb,
                        colors = listOf(0xCC090F0CL, 0xFF00B86BL, 0xFFB8FF4DL, 0xFF2DD4BFL, 0xFFFFFFFFL),
                        onSelectColor = { bgArgb = it }
                    )
                }
            }
        }
    }
}

@Composable
fun StickersDeck(
    state: EditorUiState,
    onAddSticker: (StickerKind, Long) -> Unit,
    onDeleteSelected: (String) -> Unit
) {
    var chosenColor by remember { mutableLongStateOf(0xFF10E88AL) }
    val selectedId = state.selectedStickerId

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ColorSwatchRow(
                selectedColorArgb = chosenColor,
                onSelectColor = { chosenColor = it },
                modifier = Modifier.weight(1f)
            )
            if (selectedId != null) {
                IconButton(
                    onClick = { onDeleteSelected(selectedId) },
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.16f))
                ) {
                    Icon(
                        Icons.Filled.DeleteOutline,
                        contentDescription = "Eliminar sticker",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(StickerKind.entries) { kind ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onAddSticker(kind, chosenColor) }
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        StickerPreviewGlyph(
                            kind = kind,
                            color = Color(chosenColor),
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = kind.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FramesDeck(
    state: EditorUiState,
    onSelectFrame: (FrameStyle) -> Unit,
    onThicknessChange: (Float) -> Unit,
    onThicknessChangeFinished: () -> Unit,
    onColorChange: (Long) -> Unit
) {
    val isEn = LocalBolpikStrings.current.navHome == "Home"
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(FrameStyle.entries) { style ->
                FilterChip(
                    selected = state.frameConfig.style == style,
                    onClick = { onSelectFrame(style) },
                    label = { Text(frameLabel(style, isEn), style = MaterialTheme.typography.labelLarge) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }

        if (state.frameConfig.style != FrameStyle.NONE) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(if (isEn) "Width" else "Grosor", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Slider(
                    value = state.frameConfig.thicknessNorm,
                    onValueChange = onThicknessChange,
                    onValueChangeFinished = onThicknessChangeFinished,
                    valueRange = 0.02f..0.14f,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
            if (state.frameConfig.style != FrameStyle.FILM_STRIP) {
                ColorSwatchRow(
                    selectedColorArgb = state.frameConfig.colorArgb,
                    colors = listOf(0xFFF2F7F4L, 0xFF090F0CL, 0xFF17221CL, 0xFF10E88AL, 0xFFB8FF4DL, 0xFF2DD4BFL),
                    onSelectColor = onColorChange
                )
            }
        }
    }
}

@Composable
fun ColorSwatchRow(
    selectedColorArgb: Long,
    onSelectColor: (Long) -> Unit,
    modifier: Modifier = Modifier,
    colors: List<Long> = studioColors
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(colors) { colorLong ->
            val selected = selectedColorArgb == colorLong
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(colorLong))
                    .border(
                        width = if (selected) 3.dp else 1.dp,
                        color = if (selected) BolpikGreen else Color.White.copy(alpha = 0.28f),
                        shape = CircleShape
                    )
                    .clickable { onSelectColor(colorLong) }
            )
        }
    }
}
