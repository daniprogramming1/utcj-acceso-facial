package edu.utcj.acceso.ui.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.EnhancedEncryption
import androidx.compose.material.icons.rounded.HideImage
import androidx.compose.material.icons.rounded.HowToReg
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import edu.utcj.acceso.brand.BrandConfig
import edu.utcj.acceso.ui.components.BrandLockup
import edu.utcj.acceso.ui.components.FaceScanIllustration
import edu.utcj.acceso.ui.components.HowItWorksIllustration
import edu.utcj.acceso.ui.components.LinkButton
import edu.utcj.acceso.ui.components.PrimaryButton
import edu.utcj.acceso.ui.components.PrivacyShieldIllustration
import edu.utcj.acceso.ui.theme.AppShapes
import edu.utcj.acceso.ui.theme.Spacing
import edu.utcj.acceso.util.isCompactHeight
import edu.utcj.acceso.util.isLandscape
import kotlinx.coroutines.launch

data class OnboardingPage(
    val title: String,
    val body: String,
    val bullets: List<Pair<ImageVector, String>>,
    val illustration: @Composable (Modifier) -> Unit
)

val onboardingPages = listOf(
    OnboardingPage(
        title = "Entra al campus con tu rostro",
        body = "${BrandConfig.APP_NAME} verifica tu identidad en segundos, sin credenciales ni filas.",
        bullets = listOf(
            Icons.Rounded.Speed to "Verificación en menos de 2 segundos",
            Icons.Rounded.WifiOff to "Funciona incluso sin conexión",
            Icons.Rounded.QrCode2 to "Respaldo con QR dinámico o huella"
        ),
        illustration = { FaceScanIllustration(it) }
    ),
    OnboardingPage(
        title = "Tu privacidad, primero",
        body = "Diseñada para proteger tus datos biométricos desde el primer momento.",
        bullets = listOf(
            Icons.Rounded.HideImage to "Nunca guardamos fotografías de tu rostro",
            Icons.Rounded.EnhancedEncryption to "Solo un vector cifrado (AES-256) en el dispositivo",
            Icons.Rounded.DeleteOutline to "Elimina tus datos cuando quieras"
        ),
        illustration = { PrivacyShieldIllustration(it) }
    ),
    OnboardingPage(
        title = "Así funciona",
        body = "Tres pasos y listo para entrar.",
        bullets = listOf(
            Icons.Rounded.HowToReg to "1. Regístrate y acepta el aviso de privacidad",
            Icons.Rounded.VerifiedUser to "2. Seguridad aprueba tu registro",
            Icons.Rounded.CheckCircle to "3. Mira al kiosco y entra"
        ),
        illustration = { HowItWorksIllustration(it) }
    )
)

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pagerState = rememberPagerState { onboardingPages.size }
    OnboardingContent(pagerState = pagerState, onFinish = onFinish)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingContent(pagerState: PagerState, onFinish: () -> Unit) {
    val scope = rememberCoroutineScope()
    val last = pagerState.currentPage == onboardingPages.lastIndex
    val sideBySide = isLandscape() || isCompactHeight()
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = Spacing.screenCompact, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BrandLockup(logoSize = 32.dp, showInstitution = false, modifier = Modifier.weight(1f))
            if (!last) LinkButton("Omitir", onClick = onFinish)
        }
        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f).fillMaxWidth()) { page ->
            OnboardingPageView(onboardingPages[page], sideBySide)
        }
        Column(
            Modifier.fillMaxWidth().padding(horizontal = Spacing.screenCompact, vertical = Spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PageIndicator(onboardingPages.size, pagerState.currentPage)
            Spacer(Modifier.height(Spacing.lg))
            PrimaryButton(
                text = if (last) "Comenzar" else "Siguiente",
                icon = if (last) null else Icons.AutoMirrored.Rounded.ArrowForward,
                onClick = {
                    if (last) onFinish()
                    else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                },
                modifier = Modifier.fillMaxWidth().widthIn(max = Spacing.formMaxWidth)
            )
        }
    }
}

@Composable
private fun OnboardingPageView(page: OnboardingPage, sideBySide: Boolean) {
    if (sideBySide) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = Spacing.xxl),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xxl)
        ) {
            Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                page.illustration(Modifier.fillMaxHeight(0.9f).aspectRatio(1f))
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) { PageText(page, TextAlign.Start) }
        }
    } else {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = Spacing.screenCompact),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(Spacing.sm))
            page.illustration(Modifier.fillMaxWidth(0.7f).widthIn(max = 320.dp).aspectRatio(1f))
            Spacer(Modifier.height(Spacing.xl))
            Column(Modifier.widthIn(max = Spacing.contentMaxWidth)) { PageText(page, TextAlign.Center) }
        }
    }
}

@Composable
private fun PageText(page: OnboardingPage, align: TextAlign) {
    Text(
        page.title,
        style = MaterialTheme.typography.headlineLarge,
        textAlign = align,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(Spacing.sm))
    Text(
        page.body,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = align,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(Spacing.xl))
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        page.bullets.forEach { (icon, text) ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.medium)
                    .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(36.dp).background(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.shapes.small),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(Spacing.md))
                Text(text, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun PageIndicator(count: Int, current: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
        repeat(count) { i ->
            val selected = i == current
            val w by animateDpAsState(if (selected) 28.dp else 8.dp, label = "dotW")
            val c by animateColorAsState(
                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                label = "dotC"
            )
            Box(Modifier.height(8.dp).width(w).background(c, AppShapes.pill))
        }
    }
}
