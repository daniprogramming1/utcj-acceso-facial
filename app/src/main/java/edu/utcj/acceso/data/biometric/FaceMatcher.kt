package edu.utcj.acceso.data.biometric

import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

/**
 * Cosine-similarity matcher against stored embeddings.
 * Default threshold: **0.72** (configured via [edu.utcj.acceso.data.repository.SettingsRepository]).
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
     * @param probe L2-normalized probe embedding
     * @param gallery map of matricula → list of L2-normalized sample embeddings
     * @param threshold cosine similarity threshold (default 0.72)
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
            samples.forEachIndexed { idx, sample ->
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
        /** Default cosine similarity threshold for school-project embeddings. */
        const val DEFAULT_THRESHOLD = 0.72f
    }
}
