package edu.utcj.acceso.ui.components

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.util.Log
import android.view.HapticFeedbackConstants
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

private const val TAG = "ScanFeedback"
private const val TONE_VOLUME = 80
private const val TONE_MS = 220

/**
 * Vibración y tono al resolver un escaneo (panel del guardia y kiosco): se siente sin mirar la
 * pantalla, útil en la caseta con sol y ruido. Permitido y denegado usan patrones distintos.
 * No requiere el permiso VIBRATE: usa la retroalimentación háptica de la vista.
 *
 * @return función a llamar con `true` (permitido) o `false` (denegado).
 */
@Composable
fun rememberScanFeedback(soundEnabled: Boolean): (allowed: Boolean) -> Unit {
    val view = LocalView.current
    val tone = remember {
        runCatching { ToneGenerator(AudioManager.STREAM_NOTIFICATION, TONE_VOLUME) }
            .onFailure { Log.w(TAG, "Sin generador de tonos; solo vibración", it) }
            .getOrNull()
    }
    DisposableEffect(tone) { onDispose { tone?.release() } }
    return remember(view, tone, soundEnabled) {
        { allowed ->
            val haptic = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (allowed) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.REJECT
            } else {
                if (allowed) HapticFeedbackConstants.VIRTUAL_KEY else HapticFeedbackConstants.LONG_PRESS
            }
            view.performHapticFeedback(haptic)
            if (soundEnabled) {
                tone?.startTone(if (allowed) ToneGenerator.TONE_PROP_ACK else ToneGenerator.TONE_PROP_NACK, TONE_MS)
            }
        }
    }
}
