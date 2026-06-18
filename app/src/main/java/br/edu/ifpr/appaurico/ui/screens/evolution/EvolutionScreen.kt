package br.edu.ifpr.appaurico.ui.screens.evolution

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import br.edu.ifpr.appaurico.ui.components.EvolutionChart

@Composable
fun EvolutionScreen(
    uiState: EvolutionUiState,
) {
    if (uiState.pontos.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Faça seu primeiro registro",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
        }
        return
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Text(
            text = "Evolução do sintoma",
            style = MaterialTheme.typography.headlineSmall,
        )

        EvolutionChart(
            pontos = uiState.pontos,
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Resumo("Início", uiState.nivelInicial?.toString() ?: "-")
            Resumo("Hoje", uiState.nivelAtual?.toString() ?: "-")
            Resumo("Variação", uiState.variacao?.let { formatarVariacao(it) } ?: "-")
        }
    }
}

@Composable
private fun Resumo(rotulo: String, valor: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = valor, style = MaterialTheme.typography.headlineSmall)
        Text(text = rotulo, style = MaterialTheme.typography.bodyMedium)
    }
}

/** Mostra o sinal explicito da variacao (ex.: +2, -3, 0). */
private fun formatarVariacao(variacao: Int): String =
    if (variacao > 0) "+$variacao" else variacao.toString()
