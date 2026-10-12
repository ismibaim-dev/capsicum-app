package com.chilivision.app.ml

sealed interface ClassificationResult {
    data class Diagnosed(
        val chiliClass: ChiliClass,
        val confidence: Float,
        val probabilities: FloatArray
    ) : ClassificationResult {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as Diagnosed
            return chiliClass == other.chiliClass &&
                    confidence == other.confidence &&
                    probabilities.contentEquals(other.probabilities)
        }

        override fun hashCode(): Int {
            var result = chiliClass.hashCode()
            result = 31 * result + confidence.hashCode()
            result = 31 * result + probabilities.contentHashCode()
            return result
        }
    }

    data class Uncertain(
        val topConfidence: Float,
        val probabilities: FloatArray
    ) : ClassificationResult {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as Uncertain
            return topConfidence == other.topConfidence &&
                    probabilities.contentEquals(other.probabilities)
        }

        override fun hashCode(): Int {
            var result = topConfidence.hashCode()
            result = 31 * result + probabilities.contentHashCode()
            return result
        }
    }

    data class NotChili(
        val confidence: Float,
        val probabilities: FloatArray
    ) : ClassificationResult {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as NotChili
            return confidence == other.confidence &&
                    probabilities.contentEquals(other.probabilities)
        }

        override fun hashCode(): Int {
            var result = confidence.hashCode()
            result = 31 * result + probabilities.contentHashCode()
            return result
        }
    }

    data class InvalidOutput(val reason: String) : ClassificationResult
}
