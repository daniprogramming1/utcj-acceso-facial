package edu.utcj.acceso.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import edu.utcj.acceso.domain.time.DayPhase
import edu.utcj.acceso.ui.components.horizonPaletteFor
import edu.utcj.acceso.ui.kiosk.KioskColors
import edu.utcj.acceso.ui.theme.DarkColors
import edu.utcj.acceso.ui.theme.DarkExtendedColors
import edu.utcj.acceso.ui.theme.LightColors
import edu.utcj.acceso.ui.theme.LightExtendedColors
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Contraste WCAG 2.2 de los pares de tokens que se usan para texto (CLAUDE.md §3.8):
 * texto normal ≥ 4.5:1, bordes de controles y texto grande ≥ 3:1.
 */
class ContrastTest {

    private fun ratio(fg: Color, bg: Color): Double {
        val opaqueBg = bg.compositeOver(Color.White)
        val opaqueFg = fg.compositeOver(opaqueBg)
        val l1 = opaqueFg.luminance() + 0.05
        val l2 = opaqueBg.luminance() + 0.05
        return maxOf(l1, l2) / minOf(l1, l2)
    }

    private fun check(name: String, fg: Color, bg: Color, min: Double) {
        val r = ratio(fg, bg)
        assertTrue("$name: %.2f:1 < %.1f:1".format(r, min), r >= min)
    }

    private val text = 4.5
    private val large = 3.0

    @Test
    fun lightSchemeText() = with(LightColors) {
        check("onBackground/background", onBackground, background, text)
        check("onSurface/surface", onSurface, surface, text)
        check("onSurfaceVariant/background", onSurfaceVariant, background, text)
        check("onSurfaceVariant/surface", onSurfaceVariant, surface, text)
        check("primary/background (enlaces)", primary, background, text)
        check("onPrimary/primary (botón)", onPrimary, primary, text)
        check("onPrimaryContainer/primaryContainer", onPrimaryContainer, primaryContainer, text)
        check("onSecondaryContainer/secondaryContainer", onSecondaryContainer, secondaryContainer, text)
        check("onError/error", onError, error, text)
        check("outline/background (bordes de campos)", outline, background, large)
    }

    @Test
    fun darkSchemeText() = with(DarkColors) {
        check("onBackground/background", onBackground, background, text)
        check("onSurface/surface", onSurface, surface, text)
        check("onSurfaceVariant/background", onSurfaceVariant, background, text)
        check("onSurfaceVariant/surface", onSurfaceVariant, surface, text)
        check("primary/background (enlaces)", primary, background, text)
        check("onPrimary/primary (botón)", onPrimary, primary, text)
        check("onPrimaryContainer/primaryContainer", onPrimaryContainer, primaryContainer, text)
        check("onSecondaryContainer/secondaryContainer", onSecondaryContainer, secondaryContainer, text)
        check("outline/background (bordes de campos)", outline, background, large)
    }

    @Test
    fun semanticContainers() {
        listOf(LightExtendedColors to "claro", DarkExtendedColors to "oscuro").forEach { (e, mode) ->
            check("$mode success", e.onSuccessContainer, e.successContainer, text)
            check("$mode danger", e.onDangerContainer, e.dangerContainer, text)
            check("$mode warning", e.onWarningContainer, e.warningContainer, text)
            check("$mode info", e.onInfoContainer, e.infoContainer, text)
            check("$mode neutral", e.onNeutralContainer, e.neutralContainer, text)
            // Franja «Acceso permitido / denegado»: texto grande (onSuccess/onDanger) sobre el acento.
            check("$mode banner permitido", e.onSuccess, e.success, large)
            check("$mode banner denegado", e.onDanger, e.danger, large)
        }
    }

    @Test
    fun kioskReadsFromTheLine() {
        check("kiosco texto secundario", KioskColors.textSecondary, KioskColors.background, text)
        check("kiosco texto secundario sobre panel", KioskColors.textSecondary, KioskColors.panel, text)
    }

    @Test
    fun horizonBandLabelIsReadableInEveryPhase() {
        DayPhase.entries.forEach { phase ->
            val p = horizonPaletteFor(phase)
            listOf(p.skyTop, p.skyBottom).forEach { sky ->
                check("banda ${phase.label}", p.label, p.labelBackground.compositeOver(sky), text)
            }
        }
    }
}
