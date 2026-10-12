package com.chilivision.app.ui.dev

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chilivision.app.R
import com.chilivision.app.ml.ChiliClassifier
import com.chilivision.app.ui.result.ResultUiState
import com.chilivision.app.ui.result.ResultViewModel
import com.chilivision.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevSettingsScreen(
    viewModel: ResultViewModel,
    onNavigateBack: () -> Unit
) {
    val currentThreshold by viewModel.confidenceThreshold.collectAsState()
    val currentTemperature by viewModel.temperature.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "DEV SETTINGS",
                        style = Typography.titleMedium.copy(fontFamily = FontFamily.Monospace, color = OnSurfacePrimary)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = OnSurfacePrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceBg)
            )
        },
        containerColor = SurfaceBg
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Section 1: Confidence Threshold (τ)
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceRaised),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "CONFIDENCE THRESHOLD (τ)",
                        style = Typography.labelSmall.copy(color = OnSurfaceMuted)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "τ = %.4f".format(currentThreshold),
                        style = Typography.titleLarge.copy(color = AccentRed)
                    )
                    Slider(
                        value = currentThreshold,
                        onValueChange = { viewModel.setConfidenceThreshold(it) },
                        valueRange = 0.50f..0.999f,
                        colors = SliderDefaults.colors(
                            thumbColor = AccentRed,
                            activeTrackColor = AccentRed,
                            inactiveTrackColor = BorderHairline
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Presets:",
                        style = Typography.bodyMedium.copy(color = OnSurfaceMuted)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val presets = listOf(0.70f, 0.8467f, 0.8788f, 0.9929f)
                        presets.forEach { preset ->
                            OutlinedButton(
                                onClick = { viewModel.setConfidenceThreshold(preset) },
                                shape = RoundedCornerShape(4.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (currentThreshold == preset) AccentRed else BorderHairline
                                ),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "%.2f".format(preset),
                                    style = Typography.labelSmall.copy(fontSize = 10.sp),
                                    color = if (currentThreshold == preset) AccentRed else OnSurfacePrimary
                                )
                            }
                        }
                    }
                }
            }

            // Section 2: Temperature Scaling (T)
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceRaised),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "TEMPERATURE SCALING (T)",
                        style = Typography.labelSmall.copy(color = OnSurfaceMuted)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (currentTemperature == 1.0f) "T = 1.0 (Identity)" else "T = 1.220418 (Calibrated)",
                            style = Typography.bodyLarge.copy(color = OnSurfacePrimary)
                        )
                        Switch(
                            checked = currentTemperature != 1.0f,
                            onCheckedChange = { isCalibrated ->
                                viewModel.setTemperature(if (isCalibrated) 1.220418f else 1.0f)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = AccentRed,
                                checkedTrackColor = SurfaceRaised,
                                uncheckedThumbColor = OnSurfaceMuted,
                                uncheckedTrackColor = BorderHairline
                            )
                        )
                    }
                }
            }

            // Section 3: Last Inference Vector & Latency
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceRaised),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "LAST INFERENCE METRICS",
                        style = Typography.labelSmall.copy(color = OnSurfaceMuted)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    when (val state = uiState) {
                        is ResultUiState.Success -> {
                            Text(
                                text = "Latency: ${state.latencyMs} ms",
                                style = Typography.titleLarge.copy(color = StatusHealthy)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Raw Softmax Vector:",
                                style = Typography.bodyMedium.copy(color = OnSurfaceMuted)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            state.rawProbabilities.forEachIndexed { idx, prob ->
                                Text(
                                    text = "[Class $idx]: %.8f".format(prob),
                                    style = Typography.bodyMedium.copy(color = OnSurfacePrimary)
                                )
                            }
                        }
                        else -> {
                            Text(
                                text = "No inference run in this session.",
                                style = Typography.bodyLarge.copy(color = OnSurfaceMuted)
                            )
                        }
                    }
                }
            }
        }
    }
}
