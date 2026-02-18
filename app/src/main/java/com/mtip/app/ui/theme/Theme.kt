package com.mtip.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Orange = Color(0xFFFF6600)
private val Light = lightColorScheme(primary = Orange, onPrimary = Color.White, primaryContainer = Color(0xFFFFE0CC))
private val Dark = darkColorScheme(primary = Orange, onPrimary = Color.Black, primaryContainer = Color(0xFFCC5200))

@Composable
fun MTipTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) Dark else Light, content = content)
}