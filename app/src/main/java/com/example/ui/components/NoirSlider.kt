package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NoirAccentWhite
import com.example.ui.theme.NoirBorderLight
import com.example.ui.theme.NoirTextPrimary
import com.example.ui.theme.NoirTextSecondary
import com.example.ui.theme.NoirTextTertiary

@Composable
fun NoirSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    defaultValue: Float = 0f,
    step: Float = 0f,
    onValueChangeFinished: (() -> Unit)? = null,
    displayFormatter: (Float) -> String = {
        if (it > 0) "+${it.toInt()}" else it.toInt().toString()
    },
    modifier: Modifier = Modifier
) {
    val isChanged = kotlin.math.abs(value - defaultValue) > 0.001f

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("slider_$label")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelLarge.copy(
                    letterSpacing = 1.6.sp,
                    fontSize = 11.sp,
                    color = if (isChanged) NoirTextPrimary else NoirTextSecondary
                )
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = displayFormatter(value),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = if (isChanged) FontWeight.Bold else FontWeight.Normal,
                        color = if (isChanged) NoirAccentWhite else NoirTextTertiary
                    )
                )

                if (isChanged) {
                    Text(
                        text = "RESET",
                        modifier = Modifier
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                onValueChange(defaultValue)
                                onValueChangeFinished?.invoke()
                            }
                            .padding(start = 4.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            letterSpacing = 1.sp,
                            color = NoirTextTertiary
                        )
                    )
                }
            }
        }

        Slider(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            valueRange = valueRange,
            steps = if (step > 0f) (((valueRange.endInclusive - valueRange.start) / step).toInt() - 1).coerceAtLeast(0) else 0,
            colors = SliderDefaults.colors(
                thumbColor = NoirAccentWhite,
                activeTrackColor = NoirAccentWhite,
                inactiveTrackColor = NoirBorderLight,
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
        )
    }
}
