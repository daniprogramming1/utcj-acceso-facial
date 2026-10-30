package edu.utcj.acceso.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import edu.utcj.acceso.R
import edu.utcj.acceso.brand.BrandConfig
import edu.utcj.acceso.ui.theme.AppTheme
import edu.utcj.acceso.ui.theme.BrandPrimary
import edu.utcj.acceso.ui.theme.Motion
import edu.utcj.acceso.ui.theme.Spacing
import edu.utcj.acceso.util.isCompactHeight
import kotlinx.coroutines.delay

/** Tiempo total que la bienvenida permanece visible antes de navegar sola. */
const val SPLASH_DURATION_MS = 1_600L

/**
 * Continuación en Compose del splash nativo (AndroidX SplashScreen): mismo fondo y mismo
 * logotipo, de modo que el cambio no se nota; aquí se añaden el nombre, el lema y el
 * indicador de progreso. Al terminar llama a [onFinished] sin intervención del usuario.
 */
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val finish by rememberUpdatedState(onFinished)
    LaunchedEffect(Unit) {
        delay(SPLASH_DURATION_MS)
        finish()
    }
    SplashContent()
}

@Composable
fun SplashContent(modifier: Modifier = Modifier) {
    val reduced = AppTheme.reducedMotion
    val compact = isCompactHeight()
    val logoSize = if (compact) 88.dp else 120.dp

    // Los textos entran justo después de que el logo del sistema "aterriza".
    val reveal = remember { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(Unit) { reveal.animateTo(1f, tween(Motion.LONG, easing = Motion.emphasized)) }

    Box(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .semantics { contentDescription = "Iniciando ${BrandConfig.APP_NAME}" },
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier.widthIn(max = Spacing.formMaxWidth).padding(horizontal = Spacing.screenCompact),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SplashLogo(logoSize)
            Spacer(Modifier.height(if (compact) Spacing.lg else Spacing.xxl))
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer {
                    alpha = reveal.value
                    translationY = (1f - reveal.value) * 24.dp.toPx()
                }
            ) {
                Text(
                    BrandConfig.APP_NAME,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    BrandConfig.TAGLINE,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(if (compact) Spacing.lg else Spacing.xxl))
                LoadingDots(animated = !reduced)
                Spacer(Modifier.height(Spacing.md))
                Text(
                    "Iniciando aplicación…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
            BrandConfig.INSTITUTION_NAME,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = Spacing.screenCompact, vertical = Spacing.xxl)
                .graphicsLayer { alpha = reveal.value }
        )
    }
}

/** Mismo dibujo que el ícono del splash nativo (`ic_launcher_foreground` sobre `brand_primary`). */
@Composable
private fun SplashLogo(size: Dp) {
    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.28f))
            .background(BrandPrimary),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier.size(size * 1.35f)
        )
    }
}

/** Tres puntos que se encienden en secuencia; estáticos con movimiento reducido. */
@Composable
private fun LoadingDots(animated: Boolean) {
    val color = MaterialTheme.colorScheme.primary
    val transition = rememberInfiniteTransition(label = "splash-dots")
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        repeat(3) { i ->
            val pulse by transition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 600, delayMillis = i * 200),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dot-$i"
            )
            val level = if (animated) pulse else 1f - i * 0.3f
            Box(
                Modifier
                    .size(10.dp)
                    .graphicsLayer {
                        alpha = level
                        scaleX = 0.8f + 0.2f * level
                        scaleY = scaleX
                    }
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}
