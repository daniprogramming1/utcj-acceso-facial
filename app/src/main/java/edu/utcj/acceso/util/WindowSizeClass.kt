package edu.utcj.acceso.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

enum class AppWindowSize { Compact, Medium, Expanded }

@Composable
fun rememberAppWindowSize(): AppWindowSize {
    val widthDp = LocalConfiguration.current.screenWidthDp
    return when {
        widthDp < 600 -> AppWindowSize.Compact
        widthDp < 840 -> AppWindowSize.Medium
        else -> AppWindowSize.Expanded
    }
}

@Composable
fun isCompactWidth(): Boolean = rememberAppWindowSize() == AppWindowSize.Compact
