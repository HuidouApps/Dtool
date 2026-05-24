package dev.huidou.util

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.huidou.util.ui.DatabaseManagementScreen
import dev.huidou.util.ui.theme.DtoolTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DtoolTheme {
                DatabaseManagementScreen()
            }
        }
    }
}