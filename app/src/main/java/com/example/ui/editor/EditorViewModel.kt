package com.example.ui.editor

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.GeminiDarkroomAssistant
import com.example.data.ProjectRepository
import com.example.model.EditState
import com.example.model.ExportSettings
import com.example.model.NoirPresetId
import com.example.processing.ExportProcessor
import com.example.processing.NoirProcessor
import com.example.utils.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class EditorTab(val label: String) {
    ADJUST("ADJUST"),
    NOIR("NOIR"),
    BW_MIX("B&W MIX"),
    CURVES("CURVES"),
    GRAIN("GRAIN"),
    DETAIL("DETAIL"),
    VIGNETTE("VIGNETTE")
}

data class EditorUiState(
    val imageUri: Uri? = null,
    val originalWidth: Int = 0,
    val originalHeight: Int = 0,
    val originalPreviewBitmap: Bitmap? = null,
    val renderedPreviewBitmap: Bitmap? = null,
    val isRendering: Boolean = false,

    val editState: EditState = EditState.DEFAULT,
    val selectedTab: EditorTab = EditorTab.ADJUST,

    // Undo / Redo
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,

    // Before / After
    val isHoldComparing: Boolean = false,
    val splitPosition: Float? = null, // null = regular view, 0.05..0.95 = split

    // Zoom & Pan
    val scale: Float = 1.0f,
    val offset: Offset = Offset.Zero,

    // Dialogs & Progress
    val isExportDialogVisible: Boolean = false,
    val isExporting: Boolean = false,
    val exportProgress: Float = 0f,
    val exportStatusMessage: String = "",
    val exportResult: ExportProcessor.ExportResult? = null,

    val isAiDialogVisible: Boolean = false,
    val isAiAnalyzing: Boolean = false,
    val aiAnalysis: GeminiDarkroomAssistant.DarkroomAnalysis? = null,

    val snackbarMessage: String? = null
) {
    fun resolutionString(): String = "$originalWidth × $originalHeight"
    fun megapixelsString(): String {
        val mp = (originalWidth.toLong() * originalHeight.toLong()) / 1_000_000.0
        return String.format(java.util.Locale.US, "%.1f MP", mp)
    }
}

class EditorViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ProjectRepository(application)

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    // Undo / Redo Stack of parameter states
    private val history = mutableListOf<EditState>(EditState.DEFAULT)
    private var historyIndex = 0

    private var sampleFallbackBitmap: Bitmap? = null
    private var renderJob: Job? = null

    fun initializeWithUri(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isRendering = true) }
            val context = getApplication<Application>()
            val dims = ImageUtils.getImageDimensions(context, uri)
            val preview = ImageUtils.decodePreviewBitmap(context, uri)

            history.clear()
            history.add(EditState.DEFAULT)
            historyIndex = 0

            _uiState.update {
                it.copy(
                    imageUri = uri,
                    originalWidth = dims.width,
                    originalHeight = dims.height,
                    originalPreviewBitmap = preview,
                    renderedPreviewBitmap = preview,
                    editState = EditState.DEFAULT,
                    canUndo = false,
                    canRedo = false,
                    isRendering = false
                )
            }
            triggerRender(EditState.DEFAULT, preview)
        }
    }

    fun initializeWithSample() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRendering = true) }
            val sample = ImageUtils.createSamplePhoto(2400, 3200)
            sampleFallbackBitmap = sample

            history.clear()
            history.add(EditState.DEFAULT)
            historyIndex = 0

            _uiState.update {
                it.copy(
                    imageUri = null,
                    originalWidth = sample.width,
                    originalHeight = sample.height,
                    originalPreviewBitmap = sample,
                    renderedPreviewBitmap = sample,
                    editState = EditState.DEFAULT,
                    canUndo = false,
                    canRedo = false,
                    isRendering = false
                )
            }
            triggerRender(EditState.DEFAULT, sample)
        }
    }

    fun initializeFromSavedProject(
        imageUriStr: String,
        width: Int,
        height: Int,
        savedEditState: EditState
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isRendering = true) }
            val context = getApplication<Application>()
            val uri = if (imageUriStr.startsWith("content://") || imageUriStr.startsWith("file://")) {
                Uri.parse(imageUriStr)
            } else null

            val preview = if (uri != null) {
                ImageUtils.decodePreviewBitmap(context, uri)
            } else {
                val sample = sampleFallbackBitmap ?: ImageUtils.createSamplePhoto(width, height)
                sampleFallbackBitmap = sample
                sample
            }

            history.clear()
            history.add(savedEditState)
            historyIndex = 0

            _uiState.update {
                it.copy(
                    imageUri = uri,
                    originalWidth = width,
                    originalHeight = height,
                    originalPreviewBitmap = preview,
                    renderedPreviewBitmap = preview,
                    editState = savedEditState,
                    canUndo = false,
                    canRedo = false,
                    isRendering = false
                )
            }
            triggerRender(savedEditState, preview)
        }
    }

    fun selectTab(tab: EditorTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun commitToHistory() {
        val currentState = _uiState.value.editState
        if (history.isNotEmpty() && history[historyIndex] == currentState) return
        while (history.size > historyIndex + 1) {
            history.removeAt(history.size - 1)
        }
        history.add(currentState)
        historyIndex = history.size - 1
        _uiState.update {
            it.copy(
                canUndo = historyIndex > 0,
                canRedo = false
            )
        }
    }

    fun updateEditState(newState: EditState, pushToHistory: Boolean = false) {
        _uiState.update { it.copy(editState = newState) }

        if (pushToHistory) {
            commitToHistory()
        }

        triggerRender(newState, _uiState.value.originalPreviewBitmap)
    }

    fun applyPreset(presetId: NoirPresetId) {
        val newState = EditState.fromPreset(presetId)
        updateEditState(newState, pushToHistory = true)
    }

    private fun triggerRender(state: EditState, source: Bitmap?) {
        if (source == null) return
        renderJob?.cancel()
        renderJob = viewModelScope.launch(Dispatchers.Default) {
            delay(12) // Responsive throttle to cancel obsolete frames during rapid scrubbing
            _uiState.update { it.copy(isRendering = true) }
            val rendered = NoirProcessor.render(source, state)
            _uiState.update {
                it.copy(
                    renderedPreviewBitmap = rendered,
                    isRendering = false
                )
            }
        }
    }

    fun undo() {
        if (historyIndex > 0) {
            historyIndex--
            val previous = history[historyIndex]
            _uiState.update {
                it.copy(
                    editState = previous,
                    canUndo = historyIndex > 0,
                    canRedo = historyIndex < history.size - 1
                )
            }
            triggerRender(previous, _uiState.value.originalPreviewBitmap)
        }
    }

    fun redo() {
        if (historyIndex < history.size - 1) {
            historyIndex++
            val next = history[historyIndex]
            _uiState.update {
                it.copy(
                    editState = next,
                    canUndo = true,
                    canRedo = historyIndex < history.size - 1
                )
            }
            triggerRender(next, _uiState.value.originalPreviewBitmap)
        }
    }

    fun resetAll() {
        updateEditState(EditState.DEFAULT, pushToHistory = true)
        _uiState.update { it.copy(snackbarMessage = "All adjustments reset") }
    }

    fun resetCurrentTab() {
        val current = _uiState.value.editState
        val updated = when (_uiState.value.selectedTab) {
            EditorTab.ADJUST -> current.resetAdjustments()
            EditorTab.NOIR -> current.resetNoir()
            EditorTab.BW_MIX -> current.resetBwMix()
            EditorTab.CURVES -> current.resetCurves()
            EditorTab.GRAIN -> current.resetGrain()
            EditorTab.DETAIL -> current.resetDetail()
            EditorTab.VIGNETTE -> current.resetVignette()
        }
        updateEditState(updated, pushToHistory = true)
    }

    fun setHoldComparing(holding: Boolean) {
        _uiState.update { it.copy(isHoldComparing = holding) }
    }

    fun toggleSplitComparison() {
        _uiState.update {
            it.copy(
                splitPosition = if (it.splitPosition == null) 0.5f else null
            )
        }
    }

    fun updateSplitPosition(pos: Float) {
        _uiState.update { it.copy(splitPosition = pos) }
    }

    fun updateTransform(scale: Float, offset: Offset) {
        _uiState.update { it.copy(scale = scale, offset = offset) }
    }

    fun resetZoom() {
        _uiState.update { it.copy(scale = 1.0f, offset = Offset.Zero) }
    }

    fun toggle100PercentZoom() {
        _uiState.update {
            if (it.scale > 1.2f) {
                it.copy(scale = 1.0f, offset = Offset.Zero)
            } else {
                it.copy(scale = 2.5f, offset = Offset.Zero)
            }
        }
    }

    // Export Controls
    fun openExportDialog() {
        _uiState.update {
            it.copy(
                isExportDialogVisible = true,
                exportResult = null,
                exportProgress = 0f,
                exportStatusMessage = ""
            )
        }
    }

    fun dismissExportDialog() {
        _uiState.update { it.copy(isExportDialogVisible = false) }
    }

    fun startExport(settings: ExportSettings, saveToGallery: Boolean) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isExporting = true,
                    exportProgress = 0.05f,
                    exportStatusMessage = "Preparing full resolution export...",
                    exportResult = null
                )
            }

            val context = getApplication<Application>()
            val result = ExportProcessor.exportImage(
                context = context,
                sourceUri = _uiState.value.imageUri,
                sampleBitmapFallback = sampleFallbackBitmap,
                editState = _uiState.value.editState,
                settings = settings,
                saveToGallery = saveToGallery
            ) { progress, message ->
                _uiState.update {
                    it.copy(
                        exportProgress = progress,
                        exportStatusMessage = message
                    )
                }
            }

            _uiState.update {
                it.copy(
                    isExporting = false,
                    exportResult = result
                )
            }
        }
    }

    fun shareExportedResult() {
        val res = _uiState.value.exportResult ?: return
        val uri = res.outputUri ?: return
        val context = getApplication<Application>()
        ExportProcessor.shareExportedImage(context, uri, res.format.mimeType)
    }

    // AI Darkroom Intelligence
    fun openAiDialog() {
        _uiState.update {
            it.copy(
                isAiDialogVisible = true,
                isAiAnalyzing = true,
                aiAnalysis = null
            )
        }
        viewModelScope.launch {
            val preview = _uiState.value.renderedPreviewBitmap
                ?: _uiState.value.originalPreviewBitmap
            if (preview != null) {
                val analysis = GeminiDarkroomAssistant.analyzePhoto(
                    preview,
                    _uiState.value.editState
                )
                _uiState.update {
                    it.copy(
                        isAiAnalyzing = false,
                        aiAnalysis = analysis
                    )
                }
            } else {
                _uiState.update { it.copy(isAiAnalyzing = false) }
            }
        }
    }

    fun dismissAiDialog() {
        _uiState.update { it.copy(isAiDialogVisible = false) }
    }

    fun applyAiRecommendation(recommendedState: EditState) {
        updateEditState(recommendedState, pushToHistory = true)
        _uiState.update {
            it.copy(
                isAiDialogVisible = false,
                snackbarMessage = "Applied ${recommendedState.presetId.displayName} look"
            )
        }
    }

    fun saveCurrentProject(projectName: String = "") {
        viewModelScope.launch {
            val uriStr = _uiState.value.imageUri?.toString() ?: "sample_arch"
            repository.saveProject(
                imageUri = uriStr,
                name = projectName,
                width = _uiState.value.originalWidth,
                height = _uiState.value.originalHeight,
                editState = _uiState.value.editState
            )
            _uiState.update { it.copy(snackbarMessage = "Project saved to Darkroom Archive") }
        }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }
}
