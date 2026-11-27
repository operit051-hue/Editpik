package com.bolpik.studio.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bolpik.studio.data.BolpikRepository
import com.bolpik.studio.data.PresetsCatalog
import com.bolpik.studio.data.ProjectsStorage
import com.bolpik.studio.engine.ImageProcessor
import com.bolpik.studio.model.AdjustmentParam
import com.bolpik.studio.model.AdjustmentsState
import com.bolpik.studio.model.AppliedCropStep
import com.bolpik.studio.model.BolpikProject
import com.bolpik.studio.model.BrushType
import com.bolpik.studio.model.CollageSlotItem
import com.bolpik.studio.model.CollageTemplate
import com.bolpik.studio.model.CropAspectRatio
import com.bolpik.studio.model.CropRectNorm
import com.bolpik.studio.model.DrawStroke
import com.bolpik.studio.model.EditorDocumentSnapshot
import com.bolpik.studio.model.EditorToolTab
import com.bolpik.studio.model.ExportFormat
import com.bolpik.studio.model.FilterCategory
import com.bolpik.studio.model.FrameConfig
import com.bolpik.studio.model.FrameStyle
import com.bolpik.studio.model.PresetRecipe
import com.bolpik.studio.model.ProjectEditState
import com.bolpik.studio.model.SamplePhoto
import com.bolpik.studio.model.SavedExportItem
import com.bolpik.studio.model.StickerKind
import com.bolpik.studio.model.StickerLayer
import com.bolpik.studio.model.TextFontStyle
import com.bolpik.studio.model.TextLayer
import com.bolpik.studio.model.TransformState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class EditorUiState(
    val activeProjectId: String? = null,
    val projectTitle: String = "Proyecto",
    val originalBitmap: Bitmap? = null,
    val workingBitmap: Bitmap? = null,
    val appliedCrops: List<AppliedCropStep> = emptyList(),
    val adjustments: AdjustmentsState = AdjustmentsState(),
    val selectedAdjParam: AdjustmentParam = AdjustmentParam.EXPOSURE,
    val activeFilterId: String = "original",
    val filterIntensity: Float = 1f,
    val filterThumbnails: Map<String, Bitmap> = emptyMap(),
    val cropAspect: CropAspectRatio = CropAspectRatio.FREE,
    val cropRect: CropRectNorm = CropRectNorm(),
    val transform: TransformState = TransformState(),
    val strokes: List<DrawStroke> = emptyList(),
    val activeBrushType: BrushType = BrushType.NORMAL,
    val activeBrushColor: Long = 0xFF10E88A,
    val activeBrushSizeNorm: Float = 0.018f,
    val activeBrushOpacity: Float = 1f,
    val textLayers: List<TextLayer> = emptyList(),
    val selectedTextId: String? = null,
    val stickerLayers: List<StickerLayer> = emptyList(),
    val selectedStickerId: String? = null,
    val frameConfig: FrameConfig = FrameConfig(),
    val activeToolTab: EditorToolTab = EditorToolTab.ADJUST,
    val isComparing: Boolean = false,
    val splitPosition: Float = 0.5f,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val isBusy: Boolean = false,
    val lastAutoSavedMillis: Long = 0L,
    val statusMessage: String? = null
)

data class ExportUiState(
    val targetProjectId: String? = null,
    val targetProjectName: String = "Proyecto",
    val previewBitmap: Bitmap? = null,
    val format: ExportFormat = ExportFormat.JPEG,
    val quality: Int = 92,
    val addWatermark: Boolean = false,
    val estimatedSizeBytes: Long = 0L,
    val isExporting: Boolean = false,
    val savedItem: SavedExportItem? = null
)

data class CollageUiState(
    val template: CollageTemplate = CollageTemplate.FOUR_GRID,
    val slots: List<CollageSlotItem> = List(6) { CollageSlotItem(index = it) },
    val spacingNorm: Float = 0.022f,
    val cornerRadiusNorm: Float = 0.025f,
    val bgColorArgb: Long = 0xFF101814,
    val aspectRatio: Float = 1f,
    val isBusy: Boolean = false
)

data class PresetsUiState(
    val selectedCategory: FilterCategory = FilterCategory.ALL,
    val previewBitmap: Bitmap? = null,
    val previewTitle: String = "Neón Nocturno",
    val selectedSampleId: String? = null,
    val previewUri: Uri? = null,
    val selectedRecipeId: String? = null
)

class BolpikViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = BolpikRepository(application.applicationContext)
    private val projectsStorage = ProjectsStorage(application.applicationContext)
    private val appContext get() = getApplication<Application>().applicationContext

    val darkroomMode: StateFlow<Boolean> = repository.darkroomModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val appLanguage: StateFlow<String> = repository.appLanguageFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "es")

    val customPresets: StateFlow<List<PresetRecipe>> = repository.customPresetsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val exportsHistory: StateFlow<List<SavedExportItem>> = repository.exportsHistoryFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _projects = MutableStateFlow<List<BolpikProject>>(emptyList())
    val projects: StateFlow<List<BolpikProject>> = _projects.asStateFlow()

    private val _editorState = MutableStateFlow(EditorUiState())
    val editorState: StateFlow<EditorUiState> = _editorState.asStateFlow()

    private val _exportState = MutableStateFlow(ExportUiState())
    val exportState: StateFlow<ExportUiState> = _exportState.asStateFlow()

    private val _collageState = MutableStateFlow(CollageUiState())
    val collageState: StateFlow<CollageUiState> = _collageState.asStateFlow()

    private val _presetsState = MutableStateFlow(PresetsUiState())
    val presetsState: StateFlow<PresetsUiState> = _presetsState.asStateFlow()

    private val undoStack = ArrayDeque<EditorDocumentSnapshot>()
    private val redoStack = ArrayDeque<EditorDocumentSnapshot>()
    private var pendingUndoSnapshot: EditorDocumentSnapshot? = null
    private var autoSaveJob: Job? = null

    init {
        preloadStudioDefaults()
        refreshProjects()
    }

    fun refreshProjects() {
        viewModelScope.launch {
            val loaded = withContext(Dispatchers.IO) {
                projectsStorage.loadProjects()
            }
            _projects.value = loaded
        }
    }

    private fun preloadStudioDefaults() {
        viewModelScope.launch {
            val firstSample = PresetsCatalog.samplePhotos.first()
            val sampleBmp = withContext(Dispatchers.IO) {
                ImageProcessor.decodeDrawableBitmap(appContext, firstSample.drawableRes, 900)
            }
            if (sampleBmp != null) {
                _presetsState.update {
                    it.copy(
                        previewBitmap = sampleBmp,
                        previewTitle = firstSample.title,
                        selectedSampleId = firstSample.id,
                        previewUri = null,
                        selectedRecipeId = PresetsCatalog.builtInRecipes.first().id
                    )
                }
            }

            val collageSamples = withContext(Dispatchers.IO) {
                PresetsCatalog.samplePhotos.mapNotNull { sample ->
                    ImageProcessor.decodeDrawableBitmap(appContext, sample.drawableRes, 800)?.let {
                        sample.title to it
                    }
                }
            }
            if (collageSamples.isNotEmpty()) {
                _collageState.update { current ->
                    val updatedSlots = current.slots.mapIndexed { idx, slot ->
                        if (idx < collageSamples.size) {
                            slot.copy(
                                bitmap = collageSamples[idx].second,
                                label = collageSamples[idx].first
                            )
                        } else {
                            slot
                        }
                    }
                    current.copy(slots = updatedSlots)
                }
            }
        }
    }

    fun toggleDarkroomMode() {
        viewModelScope.launch {
            repository.setDarkroomMode(!darkroomMode.value)
        }
    }

    fun setAppLanguage(langCode: String) {
        viewModelScope.launch {
            repository.setAppLanguage(langCode)
        }
    }

    fun toggleLanguage() {
        val next = if (appLanguage.value == "es") "en" else "es"
        setAppLanguage(next)
    }

    fun openUriInEditor(
        uri: Uri,
        title: String? = null,
        initialTab: EditorToolTab = EditorToolTab.ADJUST,
        initialRecipe: PresetRecipe? = null,
        onOpened: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _editorState.update { it.copy(isBusy = true, statusMessage = null) }
            val bmp = withContext(Dispatchers.IO) {
                ImageProcessor.decodeUriBitmap(appContext, uri, 1600)
            }
            if (bmp != null) {
                val defaultName = title ?: "Foto ${SimpleDateFormat("dd MMM HH:mm", Locale.getDefault()).format(Date())}"
                openOrCreateProjectForBitmap(
                    suggestedName = defaultName,
                    bitmap = bmp,
                    initialTab = initialTab,
                    initialRecipe = initialRecipe
                )
                onOpened()
            } else {
                _editorState.update {
                    it.copy(isBusy = false, statusMessage = "No se pudo cargar la imagen seleccionada")
                }
            }
        }
    }

    fun openSampleInEditor(
        sample: SamplePhoto,
        initialTab: EditorToolTab = EditorToolTab.ADJUST,
        initialRecipe: PresetRecipe? = null
    ) {
        viewModelScope.launch {
            _editorState.update { it.copy(isBusy = true, statusMessage = null) }
            val bmp = withContext(Dispatchers.IO) {
                ImageProcessor.decodeDrawableBitmap(appContext, sample.drawableRes, 1600)
            }
            if (bmp != null) {
                openOrCreateProjectForBitmap(
                    suggestedName = sample.title,
                    bitmap = bmp,
                    initialTab = initialTab,
                    initialRecipe = initialRecipe
                )
            } else {
                _editorState.update { it.copy(isBusy = false, statusMessage = "No se pudo abrir la imagen") }
            }
        }
    }

    fun openProjectInEditor(
        project: BolpikProject,
        initialTab: EditorToolTab = EditorToolTab.ADJUST,
        onOpened: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _editorState.update { it.copy(isBusy = true, statusMessage = null) }
            val loaded = withContext(Dispatchers.IO) {
                val orig = projectsStorage.loadOriginalBitmap(project) ?: return@withContext null
                val editState = projectsStorage.loadEditState(project.id)
                var working = orig
                editState.appliedCrops.forEach { step ->
                    working = ImageProcessor.applyCropAndTransform(working, step.cropRect, step.transform)
                }
                Triple(orig, working, editState)
            }
            if (loaded != null) {
                val (orig, working, editState) = loaded
                initializeEditorFromProject(
                    project = project,
                    originalBitmap = orig,
                    workingBitmap = working,
                    editState = editState,
                    initialTab = initialTab
                )
                onOpened()
            } else {
                _editorState.update {
                    it.copy(isBusy = false, statusMessage = "No se encontró el archivo original del proyecto")
                }
            }
        }
    }

    private suspend fun openOrCreateProjectForBitmap(
        suggestedName: String,
        bitmap: Bitmap,
        initialTab: EditorToolTab,
        initialRecipe: PresetRecipe?
    ) {
        val signature = withContext(Dispatchers.Default) {
            projectsStorage.computeBitmapSignature(bitmap)
        }
        val existing = withContext(Dispatchers.IO) {
            projectsStorage.findBySourceHash(signature)
        }

        if (existing != null) {
            // Avoid creating duplicate projects when opening an already imported photo!
            val loaded = withContext(Dispatchers.IO) {
                val orig = projectsStorage.loadOriginalBitmap(existing) ?: bitmap
                val editState = projectsStorage.loadEditState(existing.id)
                var working = orig
                editState.appliedCrops.forEach { step ->
                    working = ImageProcessor.applyCropAndTransform(working, step.cropRect, step.transform)
                }
                Triple(orig, working, editState)
            }
            val finalState = if (initialRecipe != null) {
                loaded.third.copy(
                    adjustments = initialRecipe.adjustments,
                    filterId = initialRecipe.filterId,
                    filterIntensity = initialRecipe.filterIntensity
                )
            } else loaded.third

            initializeEditorFromProject(
                project = existing,
                originalBitmap = loaded.first,
                workingBitmap = loaded.second,
                editState = finalState,
                initialTab = initialTab
            )
            if (initialRecipe != null) {
                flushAutoSaveNow()
            }
            refreshProjects()
            return
        }

        // Create new project immediately on import (even before any edits!)
        val created = withContext(Dispatchers.IO) {
            projectsStorage.createProject(
                name = suggestedName,
                sourceHash = signature,
                originalBitmap = bitmap
            )
        }
        val initialEditState = if (initialRecipe != null) {
            ProjectEditState(
                adjustments = initialRecipe.adjustments,
                filterId = initialRecipe.filterId,
                filterIntensity = initialRecipe.filterIntensity
            )
        } else ProjectEditState()

        initializeEditorFromProject(
            project = created,
            originalBitmap = bitmap,
            workingBitmap = bitmap,
            editState = initialEditState,
            initialTab = initialTab
        )
        if (initialRecipe != null) {
            flushAutoSaveNow()
        } else {
            refreshProjects()
        }
    }

    private suspend fun initializeEditorFromProject(
        project: BolpikProject,
        originalBitmap: Bitmap,
        workingBitmap: Bitmap,
        editState: ProjectEditState,
        initialTab: EditorToolTab
    ) {
        undoStack.clear()
        redoStack.clear()
        pendingUndoSnapshot = null

        _editorState.value = EditorUiState(
            activeProjectId = project.id,
            projectTitle = project.name,
            originalBitmap = originalBitmap,
            workingBitmap = workingBitmap,
            appliedCrops = editState.appliedCrops,
            adjustments = editState.adjustments,
            activeFilterId = editState.filterId,
            filterIntensity = editState.filterIntensity,
            strokes = editState.strokes,
            textLayers = editState.textLayers,
            stickerLayers = editState.stickerLayers,
            frameConfig = editState.frameConfig,
            activeToolTab = initialTab,
            lastAutoSavedMillis = project.updatedAtMillis,
            isBusy = false
        )

        val thumbs = withContext(Dispatchers.Default) {
            PresetsCatalog.filters.associate { filter ->
                filter.id to ImageProcessor.renderFilterThumbnail(workingBitmap, filter.id, 140)
            }
        }
        _editorState.update { it.copy(filterThumbnails = thumbs) }
    }

    private fun scheduleAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(350)
            performAutoSave()
        }
    }

    fun flushAutoSaveNow() {
        autoSaveJob?.cancel()
        viewModelScope.launch(NonCancellable) {
            performAutoSave()
        }
    }

    private suspend fun performAutoSave(markExported: Boolean = false) {
        val state = _editorState.value
        val projectId = state.activeProjectId ?: return
        val working = state.workingBitmap ?: return

        val editState = ProjectEditState(
            appliedCrops = state.appliedCrops,
            adjustments = state.adjustments,
            filterId = state.activeFilterId,
            filterIntensity = state.filterIntensity,
            strokes = state.strokes,
            textLayers = state.textLayers,
            stickerLayers = state.stickerLayers,
            frameConfig = state.frameConfig
        )

        val updatedList = withContext(Dispatchers.IO + NonCancellable) {
            val scaled = ImageProcessor.scaleDownIfNeeded(working, 540)
            val previewComposite = ImageProcessor.renderFinalComposite(
                context = appContext,
                source = scaled,
                adjustments = state.adjustments,
                filterId = state.activeFilterId,
                filterIntensity = state.filterIntensity,
                strokes = state.strokes,
                textLayers = state.textLayers,
                stickerLayers = state.stickerLayers,
                frameConfig = state.frameConfig,
                addWatermark = false
            )
            if (scaled !== working) {
                scaled.recycle()
            }
            val savedList = projectsStorage.saveProjectStateAndPreview(
                projectId = projectId,
                editState = editState,
                renderedPreviewBitmap = previewComposite,
                markExported = markExported
            )
            if (previewComposite !== working) {
                previewComposite.recycle()
            }
            savedList
        }
        _projects.value = updatedList
        _editorState.update { it.copy(lastAutoSavedMillis = System.currentTimeMillis()) }
    }

    private fun currentSnapshot(): EditorDocumentSnapshot {
        val s = _editorState.value
        return EditorDocumentSnapshot(
            workingBitmap = s.workingBitmap,
            appliedCrops = s.appliedCrops,
            adjustments = s.adjustments,
            filterId = s.activeFilterId,
            filterIntensity = s.filterIntensity,
            strokes = s.strokes,
            textLayers = s.textLayers,
            stickerLayers = s.stickerLayers,
            frameConfig = s.frameConfig
        )
    }

    private fun pushUndoSnapshot(snapshot: EditorDocumentSnapshot = currentSnapshot()) {
        if (undoStack.size >= 25) {
            undoStack.removeFirst()
        }
        undoStack.addLast(snapshot)
        redoStack.clear()
        syncUndoRedoFlags()
    }

    private fun applySnapshot(snapshot: EditorDocumentSnapshot) {
        _editorState.update {
            it.copy(
                workingBitmap = snapshot.workingBitmap,
                appliedCrops = snapshot.appliedCrops,
                adjustments = snapshot.adjustments,
                activeFilterId = snapshot.filterId,
                filterIntensity = snapshot.filterIntensity,
                strokes = snapshot.strokes,
                textLayers = snapshot.textLayers,
                stickerLayers = snapshot.stickerLayers,
                frameConfig = snapshot.frameConfig,
                selectedTextId = null,
                selectedStickerId = null
            )
        }
        syncUndoRedoFlags()
        scheduleAutoSave()
    }

    private fun syncUndoRedoFlags() {
        _editorState.update {
            it.copy(
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty()
            )
        }
    }

    fun undo() {
        val prev = undoStack.removeLastOrNull() ?: return
        redoStack.addLast(currentSnapshot())
        applySnapshot(prev)
    }

    fun redo() {
        val next = redoStack.removeLastOrNull() ?: return
        undoStack.addLast(currentSnapshot())
        applySnapshot(next)
    }

    fun resetAllEdits() {
        val orig = _editorState.value.originalBitmap ?: return
        pushUndoSnapshot()
        _editorState.update {
            it.copy(
                workingBitmap = orig,
                appliedCrops = emptyList(),
                adjustments = AdjustmentsState(),
                activeFilterId = "original",
                filterIntensity = 1f,
                cropAspect = CropAspectRatio.FREE,
                cropRect = CropRectNorm(),
                transform = TransformState(),
                strokes = emptyList(),
                textLayers = emptyList(),
                stickerLayers = emptyList(),
                frameConfig = FrameConfig(),
                selectedTextId = null,
                selectedStickerId = null,
                statusMessage = "Ediciones restablecidas"
            )
        }
        regenerateFilterThumbnails(orig)
        scheduleAutoSave()
    }

    fun selectToolTab(tab: EditorToolTab) {
        _editorState.update {
            it.copy(
                activeToolTab = tab,
                selectedTextId = if (tab == EditorToolTab.TEXT) it.selectedTextId else null,
                selectedStickerId = if (tab == EditorToolTab.STICKERS) it.selectedStickerId else null
            )
        }
    }

    fun toggleCompareMode() {
        _editorState.update { it.copy(isComparing = !it.isComparing) }
    }

    fun updateSplitPosition(pos: Float) {
        _editorState.update { it.copy(splitPosition = pos.coerceIn(0.05f, 0.95f)) }
    }

    // --- Adjustments ---
    fun selectAdjustmentParam(param: AdjustmentParam) {
        _editorState.update { it.copy(selectedAdjParam = param) }
    }

    fun commitContinuousEdit() {
        val pending = pendingUndoSnapshot
        pendingUndoSnapshot = null
        if (pending != null && pending != currentSnapshot()) {
            pushUndoSnapshot(pending)
        }
        scheduleAutoSave()
    }

    fun updateAdjustmentValue(param: AdjustmentParam, value: Float) {
        if (pendingUndoSnapshot == null) {
            pendingUndoSnapshot = currentSnapshot()
        }
        _editorState.update { state ->
            val adj = state.adjustments
            val updated = when (param) {
                AdjustmentParam.EXPOSURE -> adj.copy(exposure = value)
                AdjustmentParam.BRIGHTNESS -> adj.copy(brightness = value)
                AdjustmentParam.CONTRAST -> adj.copy(contrast = value)
                AdjustmentParam.SATURATION -> adj.copy(saturation = value)
                AdjustmentParam.WARMTH -> adj.copy(warmth = value)
                AdjustmentParam.TINT -> adj.copy(tint = value)
                AdjustmentParam.FADE -> adj.copy(fade = value)
                AdjustmentParam.VIGNETTE -> adj.copy(vignette = value)
            }
            state.copy(adjustments = updated)
        }
    }

    fun resetCurrentAdjustmentParam() {
        pushUndoSnapshot()
        val param = _editorState.value.selectedAdjParam
        _editorState.update { state ->
            val adj = state.adjustments
            val updated = when (param) {
                AdjustmentParam.EXPOSURE -> adj.copy(exposure = 0f)
                AdjustmentParam.BRIGHTNESS -> adj.copy(brightness = 0f)
                AdjustmentParam.CONTRAST -> adj.copy(contrast = 0f)
                AdjustmentParam.SATURATION -> adj.copy(saturation = 0f)
                AdjustmentParam.WARMTH -> adj.copy(warmth = 0f)
                AdjustmentParam.TINT -> adj.copy(tint = 0f)
                AdjustmentParam.FADE -> adj.copy(fade = 0f)
                AdjustmentParam.VIGNETTE -> adj.copy(vignette = 0f)
            }
            state.copy(adjustments = updated)
        }
        scheduleAutoSave()
    }

    // --- Filters ---
    fun selectFilter(filterId: String) {
        if (_editorState.value.activeFilterId == filterId) return
        pushUndoSnapshot()
        _editorState.update {
            it.copy(
                activeFilterId = filterId,
                filterIntensity = if (filterId == "original") 1f else 0.9f
            )
        }
        scheduleAutoSave()
    }

    fun updateFilterIntensity(intensity: Float) {
        if (pendingUndoSnapshot == null) {
            pendingUndoSnapshot = currentSnapshot()
        }
        _editorState.update { it.copy(filterIntensity = intensity.coerceIn(0f, 1f)) }
    }

    // --- Crop & Transform ---
    fun selectCropAspect(aspect: CropAspectRatio) {
        val bmp = _editorState.value.workingBitmap ?: return
        val ratio = aspect.ratio
        if (ratio == null) {
            _editorState.update { it.copy(cropAspect = aspect, cropRect = CropRectNorm()) }
            return
        }
        val imgAspect = bmp.width.toFloat() / bmp.height.toFloat().coerceAtLeast(1f)
        val newRect = if (imgAspect > ratio) {
            val targetNormW = (ratio / imgAspect).coerceIn(0.1f, 1f)
            val margin = (1f - targetNormW) / 2f
            CropRectNorm(left = margin, top = 0f, right = 1f - margin, bottom = 1f)
        } else {
            val targetNormH = (imgAspect / ratio).coerceIn(0.1f, 1f)
            val margin = (1f - targetNormH) / 2f
            CropRectNorm(left = 0f, top = margin, right = 1f, bottom = 1f - margin)
        }
        _editorState.update { it.copy(cropAspect = aspect, cropRect = newRect) }
    }

    fun updateCropRect(rect: CropRectNorm) {
        _editorState.update { it.copy(cropRect = rect) }
    }

    fun rotate90(clockwise: Boolean = true) {
        _editorState.update { state ->
            val delta = if (clockwise) 90 else 270
            val nextRot = (state.transform.rotationQuarterTurns + delta) % 360
            state.copy(transform = state.transform.copy(rotationQuarterTurns = nextRot))
        }
    }

    fun updateStraighten(degrees: Float) {
        _editorState.update {
            it.copy(transform = it.transform.copy(straightenDegrees = degrees.coerceIn(-45f, 45f)))
        }
    }

    fun toggleFlipHorizontal() {
        _editorState.update {
            it.copy(transform = it.transform.copy(flipHorizontal = !it.transform.flipHorizontal))
        }
    }

    fun toggleFlipVertical() {
        _editorState.update {
            it.copy(transform = it.transform.copy(flipVertical = !it.transform.flipVertical))
        }
    }

    fun resetCropAndTransform() {
        _editorState.update {
            it.copy(
                cropAspect = CropAspectRatio.FREE,
                cropRect = CropRectNorm(),
                transform = TransformState()
            )
        }
    }

    fun applyCropAndTransform() {
        val state = _editorState.value
        val src = state.workingBitmap ?: return
        val hasCropOrTransform = !state.cropRect.isFull ||
            state.transform.rotationQuarterTurns != 0 ||
            state.transform.straightenDegrees != 0f ||
            state.transform.flipHorizontal ||
            state.transform.flipVertical
        if (!hasCropOrTransform) return

        val step = AppliedCropStep(cropRect = state.cropRect, transform = state.transform)
        viewModelScope.launch {
            pushUndoSnapshot()
            _editorState.update { it.copy(isBusy = true) }
            val result = withContext(Dispatchers.Default) {
                ImageProcessor.applyCropAndTransform(src, state.cropRect, state.transform)
            }
            _editorState.update {
                it.copy(
                    workingBitmap = result,
                    appliedCrops = it.appliedCrops + step,
                    cropAspect = CropAspectRatio.FREE,
                    cropRect = CropRectNorm(),
                    transform = TransformState(),
                    isBusy = false,
                    statusMessage = "Recorte aplicado (${result.width}×${result.height})"
                )
            }
            regenerateFilterThumbnails(result)
            scheduleAutoSave()
        }
    }

    private fun regenerateFilterThumbnails(bitmap: Bitmap) {
        viewModelScope.launch {
            val thumbs = withContext(Dispatchers.Default) {
                PresetsCatalog.filters.associate { filter ->
                    filter.id to ImageProcessor.renderFilterThumbnail(bitmap, filter.id, 140)
                }
            }
            _editorState.update { it.copy(filterThumbnails = thumbs) }
        }
    }

    // --- Brush / Draw ---
    fun setBrushType(type: BrushType) {
        _editorState.update { it.copy(activeBrushType = type) }
    }

    fun setBrushColor(colorArgb: Long) {
        _editorState.update { it.copy(activeBrushColor = colorArgb) }
    }

    fun setBrushSizeNorm(sizeNorm: Float) {
        _editorState.update { it.copy(activeBrushSizeNorm = sizeNorm.coerceIn(0.005f, 0.06f)) }
    }

    fun setBrushOpacity(opacity: Float) {
        _editorState.update { it.copy(activeBrushOpacity = opacity.coerceIn(0.15f, 1f)) }
    }

    fun addDrawStroke(stroke: DrawStroke) {
        if (stroke.points.size < 2) return
        pushUndoSnapshot()
        _editorState.update { it.copy(strokes = it.strokes + stroke) }
        scheduleAutoSave()
    }

    fun clearAllStrokes() {
        if (_editorState.value.strokes.isEmpty()) return
        pushUndoSnapshot()
        _editorState.update { it.copy(strokes = emptyList()) }
        scheduleAutoSave()
    }

    // --- Text Layers ---
    fun addTextLayer(
        text: String,
        colorArgb: Long,
        backgroundArgb: Long?,
        fontStyle: TextFontStyle
    ) {
        if (text.isBlank()) return
        pushUndoSnapshot()
        val layer = TextLayer(
            text = text.trim(),
            colorArgb = colorArgb,
            backgroundArgb = backgroundArgb,
            fontStyle = fontStyle
        )
        _editorState.update {
            it.copy(
                textLayers = it.textLayers + layer,
                selectedTextId = layer.id
            )
        }
        scheduleAutoSave()
    }

    fun selectTextLayer(id: String?) {
        _editorState.update { it.copy(selectedTextId = id, selectedStickerId = null) }
    }

    fun updateTextLayerTransform(id: String, dxNorm: Float, dyNorm: Float, scaleDelta: Float, rotationDelta: Float) {
        if (pendingUndoSnapshot == null) {
            pendingUndoSnapshot = currentSnapshot()
        }
        _editorState.update { state ->
            val updated = state.textLayers.map { layer ->
                if (layer.id == id) {
                    layer.copy(
                        xNorm = (layer.xNorm + dxNorm).coerceIn(0.05f, 0.95f),
                        yNorm = (layer.yNorm + dyNorm).coerceIn(0.05f, 0.95f),
                        scale = (layer.scale * scaleDelta).coerceIn(0.4f, 3.5f),
                        rotationDeg = layer.rotationDeg + rotationDelta
                    )
                } else layer
            }
            state.copy(textLayers = updated)
        }
    }

    fun updateTextLayerStyle(
        id: String,
        text: String,
        colorArgb: Long,
        backgroundArgb: Long?,
        fontStyle: TextFontStyle
    ) {
        pushUndoSnapshot()
        _editorState.update { state ->
            state.copy(
                textLayers = state.textLayers.map {
                    if (it.id == id) {
                        it.copy(
                            text = text.trim(),
                            colorArgb = colorArgb,
                            backgroundArgb = backgroundArgb,
                            fontStyle = fontStyle
                        )
                    } else it
                }
            )
        }
        scheduleAutoSave()
    }

    fun deleteTextLayer(id: String) {
        pushUndoSnapshot()
        _editorState.update { state ->
            state.copy(
                textLayers = state.textLayers.filterNot { it.id == id },
                selectedTextId = if (state.selectedTextId == id) null else state.selectedTextId
            )
        }
        scheduleAutoSave()
    }

    // --- Sticker Layers ---
    fun addStickerLayer(kind: StickerKind, colorArgb: Long) {
        pushUndoSnapshot()
        val layer = StickerLayer(
            kind = kind,
            colorArgb = colorArgb
        )
        _editorState.update {
            it.copy(
                stickerLayers = it.stickerLayers + layer,
                selectedStickerId = layer.id
            )
        }
        scheduleAutoSave()
    }

    fun selectStickerLayer(id: String?) {
        _editorState.update { it.copy(selectedStickerId = id, selectedTextId = null) }
    }

    fun updateStickerTransform(id: String, dxNorm: Float, dyNorm: Float, scaleDelta: Float, rotationDelta: Float) {
        if (pendingUndoSnapshot == null) {
            pendingUndoSnapshot = currentSnapshot()
        }
        _editorState.update { state ->
            val updated = state.stickerLayers.map { layer ->
                if (layer.id == id) {
                    layer.copy(
                        xNorm = (layer.xNorm + dxNorm).coerceIn(0.05f, 0.95f),
                        yNorm = (layer.yNorm + dyNorm).coerceIn(0.05f, 0.95f),
                        scale = (layer.scale * scaleDelta).coerceIn(0.4f, 3.5f),
                        rotationDeg = layer.rotationDeg + rotationDelta
                    )
                } else layer
            }
            state.copy(stickerLayers = updated)
        }
    }

    fun deleteStickerLayer(id: String) {
        pushUndoSnapshot()
        _editorState.update { state ->
            state.copy(
                stickerLayers = state.stickerLayers.filterNot { it.id == id },
                selectedStickerId = if (state.selectedStickerId == id) null else state.selectedStickerId
            )
        }
        scheduleAutoSave()
    }

    // --- Frames ---
    fun selectFrameStyle(style: FrameStyle) {
        pushUndoSnapshot()
        _editorState.update {
            it.copy(frameConfig = it.frameConfig.copy(style = style))
        }
        scheduleAutoSave()
    }

    fun updateFrameThickness(thicknessNorm: Float) {
        if (pendingUndoSnapshot == null) {
            pendingUndoSnapshot = currentSnapshot()
        }
        _editorState.update {
            it.copy(frameConfig = it.frameConfig.copy(thicknessNorm = thicknessNorm.coerceIn(0.02f, 0.14f)))
        }
    }

    fun updateFrameColor(colorArgb: Long) {
        pushUndoSnapshot()
        _editorState.update {
            it.copy(frameConfig = it.frameConfig.copy(colorArgb = colorArgb))
        }
        scheduleAutoSave()
    }

    // --- Save Current Look as Custom Preset ---
    fun saveCurrentLookAsPreset(name: String) {
        val state = _editorState.value
        val recipe = PresetRecipe(
            id = UUID.randomUUID().toString(),
            name = name.trim().ifBlank { "Mi Preset" },
            subtitle = "Filtro ${PresetsCatalog.findFilter(state.activeFilterId).name} • Personalizado",
            category = FilterCategory.CUSTOM,
            filterId = state.activeFilterId,
            filterIntensity = state.filterIntensity,
            adjustments = state.adjustments,
            isCustom = true
        )
        viewModelScope.launch {
            repository.saveCustomPreset(recipe)
            _editorState.update { it.copy(statusMessage = "Preset '${recipe.name}' guardado") }
        }
    }

    fun deleteCustomPreset(presetId: String) {
        viewModelScope.launch {
            repository.deleteCustomPreset(presetId)
        }
    }

    // --- Presets Lab Screen ---
    fun selectPresetCategory(category: FilterCategory) {
        _presetsState.update { it.copy(selectedCategory = category) }
    }

    fun selectPresetRecipe(recipeId: String) {
        _presetsState.update { it.copy(selectedRecipeId = recipeId) }
    }

    fun setPresetsPreviewFromSample(sample: SamplePhoto) {
        viewModelScope.launch {
            val bmp = withContext(Dispatchers.IO) {
                ImageProcessor.decodeDrawableBitmap(appContext, sample.drawableRes, 900)
            }
            if (bmp != null) {
                _presetsState.update {
                    it.copy(
                        previewBitmap = bmp,
                        previewTitle = sample.title,
                        selectedSampleId = sample.id,
                        previewUri = null
                    )
                }
            }
        }
    }

    fun setPresetsPreviewFromUri(uri: Uri) {
        viewModelScope.launch {
            val bmp = withContext(Dispatchers.IO) {
                ImageProcessor.decodeUriBitmap(appContext, uri, 900)
            }
            if (bmp != null) {
                _presetsState.update {
                    it.copy(
                        previewBitmap = bmp,
                        previewTitle = "Foto propia",
                        selectedSampleId = null,
                        previewUri = uri
                    )
                }
            }
        }
    }

    fun openPresetsPreviewInEditor(recipe: PresetRecipe, onOpened: () -> Unit) {
        val ps = _presetsState.value
        viewModelScope.launch {
            val fullBmp = withContext(Dispatchers.IO) {
                when {
                    ps.previewUri != null -> ImageProcessor.decodeUriBitmap(appContext, ps.previewUri, 1600)
                    ps.selectedSampleId != null -> {
                        val sample = PresetsCatalog.samplePhotos.firstOrNull { it.id == ps.selectedSampleId }
                            ?: PresetsCatalog.samplePhotos.first()
                        ImageProcessor.decodeDrawableBitmap(appContext, sample.drawableRes, 1600)
                    }
                    else -> ps.previewBitmap ?: ImageProcessor.decodeDrawableBitmap(
                        appContext,
                        PresetsCatalog.samplePhotos.first().drawableRes,
                        1600
                    )
                }
            }
            if (fullBmp != null) {
                openOrCreateProjectForBitmap(
                    suggestedName = "${ps.previewTitle} (${recipe.name})",
                    bitmap = fullBmp,
                    initialTab = EditorToolTab.FILTERS,
                    initialRecipe = recipe
                )
                onOpened()
            }
        }
    }

    // --- Project Management Actions (Long Press Menu) ---
    fun renameProject(projectId: String, newName: String) {
        viewModelScope.launch {
            val updated = withContext(Dispatchers.IO) {
                projectsStorage.renameProject(projectId, newName)
            }
            _projects.value = updated
            if (_editorState.value.activeProjectId == projectId) {
                _editorState.update { it.copy(projectTitle = newName.trim()) }
            }
        }
    }

    fun deleteProject(projectId: String) {
        viewModelScope.launch {
            val updated = withContext(Dispatchers.IO) {
                projectsStorage.deleteProject(projectId)
            }
            _projects.value = updated
            if (_editorState.value.activeProjectId == projectId) {
                _editorState.update { it.copy(activeProjectId = null) }
            }
        }
    }

    fun prepareExportForProject(project: BolpikProject, onReady: () -> Unit) {
        viewModelScope.launch {
            _exportState.update {
                it.copy(
                    targetProjectId = project.id,
                    targetProjectName = project.name,
                    isExporting = true,
                    savedItem = null
                )
            }
            val rendered = renderProjectComposite(project, addWatermark = _exportState.value.addWatermark)
            if (rendered != null) {
                val size = withContext(Dispatchers.Default) {
                    ImageProcessor.estimateSizeBytes(rendered, _exportState.value.format, _exportState.value.quality)
                }
                _exportState.update {
                    it.copy(
                        previewBitmap = rendered,
                        estimatedSizeBytes = size,
                        isExporting = false
                    )
                }
                onReady()
            } else {
                _exportState.update { it.copy(isExporting = false) }
            }
        }
    }

    suspend fun createShareUriForProject(project: BolpikProject): Uri? {
        val rendered = renderProjectComposite(project, addWatermark = false) ?: return null
        return withContext(Dispatchers.IO) {
            ImageProcessor.createShareUri(appContext, rendered, ExportFormat.JPEG, 92)
        }
    }

    private suspend fun renderProjectComposite(project: BolpikProject, addWatermark: Boolean): Bitmap? {
        return withContext(Dispatchers.Default) {
            val orig = projectsStorage.loadOriginalBitmap(project) ?: return@withContext null
            val editState = projectsStorage.loadEditState(project.id)
            var working = orig
            editState.appliedCrops.forEach { step ->
                working = ImageProcessor.applyCropAndTransform(working, step.cropRect, step.transform)
            }
            ImageProcessor.renderFinalComposite(
                context = appContext,
                source = working,
                adjustments = editState.adjustments,
                filterId = editState.filterId,
                filterIntensity = editState.filterIntensity,
                strokes = editState.strokes,
                textLayers = editState.textLayers,
                stickerLayers = editState.stickerLayers,
                frameConfig = editState.frameConfig,
                addWatermark = addWatermark
            )
        }
    }

    // --- Export Studio ---
    fun prepareExportPreview() {
        val state = _editorState.value
        val src = state.workingBitmap ?: return
        viewModelScope.launch {
            _exportState.update {
                it.copy(
                    targetProjectId = state.activeProjectId,
                    targetProjectName = state.projectTitle,
                    isExporting = true,
                    savedItem = null
                )
            }
            val rendered = withContext(Dispatchers.Default) {
                ImageProcessor.renderFinalComposite(
                    context = appContext,
                    source = src,
                    adjustments = state.adjustments,
                    filterId = state.activeFilterId,
                    filterIntensity = state.filterIntensity,
                    strokes = state.strokes,
                    textLayers = state.textLayers,
                    stickerLayers = state.stickerLayers,
                    frameConfig = state.frameConfig,
                    addWatermark = _exportState.value.addWatermark
                )
            }
            val size = withContext(Dispatchers.Default) {
                ImageProcessor.estimateSizeBytes(
                    rendered,
                    _exportState.value.format,
                    _exportState.value.quality
                )
            }
            _exportState.update {
                it.copy(
                    previewBitmap = rendered,
                    estimatedSizeBytes = size,
                    isExporting = false
                )
            }
        }
    }

    fun setExportFormat(format: ExportFormat) {
        _exportState.update { it.copy(format = format, savedItem = null) }
        recalculateExportSize()
    }

    fun setExportQuality(quality: Int) {
        _exportState.update { it.copy(quality = quality.coerceIn(50, 100), savedItem = null) }
        recalculateExportSize()
    }

    fun setExportWatermark(enabled: Boolean) {
        _exportState.update { it.copy(addWatermark = enabled, savedItem = null) }
        val targetId = _exportState.value.targetProjectId
        val activeId = _editorState.value.activeProjectId
        if (targetId != null && targetId != activeId) {
            val targetProj = _projects.value.firstOrNull { it.id == targetId }
            if (targetProj != null) {
                prepareExportForProject(targetProj) {}
                return
            }
        }
        prepareExportPreview()
    }

    private fun recalculateExportSize() {
        val bmp = _exportState.value.previewBitmap ?: return
        viewModelScope.launch {
            val size = withContext(Dispatchers.Default) {
                ImageProcessor.estimateSizeBytes(bmp, _exportState.value.format, _exportState.value.quality)
            }
            _exportState.update { it.copy(estimatedSizeBytes = size) }
        }
    }

    fun saveExportToGallery() {
        val exp = _exportState.value
        val bmp = exp.previewBitmap ?: return
        val title = exp.targetProjectName.ifBlank { "Bolpik" }.replace(" ", "_")
        viewModelScope.launch {
            _exportState.update { it.copy(isExporting = true) }
            val saved = withContext(Dispatchers.IO) {
                ImageProcessor.saveBitmapToGallery(
                    context = appContext,
                    bitmap = bmp,
                    format = exp.format,
                    quality = exp.quality,
                    displayName = "Bolpik_$title"
                )
            }
            if (saved != null) {
                repository.recordExport(saved)
                val targetId = exp.targetProjectId
                if (targetId != null) {
                    if (_editorState.value.activeProjectId == targetId && _editorState.value.workingBitmap != null) {
                        performAutoSave(markExported = true)
                    } else {
                        val updated = withContext(Dispatchers.IO + NonCancellable) {
                            projectsStorage.markProjectExported(targetId)
                        }
                        _projects.value = updated
                    }
                }
            }
            _exportState.update { it.copy(isExporting = false, savedItem = saved) }
        }
    }

    suspend fun createShareUriForCurrentExport(): Uri? {
        val exp = _exportState.value
        val bmp = exp.previewBitmap ?: return null
        return withContext(Dispatchers.IO) {
            ImageProcessor.createShareUri(appContext, bmp, exp.format, exp.quality)
        }
    }

    // --- Collage Studio ---
    fun setCollageTemplate(template: CollageTemplate) {
        _collageState.update { it.copy(template = template) }
    }

    fun setCollageSpacing(spacingNorm: Float) {
        _collageState.update { it.copy(spacingNorm = spacingNorm.coerceIn(0f, 0.06f)) }
    }

    fun setCollageCornerRadius(radiusNorm: Float) {
        _collageState.update { it.copy(cornerRadiusNorm = radiusNorm.coerceIn(0f, 0.08f)) }
    }

    fun setCollageBackgroundColor(colorArgb: Long) {
        _collageState.update { it.copy(bgColorArgb = colorArgb) }
    }

    fun setCollageAspectRatio(ratio: Float) {
        _collageState.update { it.copy(aspectRatio = ratio) }
    }

    fun setCollageSlotUri(slotIndex: Int, uri: Uri) {
        viewModelScope.launch {
            val bmp = withContext(Dispatchers.IO) {
                ImageProcessor.decodeUriBitmap(appContext, uri, 900)
            }
            if (bmp != null) {
                _collageState.update { state ->
                    val updated = state.slots.map {
                        if (it.index == slotIndex) it.copy(bitmap = bmp, label = "Foto ${slotIndex + 1}", panX = 0f, panY = 0f)
                        else it
                    }
                    state.copy(slots = updated)
                }
            }
        }
    }

    fun setCollageMultipleUris(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _collageState.update { it.copy(isBusy = true) }
            val decoded = withContext(Dispatchers.IO) {
                uris.take(6).mapNotNull { uri -> ImageProcessor.decodeUriBitmap(appContext, uri, 900) }
            }
            _collageState.update { state ->
                val updated = state.slots.mapIndexed { index, slot ->
                    if (index < decoded.size) {
                        slot.copy(bitmap = decoded[index], label = "Foto ${index + 1}", panX = 0f, panY = 0f)
                    } else slot
                }
                state.copy(slots = updated, isBusy = false)
            }
        }
    }

    fun panCollageSlot(slotIndex: Int, dx: Float, dy: Float) {
        _collageState.update { state ->
            val updated = state.slots.map { slot ->
                if (slot.index == slotIndex) {
                    slot.copy(
                        panX = (slot.panX + dx).coerceIn(-1f, 1f),
                        panY = (slot.panY + dy).coerceIn(-1f, 1f)
                    )
                } else slot
            }
            state.copy(slots = updated)
        }
    }

    fun renderCollageAndOpenInEditor(onReady: () -> Unit) {
        val c = _collageState.value
        viewModelScope.launch {
            _collageState.update { it.copy(isBusy = true) }
            val rendered = withContext(Dispatchers.Default) {
                ImageProcessor.renderCollageBitmap(
                    slots = c.slots,
                    template = c.template,
                    spacingNorm = c.spacingNorm,
                    cornerRadiusNorm = c.cornerRadiusNorm,
                    bgColorArgb = c.bgColorArgb,
                    aspectRatio = c.aspectRatio,
                    targetWidth = 1440
                )
            }
            _collageState.update { it.copy(isBusy = false) }
            openOrCreateProjectForBitmap(
                suggestedName = "Collage ${c.template.label}",
                bitmap = rendered,
                initialTab = EditorToolTab.FILTERS,
                initialRecipe = null
            )
            onReady()
        }
    }

    fun saveCollageToGallery(onSaved: (SavedExportItem?) -> Unit) {
        val c = _collageState.value
        viewModelScope.launch {
            _collageState.update { it.copy(isBusy = true) }
            val rendered = withContext(Dispatchers.Default) {
                ImageProcessor.renderCollageBitmap(
                    slots = c.slots,
                    template = c.template,
                    spacingNorm = c.spacingNorm,
                    cornerRadiusNorm = c.cornerRadiusNorm,
                    bgColorArgb = c.bgColorArgb,
                    aspectRatio = c.aspectRatio,
                    targetWidth = 1600
                )
            }
            val saved = withContext(Dispatchers.IO) {
                ImageProcessor.saveBitmapToGallery(
                    context = appContext,
                    bitmap = rendered,
                    format = ExportFormat.JPEG,
                    quality = 94,
                    displayName = "Bolpik_Collage"
                )
            }
            if (saved != null) {
                repository.recordExport(saved)
            }
            _collageState.update { it.copy(isBusy = false) }
            onSaved(saved)
        }
    }

    fun clearStatusMessage() {
        _editorState.update { it.copy(statusMessage = null) }
    }
}
