package com.chilivision.app.ui.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chilivision.app.BuildConfig
import com.chilivision.app.R
import com.chilivision.app.ui.theme.*

@Composable
fun HomeScreen(
    onNavigateToCamera: () -> Unit,
    onImageSelected: (Uri) -> Unit,
    onNavigateToDevSettings: () -> Unit
) {
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            onImageSelected(uri)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = SurfaceBg
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Text(
                    text = stringResource(R.string.app_name).uppercase(),
                    style = Typography.titleMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 3.sp,
                        color = AccentRed
                    ),
                    modifier = Modifier.pointerInput(Unit) {
                        detectTapGestures(
                            onLongPress = {
                                if (BuildConfig.DEBUG) {
                                    onNavigateToDevSettings()
                                }
                            }
                        )
                    }
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.subtitle),
                    style = Typography.bodyMedium.copy(
                        color = OnSurfaceMuted
                    )
                )
            }

            // Middle Empty Upper Third & Buttons Section
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Primary: Take Photo
                Button(
                    onClick = onNavigateToCamera,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    shape = RoundedCornerShape(4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentRed,
                        contentColor = OnSurfacePrimary
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            modifier = Modifier.padding(start = 8.dp, end = 16.dp)
                        )
                        Text(
                            text = stringResource(R.string.btn_take_photo),
                            style = Typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = OnSurfacePrimary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Secondary: Choose from Gallery
                OutlinedButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(1.dp, BorderHairline),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = OnSurfacePrimary
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = null,
                            tint = OnSurfaceMuted,
                            modifier = Modifier.padding(start = 8.dp, end = 16.dp)
                        )
                        Text(
                            text = stringResource(R.string.btn_choose_gallery),
                            style = Typography.bodyLarge.copy(
                                color = OnSurfacePrimary
                            )
                        )
                    }
                }
            }

            // Bottom Anchored Spec Footer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.model_spec_footer),
                    style = Typography.labelSmall.copy(
                        color = OnSurfaceMuted
                    )
                )
            }
        }
    }
}
