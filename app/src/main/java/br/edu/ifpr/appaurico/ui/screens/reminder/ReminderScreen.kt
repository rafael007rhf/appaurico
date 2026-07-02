package br.edu.ifpr.appaurico.ui.screens.reminder

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import br.edu.ifpr.appaurico.ui.components.EarDiagram
import br.edu.ifpr.appaurico.ui.components.pontosAuricularesPadrao
import br.edu.ifpr.appaurico.ui.theme.AuricoDimens

/** Ponto que o paciente estimula nesta tela. Fixo no MVP; depois pode variar por ciclo. */
private const val INDICE_PONTO_DESTACADO = 0

@Composable
fun ReminderScreen(
    uiState: ReminderUiState,
    onMarcarComoFeito: () -> Unit,
    onConcluido: () -> Unit,
) {
    // Apos registrar a estimulacao, volta para a Home.
    LaunchedEffect(uiState.concluido) {
        if (uiState.concluido) onConcluido()
    }

    val ponto = pontosAuricularesPadrao[INDICE_PONTO_DESTACADO]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = AuricoDimens.ScreenPadding, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AuricoDimens.BlockSpacing),
    ) {
        Text(
            text = "Estimule o ponto ${ponto.nome}",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )

        EarDiagram(
            indiceDestacado = INDICE_PONTO_DESTACADO,
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = "Diagrama da orelha com o ponto ${ponto.nome} em destaque"
                },
        )

        Text(
            text = "Pressione a semente em destaque por cerca de 30 segundos, " +
                "com firmeza e sem causar dor.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Button(
            onClick = onMarcarComoFeito,
            enabled = !uiState.salvando,
            shape = RoundedCornerShape(AuricoDimens.CornerRadius),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Marcar como feito")
        }
    }
}
