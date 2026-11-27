package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.AdjustmentsState
import com.example.model.ExportFormat
import com.example.model.FilterCategory
import com.example.model.PresetRecipe
import com.example.model.SavedExportItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.bolpikDataStore: DataStore<Preferences> by preferencesDataStore(name = "bolpik_studio_prefs")

class BolpikRepository(private val context: Context) {

    private object Keys {
        val DARKROOM_MODE = booleanPreferencesKey("darkroom_mode")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
        val CUSTOM_PRESETS_JSON = stringPreferencesKey("custom_presets_json")
        val EXPORTS_HISTORY_JSON = stringPreferencesKey("exports_history_json")
    }

    val darkroomModeFlow: Flow<Boolean> = context.bolpikDataStore.data.map { prefs ->
        prefs[Keys.DARKROOM_MODE] ?: true
    }

    val appLanguageFlow: Flow<String> = context.bolpikDataStore.data.map { prefs ->
        val lang = prefs[Keys.APP_LANGUAGE] ?: "es"
        if (lang == "en") "en" else "es"
    }

    val customPresetsFlow: Flow<List<PresetRecipe>> = context.bolpikDataStore.data.map { prefs ->
        decodePresets(prefs[Keys.CUSTOM_PRESETS_JSON] ?: "[]")
    }

    val exportsHistoryFlow: Flow<List<SavedExportItem>> = context.bolpikDataStore.data.map { prefs ->
        decodeExports(prefs[Keys.EXPORTS_HISTORY_JSON] ?: "[]")
    }

    suspend fun setDarkroomMode(enabled: Boolean) {
        context.bolpikDataStore.edit { prefs ->
            prefs[Keys.DARKROOM_MODE] = enabled
        }
    }

    suspend fun setAppLanguage(langCode: String) {
        val normalized = if (langCode == "en") "en" else "es"
        context.bolpikDataStore.edit { prefs ->
            prefs[Keys.APP_LANGUAGE] = normalized
        }
    }

    suspend fun saveCustomPreset(recipe: PresetRecipe) {
        context.bolpikDataStore.edit { prefs ->
            val current = decodePresets(prefs[Keys.CUSTOM_PRESETS_JSON] ?: "[]").toMutableList()
            current.removeAll { it.id == recipe.id }
            current.add(0, recipe.copy(isCustom = true, category = FilterCategory.CUSTOM))
            prefs[Keys.CUSTOM_PRESETS_JSON] = encodePresets(current)
        }
    }

    suspend fun deleteCustomPreset(presetId: String) {
        context.bolpikDataStore.edit { prefs ->
            val current = decodePresets(prefs[Keys.CUSTOM_PRESETS_JSON] ?: "[]")
                .filterNot { it.id == presetId }
            prefs[Keys.CUSTOM_PRESETS_JSON] = encodePresets(current)
        }
    }

    suspend fun recordExport(item: SavedExportItem) {
        context.bolpikDataStore.edit { prefs ->
            val current = decodeExports(prefs[Keys.EXPORTS_HISTORY_JSON] ?: "[]").toMutableList()
            current.removeAll { it.id == item.id }
            current.add(0, item)
            val trimmed = current.take(40)
            prefs[Keys.EXPORTS_HISTORY_JSON] = encodeExports(trimmed)
        }
    }

    suspend fun removeExportRecord(exportId: String) {
        context.bolpikDataStore.edit { prefs ->
            val current = decodeExports(prefs[Keys.EXPORTS_HISTORY_JSON] ?: "[]")
                .filterNot { it.id == exportId }
            prefs[Keys.EXPORTS_HISTORY_JSON] = encodeExports(current)
        }
    }

    private fun encodePresets(list: List<PresetRecipe>): String {
        val arr = JSONArray()
        list.forEach { item ->
            val adj = JSONObject().apply {
                put("brightness", item.adjustments.brightness.toDouble())
                put("contrast", item.adjustments.contrast.toDouble())
                put("saturation", item.adjustments.saturation.toDouble())
                put("warmth", item.adjustments.warmth.toDouble())
                put("tint", item.adjustments.tint.toDouble())
                put("exposure", item.adjustments.exposure.toDouble())
                put("fade", item.adjustments.fade.toDouble())
                put("vignette", item.adjustments.vignette.toDouble())
            }
            val obj = JSONObject().apply {
                put("id", item.id)
                put("name", item.name)
                put("subtitle", item.subtitle)
                put("filterId", item.filterId)
                put("filterIntensity", item.filterIntensity.toDouble())
                put("adjustments", adj)
            }
            arr.put(obj)
        }
        return arr.toString()
    }

    private fun decodePresets(json: String): List<PresetRecipe> {
        return runCatching {
            val arr = JSONArray(json)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val adjObj = obj.optJSONObject("adjustments")
                    val adj = if (adjObj != null) {
                        AdjustmentsState(
                            brightness = adjObj.optDouble("brightness", 0.0).toFloat(),
                            contrast = adjObj.optDouble("contrast", 0.0).toFloat(),
                            saturation = adjObj.optDouble("saturation", 0.0).toFloat(),
                            warmth = adjObj.optDouble("warmth", 0.0).toFloat(),
                            tint = adjObj.optDouble("tint", 0.0).toFloat(),
                            exposure = adjObj.optDouble("exposure", 0.0).toFloat(),
                            fade = adjObj.optDouble("fade", 0.0).toFloat(),
                            vignette = adjObj.optDouble("vignette", 0.0).toFloat()
                        )
                    } else {
                        AdjustmentsState()
                    }
                    add(
                        PresetRecipe(
                            id = obj.getString("id"),
                            name = obj.getString("name"),
                            subtitle = obj.optString("subtitle", "Preset personalizado"),
                            category = FilterCategory.CUSTOM,
                            filterId = obj.optString("filterId", "original"),
                            filterIntensity = obj.optDouble("filterIntensity", 1.0).toFloat(),
                            adjustments = adj,
                            isCustom = true
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun encodeExports(list: List<SavedExportItem>): String {
        val arr = JSONArray()
        list.forEach { item ->
            val obj = JSONObject().apply {
                put("id", item.id)
                put("uriString", item.uriString)
                put("displayName", item.displayName)
                put("width", item.width)
                put("height", item.height)
                put("format", item.format.name)
                put("sizeBytes", item.sizeBytes)
                put("timestampMillis", item.timestampMillis)
            }
            arr.put(obj)
        }
        return arr.toString()
    }

    private fun decodeExports(json: String): List<SavedExportItem> {
        return runCatching {
            val arr = JSONArray(json)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val formatName = obj.optString("format", ExportFormat.JPEG.name)
                    val format = ExportFormat.entries.firstOrNull { it.name == formatName } ?: ExportFormat.JPEG
                    add(
                        SavedExportItem(
                            id = obj.getString("id"),
                            uriString = obj.getString("uriString"),
                            displayName = obj.getString("displayName"),
                            width = obj.optInt("width", 0),
                            height = obj.optInt("height", 0),
                            format = format,
                            sizeBytes = obj.optLong("sizeBytes", 0L),
                            timestampMillis = obj.optLong("timestampMillis", 0L)
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }
}
