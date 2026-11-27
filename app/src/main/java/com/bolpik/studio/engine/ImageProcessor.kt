package com.bolpik.studio.engine

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.DrawableRes
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.scale
import com.bolpik.studio.R
import com.bolpik.studio.data.PresetsCatalog
import com.bolpik.studio.model.AdjustmentsState
import com.bolpik.studio.model.BrushType
import com.bolpik.studio.model.CollageSlotItem
import com.bolpik.studio.model.CollageTemplate
import com.bolpik.studio.model.CropRectNorm
import com.bolpik.studio.model.DrawStroke
import com.bolpik.studio.model.ExportFormat
import com.bolpik.studio.model.FrameConfig
import com.bolpik.studio.model.FrameStyle
import com.bolpik.studio.model.SavedExportItem
import com.bolpik.studio.model.StickerKind
import com.bolpik.studio.model.StickerLayer
import com.bolpik.studio.model.TextFontStyle
import com.bolpik.studio.model.TextLayer
import com.bolpik.studio.model.TransformState
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

enum class CropDragHandle {
    NONE, MOVE, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
}

object ImageProcessor {

    private val typefaceCache = ConcurrentHashMap<TextFontStyle, Typeface>()

    private val identityMatrix = floatArrayOf(
        1f, 0f, 0f, 0f, 0f,
        0f, 1f, 0f, 0f, 0f,
        0f, 0f, 1f, 0f, 0f,
        0f, 0f, 0f, 1f, 0f
    )

    fun buildCombinedColorMatrixArray(
        adjustments: AdjustmentsState,
        filterId: String,
        filterIntensity: Float
    ): FloatArray {
        val result = ColorMatrix()

        // 1. Apply Filter LUT interpolated with identity
        val filter = PresetsCatalog.findFilter(filterId)
        val t = filterIntensity.coerceIn(0f, 1f)
        if (filter.id != "original" && t > 0.001f) {
            val blended = FloatArray(20) { i ->
                identityMatrix[i] * (1f - t) + filter.matrix[i] * t
            }
            result.postConcat(ColorMatrix(blended))
        }

        // 2. Exposure (-100..100 -> scale 0.45..1.65)
        if (adjustments.exposure != 0f) {
            val expScale = if (adjustments.exposure >= 0f) {
                1f + (adjustments.exposure / 100f) * 0.65f
            } else {
                1f + (adjustments.exposure / 100f) * 0.55f
            }
            result.postConcat(
                ColorMatrix(
                    floatArrayOf(
                        expScale, 0f, 0f, 0f, 0f,
                        0f, expScale, 0f, 0f, 0f,
                        0f, 0f, expScale, 0f, 0f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
        }

        // 3. Contrast (-100..100 -> scale 0.4..1.8)
        if (adjustments.contrast != 0f) {
            val c = if (adjustments.contrast >= 0f) {
                1f + (adjustments.contrast / 100f) * 0.85f
            } else {
                1f + (adjustments.contrast / 100f) * 0.60f
            }
            val offset = 128f * (1f - c)
            result.postConcat(
                ColorMatrix(
                    floatArrayOf(
                        c, 0f, 0f, 0f, offset,
                        0f, c, 0f, 0f, offset,
                        0f, 0f, c, 0f, offset,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
        }

        // 4. Brightness (-100..100 -> -90..90 translation)
        if (adjustments.brightness != 0f) {
            val b = (adjustments.brightness / 100f) * 90f
            result.postConcat(
                ColorMatrix(
                    floatArrayOf(
                        1f, 0f, 0f, 0f, b,
                        0f, 1f, 0f, 0f, b,
                        0f, 0f, 1f, 0f, b,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
        }

        // 5. Saturation (-100..100 -> 0f..2f)
        if (adjustments.saturation != 0f) {
            val sat = (1f + adjustments.saturation / 100f).coerceIn(0f, 2.2f)
            val satMatrix = ColorMatrix().apply { setSaturation(sat) }
            result.postConcat(satMatrix)
        }

        // 6. Warmth (-100..100 -> Red/Blue shift)
        if (adjustments.warmth != 0f) {
            val w = (adjustments.warmth / 100f) * 32f
            result.postConcat(
                ColorMatrix(
                    floatArrayOf(
                        1f, 0f, 0f, 0f, w,
                        0f, 1f, 0f, 0f, w * 0.25f,
                        0f, 0f, 1f, 0f, -w,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
        }

        // 7. Tint (-100..100 -> Green vs Magenta shift)
        if (adjustments.tint != 0f) {
            val tintVal = (adjustments.tint / 100f) * 28f
            result.postConcat(
                ColorMatrix(
                    floatArrayOf(
                        1f, 0f, 0f, 0f, tintVal * 0.5f,
                        0f, 1f, 0f, 0f, -tintVal,
                        0f, 0f, 1f, 0f, tintVal * 0.5f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
        }

        // 8. Fade (0..100 -> lifted blacks / reduced contrast)
        if (adjustments.fade > 0f) {
            val f = (adjustments.fade / 100f) * 0.35f
            val scale = 1f - f
            val lift = 255f * f * 0.55f
            result.postConcat(
                ColorMatrix(
                    floatArrayOf(
                        scale, 0f, 0f, 0f, lift,
                        0f, scale, 0f, 0f, lift,
                        0f, 0f, scale, 0f, lift,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
        }

        return result.array.copyOf()
    }

    fun decodeDrawableBitmap(context: Context, @DrawableRes resId: Int, maxDim: Int = 1600): Bitmap? {
        return runCatching {
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeResource(context.resources, resId, opts)
            val sampleSize = calculateInSampleSize(opts.outWidth, opts.outHeight, maxDim)
            val decodeOpts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val raw = BitmapFactory.decodeResource(context.resources, resId, decodeOpts) ?: return null
            scaleDownIfNeeded(raw, maxDim)
        }.getOrNull()
    }

    fun decodeUriBitmap(context: Context, uri: Uri, maxDim: Int = 1600): Bitmap? {
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                val decoded = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    val w = info.size.width
                    val h = info.size.height
                    val longest = max(w, h)
                    if (longest > maxDim && longest > 0) {
                        val scale = maxDim.toFloat() / longest.toFloat()
                        decoder.setTargetSize(
                            (w * scale).roundToInt().coerceAtLeast(1),
                            (h * scale).roundToInt().coerceAtLeast(1)
                        )
                    }
                    decoder.isMutableRequired = true
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                }
                decoded
            } else {
                val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, boundsOpts)
                }
                val sampleSize = calculateInSampleSize(boundsOpts.outWidth, boundsOpts.outHeight, maxDim)
                val decodeOpts = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                val raw = context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, decodeOpts)
                } ?: return null

                val orientation = context.contentResolver.openInputStream(uri)?.use { stream ->
                    ExifInterface(stream).getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    )
                } ?: ExifInterface.ORIENTATION_NORMAL

                val oriented = applyExifOrientation(raw, orientation)
                scaleDownIfNeeded(oriented, maxDim)
            }
        }.getOrNull()
    }

    private fun applyExifOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            else -> return bitmap
        }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated != bitmap) bitmap.recycle()
        return rotated
    }

    private fun calculateInSampleSize(width: Int, height: Int, maxDim: Int): Int {
        var inSampleSize = 1
        val longest = max(width, height)
        if (longest > maxDim) {
            while (longest / inSampleSize > maxDim * 2) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    fun scaleDownIfNeeded(bitmap: Bitmap, maxDim: Int): Bitmap {
        val longest = max(bitmap.width, bitmap.height)
        if (longest <= maxDim || longest <= 0) return bitmap
        val ratio = maxDim.toFloat() / longest.toFloat()
        val newW = (bitmap.width * ratio).roundToInt().coerceAtLeast(1)
        val newH = (bitmap.height * ratio).roundToInt().coerceAtLeast(1)
        return bitmap.scale(newW, newH)
    }

    fun applyCropAndTransform(
        source: Bitmap,
        cropRect: CropRectNorm,
        transform: TransformState
    ): Bitmap {
        val matrix = Matrix().apply {
            val sx = if (transform.flipHorizontal) -1f else 1f
            val sy = if (transform.flipVertical) -1f else 1f
            postScale(sx, sy)
            val totalAngle = transform.rotationQuarterTurns.toFloat() + transform.straightenDegrees
            if (totalAngle != 0f) {
                postRotate(totalAngle)
            }
        }

        val transformed = if (
            transform.rotationQuarterTurns != 0 ||
            transform.straightenDegrees != 0f ||
            transform.flipHorizontal ||
            transform.flipVertical
        ) {
            Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        } else {
            source
        }

        val leftPx = (cropRect.left.coerceIn(0f, 0.95f) * transformed.width).roundToInt()
        val topPx = (cropRect.top.coerceIn(0f, 0.95f) * transformed.height).roundToInt()
        val rightPx = (cropRect.right.coerceIn(0.05f, 1f) * transformed.width).roundToInt()
        val bottomPx = (cropRect.bottom.coerceIn(0.05f, 1f) * transformed.height).roundToInt()

        val cropW = (rightPx - leftPx).coerceAtLeast(1).coerceAtMost(transformed.width - leftPx)
        val cropH = (bottomPx - topPx).coerceAtLeast(1).coerceAtMost(transformed.height - topPx)

        if (leftPx == 0 && topPx == 0 && cropW == transformed.width && cropH == transformed.height) {
            return if (transformed === source) {
                source.copy(Bitmap.Config.ARGB_8888, false)
            } else {
                transformed
            }
        }
        return Bitmap.createBitmap(transformed, leftPx, topPx, cropW, cropH)
    }

    fun computeUpdatedCropRect(
        current: CropRectNorm,
        handle: CropDragHandle,
        dxNorm: Float,
        dyNorm: Float,
        lockedAspect: Float?,
        imgAspect: Float
    ): CropRectNorm {
        val minSpan = 0.16f
        if (handle == CropDragHandle.MOVE) {
            val cw = current.width
            val ch = current.height
            val nl = (current.left + dxNorm).coerceIn(0f, 1f - cw)
            val nt = (current.top + dyNorm).coerceIn(0f, 1f - ch)
            return CropRectNorm(nl, nt, nl + cw, nt + ch)
        }
        if (handle == CropDragHandle.NONE) return current

        if (lockedAspect == null || imgAspect <= 0f) {
            return when (handle) {
                CropDragHandle.TOP_LEFT -> {
                    val nl = (current.left + dxNorm).coerceIn(0f, current.right - minSpan)
                    val nt = (current.top + dyNorm).coerceIn(0f, current.bottom - minSpan)
                    CropRectNorm(nl, nt, current.right, current.bottom)
                }
                CropDragHandle.TOP_RIGHT -> {
                    val nr = (current.right + dxNorm).coerceIn(current.left + minSpan, 1f)
                    val nt = (current.top + dyNorm).coerceIn(0f, current.bottom - minSpan)
                    CropRectNorm(current.left, nt, nr, current.bottom)
                }
                CropDragHandle.BOTTOM_LEFT -> {
                    val nl = (current.left + dxNorm).coerceIn(0f, current.right - minSpan)
                    val nb = (current.bottom + dyNorm).coerceIn(current.top + minSpan, 1f)
                    CropRectNorm(nl, current.top, current.right, nb)
                }
                CropDragHandle.BOTTOM_RIGHT -> {
                    val nr = (current.right + dxNorm).coerceIn(current.left + minSpan, 1f)
                    val nb = (current.bottom + dyNorm).coerceIn(current.top + minSpan, 1f)
                    CropRectNorm(current.left, current.top, nr, nb)
                }
                else -> current
            }
        }

        // Aspect ratio in normalized [0..1] image coordinates: normW / normH == lockedAspect / imgAspect
        val normAspect = (lockedAspect / imgAspect).coerceIn(0.15f, 6.5f)
        val anchorX = when (handle) {
            CropDragHandle.TOP_LEFT, CropDragHandle.BOTTOM_LEFT -> current.right
            else -> current.left
        }
        val anchorY = when (handle) {
            CropDragHandle.TOP_LEFT, CropDragHandle.TOP_RIGHT -> current.bottom
            else -> current.top
        }
        val maxAvailW = when (handle) {
            CropDragHandle.TOP_LEFT, CropDragHandle.BOTTOM_LEFT -> anchorX
            else -> 1f - anchorX
        }.coerceAtLeast(minSpan)
        val maxAvailH = when (handle) {
            CropDragHandle.TOP_LEFT, CropDragHandle.TOP_RIGHT -> anchorY
            else -> 1f - anchorY
        }.coerceAtLeast(minSpan)

        val maxWByAspect = min(maxAvailW, maxAvailH * normAspect).coerceAtLeast(minSpan * 0.5f)
        val minWByAspect = max(minSpan, minSpan * normAspect).coerceAtMost(maxWByAspect)

        val candidateW = when (handle) {
            CropDragHandle.TOP_LEFT, CropDragHandle.BOTTOM_LEFT -> current.width - dxNorm
            else -> current.width + dxNorm
        }
        val candidateH = when (handle) {
            CropDragHandle.TOP_LEFT, CropDragHandle.TOP_RIGHT -> current.height - dyNorm
            else -> current.height + dyNorm
        }
        val desiredW = if (abs(dxNorm) >= abs(dyNorm * normAspect)) {
            candidateW
        } else {
            candidateH * normAspect
        }
        val finalW = desiredW.coerceIn(minWByAspect, maxWByAspect)
        val finalH = (finalW / normAspect).coerceIn(0.05f, maxAvailH)

        return when (handle) {
            CropDragHandle.TOP_LEFT -> CropRectNorm(anchorX - finalW, anchorY - finalH, anchorX, anchorY)
            CropDragHandle.TOP_RIGHT -> CropRectNorm(anchorX, anchorY - finalH, anchorX + finalW, anchorY)
            CropDragHandle.BOTTOM_LEFT -> CropRectNorm(anchorX - finalW, anchorY, anchorX, anchorY + finalH)
            CropDragHandle.BOTTOM_RIGHT -> CropRectNorm(anchorX, anchorY, anchorX + finalW, anchorY + finalH)
            else -> current
        }
    }

    fun renderFilterThumbnail(source: Bitmap, filterId: String, sizePx: Int = 140): Bitmap {
        val ratio = sizePx.toFloat() / max(source.width, source.height).coerceAtLeast(1)
        val w = (source.width * ratio).roundToInt().coerceAtLeast(1)
        val h = (source.height * ratio).roundToInt().coerceAtLeast(1)
        val thumb = source.scale(w, h)
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(
                ColorMatrix(buildCombinedColorMatrixArray(AdjustmentsState(), filterId, 1f))
            )
        }
        canvas.drawBitmap(thumb, 0f, 0f, paint)
        if (thumb !== source) {
            thumb.recycle()
        }
        return out
    }

    fun renderFinalComposite(
        context: Context,
        source: Bitmap,
        adjustments: AdjustmentsState,
        filterId: String,
        filterIntensity: Float,
        strokes: List<DrawStroke>,
        textLayers: List<TextLayer>,
        stickerLayers: List<StickerLayer>,
        frameConfig: FrameConfig,
        addWatermark: Boolean = false
    ): Bitmap {
        val imgW = source.width
        val imgH = source.height
        val baseMin = min(imgW, imgH).toFloat()

        // Compute outer frame padding
        val borderPx = if (frameConfig.style == FrameStyle.NONE) {
            0
        } else {
            (baseMin * frameConfig.thicknessNorm).roundToInt().coerceAtLeast(8)
        }
        val extraBottomPx = when (frameConfig.style) {
            FrameStyle.POLAROID -> (borderPx * 2.6f).roundToInt()
            FrameStyle.FILM_STRIP -> (borderPx * 0.5f).roundToInt()
            else -> 0
        }

        val totalW = imgW + borderPx * 2
        val totalH = imgH + borderPx * 2 + extraBottomPx

        val output = Bitmap.createBitmap(totalW, totalH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        // Draw Frame Background
        if (frameConfig.style != FrameStyle.NONE) {
            val bgColor = if (frameConfig.style == FrameStyle.FILM_STRIP) {
                0xFF101116.toInt()
            } else {
                frameConfig.colorArgb.toInt()
            }
            canvas.drawColor(bgColor)
        }

        val imageRect = RectF(
            borderPx.toFloat(),
            borderPx.toFloat(),
            (borderPx + imgW).toFloat(),
            (borderPx + imgH).toFloat()
        )

        canvas.save()
        if (frameConfig.style == FrameStyle.ROUNDED) {
            val corner = baseMin * 0.045f
            val clipPath = Path().apply { addRoundRect(imageRect, corner, corner, Path.Direction.CW) }
            canvas.clipPath(clipPath)
        } else {
            canvas.clipRect(imageRect)
        }
        canvas.translate(imageRect.left, imageRect.top)

        // 1. Draw Base Image with ColorMatrix
        val colorPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            val matrixArray = buildCombinedColorMatrixArray(adjustments, filterId, filterIntensity)
            colorFilter = ColorMatrixColorFilter(ColorMatrix(matrixArray))
        }
        canvas.drawBitmap(source, 0f, 0f, colorPaint)

        // 2. Vignette Overlay
        if (adjustments.vignette > 0f) {
            val strength = (adjustments.vignette / 100f).coerceIn(0f, 1f)
            val cx = imgW / 2f
            val cy = imgH / 2f
            val radius = max(imgW, imgH) * 0.72f
            val vignettePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(
                    cx, cy, radius,
                    intArrayOf(
                        Color.TRANSPARENT,
                        Color.argb((strength * 60).roundToInt(), 0, 0, 0),
                        Color.argb((strength * 215).roundToInt(), 0, 0, 0)
                    ),
                    floatArrayOf(0.45f, 0.75f, 1f),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, 0f, imgW.toFloat(), imgH.toFloat(), vignettePaint)
        }

        // 3. Draw Brush Strokes
        strokes.forEach { stroke ->
            if (stroke.points.isEmpty()) return@forEach
            val path = Path()
            val first = stroke.points.first()
            path.moveTo(first.x * imgW, first.y * imgH)
            for (i in 1 until stroke.points.size) {
                val prev = stroke.points[i - 1]
                val curr = stroke.points[i]
                val midX = ((prev.x + curr.x) / 2f) * imgW
                val midY = ((prev.y + curr.y) / 2f) * imgH
                path.quadTo(prev.x * imgW, prev.y * imgH, midX, midY)
            }
            val last = stroke.points.last()
            path.lineTo(last.x * imgW, last.y * imgH)

            val strokePx = (baseMin * stroke.strokeWidthNorm).coerceAtLeast(2f)
            val alphaInt = (stroke.opacity.coerceIn(0.05f, 1f) * 255f).roundToInt()

            if (stroke.brushType == BrushType.NEON) {
                val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeCap = Paint.Cap.ROUND
                    strokeJoin = Paint.Join.ROUND
                    color = stroke.colorArgb.toInt()
                    alpha = (alphaInt * 0.85f).roundToInt()
                    strokeWidth = strokePx * 1.9f
                    maskFilter = BlurMaskFilter(strokePx * 0.9f, BlurMaskFilter.Blur.NORMAL)
                }
                canvas.drawPath(path, glowPaint)
            }

            val mainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeCap = if (stroke.brushType == BrushType.HIGHLIGHTER) Paint.Cap.SQUARE else Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                color = if (stroke.brushType == BrushType.NEON) Color.WHITE else stroke.colorArgb.toInt()
                alpha = if (stroke.brushType == BrushType.HIGHLIGHTER) (alphaInt * 0.48f).roundToInt() else alphaInt
                strokeWidth = if (stroke.brushType == BrushType.NEON) strokePx * 0.65f else strokePx
            }
            canvas.drawPath(path, mainPaint)
        }

        // 4. Draw Sticker Layers
        stickerLayers.forEach { sticker ->
            drawStickerOnCanvas(canvas, sticker, imgW.toFloat(), imgH.toFloat(), baseMin, context)
        }

        // 5. Draw Text Layers
        textLayers.forEach { layer ->
            drawTextLayerOnCanvas(canvas, layer, imgW.toFloat(), imgH.toFloat(), baseMin, context)
        }

        // 6. Optional Watermark
        if (addWatermark) {
            val wmPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                alpha = 205
                textSize = (baseMin * 0.032f).coerceAtLeast(18f)
                typeface = loadTypeface(context, TextFontStyle.GROTESK)
                setShadowLayer(4f, 0f, 2f, Color.argb(140, 0, 0, 0))
            }
            val label = "BOLPIK STUDIO"
            val tw = wmPaint.measureText(label)
            val pad = baseMin * 0.03f
            canvas.drawText(label, imgW - tw - pad, imgH - pad, wmPaint)
        }

        canvas.restore()

        // 7. Film Strip Sprockets / Polaroid Accent
        if (frameConfig.style == FrameStyle.FILM_STRIP) {
            val holePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF282B38.toInt() }
            val holeW = borderPx * 0.55f
            val holeH = holeW * 0.68f
            val step = holeW * 1.9f
            var x = borderPx.toFloat()
            while (x < totalW - borderPx) {
                canvas.drawRoundRect(
                    x, borderPx * 0.22f, x + holeW, borderPx * 0.22f + holeH,
                    4f, 4f, holePaint
                )
                val bottomY = borderPx + imgH + borderPx * 0.35f
                canvas.drawRoundRect(
                    x, bottomY, x + holeW, bottomY + holeH,
                    4f, 4f, holePaint
                )
                x += step
            }
        } else if (frameConfig.style == FrameStyle.POLAROID) {
            val captionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFF252834.toInt()
                textSize = (baseMin * 0.036f).coerceAtLeast(18f)
                typeface = loadTypeface(context, TextFontStyle.MONO)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(
                "BOLPIK • 35MM",
                totalW / 2f,
                (borderPx + imgH + extraBottomPx * 0.65f),
                captionPaint
            )
        }

        return output
    }

    private fun loadTypeface(context: Context, style: TextFontStyle): Typeface {
        typefaceCache[style]?.let { return it }
        val loaded = when (style) {
            TextFontStyle.GROTESK -> ResourcesCompat.getFont(context, R.font.space_grotesk)
            TextFontStyle.SANS -> ResourcesCompat.getFont(context, R.font.plus_jakarta_sans)
            TextFontStyle.MONO -> ResourcesCompat.getFont(context, R.font.jetbrains_mono)
            TextFontStyle.SERIF -> Typeface.create(Typeface.SERIF, Typeface.BOLD)
        } ?: Typeface.DEFAULT_BOLD
        typefaceCache.putIfAbsent(style, loaded)
        return loaded
    }

    private fun drawTextLayerOnCanvas(
        canvas: Canvas,
        layer: TextLayer,
        imgW: Float,
        imgH: Float,
        baseMin: Float,
        context: Context
    ) {
        if (layer.text.isBlank()) return
        val px = layer.xNorm * imgW
        val py = layer.yNorm * imgH
        val fontSize = baseMin * 0.072f * layer.scale

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = layer.colorArgb.toInt()
            textSize = fontSize
            typeface = loadTypeface(context, layer.fontStyle)
            textAlign = Paint.Align.CENTER
            if (layer.hasShadow && layer.backgroundArgb == null) {
                setShadowLayer(fontSize * 0.14f, 0f, fontSize * 0.06f, Color.argb(170, 0, 0, 0))
            }
        }

        val lines = layer.text.split("\n")
        val lineHeight = fontSize * 1.18f
        val maxLineW = lines.maxOfOrNull { textPaint.measureText(it) } ?: 0f
        val totalTextH = lines.size * lineHeight

        canvas.save()
        canvas.translate(px, py)
        canvas.rotate(layer.rotationDeg)

        layer.backgroundArgb?.let { bg ->
            val padH = fontSize * 0.45f
            val padV = fontSize * 0.25f
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bg.toInt() }
            val rect = RectF(
                -maxLineW / 2f - padH,
                -totalTextH / 2f - padV,
                maxLineW / 2f + padH,
                totalTextH / 2f + padV
            )
            canvas.drawRoundRect(rect, fontSize * 0.28f, fontSize * 0.28f, bgPaint)
        }

        val fm = textPaint.fontMetrics
        val startY = -totalTextH / 2f - fm.ascent * 0.85f
        lines.forEachIndexed { idx, line ->
            canvas.drawText(line, 0f, startY + idx * lineHeight, textPaint)
        }
        canvas.restore()
    }

    private fun drawStickerOnCanvas(
        canvas: Canvas,
        sticker: StickerLayer,
        imgW: Float,
        imgH: Float,
        baseMin: Float,
        context: Context
    ) {
        val px = sticker.xNorm * imgW
        val py = sticker.yNorm * imgH
        val size = baseMin * 0.14f * sticker.scale

        canvas.save()
        canvas.translate(px, py)
        canvas.rotate(sticker.rotationDeg)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = sticker.colorArgb.toInt()
        }

        when (sticker.kind) {
            StickerKind.BOLPIK_TAG, StickerKind.RAW_BADGE, StickerKind.FILM_35MM -> {
                val label = sticker.kind.label
                val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    textSize = size * 0.34f
                    typeface = loadTypeface(context, TextFontStyle.MONO)
                    textAlign = Paint.Align.CENTER
                }
                val tw = textPaint.measureText(label)
                val rect = RectF(-tw * 0.72f, -size * 0.32f, tw * 0.72f, size * 0.32f)
                canvas.drawRoundRect(rect, size * 0.16f, size * 0.16f, paint)
                val fm = textPaint.fontMetrics
                val baseline = -(fm.ascent + fm.descent) / 2f
                canvas.drawText(label, 0f, baseline, textPaint)
            }
            StickerKind.REC_DOT -> {
                val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(190, 14, 14, 18) }
                val rect = RectF(-size * 0.7f, -size * 0.3f, size * 0.7f, size * 0.3f)
                canvas.drawRoundRect(rect, size * 0.15f, size * 0.15f, bgPaint)
                canvas.drawCircle(-size * 0.36f, 0f, size * 0.12f, paint)
                val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    textSize = size * 0.30f
                    typeface = loadTypeface(context, TextFontStyle.MONO)
                    textAlign = Paint.Align.LEFT
                }
                val fm = tp.fontMetrics
                canvas.drawText("REC", -size * 0.14f, -(fm.ascent + fm.descent) / 2f, tp)
            }
            StickerKind.SPARKLE -> {
                val r = size * 0.5f
                val inner = r * 0.22f
                val path = Path().apply {
                    moveTo(0f, -r)
                    quadTo(0f, 0f, r, 0f)
                    quadTo(0f, 0f, 0f, r)
                    quadTo(0f, 0f, -r, 0f)
                    quadTo(0f, 0f, 0f, -r)
                    close()
                }
                canvas.drawPath(path, paint)
                val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
                canvas.drawCircle(0f, 0f, inner * 0.45f, dotPaint)
            }
            StickerKind.STAR -> {
                val outer = size * 0.48f
                val inner = outer * 0.42f
                val path = Path()
                for (i in 0 until 10) {
                    val angle = Math.toRadians((i * 36.0) - 90.0)
                    val radius = if (i % 2 == 0) outer else inner
                    val x = (Math.cos(angle) * radius).toFloat()
                    val y = (Math.sin(angle) * radius).toFloat()
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()
                canvas.drawPath(path, paint)
            }
            StickerKind.HEART -> {
                val r = size * 0.42f
                val path = Path().apply {
                    moveTo(0f, r * 0.85f)
                    cubicTo(-r * 1.4f, r * 0.05f, -r * 0.85f, -r * 1.05f, 0f, -r * 0.35f)
                    cubicTo(r * 0.85f, -r * 1.05f, r * 1.4f, r * 0.05f, 0f, r * 0.85f)
                    close()
                }
                canvas.drawPath(path, paint)
            }
            StickerKind.CROSSHAIR -> {
                val strokeP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = sticker.colorArgb.toInt()
                    style = Paint.Style.STROKE
                    strokeWidth = size * 0.07f
                }
                val r = size * 0.42f
                canvas.drawCircle(0f, 0f, r * 0.65f, strokeP)
                canvas.drawLine(-r, 0f, -r * 0.35f, 0f, strokeP)
                canvas.drawLine(r * 0.35f, 0f, r, 0f, strokeP)
                canvas.drawLine(0f, -r, 0f, -r * 0.35f, strokeP)
                canvas.drawLine(0f, r * 0.35f, 0f, r, strokeP)
            }
        }
        canvas.restore()
    }

    fun renderCollageBitmap(
        slots: List<CollageSlotItem>,
        template: CollageTemplate,
        spacingNorm: Float,
        cornerRadiusNorm: Float,
        bgColorArgb: Long,
        aspectRatio: Float,
        targetWidth: Int = 1440
    ): Bitmap {
        val targetHeight = (targetWidth / aspectRatio).roundToInt().coerceAtLeast(200)
        val out = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.drawColor(bgColorArgb.toInt())

        val rects = computeCollageRects(template, targetWidth.toFloat(), targetHeight.toFloat(), spacingNorm)
        val cornerPx = min(targetWidth, targetHeight) * cornerRadiusNorm
        val imgPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val emptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF282B38.toInt() }

        rects.forEachIndexed { index, rect ->
            val clipPath = Path().apply {
                addRoundRect(rect, cornerPx, cornerPx, Path.Direction.CW)
            }
            canvas.save()
            canvas.clipPath(clipPath)

            val slot = slots.getOrNull(index)
            val bmp = slot?.bitmap
            if (bmp != null) {
                val scale = max(rect.width() / bmp.width.toFloat(), rect.height() / bmp.height.toFloat())
                val drawW = bmp.width * scale
                val drawH = bmp.height * scale
                val extraX = (drawW - rect.width()) * 0.5f
                val extraY = (drawH - rect.height()) * 0.5f
                val panOffsetX = slot.panX.coerceIn(-1f, 1f) * extraX
                val panOffsetY = slot.panY.coerceIn(-1f, 1f) * extraY
                val dst = RectF(
                    rect.left - extraX + panOffsetX,
                    rect.top - extraY + panOffsetY,
                    rect.left - extraX + drawW + panOffsetX,
                    rect.top - extraY + drawH + panOffsetY
                )
                canvas.drawBitmap(bmp, null, dst, imgPaint)
            } else {
                canvas.drawRect(rect, emptyPaint)
            }
            canvas.restore()
        }
        return out
    }

    fun computeCollageRects(
        template: CollageTemplate,
        w: Float,
        h: Float,
        spacingNorm: Float
    ): List<RectF> {
        val gap = min(w, h) * spacingNorm
        val innerL = gap
        val innerT = gap
        val innerR = w - gap
        val innerB = h - gap
        val iw = innerR - innerL
        val ih = innerB - innerT

        return when (template) {
            CollageTemplate.TWO_VERTICAL -> {
                val colW = (iw - gap) / 2f
                listOf(
                    RectF(innerL, innerT, innerL + colW, innerB),
                    RectF(innerL + colW + gap, innerT, innerR, innerB)
                )
            }
            CollageTemplate.TWO_HORIZONTAL -> {
                val rowH = (ih - gap) / 2f
                listOf(
                    RectF(innerL, innerT, innerR, innerT + rowH),
                    RectF(innerL, innerT + rowH + gap, innerR, innerB)
                )
            }
            CollageTemplate.THREE_FEATURE -> {
                val leftW = iw * 0.56f
                val rightX = innerL + leftW + gap
                val rowH = (ih - gap) / 2f
                listOf(
                    RectF(innerL, innerT, innerL + leftW, innerB),
                    RectF(rightX, innerT, innerR, innerT + rowH),
                    RectF(rightX, innerT + rowH + gap, innerR, innerB)
                )
            }
            CollageTemplate.FOUR_GRID -> {
                val colW = (iw - gap) / 2f
                val rowH = (ih - gap) / 2f
                listOf(
                    RectF(innerL, innerT, innerL + colW, innerT + rowH),
                    RectF(innerL + colW + gap, innerT, innerR, innerT + rowH),
                    RectF(innerL, innerT + rowH + gap, innerL + colW, innerB),
                    RectF(innerL + colW + gap, innerT + rowH + gap, innerR, innerB)
                )
            }
            CollageTemplate.FOUR_BENTO -> {
                val topH = ih * 0.58f
                val botT = innerT + topH + gap
                val colW = (iw - gap * 2f) / 3f
                listOf(
                    RectF(innerL, innerT, innerR, innerT + topH),
                    RectF(innerL, botT, innerL + colW, innerB),
                    RectF(innerL + colW + gap, botT, innerL + colW * 2f + gap, innerB),
                    RectF(innerL + (colW + gap) * 2f, botT, innerR, innerB)
                )
            }
            CollageTemplate.SIX_MOSAIC -> {
                val colW = (iw - gap) / 2f
                val rowH = (ih - gap * 2f) / 3f
                buildList {
                    for (r in 0 until 3) {
                        val top = innerT + r * (rowH + gap)
                        add(RectF(innerL, top, innerL + colW, top + rowH))
                        add(RectF(innerL + colW + gap, top, innerR, top + rowH))
                    }
                }
            }
        }
    }

    fun estimateSizeBytes(bitmap: Bitmap, format: ExportFormat, quality: Int): Long {
        val baos = ByteArrayOutputStream()
        val compressFmt = compressFormatFor(format)
        bitmap.compress(compressFmt, quality.coerceIn(10, 100), baos)
        return baos.size().toLong()
    }

    fun saveBitmapToGallery(
        context: Context,
        bitmap: Bitmap,
        format: ExportFormat,
        quality: Int,
        displayName: String
    ): SavedExportItem? {
        return runCatching {
            val cleanName = "${displayName}_${System.currentTimeMillis()}.${format.extension}"
            val compressFmt = compressFormatFor(format)
            val baos = ByteArrayOutputStream()
            bitmap.compress(compressFmt, quality.coerceIn(10, 100), baos)
            val bytes = baos.toByteArray()

            val uri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, cleanName)
                    put(MediaStore.Images.Media.MIME_TYPE, format.mimeType)
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Bolpik")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val inserted = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: return null
                resolver.openOutputStream(inserted)?.use { out ->
                    out.write(bytes)
                }
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(inserted, values, null, null)
                inserted
            } else {
                val dir = File(context.filesDir, "bolpik_saved").apply { mkdirs() }
                val file = File(dir, cleanName)
                FileOutputStream(file).use { it.write(bytes) }
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            }

            SavedExportItem(
                uriString = uri.toString(),
                displayName = cleanName,
                width = bitmap.width,
                height = bitmap.height,
                format = format,
                sizeBytes = bytes.size.toLong(),
                timestampMillis = System.currentTimeMillis()
            )
        }.getOrNull()
    }

    fun createShareUri(
        context: Context,
        bitmap: Bitmap,
        format: ExportFormat,
        quality: Int
    ): Uri? {
        return runCatching {
            val exportDir = File(context.cacheDir, "bolpik_exports").apply { mkdirs() }
            // Keep cache tidy: remove share exports older than 24h and keep at most 4 prior files
            val existing = exportDir.listFiles()?.sortedByDescending { it.lastModified() } ?: emptyList()
            val cutoff = System.currentTimeMillis() - 24L * 60L * 60L * 1000L
            existing.forEachIndexed { index, oldFile ->
                if (index >= 4 || oldFile.lastModified() < cutoff) {
                    oldFile.delete()
                }
            }
            val file = File(exportDir, "bolpik_share_${System.currentTimeMillis()}.${format.extension}")
            FileOutputStream(file).use { out ->
                bitmap.compress(compressFormatFor(format), quality.coerceIn(10, 100), out)
            }
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        }.getOrNull()
    }

    @Suppress("DEPRECATION")
    private fun compressFormatFor(format: ExportFormat): Bitmap.CompressFormat = when (format) {
        ExportFormat.JPEG -> Bitmap.CompressFormat.JPEG
        ExportFormat.PNG -> Bitmap.CompressFormat.PNG
        ExportFormat.WEBP -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Bitmap.CompressFormat.WEBP_LOSSY
        } else {
            Bitmap.CompressFormat.WEBP
        }
    }
}
