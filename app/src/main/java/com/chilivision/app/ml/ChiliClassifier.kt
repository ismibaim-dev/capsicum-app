package com.chilivision.app.ml

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max

class ChiliClassifier(
    val confidenceThreshold: Float = DEFAULT_CONFIDENCE_THRESHOLD,
    val temperature: Float = DEFAULT_TEMPERATURE
) {
    companion object {
        const val DEFAULT_CONFIDENCE_THRESHOLD = 0.70f
        const val DEFAULT_TEMPERATURE = 1.0f // Calibrated temperature: 1.220418f
        const val NUM_CLASSES = 6
        private const val PROB_EPSILON = 1e-12f
        private const val SUM_TOLERANCE = 1e-3f
    }

    fun classify(rawProbabilities: FloatArray): ClassificationResult {
        // 1. Validation
        if (rawProbabilities.size != NUM_CLASSES) {
            return ClassificationResult.InvalidOutput("Expected $NUM_CLASSES probabilities, got ${rawProbabilities.size}")
        }

        var sum = 0.0f
        for (p in rawProbabilities) {
            if (p.isNaN() || p.isInfinite()) {
                return ClassificationResult.InvalidOutput("Output vector contains NaN or Infinite values")
            }
            if (p < 0.0f || p > 1.0f) {
                return ClassificationResult.InvalidOutput("Probability value $p out of range [0, 1]")
            }
            sum += p
        }

        if (abs(sum - 1.0f) > SUM_TOLERANCE) {
            return ClassificationResult.InvalidOutput("Probabilities do not sum to 1 (sum = $sum)")
        }

        // 2. Temperature scaling: softmax(log(p) / T)
        val probabilities = applyTemperatureScaling(rawProbabilities, temperature)

        // 3. Find argmax and max confidence
        var argmax = 0
        var maxProb = probabilities[0]
        for (i in 1 until NUM_CLASSES) {
            if (probabilities[i] > maxProb) {
                maxProb = probabilities[i]
                argmax = i
            }
        }

        // 4. Decision flow
        return when {
            maxProb < confidenceThreshold -> {
                ClassificationResult.Uncertain(
                    topConfidence = maxProb,
                    probabilities = probabilities
                )
            }
            argmax == ChiliClass.NON_CHILI.ordinal -> {
                ClassificationResult.NotChili(
                    confidence = maxProb,
                    probabilities = probabilities
                )
            }
            else -> {
                val chiliClass = ChiliClass.fromOrdinal(argmax)
                    ?: return ClassificationResult.InvalidOutput("Invalid class index $argmax")
                ClassificationResult.Diagnosed(
                    chiliClass = chiliClass,
                    confidence = maxProb,
                    probabilities = probabilities
                )
            }
        }
    }

    fun applyTemperatureScaling(rawProbs: FloatArray, temp: Float): FloatArray {
        if (temp == 1.0f) {
            return rawProbs.copyOf()
        }

        val scaledLogits = FloatArray(NUM_CLASSES)
        var maxLogit = -Float.MAX_VALUE
        for (i in 0 until NUM_CLASSES) {
            val clampedP = max(rawProbs[i], PROB_EPSILON)
            val logit = ln(clampedP.toDouble()).toFloat() / temp
            scaledLogits[i] = logit
            if (logit > maxLogit) {
                maxLogit = logit
            }
        }

        val expValues = FloatArray(NUM_CLASSES)
        var sumExp = 0.0
        for (i in 0 until NUM_CLASSES) {
            val e = exp((scaledLogits[i] - maxLogit).toDouble())
            expValues[i] = e.toFloat()
            sumExp += e
        }

        val scaledProbs = FloatArray(NUM_CLASSES)
        for (i in 0 until NUM_CLASSES) {
            scaledProbs[i] = (expValues[i] / sumExp).toFloat()
        }

        return scaledProbs
    }
}
