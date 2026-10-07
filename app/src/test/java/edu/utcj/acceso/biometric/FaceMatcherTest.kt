package edu.utcj.acceso.biometric

import edu.utcj.acceso.data.biometric.FaceMatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt

class FaceMatcherTest {
    private val matcher = FaceMatcher()

    /** Dimensión típica MobileFaceNet (sin cargar el motor TFLite en JVM). */
    private val mobileFaceNetDim = 192
    /** Dimensión del motor legado histograma+geometría. */
    private val legacyDim = 268

    private fun norm(vararg v: Float): FloatArray {
        val a = floatArrayOf(*v)
        var s = 0f
        for (x in a) s += x * x
        val n = sqrt(s)
        return FloatArray(a.size) { a[it] / n }
    }

    /** Vector L2-normalizado de dimensión [dim] (determinista). */
    private fun unitVector(dim: Int, seed: Int = 1): FloatArray {
        val a = FloatArray(dim) { i -> ((i * 17 + seed * 13) % 97) / 97f - 0.5f }
        var s = 0f
        for (x in a) s += x * x
        val n = sqrt(s).coerceAtLeast(1e-8f)
        return FloatArray(a.size) { a[it] / n }
    }

    @Test
    fun identicalVectors_matchAboveDefaultThreshold() {
        val v = norm(1f, 0f, 0f, 1f)
        val result = matcher.bestMatch(v, mapOf("20210001" to listOf(v)), FaceMatcher.DEFAULT_THRESHOLD)
        assertTrue(result.matched)
        assertEquals("20210001", result.matricula)
        assertTrue(result.bestSimilarity >= 0.99f)
    }

    @Test
    fun orthogonalVectors_doNotMatch() {
        val a = norm(1f, 0f, 0f)
        val b = norm(0f, 1f, 0f)
        val result = matcher.bestMatch(a, mapOf("x" to listOf(b)), FaceMatcher.DEFAULT_THRESHOLD)
        assertFalse(result.matched)
        assertTrue(result.bestSimilarity < 0.2f)
    }

    @Test
    fun defaultThreshold_is060_forMobileFaceNet() {
        assertEquals(0.60f, FaceMatcher.DEFAULT_THRESHOLD, 0.0001f)
        assertEquals(0.72f, FaceMatcher.LEGACY_THRESHOLD, 0.0001f)
    }

    @Test
    fun bestOfMultipleSamples_picksHighest() {
        val probe = norm(1f, 1f, 0f)
        val weak = norm(1f, 0f, 0f)
        val strong = norm(1f, 1f, 0.01f)
        val result = matcher.bestMatch(
            probe,
            mapOf("a" to listOf(weak), "b" to listOf(strong)),
            0.5f
        )
        assertEquals("b", result.matricula)
        assertTrue(result.matched)
    }

    @Test
    fun mobileFaceNetDim_192_identical_matches() {
        val v = unitVector(mobileFaceNetDim, seed = 7)
        val result = matcher.bestMatch(
            v,
            mapOf("m1" to listOf(v)),
            FaceMatcher.DEFAULT_THRESHOLD
        )
        assertTrue(result.matched)
        assertEquals("m1", result.matricula)
    }

    @Test
    fun legacyDim_268_worksWithLegacyThreshold() {
        val v = unitVector(legacyDim, seed = 3)
        val result = matcher.bestMatch(
            v,
            mapOf("legado" to listOf(v)),
            FaceMatcher.LEGACY_THRESHOLD
        )
        assertTrue(result.matched)
    }

    @Test
    fun mismatchedDimensions_areSkipped() {
        val probe192 = unitVector(mobileFaceNetDim, seed = 1)
        val sample268 = unitVector(legacyDim, seed = 2)
        val result = matcher.bestMatch(
            probe192,
            mapOf("viejo" to listOf(sample268)),
            FaceMatcher.DEFAULT_THRESHOLD
        )
        assertFalse(result.matched)
        assertNull(result.matricula)
        assertEquals(-1f, result.bestSimilarity, 0.0001f)
    }
}
