package br.edu.ifpr.appaurico

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import br.edu.ifpr.appaurico.ui.navigation.AuricoNavHost
import br.edu.ifpr.appaurico.ui.theme.AppauricoTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // Rota pedida por uma notificacao; consumida pelo NavHost ao navegar.
    private val rotaInicial = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        rotaInicial.value = intent?.getStringExtra(EXTRA_ROTA)
        enableEdgeToEdge()
        setContent {
            AppauricoTheme {
                AuricoNavHost(
                    rotaInicial = rotaInicial.value,
                    onRotaConsumida = { rotaInicial.value = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        rotaInicial.value = intent.getStringExtra(EXTRA_ROTA)
    }

    companion object {
        const val EXTRA_ROTA = "rota_inicial"
    }
}
