package com.bolpik.studio.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bolpik.studio.engine.CropDragHandle
import com.bolpik.studio.engine.ImageProcessor
import com.bolpik.studio.i18n.LocalBolpikStrings
import com.bolpik.studio.model.BrushType
import com.bolpik.studio.model.CropRectNorm
import com.bolpik.studio.model.DrawStroke
import com.bolpik.studio.model.EditorToolTab
import com.bolpik.studio.model.FrameStyle
import com.bolpik.studio.model.NormPoint
import com.bolpik.studio.model.StickerKind
import com.bolpik.studio.model.StickerLayer
import com.bolpik.studio.model.TextFontStyle
import com.bolpik.studio.model.TextLayer
import com.bolpik.studio.ui.theme.BolpikCoral
import com.bolpik.studio.ui.theme.BolpikCyan
import com.bolpik.studio.ui.theme.JetBrainsMonoFamily
import com.bolpik.studio.ui.theme.PlusJakartaSansFamily
import com.bolpik.studio.ui.theme.SpaceGroteskFamily
import com.bolpik.studio.viewmodel.EditorUiState
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun EditorCanvas(
    state: EditorUiState,
    onSplitPositionChange: (Float) -> Unit,
    onCropRectChange: (CropRectNorm) -> Unit,
    onStrokeCompleted: (DrawStroke) -> Unit,
    onSelectText: (String?) -> Unit,
    onTransformText: (String, Float, Float, Float, Float) -> Unit,
    onEndTransformGesture: () -> Unit,
    onSelectSticker: (String?) -> Unit,
    onTransformSticker: (String, Float, Float, Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val bmp = state.workingBitmap ?: return
    val origBmp = state.originalBitmap ?: bmp

    val colorMatrixArray = remember(state.adjustments, state.activeFilterId, state.filterIntensity) {
        ImageProcessor.buildCombinedColorMatrixArray(
            state.adjustments,
            state.activeFilterId,
            state.filterIntensity
        )
    }
    val colorFilter = remember(colorMatrixArray) {
        ColorFilter.colorMatrix(ColorMatrix(colorMatrixArray))
    }

    var viewZoom by remember(bmp) { mutableFloatStateOf(1f) }
    var viewPanX by remember(bmp) { mutableFloatStateOf(0f) }
    var viewPanY by remember(bmp) { mutableFloatStateOf(0f) }

    val activeStrokePoints = remember { mutableStateListOf<NormPoint>() }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .background(Color(0xFF0A0A0E))
            .testTag("editor_canvas_container"),
        contentAlignment = Alignment.Center
    ) {
        val density = LocalDensity.current
        val availW = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val availH = constraints.maxHeight.toFloat().coerceAtLeast(1f)

        val padPx = with(density) { 16.dp.toPx() }
        val maxDrawW = (availW - padPx * 2).coerceAtLeast(100f)
        val maxDrawH = (availH - padPx * 2).coerceAtLeast(100f)

        val imgAspect = bmp.width.toFloat() / bmp.height.toFloat().coerceAtLeast(1f)
        val boxAspect = maxDrawW / maxDrawH

        val fitW: Float
        val fitH: Float
        if (imgAspect > boxAspect) {
            fitW = maxDrawW
            fitH = maxDrawW / imgAspect
        } else {
            fitH = maxDrawH
            fitW = maxDrawH * imgAspect
        }

        val framePadPx = if (state.frameConfig.style == FrameStyle.NONE) {
            0f
        } else {
            min(fitW, fitH) * state.frameConfig.thicknessNorm
        }
        val extraBottomFramePx = when (state.frameConfig.style) {
            FrameStyle.POLAROID -> framePadPx * 2.5f
            FrameStyle.FILM_STRIP -> framePadPx * 0.5f
            else -> 0f
        }

        val frameColor = if (state.frameConfig.style == FrameStyle.FILM_STRIP) {
            Color(0xFF101116)
        } else {
            Color(state.frameConfig.colorArgb)
        }

        // Allow pinch-zoom when NOT in Crop or Draw mode
        val allowCanvasZoom = state.activeToolTab != EditorToolTab.CROP &&
            state.activeToolTab != EditorToolTab.DRAW &&
            !state.isComparing

        val currentAllowZoom by rememberUpdatedState(allowCanvasZoom)
        val currentFitW by rememberUpdatedState(fitW)
        val currentFitH by rememberUpdatedState(fitH)
        val currentSplitPos by rememberUpdatedState(state.splitPosition)

        val imageBoxModifier = Modifier
            .size(
                width = with(density) { (fitW + framePadPx * 2).toDp() },
                height = with(density) { (fitH + framePadPx * 2 + extraBottomFramePx).toDp() }
            )
            .graphicsLayer {
                if (state.activeToolTab == EditorToolTab.CROP) {
                    rotationZ = state.transform.rotationQuarterTurns.toFloat() + state.transform.straightenDegrees
                    scaleX = if (state.transform.flipHorizontal) -1f else 1f
                    scaleY = if (state.transform.flipVertical) -1f else 1f
                } else {
                    scaleX = viewZoom
                    scaleY = viewZoom
                    translationX = viewPanX
                    translationY = viewPanY
                }
            }
            .then(
                if (allowCanvasZoom) {
                    Modifier.pointerInput(bmp) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val nextZoom = (viewZoom * zoom).coerceIn(1f, 4f)
                            viewZoom = nextZoom
                            if (nextZoom > 1.02f) {
                                viewPanX = (viewPanX + pan.x).coerceIn(-currentFitW * 0.6f, currentFitW * 0.6f)
                                viewPanY = (viewPanY + pan.y).coerceIn(-currentFitH * 0.6f, currentFitH * 0.6f)
                            } else {
                                viewPanX = 0f
                                viewPanY = 0f
                            }
                        }
                    }
                } else Modifier
            )
            .pointerInput(bmp) {
                detectTapGestures(
                    onTap = {
                        onSelectText(null)
                        onSelectSticker(null)
                    },
                    onDoubleTap = {
                        if (currentAllowZoom) {
                            if (viewZoom > 1.05f) {
                                viewZoom = 1f
                                viewPanX = 0f
                                viewPanY = 0f
                            } else {
                                viewZoom = 2.2f
                            }
                        }
                    }
                )
            }

        Box(
            modifier = imageBoxModifier
                .background(if (state.frameConfig.style != FrameStyle.NONE) frameColor else Color.Transparent),
            contentAlignment = Alignment.TopCenter
        ) {
            val contentClipShape = if (state.frameConfig.style == FrameStyle.ROUNDED) {
                RoundedCornerShape(with(density) { (min(fitW, fitH) * 0.045f).toDp() })
            } else {
                RoundedCornerShape(0.dp)
            }

            // Inner Photo Surface
            Box(
                modifier = Modifier
                    .padding(
                        start = with(density) { framePadPx.toDp() },
                        end = with(density) { framePadPx.toDp() },
                        top = with(density) { framePadPx.toDp() }
                    )
                    .size(
                        width = with(density) { fitW.toDp() },
                        height = with(density) { fitH.toDp() }
                    )
                    .clip(contentClipShape)
            ) {
                val workingImageBitmap = remember(bmp) { bmp.asImageBitmap() }
                val originalImageBitmap = remember(origBmp) { origBmp.asImageBitmap() }

                if (state.isComparing) {
                    // Left side: Original | Right side: Edited
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    val w = currentFitW.coerceAtLeast(1f)
                                    onSplitPositionChange(currentSplitPos + dragAmount.x / w)
                                }
                            }
                    ) {
                        val splitX = size.width * state.splitPosition
                        clipRect(left = 0f, top = 0f, right = splitX, bottom = size.height) {
                            drawImage(
                                image = originalImageBitmap,
                                dstSize = androidx.compose.ui.unit.IntSize(
                                    size.width.roundToInt(),
                                    size.height.roundToInt()
                                )
                            )
                        }
                        clipRect(left = splitX, top = 0f, right = size.width, bottom = size.height) {
                            drawImage(
                                image = workingImageBitmap,
                                dstSize = androidx.compose.ui.unit.IntSize(
                                    size.width.roundToInt(),
                                    size.height.roundToInt()
                                ),
                                colorFilter = colorFilter
                            )
                        }
                    }
                } else {
                    Image(
                        bitmap = workingImageBitmap,
                        contentDescription = "Foto editada en Bolpik",
                        contentScale = ContentScale.FillBounds,
                        colorFilter = colorFilter,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Vignette Preview Overlay
                if (!state.isComparing && state.adjustments.vignette > 0f) {
                    val strength = (state.adjustments.vignette / 100f).coerceIn(0f, 1f)
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val radius = kotlin.math.max(size.width, size.height) * 0.72f
                        drawRect(
                            brush = Brush.radialGradient(
                                0.45f to Color.Transparent,
                                0.75f to Color.Black.copy(alpha = strength * 0.24f),
                                1f to Color.Black.copy(alpha = strength * 0.84f),
                                center = Offset(size.width / 2f, size.height / 2f),
                                radius = radius
                            )
                        )
                    }
                }

                // Vector Strokes Preview + Active Drawing Input
                val drawModifier = if (state.activeToolTab == EditorToolTab.DRAW && !state.isComparing) {
                    Modifier.pointerInput(
                        state.activeBrushType,
                        state.activeBrushColor,
                        state.activeBrushSizeNorm,
                        state.activeBrushOpacity
                    ) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                activeStrokePoints.clear()
                                activeStrokePoints.add(
                                    NormPoint(
                                        (offset.x / fitW).coerceIn(0f, 1f),
                                        (offset.y / fitH).coerceIn(0f, 1f)
                                    )
                                )
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                activeStrokePoints.add(
                                    NormPoint(
                                        (change.position.x / fitW).coerceIn(0f, 1f),
                                        (change.position.y / fitH).coerceIn(0f, 1f)
                                    )
                                )
                            },
                            onDragEnd = {
                                if (activeStrokePoints.size >= 2) {
                                    onStrokeCompleted(
                                        DrawStroke(
                                            points = activeStrokePoints.toList(),
                                            colorArgb = state.activeBrushColor,
                                            strokeWidthNorm = state.activeBrushSizeNorm,
                                            opacity = state.activeBrushOpacity,
                                            brushType = state.activeBrushType
                                        )
                                    )
                                }
                                activeStrokePoints.clear()
                            },
                            onDragCancel = {
                                activeStrokePoints.clear()
                            }
                        )
                    }
                } else Modifier

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(drawModifier)
                ) {
                    if (!state.isComparing) {
                        val baseMin = min(size.width, size.height)
                        val allStrokes = if (activeStrokePoints.size >= 2) {
                            state.strokes + DrawStroke(
                                points = activeStrokePoints.toList(),
                                colorArgb = state.activeBrushColor,
                                strokeWidthNorm = state.activeBrushSizeNorm,
                                opacity = state.activeBrushOpacity,
                                brushType = state.activeBrushType
                            )
                        } else state.strokes

                        allStrokes.forEach { stroke ->
                            if (stroke.points.size < 2) return@forEach
                            val path = Path().apply {
                                val first = stroke.points.first()
                                moveTo(first.x * size.width, first.y * size.height)
                                for (i in 1 until stroke.points.size) {
                                    val p0 = stroke.points[i - 1]
                                    val p1 = stroke.points[i]
                                    quadraticTo(
                                        p0.x * size.width,
                                        p0.y * size.height,
                                        ((p0.x + p1.x) / 2f) * size.width,
                                        ((p0.y + p1.y) / 2f) * size.height
                                    )
                                }
                                val last = stroke.points.last()
                                lineTo(last.x * size.width, last.y * size.height)
                            }
                            val strokePx = (baseMin * stroke.strokeWidthNorm).coerceAtLeast(2f)
                            val cap = if (stroke.brushType == BrushType.HIGHLIGHTER) StrokeCap.Square else StrokeCap.Round
                            val c = Color(stroke.colorArgb)
                            if (stroke.brushType == BrushType.NEON) {
                                drawPath(
                                    path = path,
                                    color = c.copy(alpha = stroke.opacity * 0.45f),
                                    style = Stroke(width = strokePx * 2.1f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                                )
                                drawPath(
                                    path = path,
                                    color = Color.White.copy(alpha = stroke.opacity),
                                    style = Stroke(width = strokePx * 0.68f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                                )
                            } else {
                                val alpha = if (stroke.brushType == BrushType.HIGHLIGHTER) stroke.opacity * 0.48f else stroke.opacity
                                drawPath(
                                    path = path,
                                    color = c.copy(alpha = alpha),
                                    style = Stroke(width = strokePx, cap = cap, join = StrokeJoin.Round)
                                )
                            }
                        }
                    }
                }

                // Sticker & Text Interactive Overlays
                if (!state.isComparing && state.activeToolTab != EditorToolTab.CROP) {
                    val baseMinDp = with(density) { min(fitW, fitH).toDp() }
                    state.stickerLayers.forEach { sticker ->
                        val isSelected = state.selectedStickerId == sticker.id
                        StickerOverlayItem(
                            sticker = sticker,
                            parentW = fitW,
                            parentH = fitH,
                            baseMinDp = baseMinDp,
                            isSelected = isSelected,
                            onSelect = { onSelectSticker(sticker.id) },
                            onTransform = { dx, dy, scale, rot ->
                                onTransformSticker(sticker.id, dx, dy, scale, rot)
                            },
                            onEnd = onEndTransformGesture
                        )
                    }

                    state.textLayers.forEach { layer ->
                        val isSelected = state.selectedTextId == layer.id
                        TextOverlayItem(
                            layer = layer,
                            parentW = fitW,
                            parentH = fitH,
                            baseMinPx = min(fitW, fitH),
                            isSelected = isSelected,
                            onSelect = { onSelectText(layer.id) },
                            onTransform = { dx, dy, scale, rot ->
                                onTransformText(layer.id, dx, dy, scale, rot)
                            },
                            onEnd = onEndTransformGesture
                        )
                    }
                }

                // Interactive Crop Overlay
                if (state.activeToolTab == EditorToolTab.CROP && !state.isComparing) {
                    CropOverlayCanvas(
                        cropRect = state.cropRect,
                        lockedAspect = state.cropAspect.ratio,
                        imgAspect = imgAspect,
                        onCropChange = onCropRectChange
                    )
                }

                // Split Curtain Comparison Divider Handle
                if (state.isComparing) {
                    val dividerOffsetPx = (fitW * state.splitPosition).roundToInt()
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(dividerOffsetPx - 1, 0) }
                            .size(width = 2.dp, height = with(density) { fitH.toDp() })
                            .background(BolpikCoral)
                    )
                    Surface(
                        color = BolpikCoral,
                        shape = CircleShape,
                        shadowElevation = 6.dp,
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    dividerOffsetPx - with(density) { 18.dp.roundToPx() },
                                    (fitH / 2f).roundToInt() - with(density) { 18.dp.roundToPx() }
                                )
                            }
                            .size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.CompareArrows,
                                contentDescription = "Desliza para comparar",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Before / After Corner Tags
                    val isEn = LocalBolpikStrings.current.navHome == "Home"
                    Surface(
                        color = Color.Black.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = if (isEn) "BEFORE" else "ANTES",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                    Surface(
                        color = BolpikCoral.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = if (isEn) "AFTER" else "DESPUÉS",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            // Polaroid Caption Preview
            if (state.frameConfig.style == FrameStyle.POLAROID) {
                Text(
                    text = "BOLPIK • 35MM",
                    fontFamily = JetBrainsMonoFamily,
                    fontSize = 11.sp,
                    color = Color(0xFF252834),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = with(density) { (extraBottomFramePx * 0.25f).toDp() })
                )
            }
        }
    }
}

@Composable
private fun TextOverlayItem(
    layer: TextLayer,
    parentW: Float,
    parentH: Float,
    baseMinPx: Float,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onTransform: (Float, Float, Float, Float) -> Unit,
    onEnd: () -> Unit
) {
    val density = LocalDensity.current
    val fontSizeSp = with(density) { (baseMinPx * 0.072f * layer.scale).toSp() }
    val family: FontFamily = when (layer.fontStyle) {
        TextFontStyle.GROTESK -> SpaceGroteskFamily
        TextFontStyle.SANS -> PlusJakartaSansFamily
        TextFontStyle.MONO -> JetBrainsMonoFamily
        TextFontStyle.SERIF -> FontFamily.Serif
    }

    val currentParentW by rememberUpdatedState(parentW)
    val currentParentH by rememberUpdatedState(parentH)
    val currentOnEnd by rememberUpdatedState(onEnd)

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    (layer.xNorm * parentW).roundToInt(),
                    (layer.yNorm * parentH).roundToInt()
                )
            }
            .graphicsLayer {
                translationX = -size.width / 2f
                translationY = -size.height / 2f
                rotationZ = layer.rotationDeg
            }
            .then(
                if (isSelected) {
                    Modifier.border(1.5.dp, BolpikCyan, RoundedCornerShape(8.dp))
                } else Modifier
            )
            .pointerInput(layer.id) {
                detectTapGestures { onSelect() }
            }
            .pointerInput(layer.id) {
                detectTransformGestures { _, pan, zoom, rotation ->
                    onSelect()
                    onTransform(
                        pan.x / currentParentW.coerceAtLeast(1f),
                        pan.y / currentParentH.coerceAtLeast(1f),
                        zoom,
                        rotation
                    )
                }
            }
            .pointerInput(layer.id) {
                awaitPointerEventScope {
                    var wasPressed = false
                    while (true) {
                        val event = awaitPointerEvent()
                        val anyPressed = event.changes.any { it.pressed }
                        if (wasPressed && !anyPressed) {
                            currentOnEnd()
                        }
                        wasPressed = anyPressed
                    }
                }
            }
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .let { mod ->
                if (layer.backgroundArgb != null) {
                    mod.background(Color(layer.backgroundArgb), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                } else mod
            }
    ) {
        Text(
            text = layer.text,
            color = Color(layer.colorArgb),
            fontFamily = family,
            fontWeight = FontWeight.Bold,
            fontSize = fontSizeSp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun StickerOverlayItem(
    sticker: StickerLayer,
    parentW: Float,
    parentH: Float,
    baseMinDp: androidx.compose.ui.unit.Dp,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onTransform: (Float, Float, Float, Float) -> Unit,
    onEnd: () -> Unit
) {
    val sizeDp = baseMinDp * 0.16f * sticker.scale
    val currentParentW by rememberUpdatedState(parentW)
    val currentParentH by rememberUpdatedState(parentH)
    val currentOnEnd by rememberUpdatedState(onEnd)

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    (sticker.xNorm * parentW).roundToInt(),
                    (sticker.yNorm * parentH).roundToInt()
                )
            }
            .graphicsLayer {
                translationX = -size.width / 2f
                translationY = -size.height / 2f
                rotationZ = sticker.rotationDeg
            }
            .then(
                if (isSelected) {
                    Modifier.border(1.5.dp, BolpikCyan, RoundedCornerShape(8.dp))
                } else Modifier
            )
            .pointerInput(sticker.id) {
                detectTapGestures { onSelect() }
            }
            .pointerInput(sticker.id) {
                detectTransformGestures { _, pan, zoom, rotation ->
                    onSelect()
                    onTransform(
                        pan.x / currentParentW.coerceAtLeast(1f),
                        pan.y / currentParentH.coerceAtLeast(1f),
                        zoom,
                        rotation
                    )
                }
            }
            .pointerInput(sticker.id) {
                awaitPointerEventScope {
                    var wasPressed = false
                    while (true) {
                        val event = awaitPointerEvent()
                        val anyPressed = event.changes.any { it.pressed }
                        if (wasPressed && !anyPressed) {
                            currentOnEnd()
                        }
                        wasPressed = anyPressed
                    }
                }
            }
            .padding(6.dp),
        contentAlignment = Alignment.Center
    ) {
        StickerPreviewGlyph(
            kind = sticker.kind,
            color = Color(sticker.colorArgb),
            modifier = Modifier.size(sizeDp)
        )
    }
}

@Composable
fun StickerPreviewGlyph(
    kind: StickerKind,
    color: Color,
    modifier: Modifier = Modifier
) {
    when (kind) {
        StickerKind.BOLPIK_TAG, StickerKind.RAW_BADGE, StickerKind.FILM_35MM -> {
            Surface(
                color = color,
                shape = RoundedCornerShape(8.dp),
                modifier = modifier
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)) {
                    Text(
                        text = kind.label,
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
            }
        }
        StickerKind.REC_DOT -> {
            Surface(
                color = Color(0xCC0E0E12),
                shape = RoundedCornerShape(8.dp),
                modifier = modifier
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 6.dp)) {
                    Text(
                        text = "● REC",
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold,
                        color = color,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
            }
        }
        else -> {
            Canvas(modifier = modifier) {
                val r = min(size.width, size.height) * 0.45f
                val cx = size.width / 2f
                val cy = size.height / 2f
                when (kind) {
                    StickerKind.SPARKLE -> {
                        val path = Path().apply {
                            moveTo(cx, cy - r)
                            quadraticTo(cx, cy, cx + r, cy)
                            quadraticTo(cx, cy, cx, cy + r)
                            quadraticTo(cx, cy, cx - r, cy)
                            quadraticTo(cx, cy, cx, cy - r)
                            close()
                        }
                        drawPath(path, color = color)
                        drawCircle(Color.White, radius = r * 0.14f, center = Offset(cx, cy))
                    }
                    StickerKind.STAR -> {
                        val path = Path()
                        val inner = r * 0.42f
                        for (i in 0 until 10) {
                            val angle = Math.toRadians((i * 36.0) - 90.0)
                            val rad = if (i % 2 == 0) r else inner
                            val x = cx + (cos(angle) * rad).toFloat()
                            val y = cy + (sin(angle) * rad).toFloat()
                            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }
                        path.close()
                        drawPath(path, color = color)
                    }
                    StickerKind.HEART -> {
                        val path = Path().apply {
                            moveTo(cx, cy + r * 0.8f)
                            cubicTo(cx - r * 1.3f, cy + r * 0.05f, cx - r * 0.8f, cy - r, cx, cy - r * 0.35f)
                            cubicTo(cx + r * 0.8f, cy - r, cx + r * 1.3f, cy + r * 0.05f, cx, cy + r * 0.8f)
                            close()
                        }
                        drawPath(path, color = color)
                    }
                    StickerKind.CROSSHAIR -> {
                        val sw = r * 0.14f
                        drawCircle(color, radius = r * 0.65f, center = Offset(cx, cy), style = Stroke(sw))
                        drawLine(color, Offset(cx - r, cy), Offset(cx - r * 0.35f, cy), strokeWidth = sw)
                        drawLine(color, Offset(cx + r * 0.35f, cy), Offset(cx + r, cy), strokeWidth = sw)
                        drawLine(color, Offset(cx, cy - r), Offset(cx, cy - r * 0.35f), strokeWidth = sw)
                        drawLine(color, Offset(cx, cy + r * 0.35f), Offset(cx, cy + r), strokeWidth = sw)
                    }
                    else -> Unit
                }
            }
        }
    }
}

@Composable
private fun CropOverlayCanvas(
    cropRect: CropRectNorm,
    lockedAspect: Float?,
    imgAspect: Float,
    onCropChange: (CropRectNorm) -> Unit
) {
    var activeHandle by remember { mutableStateOf(CropDragHandle.NONE) }
    val currentCrop by rememberUpdatedState(cropRect)
    val currentLockedAspect by rememberUpdatedState(lockedAspect)
    val currentImgAspect by rememberUpdatedState(imgAspect)
    val currentOnCropChange by rememberUpdatedState(onCropChange)

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { start ->
                        val rect = currentCrop
                        val w = size.width.toFloat()
                        val h = size.height.toFloat()
                        val lx = rect.left * w
                        val rx = rect.right * w
                        val ty = rect.top * h
                        val by = rect.bottom * h
                        val hitRadius = 68f

                        activeHandle = when {
                            (start - Offset(lx, ty)).getDistance() < hitRadius -> CropDragHandle.TOP_LEFT
                            (start - Offset(rx, ty)).getDistance() < hitRadius -> CropDragHandle.TOP_RIGHT
                            (start - Offset(lx, by)).getDistance() < hitRadius -> CropDragHandle.BOTTOM_LEFT
                            (start - Offset(rx, by)).getDistance() < hitRadius -> CropDragHandle.BOTTOM_RIGHT
                            start.x in lx..rx && start.y in ty..by -> CropDragHandle.MOVE
                            else -> CropDragHandle.NONE
                        }
                    },
                    onDrag = { change, drag ->
                        change.consume()
                        val w = size.width.toFloat().coerceAtLeast(1f)
                        val h = size.height.toFloat().coerceAtLeast(1f)
                        val next = ImageProcessor.computeUpdatedCropRect(
                            current = currentCrop,
                            handle = activeHandle,
                            dxNorm = drag.x / w,
                            dyNorm = drag.y / h,
                            lockedAspect = currentLockedAspect,
                            imgAspect = currentImgAspect
                        )
                        currentOnCropChange(next)
                    },
                    onDragEnd = { activeHandle = CropDragHandle.NONE },
                    onDragCancel = { activeHandle = CropDragHandle.NONE }
                )
            }
    ) {
        val l = cropRect.left * size.width
        val t = cropRect.top * size.height
        val r = cropRect.right * size.width
        val b = cropRect.bottom * size.height
        val rect = Rect(l, t, r, b)

        // Darken outer area
        val scrim = Color.Black.copy(alpha = 0.58f)
        drawRect(scrim, Offset.Zero, Size(size.width, t))
        drawRect(scrim, Offset(0f, b), Size(size.width, size.height - b))
        drawRect(scrim, Offset(0f, t), Size(l, b - t))
        drawRect(scrim, Offset(r, t), Size(size.width - r, b - t))

        // Border & rule of thirds
        drawRect(
            color = Color.White,
            topLeft = Offset(l, t),
            size = Size(rect.width, rect.height),
            style = Stroke(width = 2.dp.toPx())
        )
        val gridColor = Color.White.copy(alpha = 0.38f)
        val thirdW = rect.width / 3f
        val thirdH = rect.height / 3f
        for (i in 1..2) {
            drawLine(gridColor, Offset(l + thirdW * i, t), Offset(l + thirdW * i, b), strokeWidth = 1.dp.toPx())
            drawLine(gridColor, Offset(l, t + thirdH * i), Offset(r, t + thirdH * i), strokeWidth = 1.dp.toPx())
        }

        // Corner Handles
        val handleR = 7.dp.toPx()
        listOf(
            Offset(l, t),
            Offset(r, t),
            Offset(l, b),
            Offset(r, b)
        ).forEach { corner ->
            drawCircle(BolpikCoral, radius = handleR, center = corner)
            drawCircle(Color.White, radius = handleR, center = corner, style = Stroke(2.dp.toPx()))
        }
    }
}
