package br.edu.ifpr.appaurico.ui.screens.evolution

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.edu.ifpr.appaurico.ui.components.AuricoCard
import br.edu.ifpr.appaurico.ui.components.EvolutionChart
import br.edu.ifpr.appaurico.ui.theme.AuricoDimens
import kotlin.math.abs

@Composable
fun EvolutionScreen(
    uiState: EvolutionUiState,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = AuricoDimens.ScreenPadding, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(AuricoDimens.BlockSpacing),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Evolução",
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = "Acompanhe os registros do sintoma ao longo deste ciclo.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (uiState.pontos.isEmpty()) {
            AuricoCard {
                Text(
                    text = "Seu histórico começa no primeiro registro",
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = "Depois de registrar a intensidade do sintoma, o AURICO organiza os dados aqui em uma linha de evolução.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@Column
        }

        AuricoCard(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Histórico do ciclo",
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = "${uiState.pontos.size} ${if (uiState.pontos.size == 1) "registro" else "registros"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            EvolutionChart(
                pontos = uiState.pontos,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Resumo(
                    rotulo = "Início",
                    valor = uiState.nivelInicial?.toString() ?: "—",
                    modifier = Modifier.weight(1f),
                )
                Resumo(
                    rotulo = "Agora",
                    valor = uiState.nivelAtual?.toString() ?: "—",
                    modifier = Modifier.weight(1f),
                )
                Resumo(
                    rotulo = "Variação",
                    valor = uiState.variacao?.let(::formatarVariacao) ?: "—",
                    modifier = Modifier.weight(1f),
                )
            }
        }

        AuricoCard {
            Text(
                text = "Leitura do histórico",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = descricaoVariacao(uiState.variacao),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "O gráfico organiza as percepções registradas e não representa diagnóstico ou avaliação clínica.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Resumo(
    rotulo: String,
    valor: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = valor,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = rotulo,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun formatarVariacao(variacao: Int): String =
    if (variacao > 0) "+$variacao" else variacao.toString()

private fun descricaoVariacao(variacao: Int?): String = when {
    variacao == null -> "Ainda não há registros suficientes para comparar o início e o momento atual."
    variacao < 0 -> "O último registro está ${abs(variacao)} ${if (abs(variacao) == 1) "ponto" else "pontos"} abaixo do primeiro registro."
    variacao > 0 -> "O último registro está $variacao ${if (variacao == 1) "ponto" else "pontos"} acima do primeiro registro."
    else -> "O último registro está no mesmo nível do primeiro registro."
}
