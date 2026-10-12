package com.chilivision.app.ml

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.nio.ByteBuffer
import java.nio.ByteOrder

@RunWith(AndroidJUnit4::class)
class ModelIntegrationTest {

    @Test
    fun testModelAssetInitializationAndTensorIntrospection() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val model = TfLiteChiliModel(appContext)

        assertNotNull("Model instance should not be null", model)
        assertEquals("Labels count should be 6", 6, model.labels.size)
        assertEquals("Bacterial", model.labels[0])
        assertEquals("Fungal", model.labels[1])
        assertEquals("Healthy", model.labels[2])
        assertEquals("NonChili", model.labels[3])
        assertEquals("Pests", model.labels[4])
        assertEquals("Virus", model.labels[5])

        model.close()
    }

    @Test
    fun testSyntheticBufferInferenceExecution() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val model = TfLiteChiliModel(appContext)

        val buffer = ByteBuffer.allocateDirect(1 * 299 * 299 * 3 * 4).order(ByteOrder.nativeOrder())
        buffer.rewind()
        // Fill synthetic constant Float32 pixels
        for (i in 0 until 299 * 299 * 3) {
            buffer.putFloat(127.5f)
        }
        buffer.rewind()

        kotlinx.coroutines.runBlocking {
            val output = model.runInference(buffer)
            assertNotNull(output.probabilities)
            assertEquals(6, output.probabilities.size)
            assertTrue("Inference latency should be positive", output.latencyMs >= 0)
        }

        model.close()
    }
}
