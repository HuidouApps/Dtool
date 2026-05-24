package dev.huidou.db

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.huidou.db.ui.DatabaseAppScreen
import dev.huidou.db.ui.theme.DtoolTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DtoolTheme {
                DatabaseAppScreen()
            }
        }
    }
}