package com.example.ui.splash

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.EclipseEyeLogo
import com.example.ui.theme.NoirAccentWhite
import com.example.ui.theme.NoirPitchBlack
import com.example.ui.theme.NoirTextTertiary
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isVisible = true
        delay(950) // Fast, clean, minimal branded transition
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NoirPitchBlack),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(animationSpec = tween(500)),
            exit = fadeOut(animationSpec = tween(300))
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // The Eclipse Eye Logo
                EclipseEyeLogo(size = 72.dp)

                Spacer(modifier = Modifier.height(28.dp))

                // Brand Name
                Text(
                    text = "UMBRA",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 38.sp,
                        letterSpacing = 12.sp,
                        color = NoirAccentWhite,
                        fontWeight = FontWeight.ExtraLight
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "SEE THE DARK DIFFERENTLY",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 4.sp,
                        fontSize = 10.sp,
                        color = NoirTextTertiary
                    )
                )
            }
        }
    }
}
