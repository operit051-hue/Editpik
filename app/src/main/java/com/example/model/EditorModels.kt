package com.example.model

import android.graphics.Bitmap
import androidx.annotation.DrawableRes
import java.util.UUID

enum class EditorToolTab(val label: String) {
    ADJUST("Ajustes"),
    FILTERS("Filtros"),
    CROP("Recorte"),
    DRAW("Pincel"),
    TEXT("Texto"),
    STICKERS("Stickers"),
    FRAME("Marcos")
}

data class SamplePhoto(
    val id: String,
    val title: String,
    val category: String,
    @DrawableRes val drawableRes: Int,
    val dimensionsLabel: String
)

data class AdjustmentsState(
    val brightness: Float = 0f,
    val contrast: Float = 0f,
    val saturation: Float = 0f,
    val warmth: Float = 0f,
    val tint: Float = 0f,
    val exposure: Float = 0f,
    val fade: Float = 0f,
    val vignette: Float = 0f
) {
    val hasChanges: Boolean
        get() = brightness != 0f || contrast != 0f || saturation != 0f ||
            warmth != 0f || tint != 0f || exposure != 0f || fade != 0f || vignette != 0f

    fun modifiedCount(): Int = listOf(
        brightness, contrast, saturation, warmth, tint, exposure, fade, vignette
    ).count { it != 0f }
}

enum class AdjustmentParam(val label: String, val min: Float, val max: Float) {
    EXPOSURE("Exposición", -100f, 100f),
    BRIGHTNESS("Brillo", -100f, 100f),
    CONTRAST("Contraste", -100f, 100f),
    SATURATION("Saturación", -100f, 100f),
    WARMTH("Calidez", -100f, 100f),
    TINT("Matiz", -100f, 100f),
    FADE("Desvanecido", 0f, 100f),
    VIGNETTE("Viñeta", 0f, 100f)
}

enum class FilterCategory(val label: String) {
    ALL("Todos"),
    CINE("Cine"),
    RETRO("Retro"),
    PORTRAIT("Retrato"),
    BW("B&N"),
    CUSTOM("Mis Presets")
}

data class FilterPreset(
    val id: String,
    val name: String,
    val category: FilterCategory,
    val matrix: FloatArray,
    val accentColorHex: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FilterPreset) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}

data class PresetRecipe(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val subtitle: String,
    val category: FilterCategory,
    val filterId: String,
    val filterIntensity: Float,
    val adjustments: AdjustmentsState,
    val isCustom: Boolean = false
)

enum class CropAspectRatio(val label: String, val ratio: Float?) {
    FREE("Libre", null),
    SQUARE_1_1("1:1", 1f),
    PORTRAIT_4_5("4:5", 4f / 5f),
    LANDSCAPE_16_9("16:9", 16f / 9f),
    STORY_9_16("9:16", 9f / 16f),
    CLASSIC_3_2("3:2", 3f / 2f)
}

data class CropRectNorm(
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 1f,
    val bottom: Float = 1f
) {
    val width: Float get() = (right - left).coerceAtLeast(0.05f)
    val height: Float get() = (bottom - top).coerceAtLeast(0.05f)
    val isFull: Boolean get() = left <= 0.005f && top <= 0.005f && right >= 0.995f && bottom >= 0.995f
}

data class TransformState(
    val rotationQuarterTurns: Int = 0,
    val straightenDegrees: Float = 0f,
    val flipHorizontal: Boolean = false,
    val flipVertical: Boolean = false
)

data class AppliedCropStep(
    val cropRect: CropRectNorm,
    val transform: TransformState
)

data class NormPoint(val x: Float, val y: Float)

enum class BrushType(val label: String) {
    NORMAL("Pincel"),
    NEON("Neón"),
    HIGHLIGHTER("Marcador")
}

data class DrawStroke(
    val id: String = UUID.randomUUID().toString(),
    val points: List<NormPoint>,
    val colorArgb: Long,
    val strokeWidthNorm: Float,
    val opacity: Float,
    val brushType: BrushType
)

enum class TextFontStyle(val label: String) {
    GROTESK("Grotesk"),
    SANS("Moderna"),
    MONO("Mono"),
    SERIF("Editorial")
}

data class TextLayer(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val xNorm: Float = 0.5f,
    val yNorm: Float = 0.5f,
    val scale: Float = 1f,
    val rotationDeg: Float = 0f,
    val colorArgb: Long = 0xFFFFFFFF,
    val backgroundArgb: Long? = null,
    val fontStyle: TextFontStyle = TextFontStyle.GROTESK,
    val hasShadow: Boolean = true
)

enum class StickerKind(val label: String) {
    BOLPIK_TAG("BOLPIK"),
    RAW_BADGE("RAW"),
    FILM_35MM("35MM"),
    SPARKLE("Destello"),
    HEART("Corazón"),
    STAR("Estrella"),
    CROSSHAIR("Enfoque"),
    REC_DOT("REC")
}

data class StickerLayer(
    val id: String = UUID.randomUUID().toString(),
    val kind: StickerKind,
    val xNorm: Float = 0.5f,
    val yNorm: Float = 0.5f,
    val scale: Float = 1f,
    val rotationDeg: Float = 0f,
    val colorArgb: Long = 0xFF10E88A
)

enum class FrameStyle(val label: String) {
    NONE("Sin marco"),
    MINIMAL("Galería"),
    POLAROID("Polaroid"),
    FILM_STRIP("Película"),
    ROUNDED("Tarjeta")
}

data class FrameConfig(
    val style: FrameStyle = FrameStyle.NONE,
    val thicknessNorm: Float = 0.05f,
    val colorArgb: Long = 0xFFF2F7F4
)

data class EditorDocumentSnapshot(
    val workingBitmap: Bitmap?,
    val appliedCrops: List<AppliedCropStep> = emptyList(),
    val adjustments: AdjustmentsState = AdjustmentsState(),
    val filterId: String = "original",
    val filterIntensity: Float = 1f,
    val strokes: List<DrawStroke> = emptyList(),
    val textLayers: List<TextLayer> = emptyList(),
    val stickerLayers: List<StickerLayer> = emptyList(),
    val frameConfig: FrameConfig = FrameConfig()
)

enum class ProjectStatus {
    IMPORTED,
    EDITING,
    EXPORTED
}

data class ProjectEditState(
    val appliedCrops: List<AppliedCropStep> = emptyList(),
    val adjustments: AdjustmentsState = AdjustmentsState(),
    val filterId: String = "original",
    val filterIntensity: Float = 1f,
    val strokes: List<DrawStroke> = emptyList(),
    val textLayers: List<TextLayer> = emptyList(),
    val stickerLayers: List<StickerLayer> = emptyList(),
    val frameConfig: FrameConfig = FrameConfig()
) {
    val hasEdits: Boolean
        get() = appliedCrops.isNotEmpty() ||
            adjustments.hasChanges ||
            filterId != "original" ||
            strokes.isNotEmpty() ||
            textLayers.isNotEmpty() ||
            stickerLayers.isNotEmpty() ||
            frameConfig.style != FrameStyle.NONE
}

data class BolpikProject(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val sourceHash: String,
    val originalPath: String,
    val previewPath: String,
    val width: Int,
    val height: Int,
    val status: ProjectStatus = ProjectStatus.IMPORTED,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis(),
    val editCount: Int = 0
)

enum class ExportFormat(val label: String, val extension: String, val mimeType: String) {
    JPEG("JPEG", "jpg", "image/jpeg"),
    PNG("PNG", "png", "image/png"),
    WEBP("WEBP", "webp", "image/webp")
}

data class SavedExportItem(
    val id: String = UUID.randomUUID().toString(),
    val uriString: String,
    val displayName: String,
    val width: Int,
    val height: Int,
    val format: ExportFormat,
    val sizeBytes: Long,
    val timestampMillis: Long
)

enum class CollageTemplate(val label: String, val slotCount: Int) {
    TWO_VERTICAL("2 Vertical", 2),
    TWO_HORIZONTAL("2 Horizontal", 2),
    THREE_FEATURE("3 Asimétrico", 3),
    FOUR_GRID("4 Clásico", 4),
    FOUR_BENTO("4 Bento", 4),
    SIX_MOSAIC("6 Mosaico", 6)
}

data class CollageSlotItem(
    val index: Int,
    val bitmap: Bitmap? = null,
    val label: String = "Foto ${index + 1}",
    val panX: Float = 0f,
    val panY: Float = 0f
)
