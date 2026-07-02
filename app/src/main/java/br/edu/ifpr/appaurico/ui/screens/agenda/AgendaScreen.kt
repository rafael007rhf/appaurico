package br.edu.ifpr.appaurico.ui.screens.agenda

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.edu.ifpr.appaurico.ui.components.AuricoCard
import br.edu.ifpr.appaurico.ui.theme.AuricoDimens

@Composable
fun AgendaScreen(
    uiState: AgendaUiState,
    onEstimulacaoChange: (Boolean) -> Unit,
    onRetornoChange: (Boolean) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = AuricoDimens.ScreenPadding, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(AuricoDimens.BlockSpacing),
    ) {
        Text(
            text = "Agenda",
            style = MaterialTheme.typography.headlineSmall,
        )

        AuricoCard {
            Text(
                text = "Próximo retorno",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = uiState.proximoRetorno.ifEmpty { "—" },
                style = MaterialTheme.typography.headlineSmall,
            )
        }

        AuricoCard {
            Text(
                text = "Lembretes",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            LembreteLinha(
                titulo = "Estimulação",
                descricao = "Avisa nos horários do dia para estimular as sementes.",
                ativo = uiState.estimulacaoAtiva,
                onChange = onEstimulacaoChange,
            )

            HorizontalDivider()

            LembreteLinha(
                titulo = "Retorno",
                descricao = "Avisa no dia da sessão de retorno.",
                ativo = uiState.retornoAtivo,
                onChange = onRetornoChange,
            )
        }
    }
}

@Composable
private fun LembreteLinha(
    titulo: String,
    descricao: String,
    ativo: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = titulo, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = descricao,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = ativo, onCheckedChange = onChange)
    }
}
