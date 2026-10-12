package com.chilivision.app.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

object ImagePreprocessor {
    const val INPUT_SIZE = 299
    private const val BATCH_SIZE = 1
    private const val CHANNELS = 3
    private const val BYTES_PER_CHANNEL = 4 // Float32

    fun preprocessUri(context: Context, uri: Uri): BitmapPreprocessResult {
        var inputStream: InputStream? = null
        try {
            // 1. First pass: Check dimensions to avoid OOM
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            inputStream = context.contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            // Calculate sample size for large images
            options.inSampleSize = calculateInSampleSize(options, 1024, 1024)
            options.inJustDecodeBounds = false
            options.inPreferredColorSpace = android.graphics.ColorSpace.get(android.graphics.ColorSpace.Named.SRGB)

            // 2. Decode bitmap
            inputStream = context.contentResolver.openInputStream(uri)
            var bitmap = BitmapFactory.decodeStream(inputStream, null, options)
                ?: return BitmapPreprocessResult.Error("Could not decode image from URI")
            inputStream?.close()

            // 3. EXIF Rotation Correction
            val orientation = getExifOrientation(context, uri)
            bitmap = rotateBitmapIfNeeded(bitmap, orientation)

            // 4. Preprocess bitmap to ByteBuffer
            val byteBuffer = preprocessBitmap(bitmap)
            return BitmapPreprocessResult.Success(bitmap = bitmap, inputBuffer = byteBuffer)
        } catch (e: Exception) {
            return BitmapPreprocessResult.Error("Preprocessing failed: ${e.message}")
        } finally {
            inputStream?.close()
        }
    }

    fun preprocessBitmap(bitmap: Bitmap): ByteBuffer {
        // Rescale to 299x299 bilinear (matching training image_dataset_from_directory squash)
        val scaledBitmap = if (bitmap.width == INPUT_SIZE && bitmap.height == INPUT_SIZE) {
            bitmap
        } else {
            Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true)
        }

        val buffer = ByteBuffer.allocateDirect(BATCH_SIZE * INPUT_SIZE * INPUT_SIZE * CHANNELS * BYTES_PER_CHANNEL)
        buffer.order(ByteOrder.nativeOrder())
        buffer.rewind()

        val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
        scaledBitmap.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)

        // Raw RGB in [0.0, 255.0] - NO division by 255
        for (pixel in pixels) {
            val r = (pixel shr 16 and 0xFF).toFloat()
            val g = (pixel shr 8 and 0xFF).toFloat()
            val b = (pixel and 0xFF).toFloat()

            buffer.putFloat(r)
            buffer.putFloat(g)
            buffer.putFloat(b)
        }

        buffer.rewind()
        return buffer
    }

    private fun getExifOrientation(context: Context, uri: Uri): Int {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                ExifInterface(stream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            } ?: ExifInterface.ORIENTATION_NORMAL
        } catch (e: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }
    }

    private fun rotateBitmapIfNeeded(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            else -> return bitmap
        }
        return try {
            val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            if (rotated != bitmap) bitmap.recycle()
            rotated
        } catch (e: OutOfMemoryError) {
            bitmap
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}

sealed interface BitmapPreprocessResult {
    data class Success(val bitmap: Bitmap, val inputBuffer: ByteBuffer) : BitmapPreprocessResult
    data class Error(val message: String) : BitmapPreprocessResult
}
