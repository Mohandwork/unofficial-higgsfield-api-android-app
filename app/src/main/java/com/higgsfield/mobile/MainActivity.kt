package com.higgsfield.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.higgsfield.mobile.ui.navigation.HiggsfieldApp
import com.higgsfield.mobile.ui.theme.AppViewModel
import com.higgsfield.mobile.ui.theme.HiggsfieldTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val appViewModel: AppViewModel = hiltViewModel()
            val theme = appViewModel.theme.collectAsStateWithLifecycle().value
            HiggsfieldTheme(themePreference = theme) {
                HiggsfieldApp()
            }
        }
    }
}
