package com.chilivision.app

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.chilivision.app.ui.camera.CameraScreen
import com.chilivision.app.ui.dev.DevSettingsScreen
import com.chilivision.app.ui.home.HomeScreen
import com.chilivision.app.ui.result.ResultScreen
import com.chilivision.app.ui.result.ResultViewModel
import com.chilivision.app.ui.theme.CapsibusTheme

class MainActivity : ComponentActivity() {

    private val resultViewModel: ResultViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CapsibusTheme {
                CapsibusAppNavigation(resultViewModel = resultViewModel)
            }
        }
    }
}

@Composable
fun CapsibusAppNavigation(resultViewModel: ResultViewModel) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onNavigateToCamera = { navController.navigate("camera") },
                onImageSelected = { uri ->
                    val encodedUri = Uri.encode(uri.toString())
                    navController.navigate("result/$encodedUri")
                },
                onNavigateToDevSettings = { navController.navigate("dev_settings") }
            )
        }

        composable("camera") {
            CameraScreen(
                onImageCaptured = { uri ->
                    val encodedUri = Uri.encode(uri.toString())
                    navController.navigate("result/$encodedUri") {
                        popUpTo("home")
                    }
                },
                onClose = { navController.popBackStack() }
            )
        }

        composable(
            route = "result/{imageUri}",
            arguments = listOf(navArgument("imageUri") { type = NavType.StringType })
        ) { backStackEntry ->
            // Navigation already decodes path arguments. Decoding again turns the
            // gallery's "image%3A123" into "image:123", which no longer matches
            // the URI the read grant was issued for.
            val uriString = backStackEntry.arguments?.getString("imageUri")
            val imageUri = Uri.parse(uriString)

            ResultScreen(
                imageUri = imageUri,
                viewModel = resultViewModel,
                onAnalyseAnother = {
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                }
            )
        }

        composable("dev_settings") {
            DevSettingsScreen(
                viewModel = resultViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
