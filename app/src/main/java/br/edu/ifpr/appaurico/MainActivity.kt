package br.edu.ifpr.appaurico

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import br.edu.ifpr.appaurico.ui.navigation.SementeNavHost
import br.edu.ifpr.appaurico.ui.theme.AppauricoTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppauricoTheme {
                SementeNavHost()
            }
        }
    }
}
