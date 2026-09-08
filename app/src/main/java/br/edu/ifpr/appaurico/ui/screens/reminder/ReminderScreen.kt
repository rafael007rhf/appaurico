package br.edu.ifpr.appaurico.ui.screens.reminder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import br.edu.ifpr.appaurico.ui.components.AuricoCard
import br.edu.ifpr.appaurico.ui.components.EarDiagram
import br.edu.ifpr.appaurico.ui.components.pontosAuricularesPadrao
import br.edu.ifpr.appaurico.ui.theme.AuricoDimens
import kotlinx.coroutines.delay

/** Ponto que o paciente estimula nesta tela. Fixo no MVP; depois pode variar por ciclo. */
private const val INDICE_PONTO_DESTACADO = 0
private const val DURACAO_TIMER_SEGUNDOS = 30

@Composable
fun ReminderScreen(
    uiState: ReminderUiState,
    onMarcarComoFeito: () -> Unit,
    onConcluido: () -> Unit,
) {
    var segundosRestantes by remember { mutableStateOf(DURACAO_TIMER_SEGUNDOS) }
    var timerRodando by remember { mutableStateOf(false) }

    LaunchedEffect(timerRodando) {
        if (!timerRodando) return@LaunchedEffect
        while (segundosRestantes > 0) {
            delay(1_000)
            segundosRestantes -= 1
        }
        timerRodando = false
    }

    LaunchedEffect(uiState.concluido) {
        if (uiState.concluido) {
            delay(1_200)
            onConcluido()
        }
    }

    val ponto = pontosAuricularesPadrao[INDICE_PONTO_DESTACADO]
    val numeroDaEstimulacao = (uiState.realizadasHoje + 1).coerceAtMost(uiState.previstasHoje)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = AuricoDimens.ScreenPadding, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AuricoDimens.BlockSpacing),
    ) {
        if (uiState.concluido) {
            AuricoCard {
                Text(
                    text = "Estimulação registrada",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "Seu acompanhamento foi atualizado.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@Column
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "Estimulação de agora",
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = "$numeroDaEstimulacao de ${uiState.previstasHoje} previstas hoje",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        AuricoCard(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Ponto indicado",
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = ponto.nome,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            EarDiagram(
                indiceDestacado = INDICE_PONTO_DESTACADO,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = "Diagrama da orelha com o ponto ${ponto.nome} em destaque"
                    },
            )

            Text(
                text = "Pressione a semente em destaque por cerca de 30 segundos, com firmeza e sem causar dor.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        AuricoCard(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = "Cronômetro opcional",
                style = MaterialTheme.typography.titleLarge,
            )

            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(104.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    progress = {
                        1f - (segundosRestantes.toFloat() / DURACAO_TIMER_SEGUNDOS)
                    },
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.tertiary,
                    trackColor = MaterialTheme.colorScheme.outlineVariant,
                )
                Text(
                    text = if (segundosRestantes == 0) "Pronto" else "${segundosRestantes}s",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            if (segundosRestantes > 0) {
                OutlinedButton(
                    onClick = { timerRodando = !timerRodando },
                    shape = RoundedCornerShape(AuricoDimens.CornerRadius),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (timerRodando) "Pausar cronômetro" else if (segundosRestantes == DURACAO_TIMER_SEGUNDOS) "Iniciar 30 segundos" else "Continuar cronômetro")
                }
            } else {
                Text(
                    text = "Tempo concluído. Agora você pode registrar esta estimulação.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        Button(
            onClick = onMarcarComoFeito,
            enabled = !uiState.salvando,
            shape = RoundedCornerShape(AuricoDimens.CornerRadius),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (uiState.salvando) "Registrando..." else "Marcar como feito")
        }

        Text(
            text = "O cronômetro é apenas um apoio visual e não é obrigatório para registrar a estimulação.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
