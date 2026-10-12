package com.chilivision.app.ui.result

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.chilivision.app.ml.BitmapPreprocessResult
import com.chilivision.app.ml.ChiliClassifier
import com.chilivision.app.ml.ClassificationResult
import com.chilivision.app.ml.ImagePreprocessor
import com.chilivision.app.ml.TfLiteChiliModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ResultUiState {
    object Idle : ResultUiState
    object Loading : ResultUiState
    data class Success(
        val bitmap: Bitmap,
        val result: ClassificationResult,
        val rawProbabilities: FloatArray,
        val latencyMs: Long
    ) : ResultUiState {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as Success
            return bitmap == other.bitmap &&
                    result == other.result &&
                    rawProbabilities.contentEquals(other.rawProbabilities) &&
                    latencyMs == other.latencyMs
        }

        override fun hashCode(): Int {
            var res = bitmap.hashCode()
            res = 31 * res + result.hashCode()
            res = 31 * res + rawProbabilities.contentHashCode()
            res = 31 * res + latencyMs.hashCode()
            return res
        }
    }
    data class Error(val message: String) : ResultUiState
}

class ResultViewModel(application: Application) : AndroidViewModel(application) {

    private var tfliteModel: TfLiteChiliModel? = null

    private val _uiState = MutableStateFlow<ResultUiState>(ResultUiState.Idle)
    val uiState: StateFlow<ResultUiState> = _uiState.asStateFlow()

    // Configurable thresholds for Dev Settings
    val confidenceThreshold = MutableStateFlow(ChiliClassifier.DEFAULT_CONFIDENCE_THRESHOLD)
    val temperature = MutableStateFlow(ChiliClassifier.DEFAULT_TEMPERATURE)

    init {
        try {
            tfliteModel = TfLiteChiliModel(application.applicationContext)
        } catch (e: Exception) {
            _uiState.value = ResultUiState.Error("Model initialization failed: ${e.message}")
        }
    }

    fun analyzeUri(uri: Uri) {
        val model = tfliteModel
        if (model == null) {
            _uiState.value = ResultUiState.Error("Model is not initialized")
            return
        }

        viewModelScope.launch {
            _uiState.value = ResultUiState.Loading

            // 1. Preprocess Uri to Bitmap and ByteBuffer
            when (val prepResult = ImagePreprocessor.preprocessUri(getApplication(), uri)) {
                is BitmapPreprocessResult.Error -> {
                    _uiState.value = ResultUiState.Error(prepResult.message)
                }
                is BitmapPreprocessResult.Success -> {
                    try {
                        // 2. Run TFLite Model inference
                        val inferenceOutput = model.runInference(prepResult.inputBuffer)

                        // 3. Apply rejection logic classifier
                        val classifier = ChiliClassifier(
                            confidenceThreshold = confidenceThreshold.value,
                            temperature = temperature.value
                        )
                        val classificationResult = classifier.classify(inferenceOutput.probabilities)

                        _uiState.value = ResultUiState.Success(
                            bitmap = prepResult.bitmap,
                            result = classificationResult,
                            rawProbabilities = inferenceOutput.probabilities,
                            latencyMs = inferenceOutput.latencyMs
                        )
                    } catch (e: Exception) {
                        _uiState.value = ResultUiState.Error("Inference failed: ${e.message}")
                    }
                }
            }
        }
    }

    fun setConfidenceThreshold(threshold: Float) {
        confidenceThreshold.value = threshold
    }

    fun setTemperature(temp: Float) {
        temperature.value = temp
    }

    override fun onCleared() {
        super.onCleared()
        tfliteModel?.close()
    }
}
