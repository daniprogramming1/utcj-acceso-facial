package edu.utcj.acceso.biometric

import edu.utcj.acceso.data.biometric.FaceMatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt

class FaceMatcherTest {
    private val matcher = FaceMatcher()

    private fun norm(vararg v: Float): FloatArray {
        val a = floatArrayOf(*v)
        var s = 0f
        for (x in a) s += x * x
        val n = sqrt(s)
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
        val result = matcher.bestMatch(a, mapOf("x" to listOf(b)), 0.72f)
        assertFalse(result.matched)
        assertTrue(result.bestSimilarity < 0.2f)
    }

    @Test
    fun defaultThreshold_is072() {
        assertEquals(0.72f, FaceMatcher.DEFAULT_THRESHOLD, 0.0001f)
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
}
