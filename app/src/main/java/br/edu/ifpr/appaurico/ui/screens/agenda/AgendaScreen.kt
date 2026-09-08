package br.edu.ifpr.appaurico.ui.screens.agenda

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.edu.ifpr.appaurico.core.cycle.Ciclo
import br.edu.ifpr.appaurico.ui.components.AuricoCard
import br.edu.ifpr.appaurico.ui.theme.AuricoDimens

@Composable
fun AgendaScreen(
    uiState: AgendaUiState,
    onEstimulacaoChange: (Boolean) -> Unit,
    onRetornoChange: (Boolean) -> Unit,
) {
    val horarios = Ciclo.HORARIOS.joinToString(" · ") { hora -> "%02d:00".format(hora) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = AuricoDimens.ScreenPadding, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(AuricoDimens.BlockSpacing),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Agenda",
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = "Organize os lembretes deste ciclo e acompanhe a data de retorno.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        AuricoCard(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "PRÓXIMO RETORNO",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.tertiary,
            )
            Text(
                text = uiState.proximoRetorno.ifEmpty { "—" },
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = "Data calculada para o ciclo demonstrativo atual.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        LembreteCard(
            titulo = "Lembretes de estimulação",
            descricao = "Receba avisos nos horários previstos para o acompanhamento.",
            detalhe = horarios,
            ativo = uiState.estimulacaoAtiva,
            onChange = onEstimulacaoChange,
        )

        LembreteCard(
            titulo = "Lembrete de retorno",
            descricao = "Receba um aviso para a próxima sessão de retorno.",
            detalhe = uiState.proximoRetorno.ifEmpty { "Data indisponível" },
            ativo = uiState.retornoAtivo,
            onChange = onRetornoChange,
        )

        Text(
            text = "Os horários e a duração do ciclo são demonstrativos nesta versão do protótipo.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LembreteCard(
    titulo: String,
    descricao: String,
    detalhe: String,
    ativo: Boolean,
    onChange: (Boolean) -> Unit,
) {
    AuricoCard(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = titulo,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = if (ativo) "Ativo" else "Desativado",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (ativo) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            Switch(checked = ativo, onCheckedChange = onChange)
        }

        Text(
            text = descricao,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Text(
            text = detalhe,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
