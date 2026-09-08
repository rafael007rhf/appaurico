package br.edu.ifpr.appaurico.ui.screens.professional

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.edu.ifpr.appaurico.ui.components.AuricoCard
import br.edu.ifpr.appaurico.ui.components.EvolutionChart
import br.edu.ifpr.appaurico.ui.theme.AuricoDimens

@Composable
fun ProfessionalScreen(
    uiState: ProfessionalUiState,
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = AuricoDimens.ScreenPadding, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(AuricoDimens.BlockSpacing),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(
                text = "Resumo do acompanhamento",
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = "Visão do profissional · ${uiState.nomePaciente}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        ResumoGeralCard(uiState)
        TendenciaCard(uiState)
        RegistrosRecentesCard(uiState)

        Button(
            onClick = { compartilharRelatorio(context, uiState.relatorio) },
            shape = RoundedCornerShape(AuricoDimens.CornerRadius),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Compartilhar relatório")
        }

        AuricoCard {
            Text(
                text = "Sobre estes dados",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "O protótipo organiza registros feitos pelo próprio paciente para apoiar a conversa no retorno. Não realiza diagnóstico nem define conduta clínica.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ResumoGeralCard(uiState: ProfessionalUiState) {
    AuricoCard(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "Visão geral",
            style = MaterialTheme.typography.titleLarge,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Metrica(
                valor = "${uiState.adesaoPercentual}%",
                rotulo = "adesão",
                modifier = Modifier.weight(1f),
            )
            Metrica(
                valor = "${uiState.diaCiclo}/${uiState.duracaoCiclo}",
                rotulo = "dia do ciclo",
                modifier = Modifier.weight(1f),
            )
            Metrica(
                valor = uiState.nivelAtual?.toString() ?: "—",
                rotulo = "nível atual",
                modifier = Modifier.weight(1f),
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Estimulações registradas",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "${uiState.estimulacoesFeitas}/${uiState.estimulacoesPrevistas}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            LinearProgressIndicator(
                progress = { uiState.adesaoPercentual / 100f },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun TendenciaCard(uiState: ProfessionalUiState) {
    AuricoCard(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Evolução do sintoma",
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = if (uiState.pontos.isEmpty()) {
                    "Ainda não há registros suficientes para visualizar o histórico."
                } else {
                    "Histórico construído a partir dos registros do paciente."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (uiState.pontos.isNotEmpty()) {
            EvolutionChart(
                pontos = uiState.pontos,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Metrica(
                    valor = uiState.nivelInicial?.toString() ?: "—",
                    rotulo = "início",
                    modifier = Modifier.weight(1f),
                )
                Metrica(
                    valor = uiState.nivelAtual?.toString() ?: "—",
                    rotulo = "agora",
                    modifier = Modifier.weight(1f),
                )
                Metrica(
                    valor = uiState.variacao?.let(::formatarSinal) ?: "—",
                    rotulo = "variação",
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun RegistrosRecentesCard(uiState: ProfessionalUiState) {
    AuricoCard(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        Column(
            modifier = Modifier.padding(bottom = if (uiState.registrosRecentes.isEmpty()) 0.dp else 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "Registros recentes",
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = if (uiState.registrosRecentes.isEmpty()) {
                    "Nenhum registro realizado ainda."
                } else {
                    "Últimas percepções registradas pelo paciente."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        uiState.registrosRecentes.forEachIndexed { indice, registro ->
            if (indice > 0) HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
            RegistroLinha(registro)
        }
    }
}

@Composable
private fun RegistroLinha(registro: RegistroResumo) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = registro.quando,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            if (!registro.nota.isNullOrBlank()) {
                Text(
                    text = registro.nota,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            text = "Nível ${registro.nivel}",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun Metrica(
    valor: String,
    rotulo: String,
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

private fun formatarSinal(variacao: Int): String =
    if (variacao > 0) "+$variacao" else variacao.toString()

private fun compartilharRelatorio(context: Context, texto: String) {
    val envio = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, texto)
    }
    context.startActivity(Intent.createChooser(envio, "Compartilhar relatório"))
}
