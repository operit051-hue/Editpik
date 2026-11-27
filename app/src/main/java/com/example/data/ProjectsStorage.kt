package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.engine.ImageProcessor
import com.example.model.AdjustmentsState
import com.example.model.AppliedCropStep
import com.example.model.BolpikProject
import com.example.model.BrushType
import com.example.model.CropRectNorm
import com.example.model.DrawStroke
import com.example.model.FrameConfig
import com.example.model.FrameStyle
import com.example.model.NormPoint
import com.example.model.ProjectEditState
import com.example.model.ProjectStatus
import com.example.model.StickerKind
import com.example.model.StickerLayer
import com.example.model.TextFontStyle
import com.example.model.TextLayer
import com.example.model.TransformState
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

class ProjectsStorage(private val context: Context) {

    private val rootDir: File
        get() = File(context.filesDir, "bolpik_projects").apply { mkdirs() }

    private val indexFile: File
        get() = File(rootDir, "projects_index.json")

    fun loadProjects(): List<BolpikProject> {
        if (!indexFile.exists()) return emptyList()
        return runCatching {
            val arr = JSONArray(indexFile.readText())
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val id = obj.getString("id")
                    val origFile = File(projectDir(id), "original.jpg")
                    if (!origFile.exists()) continue
                    val statusName = obj.optString("status", ProjectStatus.IMPORTED.name)
                    val status = ProjectStatus.entries.firstOrNull { it.name == statusName }
                        ?: ProjectStatus.IMPORTED
                    add(
                        BolpikProject(
                            id = id,
                            name = obj.optString("name", "Proyecto"),
                            sourceHash = obj.optString("sourceHash", id),
                            originalPath = origFile.absolutePath,
                            previewPath = File(projectDir(id), "preview.jpg").absolutePath,
                            width = obj.optInt("width", 0),
                            height = obj.optInt("height", 0),
                            status = status,
                            createdAtMillis = obj.optLong("createdAtMillis", System.currentTimeMillis()),
                            updatedAtMillis = obj.optLong("updatedAtMillis", System.currentTimeMillis()),
                            editCount = obj.optInt("editCount", 0)
                        )
                    )
                }
            }.sortedByDescending { it.updatedAtMillis }
        }.getOrDefault(emptyList())
    }

    fun findBySourceHash(sourceHash: String): BolpikProject? =
        loadProjects().firstOrNull { it.sourceHash == sourceHash }

    fun computeBitmapSignature(bitmap: Bitmap): String {
        val thumb = Bitmap.createScaledBitmap(bitmap, 32, 32, true)
        val pixels = IntArray(32 * 32)
        thumb.getPixels(pixels, 0, 32, 0, 0, 32, 32)
        if (thumb !== bitmap) thumb.recycle()
        val md = MessageDigest.getInstance("SHA-256")
        val buffer = java.nio.ByteBuffer.allocate(pixels.size * 4)
        buffer.asIntBuffer().put(pixels)
        md.update(buffer.array())
        val digest = md.digest()
        return "${bitmap.width}x${bitmap.height}_" + digest.take(12).joinToString("") { "%02x".format(it) }
    }

    fun createProject(
        name: String,
        sourceHash: String,
        originalBitmap: Bitmap
    ): BolpikProject {
        val id = java.util.UUID.randomUUID().toString()
        val dir = projectDir(id).apply { mkdirs() }
        val origFile = File(dir, "original.jpg")
        val previewFile = File(dir, "preview.jpg")

        FileOutputStream(origFile).use { out ->
            originalBitmap.compress(Bitmap.CompressFormat.JPEG, 94, out)
        }
        val previewBmp = ImageProcessor.scaleDownIfNeeded(originalBitmap, 480)
        FileOutputStream(previewFile).use { out ->
            previewBmp.compress(Bitmap.CompressFormat.JPEG, 84, out)
        }

        val now = System.currentTimeMillis()
        val project = BolpikProject(
            id = id,
            name = name,
            sourceHash = sourceHash,
            originalPath = origFile.absolutePath,
            previewPath = previewFile.absolutePath,
            width = originalBitmap.width,
            height = originalBitmap.height,
            status = ProjectStatus.IMPORTED,
            createdAtMillis = now,
            updatedAtMillis = now,
            editCount = 0
        )

        saveEditState(id, ProjectEditState())
        val updated = listOf(project) + loadProjects().filterNot { it.id == id }
        writeProjectsIndex(updated)
        return project
    }

    fun saveProjectStateAndPreview(
        projectId: String,
        editState: ProjectEditState,
        renderedPreviewBitmap: Bitmap,
        markExported: Boolean = false
    ): List<BolpikProject> {
        val dir = projectDir(projectId)
        if (!dir.exists()) dir.mkdirs()

        saveEditState(projectId, editState)

        val previewFile = File(dir, "preview.jpg")
        val thumb = ImageProcessor.scaleDownIfNeeded(renderedPreviewBitmap, 480)
        val tmpPreview = File(dir, "preview.tmp.jpg")
        FileOutputStream(tmpPreview).use { out ->
            thumb.compress(Bitmap.CompressFormat.JPEG, 85, out)
        }
        tmpPreview.renameTo(previewFile)

        val editCount = countEdits(editState)
        val current = loadProjects()
        val updated = current.map { p ->
            if (p.id == projectId) {
                val nextStatus = when {
                    markExported -> ProjectStatus.EXPORTED
                    p.status == ProjectStatus.EXPORTED && !editState.hasEdits -> ProjectStatus.IMPORTED
                    editState.hasEdits -> ProjectStatus.EDITING
                    else -> ProjectStatus.IMPORTED
                }
                p.copy(
                    status = nextStatus,
                    updatedAtMillis = System.currentTimeMillis(),
                    editCount = editCount,
                    previewPath = previewFile.absolutePath
                )
            } else p
        }.sortedByDescending { it.updatedAtMillis }

        writeProjectsIndex(updated)
        return updated
    }

    fun renameProject(projectId: String, newName: String): List<BolpikProject> {
        val clean = newName.trim()
        if (clean.isEmpty()) return loadProjects()
        val updated = loadProjects().map { p ->
            if (p.id == projectId) p.copy(name = clean, updatedAtMillis = System.currentTimeMillis())
            else p
        }.sortedByDescending { it.updatedAtMillis }
        writeProjectsIndex(updated)
        return updated
    }

    fun deleteProject(projectId: String): List<BolpikProject> {
        val updated = loadProjects().filterNot { it.id == projectId }
        writeProjectsIndex(updated)
        projectDir(projectId).deleteRecursively()
        return updated
    }

    fun loadOriginalBitmap(project: BolpikProject): Bitmap? {
        val file = File(project.originalPath)
        if (!file.exists()) return null
        return BitmapFactory.decodeFile(
            file.absolutePath,
            BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
        )
    }

    fun loadEditState(projectId: String): ProjectEditState {
        val file = File(projectDir(projectId), "edit_state.json")
        if (!file.exists()) return ProjectEditState()
        return runCatching {
            val root = JSONObject(file.readText())
            decodeEditState(root)
        }.getOrDefault(ProjectEditState())
    }

    private fun saveEditState(projectId: String, state: ProjectEditState) {
        val dir = projectDir(projectId).apply { mkdirs() }
        val target = File(dir, "edit_state.json")
        val tmp = File(dir, "edit_state.tmp.json")
        tmp.writeText(encodeEditState(state).toString())
        tmp.renameTo(target)
    }

    private fun writeProjectsIndex(projects: List<BolpikProject>) {
        val arr = JSONArray()
        projects.forEach { p ->
            arr.put(
                JSONObject().apply {
                    put("id", p.id)
                    put("name", p.name)
                    put("sourceHash", p.sourceHash)
                    put("width", p.width)
                    put("height", p.height)
                    put("status", p.status.name)
                    put("createdAtMillis", p.createdAtMillis)
                    put("updatedAtMillis", p.updatedAtMillis)
                    put("editCount", p.editCount)
                }
            )
        }
        val tmp = File(rootDir, "projects_index.tmp.json")
        tmp.writeText(arr.toString())
        tmp.renameTo(indexFile)
    }

    private fun projectDir(projectId: String): File = File(rootDir, projectId)

    private fun countEdits(state: ProjectEditState): Int {
        var count = state.adjustments.modifiedCount()
        if (state.filterId != "original") count++
        count += state.appliedCrops.size
        count += state.strokes.size
        count += state.textLayers.size
        count += state.stickerLayers.size
        if (state.frameConfig.style != FrameStyle.NONE) count++
        return count
    }

    private fun encodeEditState(state: ProjectEditState): JSONObject {
        val adjObj = JSONObject().apply {
            put("brightness", state.adjustments.brightness.toDouble())
            put("contrast", state.adjustments.contrast.toDouble())
            put("saturation", state.adjustments.saturation.toDouble())
            put("warmth", state.adjustments.warmth.toDouble())
            put("tint", state.adjustments.tint.toDouble())
            put("exposure", state.adjustments.exposure.toDouble())
            put("fade", state.adjustments.fade.toDouble())
            put("vignette", state.adjustments.vignette.toDouble())
        }

        val cropsArr = JSONArray()
        state.appliedCrops.forEach { step ->
            cropsArr.put(
                JSONObject().apply {
                    put("left", step.cropRect.left.toDouble())
                    put("top", step.cropRect.top.toDouble())
                    put("right", step.cropRect.right.toDouble())
                    put("bottom", step.cropRect.bottom.toDouble())
                    put("rotationQuarterTurns", step.transform.rotationQuarterTurns)
                    put("straightenDegrees", step.transform.straightenDegrees.toDouble())
                    put("flipHorizontal", step.transform.flipHorizontal)
                    put("flipVertical", step.transform.flipVertical)
                }
            )
        }

        val strokesArr = JSONArray()
        state.strokes.forEach { s ->
            val ptsArr = JSONArray()
            s.points.forEach { pt ->
                ptsArr.put(
                    JSONObject().apply {
                        put("x", pt.x.toDouble())
                        put("y", pt.y.toDouble())
                    }
                )
            }
            strokesArr.put(
                JSONObject().apply {
                    put("id", s.id)
                    put("colorArgb", s.colorArgb)
                    put("strokeWidthNorm", s.strokeWidthNorm.toDouble())
                    put("opacity", s.opacity.toDouble())
                    put("brushType", s.brushType.name)
                    put("points", ptsArr)
                }
            )
        }

        val textsArr = JSONArray()
        state.textLayers.forEach { t ->
            textsArr.put(
                JSONObject().apply {
                    put("id", t.id)
                    put("text", t.text)
                    put("xNorm", t.xNorm.toDouble())
                    put("yNorm", t.yNorm.toDouble())
                    put("scale", t.scale.toDouble())
                    put("rotationDeg", t.rotationDeg.toDouble())
                    put("colorArgb", t.colorArgb)
                    if (t.backgroundArgb != null) put("backgroundArgb", t.backgroundArgb)
                    put("fontStyle", t.fontStyle.name)
                    put("hasShadow", t.hasShadow)
                }
            )
        }

        val stickersArr = JSONArray()
        state.stickerLayers.forEach { st ->
            stickersArr.put(
                JSONObject().apply {
                    put("id", st.id)
                    put("kind", st.kind.name)
                    put("xNorm", st.xNorm.toDouble())
                    put("yNorm", st.yNorm.toDouble())
                    put("scale", st.scale.toDouble())
                    put("rotationDeg", st.rotationDeg.toDouble())
                    put("colorArgb", st.colorArgb)
                }
            )
        }

        val frameObj = JSONObject().apply {
            put("style", state.frameConfig.style.name)
            put("thicknessNorm", state.frameConfig.thicknessNorm.toDouble())
            put("colorArgb", state.frameConfig.colorArgb)
        }

        return JSONObject().apply {
            put("adjustments", adjObj)
            put("appliedCrops", cropsArr)
            put("filterId", state.filterId)
            put("filterIntensity", state.filterIntensity.toDouble())
            put("strokes", strokesArr)
            put("textLayers", textsArr)
            put("stickerLayers", stickersArr)
            put("frameConfig", frameObj)
        }
    }

    private fun decodeEditState(root: JSONObject): ProjectEditState {
        val adjObj = root.optJSONObject("adjustments")
        val adjustments = if (adjObj != null) {
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
        } else AdjustmentsState()

        val cropsArr = root.optJSONArray("appliedCrops") ?: JSONArray()
        val appliedCrops = buildList {
            for (i in 0 until cropsArr.length()) {
                val c = cropsArr.getJSONObject(i)
                add(
                    AppliedCropStep(
                        cropRect = CropRectNorm(
                            left = c.optDouble("left", 0.0).toFloat(),
                            top = c.optDouble("top", 0.0).toFloat(),
                            right = c.optDouble("right", 1.0).toFloat(),
                            bottom = c.optDouble("bottom", 1.0).toFloat()
                        ),
                        transform = TransformState(
                            rotationQuarterTurns = c.optInt("rotationQuarterTurns", 0),
                            straightenDegrees = c.optDouble("straightenDegrees", 0.0).toFloat(),
                            flipHorizontal = c.optBoolean("flipHorizontal", false),
                            flipVertical = c.optBoolean("flipVertical", false)
                        )
                    )
                )
            }
        }

        val strokesArr = root.optJSONArray("strokes") ?: JSONArray()
        val strokes = buildList {
            for (i in 0 until strokesArr.length()) {
                val s = strokesArr.getJSONObject(i)
                val ptsArr = s.optJSONArray("points") ?: JSONArray()
                val pts = buildList {
                    for (j in 0 until ptsArr.length()) {
                        val pt = ptsArr.getJSONObject(j)
                        add(NormPoint(pt.optDouble("x", 0.0).toFloat(), pt.optDouble("y", 0.0).toFloat()))
                    }
                }
                val brushName = s.optString("brushType", BrushType.NORMAL.name)
                val brush = BrushType.entries.firstOrNull { it.name == brushName } ?: BrushType.NORMAL
                add(
                    DrawStroke(
                        id = s.optString("id", java.util.UUID.randomUUID().toString()),
                        points = pts,
                        colorArgb = s.optLong("colorArgb", 0xFF10E88A),
                        strokeWidthNorm = s.optDouble("strokeWidthNorm", 0.018).toFloat(),
                        opacity = s.optDouble("opacity", 1.0).toFloat(),
                        brushType = brush
                    )
                )
            }
        }

        val textsArr = root.optJSONArray("textLayers") ?: JSONArray()
        val textLayers = buildList {
            for (i in 0 until textsArr.length()) {
                val t = textsArr.getJSONObject(i)
                val fontName = t.optString("fontStyle", TextFontStyle.GROTESK.name)
                val font = TextFontStyle.entries.firstOrNull { it.name == fontName } ?: TextFontStyle.GROTESK
                add(
                    TextLayer(
                        id = t.optString("id", java.util.UUID.randomUUID().toString()),
                        text = t.optString("text", ""),
                        xNorm = t.optDouble("xNorm", 0.5).toFloat(),
                        yNorm = t.optDouble("yNorm", 0.5).toFloat(),
                        scale = t.optDouble("scale", 1.0).toFloat(),
                        rotationDeg = t.optDouble("rotationDeg", 0.0).toFloat(),
                        colorArgb = t.optLong("colorArgb", 0xFFFFFFFF),
                        backgroundArgb = if (t.has("backgroundArgb")) t.getLong("backgroundArgb") else null,
                        fontStyle = font,
                        hasShadow = t.optBoolean("hasShadow", true)
                    )
                )
            }
        }

        val stickersArr = root.optJSONArray("stickerLayers") ?: JSONArray()
        val stickerLayers = buildList {
            for (i in 0 until stickersArr.length()) {
                val st = stickersArr.getJSONObject(i)
                val kindName = st.optString("kind", StickerKind.BOLPIK_TAG.name)
                val kind = StickerKind.entries.firstOrNull { it.name == kindName } ?: StickerKind.BOLPIK_TAG
                add(
                    StickerLayer(
                        id = st.optString("id", java.util.UUID.randomUUID().toString()),
                        kind = kind,
                        xNorm = st.optDouble("xNorm", 0.5).toFloat(),
                        yNorm = st.optDouble("yNorm", 0.5).toFloat(),
                        scale = st.optDouble("scale", 1.0).toFloat(),
                        rotationDeg = st.optDouble("rotationDeg", 0.0).toFloat(),
                        colorArgb = st.optLong("colorArgb", 0xFF10E88A)
                    )
                )
            }
        }

        val frameObj = root.optJSONObject("frameConfig")
        val frameConfig = if (frameObj != null) {
            val styleName = frameObj.optString("style", FrameStyle.NONE.name)
            val style = FrameStyle.entries.firstOrNull { it.name == styleName } ?: FrameStyle.NONE
            FrameConfig(
                style = style,
                thicknessNorm = frameObj.optDouble("thicknessNorm", 0.05).toFloat(),
                colorArgb = frameObj.optLong("colorArgb", 0xFFF2F7F4)
            )
        } else FrameConfig()

        return ProjectEditState(
            appliedCrops = appliedCrops,
            adjustments = adjustments,
            filterId = root.optString("filterId", "original"),
            filterIntensity = root.optDouble("filterIntensity", 1.0).toFloat(),
            strokes = strokes,
            textLayers = textLayers,
            stickerLayers = stickerLayers,
            frameConfig = frameConfig
        )
    }
}
