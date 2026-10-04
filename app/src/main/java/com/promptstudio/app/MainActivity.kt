package com.promptstudio.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.promptstudio.app.core.network.RequestStatusPoller
import com.promptstudio.app.ui.navigation.PromptStudioApp
import com.promptstudio.app.ui.theme.AppViewModel
import com.promptstudio.app.ui.theme.PromptStudioTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var statusPoller: RequestStatusPoller

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        lifecycleScope.launch { statusPoller.pollAcceptedRequests() }
        setContent {
            val appViewModel: AppViewModel = hiltViewModel()
            val theme = appViewModel.theme.collectAsStateWithLifecycle().value
            PromptStudioTheme(themePreference = theme) {
                PromptStudioApp()
            }
        }
    }
}
