package com.example.ui.export

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.ExportFormat
import com.example.model.ExportSettings
import com.example.processing.ExportProcessor
import com.example.ui.components.NoirSlider
import com.example.ui.theme.NoirAccentWhite
import com.example.ui.theme.NoirBorder
import com.example.ui.theme.NoirBorderLight
import com.example.ui.theme.NoirDark
import com.example.ui.theme.NoirPitchBlack
import com.example.ui.theme.NoirSurface
import com.example.ui.theme.NoirSurfaceElevated
import com.example.ui.theme.NoirTextPrimary
import com.example.ui.theme.NoirTextSecondary
import com.example.ui.theme.NoirTextTertiary

@Composable
fun ExportDialog(
    originalWidth: Int,
    originalHeight: Int,
    isExporting: Boolean,
    exportProgress: Float,
    exportStatusMessage: String,
    exportResult: ExportProcessor.ExportResult?,
    onDismiss: () -> Unit,
    onStartExport: (ExportSettings, Boolean) -> Unit, // Boolean: true = Gallery, false = Share
    onShareResult: () -> Unit
) {
    var selectedFormat by remember { mutableStateOf(ExportFormat.PNG) }
    var jpegQuality by remember { mutableIntStateOf(100) }

    val megapixels = (originalWidth.toLong() * originalHeight.toLong()) / 1_000_000.0
    val formattedMegapixels = String.format(java.util.Locale.US, "%.1f MP", megapixels)

    val currentSettings = remember(selectedFormat, jpegQuality, originalWidth, originalHeight) {
        ExportSettings(
            format = selectedFormat,
            quality = jpegQuality,
            targetWidth = originalWidth,
            targetHeight = originalHeight
        )
    }

    Dialog(onDismissRequest = {
        if (!isExporting) onDismiss()
    }) {
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = NoirSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, NoirBorderLight),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("export_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "EXPORT PHOTOGRAPH",
                            style = MaterialTheme.typography.labelLarge.copy(
                                letterSpacing = 2.5.sp,
                                color = NoirTextPrimary
                            )
                        )
                        Text(
                            text = "Full Resolution Pipeline",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = NoirTextTertiary,
                                fontSize = 11.sp
                            )
                        )
                    }

                    Box(
                        modifier = Modifier
                            .background(NoirSurfaceElevated, RoundedCornerShape(2.dp))
                            .border(1.dp, NoirBorder, RoundedCornerShape(2.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = selectedFormat.badge,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = NoirAccentWhite,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Resolution & Metadata Panel
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NoirPitchBlack, RoundedCornerShape(2.dp))
                        .border(1.dp, NoirBorder, RoundedCornerShape(2.dp))
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "ORIGINAL RESOLUTION",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    letterSpacing = 1.2.sp,
                                    color = NoirTextSecondary
                                )
                            )
                            Text(
                                text = "$originalWidth × $originalHeight",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    color = NoirAccentWhite,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "SENSOR DETAIL",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    letterSpacing = 1.2.sp,
                                    color = NoirTextSecondary
                                )
                            )
                            Text(
                                text = formattedMegapixels,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    color = NoirTextPrimary
                                )
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "PROCESSING",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    letterSpacing = 1.2.sp,
                                    color = NoirTextSecondary
                                )
                            )
                            Text(
                                text = "Full Resolution (100% Pixels)",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = NoirAccentWhite
                                )
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "ESTIMATED SIZE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    letterSpacing = 1.2.sp,
                                    color = NoirTextSecondary
                                )
                            )
                            Text(
                                text = currentSettings.estimatedSizeFormatted(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    color = NoirTextTertiary
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Format Selector (PNG, JPEG, WEBP)
                Text(
                    text = "OUTPUT FORMAT",
                    style = MaterialTheme.typography.labelLarge.copy(
                        letterSpacing = 1.5.sp,
                        fontSize = 11.sp,
                        color = NoirTextSecondary
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExportFormat.values().forEach { fmt ->
                        val isSelected = selectedFormat == fmt
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    if (isSelected) NoirAccentWhite else NoirPitchBlack,
                                    RoundedCornerShape(2.dp)
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) NoirAccentWhite else NoirBorder,
                                    RoundedCornerShape(2.dp)
                                )
                                .clickable(enabled = !isExporting) {
                                    selectedFormat = fmt
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = fmt.label,
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        color = if (isSelected) NoirPitchBlack else NoirTextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                )
                                Text(
                                    text = if (fmt == ExportFormat.PNG) "Lossless" else if (fmt == ExportFormat.JPEG) "Max Quality" else "Compact",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (isSelected) Color(0x99000000) else NoirTextTertiary,
                                        fontSize = 9.sp
                                    )
                                )
                            }
                        }
                    }
                }

                // JPEG Quality Slider (if JPEG selected)
                if (selectedFormat == ExportFormat.JPEG) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "JPEG COMPRESSION QUALITY",
                        style = MaterialTheme.typography.labelLarge.copy(
                            letterSpacing = 1.5.sp,
                            fontSize = 11.sp,
                            color = NoirTextSecondary
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(90, 95, 98, 100).forEach { q ->
                            val isQSelected = jpegQuality == q
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (isQSelected) NoirSurfaceElevated else NoirPitchBlack,
                                        RoundedCornerShape(2.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (isQSelected) NoirAccentWhite else NoirBorder,
                                        RoundedCornerShape(2.dp)
                                    )
                                    .clickable(enabled = !isExporting) { jpegQuality = q }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$q%",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        color = if (isQSelected) NoirAccentWhite else NoirTextSecondary,
                                        fontWeight = if (isQSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Progress state or completion message
                if (isExporting) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        LinearProgressIndicator(
                            progress = { exportProgress },
                            color = NoirAccentWhite,
                            trackColor = NoirBorder,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = exportStatusMessage,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = NoirTextSecondary,
                                fontSize = 12.sp
                            )
                        )
                    }
                } else if (exportResult != null && exportResult.success) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF141F14), RoundedCornerShape(2.dp))
                            .border(1.dp, Color(0xFF2E4D2E), RoundedCornerShape(2.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                text = "EXPORTED WITHOUT RESIZING",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    color = Color(0xFF8CE08C),
                                    letterSpacing = 1.5.sp,
                                    fontSize = 11.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${exportResult.exportedWidth} × ${exportResult.exportedHeight} (${exportResult.formattedMegapixels()}) • ${exportResult.formattedFileSize()} • ${if (exportResult.format == ExportFormat.PNG) "LOSSLESS" else "MAXIMUM QUALITY"}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = NoirTextPrimary,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }
                } else if (exportResult != null && !exportResult.success) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF2B1414), RoundedCornerShape(2.dp))
                            .border(1.dp, Color(0xFF5A2020), RoundedCornerShape(2.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = exportResult.errorMessage ?: "Export failed",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFFFF9999)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Actions: Save to Gallery or Share
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (exportResult != null && exportResult.success) {
                        Button(
                            onClick = onShareResult,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NoirAccentWhite,
                                contentColor = NoirPitchBlack
                            ),
                            shape = RoundedCornerShape(2.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("share_result_button")
                        ) {
                            Text(
                                text = "SHARE",
                                style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 1.5.sp)
                            )
                        }

                        OutlinedButton(
                            onClick = onDismiss,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NoirTextPrimary),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NoirBorder),
                            shape = RoundedCornerShape(2.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) {
                            Text(
                                text = "DONE",
                                style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 1.5.sp)
                            )
                        }
                    } else {
                        OutlinedButton(
                            onClick = { onStartExport(currentSettings, false) },
                            enabled = !isExporting,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NoirTextPrimary),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NoirBorder),
                            shape = RoundedCornerShape(2.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("export_share_button")
                        ) {
                            Text(
                                text = "SHARE",
                                style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 1.5.sp)
                            )
                        }

                        Button(
                            onClick = { onStartExport(currentSettings, true) },
                            enabled = !isExporting,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NoirAccentWhite,
                                contentColor = NoirPitchBlack
                            ),
                            shape = RoundedCornerShape(2.dp),
                            modifier = Modifier
                                .weight(1.3f)
                                .height(46.dp)
                                .testTag("export_save_gallery_button")
                        ) {
                            Text(
                                text = "SAVE TO GALLERY",
                                style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 1.5.sp)
                            )
                        }
                    }
                }
            }
        }
    }
}
