package edu.utcj.acceso.data.biometric

import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

/**
 * Coincidencia por similitud coseno contra embeddings almacenados.
 *
 * Umbral por defecto: **0.60** (calibrado para MobileFaceNet 192-d L2).
 * Con el motor legado (histograma 268-d) conviene subir a ~0.72
 * ([LegacyHistogramEmbeddingEngine.RECOMMENDED_THRESHOLD]).
 * Configurable en tiempo de ejecución vía [edu.utcj.acceso.data.repository.SettingsRepository].
 */
@Singleton
class FaceMatcher @Inject constructor() {

    data class MatchResult(
        val matricula: String?,
        val bestSimilarity: Float,
        val matched: Boolean,
        val sampleIndex: Int = -1
    )

    /**
     * @param probe embedding L2-normalizado de consulta
     * @param gallery mapa matrícula → lista de embeddings L2-normalizados
     * @param threshold umbral de similitud coseno (por defecto [DEFAULT_THRESHOLD])
     */
    fun bestMatch(
        probe: FloatArray,
        gallery: Map<String, List<FloatArray>>,
        threshold: Float = DEFAULT_THRESHOLD
    ): MatchResult {
        var bestMat: String? = null
        var bestSim = -1f
        var bestIdx = -1
        for ((mat, samples) in gallery) {
            for ((idx, sample) in samples.withIndex()) {
                if (sample.size != probe.size) {
                    // Dimensiones incompatibles (p. ej. legado 268 vs MobileFaceNet 192):
                    // se omiten; el alumno debe volver a registrarse tras cambio de modelo.
                    continue
                }
                val sim = cosineSimilarity(probe, sample)
                if (sim > bestSim) {
                    bestSim = sim
                    bestMat = mat
                    bestIdx = idx
                }
            }
        }
        val matched = bestMat != null && bestSim >= threshold
        return MatchResult(
            matricula = if (matched) bestMat else null,
            bestSimilarity = bestSim,
            matched = matched,
            sampleIndex = if (matched) bestIdx else -1
        )
    }

    fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        require(a.size == b.size) { "Dimension mismatch: ${a.size} vs ${b.size}" }
        var dot = 0f
        var na = 0f
        var nb = 0f
        for (i in a.indices) {
            dot += a[i] * b[i]
            na += a[i] * a[i]
            nb += b[i] * b[i]
        }
        val denom = sqrt(na) * sqrt(nb)
        return if (denom < 1e-8f) 0f else dot / denom
    }

    companion object {
        /**
         * Umbral coseno por defecto para MobileFaceNet (embeddings L2 ~192-d).
         * Antes (histograma escolar): 0.72 — ver [LEGACY_THRESHOLD].
         */
        const val DEFAULT_THRESHOLD = 0.60f

        /** Umbral sugerido si se fuerza el motor legado (histograma). */
        const val LEGACY_THRESHOLD = 0.72f
    }
}
