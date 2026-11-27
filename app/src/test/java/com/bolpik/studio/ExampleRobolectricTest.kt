package com.bolpik.studio

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.bolpik.studio.data.ProjectsStorage
import com.bolpik.studio.engine.CropDragHandle
import com.bolpik.studio.engine.ImageProcessor
import com.bolpik.studio.model.AdjustmentsState
import com.bolpik.studio.model.AppliedCropStep
import com.bolpik.studio.model.BrushType
import com.bolpik.studio.model.CollageSlotItem
import com.bolpik.studio.model.CollageTemplate
import com.bolpik.studio.model.CropRectNorm
import com.bolpik.studio.model.DrawStroke
import com.bolpik.studio.model.FrameConfig
import com.bolpik.studio.model.FrameStyle
import com.bolpik.studio.model.NormPoint
import com.bolpik.studio.model.ProjectEditState
import com.bolpik.studio.model.ProjectStatus
import com.bolpik.studio.model.StickerKind
import com.bolpik.studio.model.StickerLayer
import com.bolpik.studio.model.TextFontStyle
import com.bolpik.studio.model.TextLayer
import com.bolpik.studio.model.TransformState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private fun createSolidBitmap(w: Int, h: Int, color: Int): Bitmap {
        val pixels = IntArray(w * h) { color }
        return Bitmap.createBitmap(pixels, w, h, Bitmap.Config.ARGB_8888)
    }

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Bolpik", appName)
    }

    @Test
    fun `combined color matrix preserves identity when unchanged`() {
        val matrix = ImageProcessor.buildCombinedColorMatrixArray(
            adjustments = AdjustmentsState(),
            filterId = "original",
            filterIntensity = 1f
        )
        assertEquals(20, matrix.size)
        assertEquals(1f, matrix[0], 0.001f)
        assertEquals(1f, matrix[6], 0.001f)
        assertEquals(1f, matrix[12], 0.001f)
        assertEquals(0f, matrix[4], 0.001f)
    }

    @Test
    fun `apply crop and rotate swaps dimensions properly`() {
        val src = Bitmap.createBitmap(200, 100, Bitmap.Config.ARGB_8888)
        val rotated = ImageProcessor.applyCropAndTransform(
            source = src,
            cropRect = CropRectNorm(),
            transform = TransformState(rotationQuarterTurns = 90)
        )
        assertEquals(100, rotated.width)
        assertEquals(200, rotated.height)

        val cropped = ImageProcessor.applyCropAndTransform(
            source = src,
            cropRect = CropRectNorm(left = 0.25f, top = 0f, right = 0.75f, bottom = 1f),
            transform = TransformState()
        )
        assertEquals(100, cropped.width)
        assertEquals(100, cropped.height)
    }

    @Test
    fun `collage layout computes expected slot count`() {
        val rects = ImageProcessor.computeCollageRects(
            template = CollageTemplate.FOUR_BENTO,
            w = 1000f,
            h = 1000f,
            spacingNorm = 0.02f
        )
        assertEquals(4, rects.size)
        assertTrue(rects.all { it.width() > 0f && it.height() > 0f })

        val slots = List(4) { CollageSlotItem(index = it) }
        val bmp = ImageProcessor.renderCollageBitmap(
            slots = slots,
            template = CollageTemplate.FOUR_BENTO,
            spacingNorm = 0.02f,
            cornerRadiusNorm = 0.02f,
            bgColorArgb = 0xFF16171E,
            aspectRatio = 1f,
            targetWidth = 400
        )
        assertEquals(400, bmp.width)
        assertEquals(400, bmp.height)
    }

    @Test
    fun `projects storage creates persists deduplicates and orders newest first`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val storage = ProjectsStorage(context)

        val bmp1 = createSolidBitmap(120, 90, Color.GREEN)
        val hash1 = storage.computeBitmapSignature(bmp1)
        val p1 = storage.createProject("Esmeralda 1", hash1, bmp1)
        assertTrue(File(p1.originalPath).exists())
        assertTrue(File(p1.previewPath).exists())

        Thread.sleep(5)
        val bmp2 = createSolidBitmap(140, 105, Color.BLUE)
        val hash2 = storage.computeBitmapSignature(bmp2)
        val p2 = storage.createProject("Esmeralda 2", hash2, bmp2)

        val loaded = storage.loadProjects()
        assertEquals(2, loaded.size)
        assertEquals(p2.id, loaded.first().id)

        // Duplicate detection returns existing project for matching signature
        val duplicate = storage.findBySourceHash(hash1)
        assertNotNull(duplicate)
        assertEquals(p1.id, duplicate?.id)
    }

    @Test
    fun `projects storage saves and restores non-destructive edit state and handles rename and delete`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val storage = ProjectsStorage(context)

        val bmp = createSolidBitmap(160, 120, Color.CYAN)
        val project = storage.createProject("Original", storage.computeBitmapSignature(bmp), bmp)

        val editState = ProjectEditState(
            appliedCrops = listOf(
                AppliedCropStep(
                    cropRect = CropRectNorm(0.1f, 0.1f, 0.9f, 0.9f),
                    transform = TransformState(rotationQuarterTurns = 90, flipHorizontal = true)
                )
            ),
            adjustments = AdjustmentsState(exposure = 24f, contrast = -12f, warmth = 18f),
            filterId = "jade_cine",
            filterIntensity = 0.75f,
            strokes = listOf(
                DrawStroke(
                    points = listOf(NormPoint(0.2f, 0.2f), NormPoint(0.8f, 0.8f)),
                    colorArgb = 0xFF10E88A,
                    strokeWidthNorm = 0.02f,
                    opacity = 0.9f,
                    brushType = BrushType.NEON
                )
            ),
            textLayers = listOf(
                TextLayer(
                    text = "Bolpik Pro",
                    xNorm = 0.4f,
                    yNorm = 0.6f,
                    fontStyle = TextFontStyle.MONO
                )
            ),
            stickerLayers = listOf(
                StickerLayer(kind = StickerKind.RAW_BADGE, xNorm = 0.7f, yNorm = 0.3f)
            ),
            frameConfig = FrameConfig(style = FrameStyle.POLAROID, thicknessNorm = 0.07f)
        )

        val afterSave = storage.saveProjectStateAndPreview(project.id, editState, bmp)
        val updated = afterSave.first { it.id == project.id }
        assertEquals(ProjectStatus.EDITING, updated.status)

        val restored = storage.loadEditState(project.id)
        assertEquals(1, restored.appliedCrops.size)
        assertEquals(90, restored.appliedCrops.first().transform.rotationQuarterTurns)
        assertTrue(restored.appliedCrops.first().transform.flipHorizontal)
        assertEquals(24f, restored.adjustments.exposure, 0.01f)
        assertEquals("jade_cine", restored.filterId)
        assertEquals(0.75f, restored.filterIntensity, 0.01f)
        assertEquals(1, restored.strokes.size)
        assertEquals(BrushType.NEON, restored.strokes.first().brushType)
        assertEquals("Bolpik Pro", restored.textLayers.first().text)
        assertEquals(StickerKind.RAW_BADGE, restored.stickerLayers.first().kind)
        assertEquals(FrameStyle.POLAROID, restored.frameConfig.style)

        val renamedList = storage.renameProject(project.id, "Retrato Verde")
        assertEquals("Retrato Verde", renamedList.first { it.id == project.id }.name)

        // Exporting updates status without clobbering edit_state.json
        val exportedList = storage.markProjectExported(project.id)
        assertEquals(ProjectStatus.EXPORTED, exportedList.first { it.id == project.id }.status)
        val restoredAfterExport = storage.loadEditState(project.id)
        assertEquals("jade_cine", restoredAfterExport.filterId)
        assertEquals(1, restoredAfterExport.textLayers.size)
        assertEquals(1, restoredAfterExport.stickerLayers.size)

        val afterDelete = storage.deleteProject(project.id)
        assertNull(afterDelete.firstOrNull { it.id == project.id })
        assertFalse(File(project.originalPath).exists())
    }

    @Test
    fun `computeUpdatedCropRect preserves locked aspect ratios on non-square images`() {
        val landscapeAspect = 4f / 3f
        val startSquare = CropRectNorm(0.125f, 0f, 0.875f, 1f) // physical 1:1 on 4:3
        val dragged = ImageProcessor.computeUpdatedCropRect(
            current = startSquare,
            handle = CropDragHandle.BOTTOM_RIGHT,
            dxNorm = -0.15f,
            dyNorm = -0.15f,
            lockedAspect = 1f,
            imgAspect = landscapeAspect
        )
        val physicalAspect = (dragged.width * landscapeAspect) / dragged.height
        assertEquals(1f, physicalAspect, 0.02f)

        val portraitAspect = 3f / 4f
        val startWide = CropRectNorm(0f, 0.25f, 1f, 0.671875f) // physical 16:9 on 3:4
        val draggedWide = ImageProcessor.computeUpdatedCropRect(
            current = startWide,
            handle = CropDragHandle.TOP_LEFT,
            dxNorm = 0.2f,
            dyNorm = 0.1f,
            lockedAspect = 16f / 9f,
            imgAspect = portraitAspect
        )
        val widePhysicalAspect = (draggedWide.width * portraitAspect) / draggedWide.height
        assertEquals(16f / 9f, widePhysicalAspect, 0.03f)
    }
}

