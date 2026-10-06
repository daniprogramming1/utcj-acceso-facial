package edu.utcj.acceso.data.biometric

import android.graphics.Bitmap
import android.graphics.Color
import com.google.mlkit.vision.face.Face
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Face quality gates with Spanish guidance strings for registration / kiosk.
 */
@Singleton
class FaceQualityChecker @Inject constructor() {

    data class QualityResult(
        val ok: Boolean,
        val guidanceEs: String,
        val score: Float = 0f
    )

    fun evaluate(bitmap: Bitmap, faces: List<Face>): QualityResult {
        if (faces.isEmpty()) {
            return QualityResult(false, "No se detectó un rostro")
        }
        if (faces.size > 1) {
            return QualityResult(false, "Solo una persona a la vez")
        }
        val face = faces.first()
        val box = face.boundingBox
        val areaRatio = (box.width() * box.height().toFloat()) / (bitmap.width * bitmap.height)

        if (areaRatio < 0.08f) return QualityResult(false, "Acércate un poco", areaRatio)
        if (areaRatio > 0.65f) return QualityResult(false, "Aléjate un poco", areaRatio)

        if (abs(face.headEulerAngleY) > 20f || abs(face.headEulerAngleZ) > 20f) {
            return QualityResult(false, "Mira de frente a la cámara")
        }

        val brightness = meanBrightness(bitmap, box.left, box.top, box.width(), box.height())
        if (brightness < 40f) return QualityResult(false, "Mejora la iluminación", brightness / 255f)
        if (brightness > 230f) return QualityResult(false, "Hay demasiada luz; reduce el brillo", brightness / 255f)

        val sharp = approximateSharpness(bitmap, box.left, box.top, box.width(), box.height())
        if (sharp < 12f) return QualityResult(false, "Mantén el dispositivo estable", sharp / 100f)

        val score = (areaRatio.coerceIn(0f, 1f) * 0.4f) +
            ((brightness / 255f).coerceIn(0f, 1f) * 0.3f) +
            ((sharp / 80f).coerceIn(0f, 1f) * 0.3f)
        return QualityResult(true, "Rostro listo para capturar", score)
    }

    private fun meanBrightness(bmp: Bitmap, l: Int, t: Int, w: Int, h: Int): Float {
        val left = l.coerceIn(0, bmp.width - 1)
        val top = t.coerceIn(0, bmp.height - 1)
        val right = (l + w).coerceIn(left + 1, bmp.width)
        val bottom = (t + h).coerceIn(top + 1, bmp.height)
        var sum = 0.0
        var n = 0
        val step = maxOf(1, (right - left) / 32)
        var y = top
        while (y < bottom) {
            var x = left
            while (x < right) {
                val c = bmp.getPixel(x, y)
                sum += 0.299 * Color.red(c) + 0.587 * Color.green(c) + 0.114 * Color.blue(c)
                n++
                x += step
            }
            y += step
        }
        return if (n == 0) 0f else (sum / n).toFloat()
    }

    /** Laplacian-ish variance proxy on downsampled luma. */
    private fun approximateSharpness(bmp: Bitmap, l: Int, t: Int, w: Int, h: Int): Float {
        val left = l.coerceIn(0, bmp.width - 1)
        val top = t.coerceIn(0, bmp.height - 1)
        val ww = w.coerceAtMost(bmp.width - left).coerceAtLeast(2)
        val hh = h.coerceAtMost(bmp.height - top).coerceAtLeast(2)
        val sample = Bitmap.createScaledBitmap(
            Bitmap.createBitmap(bmp, left, top, ww, hh),
            48, 48, true
        )
        val luma = FloatArray(48 * 48)
        for (y in 0 until 48) for (x in 0 until 48) {
            val c = sample.getPixel(x, y)
            luma[y * 48 + x] = 0.299f * Color.red(c) + 0.587f * Color.green(c) + 0.114f * Color.blue(c)
        }
        var sum = 0.0
        var sumSq = 0.0
        var n = 0
        for (y in 1 until 47) for (x in 1 until 47) {
            val i = y * 48 + x
            val lap = -4 * luma[i] + luma[i - 1] + luma[i + 1] + luma[i - 48] + luma[i + 48]
            sum += lap
            sumSq += lap * lap
            n++
        }
        val mean = sum / n
        val variance = (sumSq / n) - mean * mean
        return sqrt(variance.coerceAtLeast(0.0)).toFloat()
    }
}
