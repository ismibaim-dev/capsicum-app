package com.chilivision.app.ui.camera

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.chilivision.app.R
import com.chilivision.app.ui.theme.*
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import java.io.File
import java.util.concurrent.Executor

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(
    onImageCaptured: (Uri) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val permissionState = rememberPermissionState(permission = Manifest.permission.CAMERA)

    LaunchedEffect(Unit) {
        if (!permissionState.status.isGranted) {
            permissionState.launchPermissionRequest()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = SurfaceBg
    ) {
        when {
            permissionState.status.isGranted -> {
                CameraPreviewContent(
                    context = context,
                    onImageCaptured = onImageCaptured,
                    onClose = onClose
                )
            }
            permissionState.status.shouldShowRationale -> {
                PermissionRationaleCard(
                    title = stringResource(R.string.permission_denied_title),
                    description = stringResource(R.string.permission_denied_desc),
                    buttonText = stringResource(R.string.btn_retry),
                    onButtonClick = { permissionState.launchPermissionRequest() },
                    onClose = onClose
                )
            }
            else -> {
                PermissionRationaleCard(
                    title = stringResource(R.string.permission_denied_title),
                    description = stringResource(R.string.permission_denied_desc),
                    buttonText = stringResource(R.string.btn_open_settings),
                    onButtonClick = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    },
                    onClose = onClose
                )
            }
        }
    }
}

@Composable
private fun CameraPreviewContent(
    context: Context,
    onImageCaptured: (Uri) -> Unit,
    onClose: () -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var isCapturing by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        // CameraX Preview
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val capture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build()

                    imageCapture = capture

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            capture
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            }
        )

        // Corner Ticks Framing Guide Overlay
        FramingGuideOverlay()

        // Close Button top-left
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .statusBarsPadding()
                .padding(16.dp)
                .align(Alignment.TopStart)
                .background(SurfaceRaised.copy(alpha = 0.8f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = OnSurfacePrimary
            )
        }

        // Hint Pill below framing guide
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 72.dp, start = 24.dp, end = 24.dp)
                .background(SurfaceRaised.copy(alpha = 0.85f), RoundedCornerShape(20.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = stringResource(R.string.camera_framing_hint),
                style = Typography.bodyMedium.copy(color = OnSurfacePrimary),
                color = OnSurfacePrimary
            )
        }

        // Shutter Button bottom-centre
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 32.dp)
        ) {
            IconButton(
                onClick = {
                    if (!isCapturing && imageCapture != null) {
                        isCapturing = true
                        takePhoto(
                            context = context,
                            imageCapture = imageCapture!!,
                            onImageCaptured = { uri ->
                                isCapturing = false
                                onImageCaptured(uri)
                            },
                            onError = { isCapturing = false }
                        )
                    }
                },
                enabled = !isCapturing,
                modifier = Modifier
                    .size(72.dp)
                    .border(4.dp, AccentRed, CircleShape)
                    .background(SurfaceRaised.copy(alpha = 0.5f), CircleShape)
                    .padding(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(AccentRed, CircleShape)
                )
            }
        }

        if (isCapturing) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter),
                color = AccentRed,
                trackColor = Color.Transparent
            )
        }
    }
}

@Composable
private fun FramingGuideOverlay() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val inset = width * 0.12f
        val rectSize = width - (2 * inset)
        val left = inset
        val top = (height - rectSize) / 2
        val right = left + rectSize
        val bottom = top + rectSize
        val tickLength = 24.dp.toPx()
        val strokeWidth = 2.dp.toPx()

        val tickColor = AccentRed

        // Top-Left corner
        drawLine(tickColor, Offset(left, top), Offset(left + tickLength, top), strokeWidth)
        drawLine(tickColor, Offset(left, top), Offset(left, top + tickLength), strokeWidth)

        // Top-Right corner
        drawLine(tickColor, Offset(right, top), Offset(right - tickLength, top), strokeWidth)
        drawLine(tickColor, Offset(right, top), Offset(right, top + tickLength), strokeWidth)

        // Bottom-Left corner
        drawLine(tickColor, Offset(left, bottom), Offset(left + tickLength, bottom), strokeWidth)
        drawLine(tickColor, Offset(left, bottom), Offset(left, bottom - tickLength), strokeWidth)

        // Bottom-Right corner
        drawLine(tickColor, Offset(right, bottom), Offset(right - tickLength, bottom), strokeWidth)
        drawLine(tickColor, Offset(right, bottom), Offset(right, bottom - tickLength), strokeWidth)
    }
}

@Composable
private fun PermissionRationaleCard(
    title: String,
    description: String,
    buttonText: String,
    onButtonClick: () -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceRaised),
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = title,
                    style = Typography.titleMedium.copy(color = OnSurfacePrimary)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = description,
                    style = Typography.bodyLarge.copy(color = OnSurfaceMuted)
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onButtonClick,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(buttonText, style = Typography.titleMedium)
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onClose) {
            Text(stringResource(R.string.btn_choose_gallery), color = OnSurfaceMuted)
        }
    }
}

private fun takePhoto(
    context: Context,
    imageCapture: ImageCapture,
    onImageCaptured: (Uri) -> Unit,
    onError: () -> Unit
) {
    val photoFile = File(context.cacheDir, "capsibus_capture_${System.currentTimeMillis()}.jpg")
    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
    val executor: Executor = ContextCompat.getMainExecutor(context)

    imageCapture.takePicture(
        outputOptions,
        executor,
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                val savedUri = Uri.fromFile(photoFile)
                onImageCaptured(savedUri)
            }

            override fun onError(exception: ImageCaptureException) {
                exception.printStackTrace()
                onError()
            }
        }
    )
}
