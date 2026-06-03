package dev.huidou.util

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.huidou.util.ui.DatabaseManagementScreen
import dev.huidou.util.ui.theme.DtoolTheme
import dev.huidou.util.ui.theme.ThemeMode
import dev.huidou.util.ui.theme.ThemeViewModel
import dev.huidou.util.ui.theme.isDarkTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeViewModel: ThemeViewModel = viewModel()
            val darkTheme by themeViewModel.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            val isDark = when (darkTheme) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> themeViewModel.isDarkTheme()
            }
            DtoolTheme(darkTheme = isDark) {
                DatabaseManagementScreen(
                    themeViewModel = themeViewModel
                )
            }
        }
    }
}