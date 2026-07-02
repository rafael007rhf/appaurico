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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
        Text(
            text = "Visão do profissional",
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = uiState.nomePaciente,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        AdesaoCard(uiState)

        TendenciaSecao(uiState)

        RegistrosRecentesSecao(uiState)

        Button(
            onClick = { compartilharRelatorio(context, uiState.relatorio) },
            shape = RoundedCornerShape(AuricoDimens.CornerRadius),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Compartilhar relatório")
        }

        Text(
            text = "Este resumo é de apoio e não substitui a avaliação do profissional.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AdesaoCard(uiState: ProfessionalUiState) {
    AuricoCard {
        Text(
            text = "Adesão",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "${uiState.adesaoPercentual}%",
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = "${uiState.estimulacoesFeitas} de ${uiState.estimulacoesPrevistas} estimulações",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "Ciclo: dia ${uiState.diaCiclo} de ${uiState.duracaoCiclo}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TendenciaSecao(uiState: ProfessionalUiState) {
    Text(
        text = "Tendência do sintoma",
        style = MaterialTheme.typography.titleMedium,
    )
    if (uiState.pontos.isEmpty()) {
        Text(
            text = "Sem registros de sintoma ainda.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
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
        Resumo("Variação", uiState.variacao?.let { formatarSinal(it) } ?: "-")
    }
}

@Composable
private fun RegistrosRecentesSecao(uiState: ProfessionalUiState) {
    Text(
        text = "Registros recentes",
        style = MaterialTheme.typography.titleMedium,
    )
    if (uiState.registrosRecentes.isEmpty()) {
        Text(
            text = "Nenhum registro ainda.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    AuricoCard(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        uiState.registrosRecentes.forEachIndexed { indice, registro ->
            if (indice > 0) HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
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
            Text(text = registro.quando, style = MaterialTheme.typography.bodyMedium)
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
        )
    }
}

@Composable
private fun Resumo(rotulo: String, valor: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = valor, style = MaterialTheme.typography.headlineSmall)
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
