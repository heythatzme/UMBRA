package com.example.ui.editor

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GrainPreset
import com.example.model.GrainState
import com.example.model.NoirPresetId
import com.example.ui.components.CurvesEditorView
import com.example.ui.components.EclipseEyeLogo
import com.example.ui.components.NoirSlider
import com.example.ui.components.ZoomableImageView
import com.example.ui.export.ExportDialog
import com.example.ui.theme.NoirAccentMuted
import com.example.ui.theme.NoirAccentSilver
import com.example.ui.theme.NoirAccentWhite
import com.example.ui.theme.NoirBorder
import com.example.ui.theme.NoirBorderLight
import com.example.ui.theme.NoirDark
import com.example.ui.theme.NoirPitchBlack
import com.example.ui.theme.NoirSurface
import com.example.ui.theme.NoirSurfaceElevated
import com.example.ui.theme.NoirSurfaceHighlight
import com.example.ui.theme.NoirTextPrimary
import com.example.ui.theme.NoirTextSecondary
import com.example.ui.theme.NoirTextTertiary

@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    BackHandler {
        onNavigateBack()
    }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = NoirPitchBlack,
        topBar = {
            EditorTopBar(
                canUndo = uiState.canUndo,
                canRedo = uiState.canRedo,
                isSplitActive = uiState.splitPosition != null,
                isHoldActive = uiState.isHoldComparing,
                onBack = onNavigateBack,
                onUndo = viewModel::undo,
                onRedo = viewModel::redo,
                onToggleSplit = viewModel::toggleSplitComparison,
                onHoldCompareStart = { viewModel.setHoldComparing(true) },
                onHoldCompareEnd = { viewModel.setHoldComparing(false) },
                onOpenAi = viewModel::openAiDialog,
                onOpenExport = viewModel::openExportDialog
            )
        },
        bottomBar = {
            EditorBottomSection(
                uiState = uiState,
                onSelectTab = viewModel::selectTab,
                onUpdateEdit = { viewModel.updateEditState(it, pushToHistory = false) },
                onCommitEdit = { viewModel.commitToHistory() },
                onApplyPreset = viewModel::applyPreset,
                onCurveChange = { newCurves, isFinished ->
                    viewModel.updateEditState(uiState.editState.copy(curves = newCurves), pushToHistory = isFinished)
                },
                onResetCurve = {
                    viewModel.updateEditState(uiState.editState.resetCurves(), pushToHistory = true)
                },
                onResetTab = viewModel::resetCurrentTab,
                onResetAll = viewModel::resetAll,
                onSaveProject = { viewModel.saveCurrentProject() }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(NoirPitchBlack)
        ) {
            // Main Zoomable & Pannable Viewport (Preserves exact Aspect Ratio & smooth 60fps Split)
            ZoomableImageView(
                originalBitmap = uiState.originalPreviewBitmap,
                editedBitmap = uiState.renderedPreviewBitmap,
                scale = uiState.scale,
                offset = uiState.offset,
                onTransformChange = viewModel::updateTransform,
                splitPosition = uiState.splitPosition,
                onSplitChange = viewModel::updateSplitPosition,
                isHoldComparing = uiState.isHoldComparing,
                modifier = Modifier.fillMaxSize()
            )

            // Top-left resolution & sensor badge with UMBRA logo
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 12.dp, top = 10.dp)
                    .background(Color(0xB3121212), RoundedCornerShape(2.dp))
                    .border(1.dp, NoirBorder, RoundedCornerShape(2.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    EclipseEyeLogo(size = 14.dp, modifier = Modifier.testTag("editor_badge_logo"))
                    Text(
                        text = "UMBRA • ${uiState.resolutionString()} • ${uiState.megapixelsString()} • FULL RES",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            letterSpacing = 0.5.sp,
                            color = NoirAccentSilver
                        )
                    )
                }
            }

            // Top-right floating controls: 100% Inspection / Fit-to-screen
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 12.dp, top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(Color(0xB3121212), RoundedCornerShape(2.dp))
                        .border(1.dp, NoirBorder, RoundedCornerShape(2.dp))
                        .clickable { viewModel.toggle100PercentZoom() }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .testTag("zoom_100_button")
                ) {
                    Text(
                        text = if (uiState.scale > 1.2f) "FIT" else "100%",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.sp,
                            fontWeight = FontWeight.Bold,
                            color = NoirAccentWhite
                        )
                    )
                }
            }

            // Real-time rendering indicator
            if (uiState.isRendering) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 12.dp, bottom = 12.dp)
                        .background(Color(0xCC161616), RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CircularProgressIndicator(
                            color = NoirAccentWhite,
                            strokeWidth = 1.5.dp,
                            modifier = Modifier.size(10.dp)
                        )
                        Text(
                            text = "RENDERING",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 8.sp,
                                letterSpacing = 1.sp,
                                color = NoirTextSecondary
                            )
                        )
                    }
                }
            }
        }
    }

    // Export Dialog
    if (uiState.isExportDialogVisible) {
        ExportDialog(
            originalWidth = uiState.originalWidth,
            originalHeight = uiState.originalHeight,
            isExporting = uiState.isExporting,
            exportProgress = uiState.exportProgress,
            exportStatusMessage = uiState.exportStatusMessage,
            exportResult = uiState.exportResult,
            onDismiss = viewModel::dismissExportDialog,
            onStartExport = { settings, toGallery ->
                viewModel.startExport(settings, toGallery)
            },
            onShareResult = viewModel::shareExportedResult
        )
    }

    // Gemini Darkroom Intelligence Dialog
    if (uiState.isAiDialogVisible) {
        AiDarkroomDialog(
            isLoading = uiState.isAiAnalyzing,
            analysis = uiState.aiAnalysis,
            onApplyEdits = viewModel::applyAiRecommendation,
            onDismiss = viewModel::dismissAiDialog
        )
    }
}

@Composable
private fun EditorTopBar(
    canUndo: Boolean,
    canRedo: Boolean,
    isSplitActive: Boolean,
    isHoldActive: Boolean,
    onBack: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onToggleSplit: () -> Unit,
    onHoldCompareStart: () -> Unit,
    onHoldCompareEnd: () -> Unit,
    onOpenAi: () -> Unit,
    onOpenExport: () -> Unit
) {
    Surface(
        color = NoirPitchBlack,
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = NoirAccentWhite
                    )
                }

                IconButton(
                    onClick = onUndo,
                    enabled = canUndo,
                    modifier = Modifier.testTag("undo_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "Undo",
                        tint = if (canUndo) NoirAccentWhite else NoirBorderLight
                    )
                }

                IconButton(
                    onClick = onRedo,
                    enabled = canRedo,
                    modifier = Modifier.testTag("redo_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Redo,
                        contentDescription = "Redo",
                        tint = if (canRedo) NoirAccentWhite else NoirBorderLight
                    )
                }
            }

            // Center Actions: Split & Press-to-hold compare
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Split View Toggle
                Box(
                    modifier = Modifier
                        .background(
                            if (isSplitActive) NoirSurfaceHighlight else NoirPitchBlack,
                            RoundedCornerShape(2.dp)
                        )
                        .border(
                            1.dp,
                            if (isSplitActive) NoirAccentWhite else NoirBorder,
                            RoundedCornerShape(2.dp)
                        )
                        .clickable { onToggleSplit() }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .testTag("split_slider_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ViewColumn,
                            contentDescription = "Split Comparison",
                            tint = if (isSplitActive) NoirAccentWhite else NoirTextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "SPLIT",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                letterSpacing = 1.sp,
                                color = if (isSplitActive) NoirAccentWhite else NoirTextSecondary
                            )
                        )
                    }
                }

                // Press & Hold Original Button
                Box(
                    modifier = Modifier
                        .background(
                            if (isHoldActive) NoirAccentWhite else NoirPitchBlack,
                            RoundedCornerShape(2.dp)
                        )
                        .border(1.dp, NoirBorder, RoundedCornerShape(2.dp))
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    onHoldCompareStart()
                                    tryAwaitRelease()
                                    onHoldCompareEnd()
                                }
                            )
                        }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .testTag("hold_compare_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Compare,
                            contentDescription = "Hold Compare",
                            tint = if (isHoldActive) NoirPitchBlack else NoirTextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "HOLD",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                letterSpacing = 1.sp,
                                color = if (isHoldActive) NoirPitchBlack else NoirTextSecondary
                            )
                        )
                    }
                }
            }

            // Right Actions: AI Darkroom & Export
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                IconButton(
                    onClick = onOpenAi,
                    modifier = Modifier.testTag("ai_darkroom_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "AI Darkroom Intelligence",
                        tint = NoirAccentWhite
                    )
                }

                Box(
                    modifier = Modifier
                        .background(NoirAccentWhite, RoundedCornerShape(2.dp))
                        .clickable { onOpenExport() }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                        .testTag("export_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Export",
                            tint = NoirPitchBlack,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "EXPORT",
                            style = MaterialTheme.typography.labelLarge.copy(
                                letterSpacing = 1.5.sp,
                                fontSize = 11.sp,
                                color = NoirPitchBlack,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EditorBottomSection(
    uiState: EditorUiState,
    onSelectTab: (EditorTab) -> Unit,
    onUpdateEdit: (com.example.model.EditState) -> Unit,
    onCommitEdit: () -> Unit,
    onApplyPreset: (NoirPresetId) -> Unit,
    onCurveChange: (com.example.model.CurveState, Boolean) -> Unit,
    onResetCurve: () -> Unit,
    onResetTab: () -> Unit,
    onResetAll: () -> Unit,
    onSaveProject: () -> Unit
) {
    Surface(
        color = NoirSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, NoirBorder),
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Tab Selection Bar (Horizontally scrollable)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NoirDark)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                EditorTab.values().forEach { tab ->
                    val isSelected = uiState.selectedTab == tab
                    Box(
                        modifier = Modifier
                            .background(
                                if (isSelected) NoirSurfaceElevated else Color.Transparent,
                                RoundedCornerShape(2.dp)
                            )
                            .clickable { onSelectTab(tab) }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                            .testTag("tab_${tab.name}")
                    ) {
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.labelLarge.copy(
                                letterSpacing = 1.8.sp,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) NoirAccentWhite else NoirTextSecondary
                            )
                        )
                    }
                }
            }

            HorizontalDivider(color = NoirBorder, thickness = 1.dp)

            // Panel Content Area (Spacious 300dp for Curves, 230dp for sliders)
            val isCurves = uiState.selectedTab == EditorTab.CURVES
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isCurves) 305.dp else 230.dp)
                    .background(NoirSurface)
            ) {
                when (uiState.selectedTab) {
                    EditorTab.ADJUST -> AdjustPanel(
                        editState = uiState.editState,
                        onUpdate = onUpdateEdit,
                        onFinish = onCommitEdit
                    )
                    EditorTab.NOIR -> NoirPresetsPanel(
                        editState = uiState.editState,
                        onSelectPreset = onApplyPreset,
                        onUpdate = onUpdateEdit,
                        onFinish = onCommitEdit
                    )
                    EditorTab.BW_MIX -> BwMixPanel(
                        editState = uiState.editState,
                        onUpdate = onUpdateEdit,
                        onFinish = onCommitEdit
                    )
                    EditorTab.CURVES -> CurvesEditorView(
                        curveState = uiState.editState.curves,
                        onCurveChange = onCurveChange,
                        onResetCurve = onResetCurve,
                        onDone = { onSelectTab(EditorTab.ADJUST) }
                    )
                    EditorTab.GRAIN -> GrainPanel(
                        editState = uiState.editState,
                        onUpdate = onUpdateEdit,
                        onFinish = onCommitEdit
                    )
                    EditorTab.DETAIL -> DetailPanel(
                        editState = uiState.editState,
                        onUpdate = onUpdateEdit,
                        onFinish = onCommitEdit
                    )
                    EditorTab.VIGNETTE -> VignettePanel(
                        editState = uiState.editState,
                        onUpdate = onUpdateEdit,
                        onFinish = onCommitEdit
                    )
                }
            }

            HorizontalDivider(color = NoirBorder, thickness = 1.dp)

            // Bottom Action Bar: Reset Tab | Reset All | Save Project
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NoirDark)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "RESET TAB",
                        modifier = Modifier
                            .clickable { onResetTab() }
                            .padding(vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.2.sp,
                            fontSize = 10.sp,
                            color = NoirTextSecondary
                        )
                    )

                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelSmall.copy(color = NoirBorderLight)
                    )

                    Text(
                        text = "RESET ALL",
                        modifier = Modifier
                            .clickable { onResetAll() }
                            .padding(vertical = 4.dp)
                            .testTag("reset_all_button"),
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.2.sp,
                            fontSize = 10.sp,
                            color = NoirTextTertiary
                        )
                    )
                }

                Row(
                    modifier = Modifier
                        .clickable { onSaveProject() }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = "Save Project",
                        tint = NoirAccentSilver,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "SAVE PROJECT",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.2.sp,
                            fontSize = 10.sp,
                            color = NoirAccentSilver
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun AdjustPanel(
    editState: com.example.model.EditState,
    onUpdate: (com.example.model.EditState) -> Unit,
    onFinish: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 4.dp)
    ) {
        NoirSlider(
            label = "Exposure",
            value = editState.exposure,
            onValueChange = { onUpdate(editState.copy(exposure = it)) },
            onValueChangeFinished = onFinish,
            valueRange = -2.0f..2.0f,
            defaultValue = 0f,
            displayFormatter = { String.format(java.util.Locale.US, "%.2f EV", it) }
        )
        NoirSlider(
            label = "Brightness",
            value = editState.brightness.toFloat(),
            onValueChange = { onUpdate(editState.copy(brightness = it.toInt())) },
            onValueChangeFinished = onFinish,
            valueRange = -100f..100f,
            defaultValue = 0f
        )
        NoirSlider(
            label = "Contrast",
            value = editState.contrast.toFloat(),
            onValueChange = { onUpdate(editState.copy(contrast = it.toInt())) },
            onValueChangeFinished = onFinish,
            valueRange = -100f..100f,
            defaultValue = 0f
        )
        NoirSlider(
            label = "Highlights",
            value = editState.highlights.toFloat(),
            onValueChange = { onUpdate(editState.copy(highlights = it.toInt())) },
            onValueChangeFinished = onFinish,
            valueRange = -100f..100f,
            defaultValue = 0f
        )
        NoirSlider(
            label = "Shadows",
            value = editState.shadows.toFloat(),
            onValueChange = { onUpdate(editState.copy(shadows = it.toInt())) },
            onValueChangeFinished = onFinish,
            valueRange = -100f..100f,
            defaultValue = 0f
        )
        NoirSlider(
            label = "Whites",
            value = editState.whites.toFloat(),
            onValueChange = { onUpdate(editState.copy(whites = it.toInt())) },
            onValueChangeFinished = onFinish,
            valueRange = -100f..100f,
            defaultValue = 0f
        )
        NoirSlider(
            label = "Blacks",
            value = editState.blacks.toFloat(),
            onValueChange = { onUpdate(editState.copy(blacks = it.toInt())) },
            onValueChangeFinished = onFinish,
            valueRange = -100f..100f,
            defaultValue = 0f
        )
        NoirSlider(
            label = "Clarity",
            value = editState.clarity.toFloat(),
            onValueChange = { onUpdate(editState.copy(clarity = it.toInt())) },
            onValueChangeFinished = onFinish,
            valueRange = -100f..100f,
            defaultValue = 0f
        )
        NoirSlider(
            label = "Texture",
            value = editState.texture.toFloat(),
            onValueChange = { onUpdate(editState.copy(texture = it.toInt())) },
            onValueChangeFinished = onFinish,
            valueRange = -100f..100f,
            defaultValue = 0f
        )
    }
}

@Composable
private fun NoirPresetsPanel(
    editState: com.example.model.EditState,
    onSelectPreset: (NoirPresetId) -> Unit,
    onUpdate: (com.example.model.EditState) -> Unit,
    onFinish: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp)
    ) {
        // Horizontal preset list
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(NoirPresetId.values()) { preset ->
                val isSelected = editState.presetId == preset
                Box(
                    modifier = Modifier
                        .width(118.dp)
                        .background(
                            if (isSelected) NoirSurfaceHighlight else NoirPitchBlack,
                            RoundedCornerShape(2.dp)
                        )
                        .border(
                            1.dp,
                            if (isSelected) NoirAccentWhite else NoirBorder,
                            RoundedCornerShape(2.dp)
                        )
                        .clickable { onSelectPreset(preset) }
                        .padding(10.dp)
                        .testTag("preset_${preset.name}")
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = preset.tag,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 8.sp,
                                    letterSpacing = 1.sp,
                                    color = if (isSelected) NoirAccentWhite else NoirTextTertiary
                                )
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = NoirAccentWhite,
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = preset.displayName,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontSize = 11.sp,
                                letterSpacing = 1.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) NoirAccentWhite else NoirTextPrimary
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = preset.description,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 9.sp,
                                lineHeight = 11.sp,
                                color = NoirTextSecondary
                            ),
                            maxLines = 2
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Customizable Noir Intensity & Fade Sliders
        NoirSlider(
            label = "Noir Intensity",
            value = editState.noirIntensity.toFloat(),
            onValueChange = { onUpdate(editState.copy(noirIntensity = it.toInt())) },
            onValueChangeFinished = onFinish,
            valueRange = 0f..100f,
            defaultValue = 100f
        )

        NoirSlider(
            label = "Matte Fade",
            value = editState.fade.toFloat(),
            onValueChange = { onUpdate(editState.copy(fade = it.toInt())) },
            onValueChangeFinished = onFinish,
            valueRange = 0f..100f,
            defaultValue = 0f
        )
    }
}

@Composable
private fun BwMixPanel(
    editState: com.example.model.EditState,
    onUpdate: (com.example.model.EditState) -> Unit,
    onFinish: () -> Unit
) {
    val mix = editState.bwMix
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = "COLOR CHANNEL LUMINANCE CONTRIBUTIONS",
            style = MaterialTheme.typography.labelSmall.copy(
                letterSpacing = 1.5.sp,
                fontSize = 10.sp,
                color = NoirTextTertiary
            ),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        NoirSlider(
            label = "Reds",
            value = mix.reds.toFloat(),
            onValueChange = { onUpdate(editState.copy(bwMix = mix.copy(reds = it.toInt()))) },
            onValueChangeFinished = onFinish,
            valueRange = -100f..100f
        )
        NoirSlider(
            label = "Oranges",
            value = mix.oranges.toFloat(),
            onValueChange = { onUpdate(editState.copy(bwMix = mix.copy(oranges = it.toInt()))) },
            onValueChangeFinished = onFinish,
            valueRange = -100f..100f
        )
        NoirSlider(
            label = "Yellows",
            value = mix.yellows.toFloat(),
            onValueChange = { onUpdate(editState.copy(bwMix = mix.copy(yellows = it.toInt()))) },
            onValueChangeFinished = onFinish,
            valueRange = -100f..100f
        )
        NoirSlider(
            label = "Greens",
            value = mix.greens.toFloat(),
            onValueChange = { onUpdate(editState.copy(bwMix = mix.copy(greens = it.toInt()))) },
            onValueChangeFinished = onFinish,
            valueRange = -100f..100f
        )
        NoirSlider(
            label = "Cyans",
            value = mix.cyans.toFloat(),
            onValueChange = { onUpdate(editState.copy(bwMix = mix.copy(cyans = it.toInt()))) },
            onValueChangeFinished = onFinish,
            valueRange = -100f..100f
        )
        NoirSlider(
            label = "Blues",
            value = mix.blues.toFloat(),
            onValueChange = { onUpdate(editState.copy(bwMix = mix.copy(blues = it.toInt()))) },
            onValueChangeFinished = onFinish,
            valueRange = -100f..100f
        )
        NoirSlider(
            label = "Magentas",
            value = mix.magentas.toFloat(),
            onValueChange = { onUpdate(editState.copy(bwMix = mix.copy(magentas = it.toInt()))) },
            onValueChangeFinished = onFinish,
            valueRange = -100f..100f
        )
    }
}

@Composable
private fun GrainPanel(
    editState: com.example.model.EditState,
    onUpdate: (com.example.model.EditState) -> Unit,
    onFinish: () -> Unit
) {
    val grain = editState.grain
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 6.dp)
    ) {
        // Grain preset chips
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(GrainPreset.values()) { preset ->
                val isSelected = grain.preset == preset
                Box(
                    modifier = Modifier
                        .background(
                            if (isSelected) NoirAccentWhite else NoirPitchBlack,
                            RoundedCornerShape(2.dp)
                        )
                        .border(
                            1.dp,
                            if (isSelected) NoirAccentWhite else NoirBorder,
                            RoundedCornerShape(2.dp)
                        )
                        .clickable {
                            onUpdate(editState.copy(grain = GrainState.fromPreset(preset)))
                            onFinish()
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = preset.label.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.2.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) NoirPitchBlack else NoirTextPrimary
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        NoirSlider(
            label = "Grain Amount",
            value = grain.amount.toFloat(),
            onValueChange = { onUpdate(editState.copy(grain = grain.copy(amount = it.toInt(), preset = GrainPreset.NONE))) },
            onValueChangeFinished = onFinish,
            valueRange = 0f..100f,
            defaultValue = 0f
        )
        NoirSlider(
            label = "Grain Size",
            value = grain.size.toFloat(),
            onValueChange = { onUpdate(editState.copy(grain = grain.copy(size = it.toInt()))) },
            onValueChangeFinished = onFinish,
            valueRange = 0f..100f,
            defaultValue = 30f
        )
        NoirSlider(
            label = "Roughness",
            value = grain.roughness.toFloat(),
            onValueChange = { onUpdate(editState.copy(grain = grain.copy(roughness = it.toInt()))) },
            onValueChangeFinished = onFinish,
            valueRange = 0f..100f,
            defaultValue = 50f
        )
    }
}

@Composable
private fun DetailPanel(
    editState: com.example.model.EditState,
    onUpdate: (com.example.model.EditState) -> Unit,
    onFinish: () -> Unit
) {
    val detail = editState.detail
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 4.dp)
    ) {
        NoirSlider(
            label = "Sharpening",
            value = editState.sharpness.toFloat(),
            onValueChange = { onUpdate(editState.copy(sharpness = it.toInt())) },
            onValueChangeFinished = onFinish,
            valueRange = 0f..100f,
            defaultValue = 0f
        )
        NoirSlider(
            label = "Radius",
            value = detail.radius.toFloat(),
            onValueChange = { onUpdate(editState.copy(detail = detail.copy(radius = it.toInt()))) },
            onValueChangeFinished = onFinish,
            valueRange = 0f..100f,
            defaultValue = 20f
        )
        NoirSlider(
            label = "Micro Detail",
            value = detail.detail.toFloat(),
            onValueChange = { onUpdate(editState.copy(detail = detail.copy(detail = it.toInt()))) },
            onValueChangeFinished = onFinish,
            valueRange = 0f..100f,
            defaultValue = 25f
        )
        NoirSlider(
            label = "Noise Reduction",
            value = detail.noiseReduction.toFloat(),
            onValueChange = { onUpdate(editState.copy(detail = detail.copy(noiseReduction = it.toInt()))) },
            onValueChangeFinished = onFinish,
            valueRange = 0f..100f,
            defaultValue = 0f
        )
    }
}

@Composable
private fun VignettePanel(
    editState: com.example.model.EditState,
    onUpdate: (com.example.model.EditState) -> Unit,
    onFinish: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 4.dp)
    ) {
        NoirSlider(
            label = "Vignette Amount",
            value = editState.vignetteAmount.toFloat(),
            onValueChange = { onUpdate(editState.copy(vignetteAmount = it.toInt())) },
            onValueChangeFinished = onFinish,
            valueRange = 0f..100f,
            defaultValue = 0f
        )
        NoirSlider(
            label = "Midpoint",
            value = editState.vignetteMidpoint.toFloat(),
            onValueChange = { onUpdate(editState.copy(vignetteMidpoint = it.toInt())) },
            onValueChangeFinished = onFinish,
            valueRange = 0f..100f,
            defaultValue = 50f
        )
        NoirSlider(
            label = "Feather",
            value = editState.vignetteFeather.toFloat(),
            onValueChange = { onUpdate(editState.copy(vignetteFeather = it.toInt())) },
            onValueChangeFinished = onFinish,
            valueRange = 0f..100f,
            defaultValue = 60f
        )
        NoirSlider(
            label = "Roundness",
            value = editState.vignetteRoundness.toFloat(),
            onValueChange = { onUpdate(editState.copy(vignetteRoundness = it.toInt())) },
            onValueChangeFinished = onFinish,
            valueRange = 0f..100f,
            defaultValue = 50f
        )
    }
}
