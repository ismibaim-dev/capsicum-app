package com.chilivision.app.ml

import org.junit.Assert.*
import org.junit.Test

class ChiliClassifierTest {

    private val classifier = ChiliClassifier(confidenceThreshold = 0.70f, temperature = 1.0f)

    @Test
    fun testClearDiagnosis() {
        val input = floatArrayOf(0.02f, 0.01f, 0.93f, 0.01f, 0.02f, 0.01f)
        val result = classifier.classify(input)
        assertTrue(result is ClassificationResult.Diagnosed)
        val diagnosed = result as ClassificationResult.Diagnosed
        assertEquals(ChiliClass.HEALTHY, diagnosed.chiliClass)
        assertEquals(0.93f, diagnosed.confidence, 1e-4f)
    }

    @Test
    fun testExactlyAtThreshold() {
        // max = 0.70 on Bacterial (index 0)
        val input = floatArrayOf(0.70f, 0.06f, 0.06f, 0.06f, 0.06f, 0.06f)
        val result = classifier.classify(input)
        assertTrue("Threshold comparison must be >= (0.70 is Diagnosed)", result is ClassificationResult.Diagnosed)
        val diagnosed = result as ClassificationResult.Diagnosed
        assertEquals(ChiliClass.BACTERIAL, diagnosed.chiliClass)
    }

    @Test
    fun testJustBelowThreshold() {
        // max = 0.699 on Bacterial
        val input = floatArrayOf(0.699f, 0.0602f, 0.0602f, 0.0602f, 0.0602f, 0.0602f)
        val result = classifier.classify(input)
        assertTrue("Just below 0.70 must be Uncertain", result is ClassificationResult.Uncertain)
    }

    @Test
    fun testConfidentNonChili() {
        val input = floatArrayOf(0.05f, 0.02f, 0.03f, 0.85f, 0.03f, 0.02f)
        val result = classifier.classify(input)
        assertTrue(result is ClassificationResult.NotChili)
        val notChili = result as ClassificationResult.NotChili
        assertEquals(0.85f, notChili.confidence, 1e-4f)
    }

    @Test
    fun testLowConfidenceNonChiliIsUncertain() {
        // max = 0.55 on NonChili (index 3)
        val input = floatArrayOf(0.09f, 0.09f, 0.09f, 0.55f, 0.09f, 0.09f)
        val result = classifier.classify(input)
        assertTrue(
            "Low-confidence NonChili (0.55 < 0.70) must be Uncertain, not NotChili",
            result is ClassificationResult.Uncertain
        )
    }

    @Test
    fun testNearUniformIsUncertain() {
        val p = 1.0f / 6.0f
        val input = floatArrayOf(p, p, p, p, p, p)
        val result = classifier.classify(input)
        assertTrue(result is ClassificationResult.Uncertain)
    }

    @Test
    fun testEveryClassDiagnosable() {
        ChiliClass.values().forEachIndexed { index, expectedClass ->
            val input = FloatArray(6) { 0.01f }
            input[index] = 0.95f

            val result = classifier.classify(input)
            if (expectedClass == ChiliClass.NON_CHILI) {
                assertTrue(result is ClassificationResult.NotChili)
            } else {
                assertTrue(result is ClassificationResult.Diagnosed)
                val diagnosed = result as ClassificationResult.Diagnosed
                assertEquals(expectedClass, diagnosed.chiliClass)
            }
        }
    }

    @Test
    fun testWrongLengthReturnsInvalidOutput() {
        val input5 = floatArrayOf(0.2f, 0.2f, 0.2f, 0.2f, 0.2f)
        val result5 = classifier.classify(input5)
        assertTrue(result5 is ClassificationResult.InvalidOutput)

        val input7 = floatArrayOf(0.14f, 0.14f, 0.14f, 0.14f, 0.14f, 0.14f, 0.16f)
        val result7 = classifier.classify(input7)
        assertTrue(result7 is ClassificationResult.InvalidOutput)
    }

    @Test
    fun testNaNReturnsInvalidOutput() {
        val input = floatArrayOf(0.1f, Float.NaN, 0.1f, 0.1f, 0.1f, 0.6f)
        val result = classifier.classify(input)
        assertTrue(result is ClassificationResult.InvalidOutput)
    }

    @Test
    fun testSumNotOneReturnsInvalidOutput() {
        val input = floatArrayOf(0.1f, 0.1f, 0.1f, 0.1f, 0.0f, 0.0f) // sums to 0.4
        val result = classifier.classify(input)
        assertTrue(result is ClassificationResult.InvalidOutput)
    }

    @Test
    fun testTemperatureIdentity() {
        val input = floatArrayOf(0.1f, 0.1f, 0.6f, 0.1f, 0.05f, 0.05f)
        val scaled = classifier.applyTemperatureScaling(input, 1.0f)
        assertArrayEquals(input, scaled, 1e-6f)
    }

    @Test
    fun testTemperatureApplied() {
        val input = floatArrayOf(0.1f, 0.1f, 0.6f, 0.1f, 0.05f, 0.05f)
        val tempClassifier = ChiliClassifier(confidenceThreshold = 0.70f, temperature = 1.220418f)
        val scaled = tempClassifier.applyTemperatureScaling(input, 1.220418f)

        // Temperature > 1 softens the distribution (top confidence drops)
        assertTrue("Top probability should decrease after T > 1.0", scaled[2] < input[2])
    }

    @Test
    fun testTemperatureFlipsOutcomeToUncertain() {
        // Raw top confidence is 0.72 (above 0.70 threshold)
        val input = floatArrayOf(0.05f, 0.05f, 0.72f, 0.06f, 0.06f, 0.06f)
        val tempClassifier = ChiliClassifier(confidenceThreshold = 0.70f, temperature = 1.220418f)

        val result = tempClassifier.classify(input)
        assertTrue(
            "Temperature scaling at 1.220418 softens 0.72 raw confidence below 0.70, resulting in Uncertain",
            result is ClassificationResult.Uncertain
        )
    }
}
