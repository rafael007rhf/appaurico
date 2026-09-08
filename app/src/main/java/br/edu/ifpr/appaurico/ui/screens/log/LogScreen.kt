package br.edu.ifpr.appaurico.ui.screens.log

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.edu.ifpr.appaurico.ui.components.AuricoCard
import br.edu.ifpr.appaurico.ui.components.SymptomScale
import br.edu.ifpr.appaurico.ui.theme.AuricoDimens

@Composable
fun LogScreen(
    uiState: LogUiState,
    onNivelSelecionado: (Int) -> Unit,
    onNotaChange: (String) -> Unit,
    onSalvar: () -> Unit,
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
                text = "Registro de hoje",
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = "Como está o sintoma que você está acompanhando neste ciclo?",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (uiState.salvo) {
            AuricoCard {
                Text(
                    text = "Registro salvo",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "Ele já faz parte do seu histórico de evolução.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        AuricoCard(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Intensidade agora",
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = "Use a escala de 0 a 10 de acordo com a sua percepção neste momento.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            SymptomScale(
                valorSelecionado = uiState.nivelSelecionado,
                onValorSelecionado = onNivelSelecionado,
            )
        }

        OutlinedTextField(
            value = uiState.nota,
            onValueChange = onNotaChange,
            label = { Text("Observação (opcional)") },
            placeholder = { Text("Registre algo que possa ajudar a lembrar como foi este momento.") },
            minLines = 3,
            shape = RoundedCornerShape(AuricoDimens.CornerRadius),
            modifier = Modifier.fillMaxWidth(),
        )

        Button(
            onClick = onSalvar,
            enabled = uiState.nivelSelecionado != null && !uiState.salvando,
            shape = RoundedCornerShape(AuricoDimens.CornerRadius),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (uiState.salvando) "Salvando..." else "Salvar registro")
        }

        Text(
            text = "O registro é uma percepção do paciente e não substitui a avaliação do profissional.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
