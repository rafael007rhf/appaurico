package br.edu.ifpr.appaurico

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import br.edu.ifpr.appaurico.ui.navigation.AuricoNavHost
import br.edu.ifpr.appaurico.ui.theme.AppauricoTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // Rota pedida por uma notificacao; consumida pelo NavHost ao navegar.
    private val rotaInicial = mutableStateOf<String?>(null)

    private val solicitarPermissaoNotificacoes =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        rotaInicial.value = intent?.getStringExtra(EXTRA_ROTA)
        solicitarPermissaoNotificacoesSeNecessario()
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

    private fun solicitarPermissaoNotificacoesSeNecessario() {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            solicitarPermissaoNotificacoes.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    companion object {
        const val EXTRA_ROTA = "rota_inicial"
    }
}
