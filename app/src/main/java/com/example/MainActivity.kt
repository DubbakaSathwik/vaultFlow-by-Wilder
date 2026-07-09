package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.presentation.navigation.NavGraph
import com.example.presentation.viewmodel.MainViewModel
import com.example.presentation.viewmodel.SettingsViewModel
import com.example.ui.theme.VaultFlowTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Access our App Container DI setup
        val appContainer = (application as VaultFlowApplication).container

        // Check for shared image intent (Section 1 share sheet integration)
        var sharedImageUri: Uri? = null
        val intentAction = intent?.action
        val intentType = intent?.type
        if (Intent.ACTION_SEND == intentAction && intentType != null) {
            if (intentType.startsWith("image/")) {
                @Suppress("DEPRECATION")
                sharedImageUri = intent.getParcelableExtra(Intent.EXTRA_STREAM) as? Uri
            }
        }

        setContent {
            // Provide our ViewModels with their required dependencies manually
            val mainViewModel: MainViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return MainViewModel(appContainer.settingsManager) as T
                    }
                }
            )

            val settingsViewModel: SettingsViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return SettingsViewModel(appContainer.settingsManager) as T
                    }
                }
            )

            val themeMode by mainViewModel.themeMode.collectAsState()
            val useDynamicColor by mainViewModel.useDynamicColor.collectAsState()

            VaultFlowTheme(
                themeMode = themeMode,
                useDynamicColor = useDynamicColor
            ) {
                NavGraph(
                    mainViewModel = mainViewModel,
                    settingsViewModel = settingsViewModel,
                    sharedImageUri = sharedImageUri,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
