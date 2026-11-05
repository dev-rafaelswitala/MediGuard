package com.rafaelswitala.mediguard.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.rafaelswitala.mediguard.R
import kotlinx.coroutines.delay

private val SplashLight = Color(0xFFF3FFFB)
private val SplashDark = Color(0xFF0B1B22)

/**
 * Zeigt das MD-Logo rechteckig (ohne Android-12-Kreismaske) kurz nach dem System-Splash.
 */
@Composable
fun BrandSplashOverlay(
    darkTheme: Boolean = isSystemInDarkTheme(),
    visibleMillis: Long = 1400L,
    content: @Composable () -> Unit
) {
    var showBrand by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(visibleMillis)
        showBrand = false
    }
    Box(Modifier.fillMaxSize()) {
        content()
        if (showBrand) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (darkTheme) SplashDark else SplashLight),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_splash_brand),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth(0.82f)
                        .aspectRatio(1.9f)
                        .padding(horizontal = 12.dp)
                )
            }
        }
    }
}
