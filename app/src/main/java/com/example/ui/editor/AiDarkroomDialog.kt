package com.example.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ai.GeminiDarkroomAssistant
import com.example.model.EditState
import com.example.ui.theme.NoirAccentWhite
import com.example.ui.theme.NoirBorder
import com.example.ui.theme.NoirBorderLight
import com.example.ui.theme.NoirPitchBlack
import com.example.ui.theme.NoirSurface
import com.example.ui.theme.NoirSurfaceElevated
import com.example.ui.theme.NoirTextPrimary
import com.example.ui.theme.NoirTextSecondary
import com.example.ui.theme.NoirTextTertiary

@Composable
fun AiDarkroomDialog(
    isLoading: Boolean,
    analysis: GeminiDarkroomAssistant.DarkroomAnalysis?,
    onApplyEdits: (EditState) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = NoirSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, NoirBorderLight),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("ai_darkroom_dialog")
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
                            text = "DARKROOM INTELLIGENCE",
                            style = MaterialTheme.typography.labelLarge.copy(
                                letterSpacing = 2.sp,
                                color = NoirTextPrimary
                            )
                        )
                        Text(
                            text = "Tonal Critique & Mood Recommendation",
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
                            text = "GEMINI 2.5",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = NoirAccentWhite,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (isLoading) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = NoirAccentWhite,
                            modifier = Modifier.height(32.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "ANALYZING TONAL SPECTRUM & CONTRAST...",
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 1.5.sp,
                                color = NoirTextSecondary
                            )
                        )
                    }
                } else if (analysis != null) {
                    // Recommendation Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(NoirPitchBlack, RoundedCornerShape(2.dp))
                            .border(1.dp, NoirBorder, RoundedCornerShape(2.dp))
                            .padding(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "RECOMMENDED LOOK",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        letterSpacing = 1.2.sp,
                                        color = NoirTextTertiary
                                    )
                                )
                                Text(
                                    text = analysis.recommendedPreset.displayName,
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        letterSpacing = 1.5.sp,
                                        color = NoirAccentWhite,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }

                            Text(
                                text = analysis.rationale,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = NoirTextPrimary,
                                    lineHeight = 19.sp
                                )
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "DYNAMIC RANGE",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        letterSpacing = 1.sp,
                                        color = NoirTextTertiary
                                    )
                                )
                                Text(
                                    text = analysis.dynamicRangeEvaluation,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = NoirTextSecondary
                                    )
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "HISTOGRAM BALANCE",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        letterSpacing = 1.sp,
                                        color = NoirTextTertiary
                                    )
                                )
                                Text(
                                    text = analysis.tonalBalance,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        color = NoirTextSecondary
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NoirTextPrimary),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NoirBorder),
                            shape = RoundedCornerShape(2.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                        ) {
                            Text(
                                text = "DISMISS",
                                style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 1.5.sp)
                            )
                        }

                        Button(
                            onClick = {
                                onApplyEdits(analysis.proposedEdits)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NoirAccentWhite,
                                contentColor = NoirPitchBlack
                            ),
                            shape = RoundedCornerShape(2.dp),
                            modifier = Modifier
                                .weight(1.3f)
                                .height(44.dp)
                                .testTag("apply_ai_recommendation_button")
                        ) {
                            Text(
                                text = "APPLY LOOK",
                                style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 1.5.sp)
                            )
                        }
                    }
                }
            }
        }
    }
}
