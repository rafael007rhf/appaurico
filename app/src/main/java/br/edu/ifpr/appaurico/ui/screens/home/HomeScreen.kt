package br.edu.ifpr.appaurico.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onEstimularAgora: () -> Unit,
    onRegistrar: () -> Unit,
    onVisaoProfissional: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ProximaEstimulacaoCard(
            momento = uiState.proximaEstimulacao,
            onEstimularAgora = onEstimularAgora,
        )

        CicloCard(uiState = uiState)

        Button(
            onClick = onRegistrar,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Registrar como me sinto agora")
        }

        TextButton(
            onClick = onVisaoProfissional,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Visão do profissional")
        }

        // Controles de feira: presentes apenas no build de debug (no-op em release).
        DemoControls(modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun ProximaEstimulacaoCard(
    momento: String,
    onEstimularAgora: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Próxima estimulação",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = momento.ifEmpty { "—" },
                style = MaterialTheme.typography.headlineSmall,
            )
            Button(
                onClick = onEstimularAgora,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Estimular agora")
            }
        }
    }
}

@Composable
private fun CicloCard(uiState: HomeUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Ciclo atual",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "Dia ${uiState.diaCiclo} de ${uiState.duracaoCiclo}",
                style = MaterialTheme.typography.bodyLarge,
            )

            Text(
                text = "Adesão: ${uiState.adesaoPercentual}% " +
                    "(${uiState.estimulacoesFeitas} de ${uiState.estimulacoesPrevistas})",
                style = MaterialTheme.typography.bodyMedium,
            )
            LinearProgressIndicator(
                progress = { uiState.adesaoPercentual / 100f },
                modifier = Modifier.fillMaxWidth(),
            )

            val ultimo = uiState.ultimoRegistroNivel
            if (ultimo != null) {
                Text(
                    text = "Último registro: nível $ultimo · ${uiState.ultimoRegistroQuando}",
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                Text(
                    text = "Faça seu primeiro registro do sintoma.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
