package com.chilivision.app.ui.result

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chilivision.app.R
import com.chilivision.app.ml.ChiliClass
import com.chilivision.app.ml.ClassificationResult
import com.chilivision.app.ui.theme.*

@Composable
fun ResultScreen(
    imageUri: Uri,
    viewModel: ResultViewModel,
    onAnalyseAnother: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(imageUri) {
        viewModel.analyzeUri(imageUri)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = SurfaceBg
    ) {
        when (val state = uiState) {
            is ResultUiState.Loading, ResultUiState.Idle -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = AccentRed)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "ANALYSING LEAF...",
                            style = Typography.labelSmall.copy(color = OnSurfaceMuted)
                        )
                    }
                }
            }
            is ResultUiState.Error -> {
                ErrorResultContent(
                    errorMessage = state.message,
                    onAnalyseAnother = onAnalyseAnother
                )
            }
            is ResultUiState.Success -> {
                SuccessResultContent(
                    bitmap = state.bitmap,
                    result = state.result,
                    onAnalyseAnother = onAnalyseAnother
                )
            }
        }
    }
}

@Composable
private fun SuccessResultContent(
    bitmap: Bitmap,
    result: ClassificationResult,
    onAnalyseAnother: () -> Unit
) {
    var expandedDetails by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Submitted Image Preview (16:9 crop)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(4.dp))
                .background(SurfaceRaised)
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Submitted Leaf Image",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // 2. Verdict Block Anchor
        VerdictBlock(result = result)

        // 3. Recommended Action Text Card
        ActionCard(result = result)

        // 4. Detailed Probabilities Section (Collapsible)
        val probabilities = getProbabilities(result)
        if (probabilities != null) {
            ProbabilitiesSection(
                probabilities = probabilities,
                isExpanded = expandedDetails,
                onToggleExpand = { expandedDetails = !expandedDetails }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 5. Permanent Agronomic Disclaimer Footer
        Divider(color = BorderHairline, thickness = 1.dp)
        Text(
            text = stringResource(R.string.disclaimer_footer),
            style = Typography.bodyMedium.copy(color = OnSurfaceMuted)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 6. Analyse Another Action Button
        OutlinedButton(
            onClick = onAnalyseAnother,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(4.dp),
            border = BorderStroke(1.dp, BorderHairline),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = OnSurfacePrimary)
        ) {
            Text(
                text = stringResource(R.string.btn_analyse_another),
                style = Typography.titleMedium.copy(color = OnSurfacePrimary)
            )
        }
    }
}

@Composable
private fun VerdictBlock(result: ClassificationResult) {
    val (verdictTitle, confidenceText, statusColor) = when (result) {
        is ClassificationResult.Diagnosed -> {
            val title = when (result.chiliClass) {
                ChiliClass.BACTERIAL -> stringResource(R.string.verdict_bacterial)
                ChiliClass.FUNGAL -> stringResource(R.string.verdict_fungal)
                ChiliClass.HEALTHY -> stringResource(R.string.verdict_healthy)
                ChiliClass.NON_CHILI -> stringResource(R.string.verdict_non_chili)
                ChiliClass.PESTS -> stringResource(R.string.verdict_pests)
                ChiliClass.VIRUS -> stringResource(R.string.verdict_virus)
            }
            val color = if (result.chiliClass == ChiliClass.HEALTHY) StatusHealthy else StatusDisease
            Triple(title, "%.1f%%".format(result.confidence * 100), color)
        }
        is ClassificationResult.Uncertain -> {
            Triple(
                stringResource(R.string.verdict_uncertain),
                "%.1f%%".format(result.topConfidence * 100),
                StatusUncertain
            )
        }
        is ClassificationResult.NotChili -> {
            Triple(
                stringResource(R.string.verdict_non_chili),
                "%.1f%%".format(result.confidence * 100),
                StatusUncertain
            )
        }
        is ClassificationResult.InvalidOutput -> {
            Triple(stringResource(R.string.verdict_unreadable), "N/A", StatusError)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceRaised, RoundedCornerShape(4.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 4dp status bar on left edge
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(64.dp)
                .background(statusColor, RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = verdictTitle,
                style = Typography.displayLarge.copy(fontSize = 32.sp, lineHeight = 36.sp),
                color = OnSurfacePrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.confidence_label),
                    style = Typography.labelSmall.copy(color = OnSurfaceMuted)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = confidenceText,
                    style = Typography.titleLarge.copy(color = statusColor)
                )
            }
        }
    }
}

@Composable
private fun ActionCard(result: ClassificationResult) {
    val actionText = when (result) {
        is ClassificationResult.Diagnosed -> {
            when (result.chiliClass) {
                ChiliClass.BACTERIAL -> stringResource(R.string.action_bacterial)
                ChiliClass.FUNGAL -> stringResource(R.string.action_fungal)
                ChiliClass.HEALTHY -> stringResource(R.string.action_healthy)
                ChiliClass.NON_CHILI -> stringResource(R.string.action_non_chili)
                ChiliClass.PESTS -> stringResource(R.string.action_pests)
                ChiliClass.VIRUS -> stringResource(R.string.action_virus)
            }
        }
        is ClassificationResult.Uncertain -> stringResource(R.string.action_uncertain)
        is ClassificationResult.NotChili -> stringResource(R.string.action_non_chili)
        is ClassificationResult.InvalidOutput -> stringResource(R.string.action_unreadable)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceRaised),
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = actionText,
            style = Typography.bodyLarge.copy(color = OnSurfacePrimary),
            modifier = Modifier.padding(20.dp)
        )
    }
}

@Composable
private fun ProbabilitiesSection(
    probabilities: FloatArray,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit
) {
    val classLabels = ChiliClass.values().map { it.label }
    val classProbs = classLabels.zip(probabilities.toList()).sortedByDescending { it.second }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceRaised, RoundedCornerShape(4.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleExpand() },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.detailed_probabilities),
                style = Typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold, color = OnSurfacePrimary)
            )
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = OnSurfaceMuted
            )
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(modifier = Modifier.padding(top = 16.dp)) {
                classProbs.forEach { (label, prob) ->
                    Column(modifier = Modifier.padding(vertical = 6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = label, style = Typography.bodyLarge.copy(color = OnSurfacePrimary))
                            Text(
                                text = "%.1f%%".format(prob * 100),
                                style = Typography.bodyMedium.copy(color = OnSurfaceMuted)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = prob.coerceIn(0f, 1f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp),
                            color = AccentRed,
                            trackColor = BorderHairline
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorResultContent(
    errorMessage: String,
    onAnalyseAnother: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.verdict_unreadable),
            style = Typography.displayLarge.copy(color = StatusError)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = errorMessage,
            style = Typography.bodyLarge.copy(color = OnSurfaceMuted)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onAnalyseAnother,
            colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
            shape = RoundedCornerShape(4.dp)
        ) {
            Text(stringResource(R.string.btn_analyse_another))
        }
    }
}

private fun getProbabilities(result: ClassificationResult): FloatArray? {
    return when (result) {
        is ClassificationResult.Diagnosed -> result.probabilities
        is ClassificationResult.Uncertain -> result.probabilities
        is ClassificationResult.NotChili -> result.probabilities
        is ClassificationResult.InvalidOutput -> null
    }
}
