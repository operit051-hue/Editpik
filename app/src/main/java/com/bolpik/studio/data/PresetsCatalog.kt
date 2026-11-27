package com.bolpik.studio.data

import com.bolpik.studio.R
import com.bolpik.studio.model.AdjustmentsState
import com.bolpik.studio.model.FilterCategory
import com.bolpik.studio.model.FilterPreset
import com.bolpik.studio.model.PresetRecipe
import com.bolpik.studio.model.SamplePhoto

object PresetsCatalog {

    val samplePhotos: List<SamplePhoto> = listOf(
        SamplePhoto(
            id = "sample_urban",
            title = "Neón Nocturno",
            category = "Urbano",
            drawableRes = R.drawable.img_sample_urban,
            dimensionsLabel = "3:4 • Estudio"
        ),
        SamplePhoto(
            id = "sample_mountain",
            title = "Cumbre Dorada",
            category = "Paisaje",
            drawableRes = R.drawable.img_sample_mountain,
            dimensionsLabel = "4:3 • Paisaje"
        ),
        SamplePhoto(
            id = "sample_portrait",
            title = "Retrato 35mm",
            category = "Retrato",
            drawableRes = R.drawable.img_sample_portrait,
            dimensionsLabel = "3:4 • Retrato"
        )
    )

    val filters: List<FilterPreset> = listOf(
        FilterPreset(
            id = "original",
            name = "Original",
            category = FilterCategory.ALL,
            matrix = floatArrayOf(
                1f, 0f, 0f, 0f, 0f,
                0f, 1f, 0f, 0f, 0f,
                0f, 0f, 1f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            ),
            accentColorHex = 0xFFA0A5B8
        ),
        FilterPreset(
            id = "bolpik_cine",
            name = "Bolpik Cine",
            category = FilterCategory.CINE,
            matrix = floatArrayOf(
                1.14f, -0.04f, -0.02f, 0f, 6f,
                -0.02f, 1.06f, 0.02f, 0f, 2f,
                -0.06f, 0.04f, 1.16f, 0f, -8f,
                0f, 0f, 0f, 1f, 0f
            ),
            accentColorHex = 0xFFFF5A36
        ),
        FilterPreset(
            id = "amber_gold",
            name = "Ámbar",
            category = FilterCategory.PORTRAIT,
            matrix = floatArrayOf(
                1.18f, 0.08f, 0f, 0f, 10f,
                0.02f, 1.04f, -0.02f, 0f, 4f,
                -0.08f, 0f, 0.88f, 0f, -6f,
                0f, 0f, 0f, 1f, 0f
            ),
            accentColorHex = 0xFFFFB84D
        ),
        FilterPreset(
            id = "nordic_ice",
            name = "Nórdico",
            category = FilterCategory.CINE,
            matrix = floatArrayOf(
                0.90f, 0.02f, 0.06f, 0f, -4f,
                0.02f, 1.02f, 0.08f, 0f, 2f,
                0.06f, 0.08f, 1.18f, 0f, 10f,
                0f, 0f, 0f, 1f, 0f
            ),
            accentColorHex = 0xFF38E8C6
        ),
        FilterPreset(
            id = "kodak_retro",
            name = "Retro 35mm",
            category = FilterCategory.RETRO,
            matrix = floatArrayOf(
                1.12f, 0.06f, -0.04f, 0f, 12f,
                0.03f, 1.02f, 0.02f, 0f, 8f,
                -0.04f, 0.04f, 0.86f, 0f, 14f,
                0f, 0f, 0f, 1f, 0f
            ),
            accentColorHex = 0xFFE8A838
        ),
        FilterPreset(
            id = "noir_contrast",
            name = "Noir Pro",
            category = FilterCategory.BW,
            matrix = floatArrayOf(
                0.42f, 0.72f, 0.14f, 0f, -26f,
                0.42f, 0.72f, 0.14f, 0f, -26f,
                0.42f, 0.72f, 0.14f, 0f, -26f,
                0f, 0f, 0f, 1f, 0f
            ),
            accentColorHex = 0xFFD9DCE6
        ),
        FilterPreset(
            id = "silver_mono",
            name = "Plata Suave",
            category = FilterCategory.BW,
            matrix = floatArrayOf(
                0.33f, 0.54f, 0.11f, 0f, 8f,
                0.33f, 0.54f, 0.11f, 0f, 8f,
                0.33f, 0.54f, 0.11f, 0f, 10f,
                0f, 0f, 0f, 1f, 0f
            ),
            accentColorHex = 0xFF9BA3B5
        ),
        FilterPreset(
            id = "cyber_neon",
            name = "Ciber Neón",
            category = FilterCategory.CINE,
            matrix = floatArrayOf(
                1.18f, -0.10f, 0.14f, 0f, -6f,
                -0.06f, 0.96f, 0.12f, 0f, 4f,
                0.14f, 0.08f, 1.24f, 0f, 8f,
                0f, 0f, 0f, 1f, 0f
            ),
            accentColorHex = 0xFFC855FF
        ),
        FilterPreset(
            id = "matte_pastel",
            name = "Mate Pastel",
            category = FilterCategory.PORTRAIT,
            matrix = floatArrayOf(
                0.96f, 0.05f, 0.04f, 0f, 16f,
                0.03f, 0.95f, 0.05f, 0f, 15f,
                0.02f, 0.06f, 0.94f, 0f, 18f,
                0f, 0f, 0f, 1f, 0f
            ),
            accentColorHex = 0xFFFF8FA3
        ),
        FilterPreset(
            id = "editorial_sepia",
            name = "Sepia",
            category = FilterCategory.RETRO,
            matrix = floatArrayOf(
                0.393f, 0.769f, 0.189f, 0f, 0f,
                0.349f, 0.686f, 0.168f, 0f, 0f,
                0.272f, 0.534f, 0.131f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            ),
            accentColorHex = 0xFFC98A5B
        ),
        FilterPreset(
            id = "emerald_mood",
            name = "Esmeralda",
            category = FilterCategory.CINE,
            matrix = floatArrayOf(
                0.92f, 0f, 0.02f, 0f, -4f,
                0.04f, 1.12f, 0.02f, 0f, 4f,
                0f, 0.06f, 1.02f, 0f, -2f,
                0f, 0f, 0f, 1f, 0f
            ),
            accentColorHex = 0xFF2DD4BF
        ),
        FilterPreset(
            id = "sunset_coral",
            name = "Coral Sunset",
            category = FilterCategory.PORTRAIT,
            matrix = floatArrayOf(
                1.22f, 0.06f, 0f, 0f, 8f,
                0.04f, 0.98f, 0.02f, 0f, 0f,
                0.02f, -0.02f, 0.96f, 0f, 4f,
                0f, 0f, 0f, 1f, 0f
            ),
            accentColorHex = 0xFFFF6B6B
        )
    )

    val builtInRecipes: List<PresetRecipe> = listOf(
        PresetRecipe(
            id = "recipe_cine_urbano",
            name = "Cine Nocturno",
            subtitle = "Teal & Coral cinematográfico con viñeta profunda",
            category = FilterCategory.CINE,
            filterId = "bolpik_cine",
            filterIntensity = 0.85f,
            adjustments = AdjustmentsState(
                contrast = 18f,
                saturation = 10f,
                warmth = -8f,
                vignette = 36f
            )
        ),
        PresetRecipe(
            id = "recipe_golden_portrait",
            name = "Hora Dorada",
            subtitle = "Tonos piel cálidos, brillo suave y contraste editorial",
            category = FilterCategory.PORTRAIT,
            filterId = "amber_gold",
            filterIntensity = 0.8f,
            adjustments = AdjustmentsState(
                exposure = 10f,
                brightness = 6f,
                contrast = 12f,
                warmth = 22f,
                vignette = 18f
            )
        ),
        PresetRecipe(
            id = "recipe_analog_35",
            name = "Analógico 35mm",
            subtitle = "Negros levantados estilo película clásica con grano suave",
            category = FilterCategory.RETRO,
            filterId = "kodak_retro",
            filterIntensity = 0.9f,
            adjustments = AdjustmentsState(
                contrast = 8f,
                warmth = 14f,
                fade = 28f,
                vignette = 24f
            )
        ),
        PresetRecipe(
            id = "recipe_noir_editorial",
            name = "Noir Editorial",
            subtitle = "Monocromo dramático de alto contraste para retrato y calle",
            category = FilterCategory.BW,
            filterId = "noir_contrast",
            filterIntensity = 1.0f,
            adjustments = AdjustmentsState(
                contrast = 26f,
                exposure = 4f,
                vignette = 42f
            )
        ),
        PresetRecipe(
            id = "recipe_nordic_clean",
            name = "Minimal Nórdico",
            subtitle = "Azules limpios, luz nítida y saturación contenida",
            category = FilterCategory.CINE,
            filterId = "nordic_ice",
            filterIntensity = 0.78f,
            adjustments = AdjustmentsState(
                brightness = 8f,
                contrast = 14f,
                saturation = -12f,
                warmth = -16f
            )
        ),
        PresetRecipe(
            id = "recipe_cyber_glow",
            name = "Neón Tokio",
            subtitle = "Magenta y cian eléctrico ideal para luces nocturnas",
            category = FilterCategory.CINE,
            filterId = "cyber_neon",
            filterIntensity = 0.92f,
            adjustments = AdjustmentsState(
                contrast = 22f,
                saturation = 24f,
                tint = 16f,
                vignette = 30f
            )
        ),
        PresetRecipe(
            id = "recipe_pastel_dream",
            name = "Pastel Suave",
            subtitle = "Luces aireadas y contraste delicado para lifestyle",
            category = FilterCategory.PORTRAIT,
            filterId = "matte_pastel",
            filterIntensity = 0.85f,
            adjustments = AdjustmentsState(
                brightness = 14f,
                contrast = -10f,
                fade = 22f,
                warmth = 8f
            )
        ),
        PresetRecipe(
            id = "recipe_vintage_sepia",
            name = "Archivo Vintage",
            subtitle = "Tono sepia cálido con textura desvanecida de colección",
            category = FilterCategory.RETRO,
            filterId = "editorial_sepia",
            filterIntensity = 0.75f,
            adjustments = AdjustmentsState(
                contrast = 10f,
                fade = 24f,
                vignette = 38f
            )
        )
    )

    fun findFilter(id: String): FilterPreset =
        filters.firstOrNull { it.id == id } ?: filters.first()
}
