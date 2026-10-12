package com.chilivision.app.ml

import android.content.Context
import android.content.res.AssetFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

class TfLiteChiliModel(context: Context) : AutoCloseable {

    private val interpreter: Interpreter
    private val mutex = Mutex()
    val labels: List<String>

    init {
        // 1. Verify labels.txt
        labels = context.assets.open("labels.txt").bufferedReader().useLines { lines ->
            lines.map { it.trim() }.filter { it.isNotEmpty() }.toList()
        }

        val expectedClasses = ChiliClass.values().map { it.label }
        if (labels != expectedClasses) {
            throw IllegalStateException(
                "labels.txt mismatch! Expected $expectedClasses but got $labels"
            )
        }

        // 2. Load TFLite Model mapped byte buffer
        val modelBuffer = loadModelFile(context, MODEL_FILE_NAME)
        val options = Interpreter.Options().apply {
            setNumThreads(4)
        }
        interpreter = Interpreter(modelBuffer, options)

        // 3. Tensor shape verification
        val inputTensor = interpreter.getInputTensor(0)
        val outputTensor = interpreter.getOutputTensor(0)

        check(inputTensor.shape().contentEquals(intArrayOf(1, 299, 299, 3))) {
            "Unexpected input shape: ${inputTensor.shape().contentToString()}"
        }
        check(outputTensor.shape().contentEquals(intArrayOf(1, 6))) {
            "Unexpected output shape: ${outputTensor.shape().contentToString()}"
        }
    }

    suspend fun runInference(inputBuffer: ByteBuffer): InferenceOutput = withContext(Dispatchers.Default) {
        mutex.withLock {
            val outputArray = Array(1) { FloatArray(6) }
            val startTime = System.currentTimeMillis()
            interpreter.run(inputBuffer, outputArray)
            val latencyMs = System.currentTimeMillis() - startTime
            InferenceOutput(probabilities = outputArray[0], latencyMs = latencyMs)
        }
    }

    override fun close() {
        interpreter.close()
    }

    private fun loadModelFile(context: Context, filename: String): ByteBuffer {
        val fileDescriptor: AssetFileDescriptor = context.assets.openFd(filename)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel: FileChannel = inputStream.channel
        val startOffset: Long = fileDescriptor.startOffset
        val declaredLength: Long = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    companion object {
        const val MODEL_FILE_NAME = "chilivision_inceptionv3.tflite"
    }
}

data class InferenceOutput(
    val probabilities: FloatArray,
    val latencyMs: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as InferenceOutput
        return probabilities.contentEquals(other.probabilities) && latencyMs == other.latencyMs
    }

    override fun hashCode(): Int {
        var result = probabilities.contentHashCode()
        result = 31 * result + latencyMs.hashCode()
        return result
    }
}
