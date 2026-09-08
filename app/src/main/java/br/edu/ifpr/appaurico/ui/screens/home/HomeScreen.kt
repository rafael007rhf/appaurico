package br.edu.ifpr.appaurico.ui.screens.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.edu.ifpr.appaurico.R
import br.edu.ifpr.appaurico.ui.components.AuricoCard
import br.edu.ifpr.appaurico.ui.theme.AuricoDimens
import java.time.LocalTime

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onEstimularAgora: () -> Unit,
    onRegistrar: () -> Unit,
    onVisaoProfissional: () -> Unit,
    onAbrirAjustes: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = AuricoDimens.ScreenPadding, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(AuricoDimens.BlockSpacing),
    ) {
        BrandHeader(onAbrirAjustes = onAbrirAjustes)

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = saudacaoAtual(),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = "Dia ${uiState.diaCiclo} do seu acompanhamento",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        ProximaAcaoCard(
            momento = uiState.proximaEstimulacao,
            onEstimularAgora = onEstimularAgora,
        )

        AcompanhamentoCard(uiState = uiState)

        RegistroHojeCard(
            ultimoNivel = uiState.ultimoRegistroNivel,
            ultimoQuando = uiState.ultimoRegistroQuando,
            onRegistrar = onRegistrar,
        )

        TextButton(
            onClick = onVisaoProfissional,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Abrir visão do profissional")
        }
    }
}

@Composable
private fun BrandHeader(onAbrirAjustes: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_aurico_logo),
            contentDescription = null,
            modifier = Modifier.size(36.dp),
        )
        Spacer(modifier = Modifier.size(10.dp))
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.weight(1f))
        DemoSettingsButton(onClick = onAbrirAjustes)
    }
}

@Composable
private fun ProximaAcaoCard(
    momento: String,
    onEstimularAgora: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f),
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "PRÓXIMA AÇÃO",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.tertiary,
            )
            Text(
                text = "Estimulação",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = momento.ifEmpty { "Horário não disponível" },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Quando chegar a hora, use o guia para registrar a estimulação no acompanhamento.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = onEstimularAgora,
                shape = RoundedCornerShape(AuricoDimens.CornerRadius),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Estimular agora")
            }
        }
    }
}

@Composable
private fun AcompanhamentoCard(uiState: HomeUiState) {
    val progressoCiclo = if (uiState.duracaoCiclo == 0) {
        0f
    } else {
        (uiState.diaCiclo.toFloat() / uiState.duracaoCiclo).coerceIn(0f, 1f)
    }

    AuricoCard(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Seu acompanhamento",
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = "Veja rapidamente onde você está neste ciclo.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Metrica(
                valor = "${uiState.diaCiclo}/${uiState.duracaoCiclo}",
                rotulo = "dia do ciclo",
                modifier = Modifier.weight(1f),
            )
            Metrica(
                valor = "${uiState.adesaoPercentual}%",
                rotulo = "adesão",
                modifier = Modifier.weight(1f),
            )
            Metrica(
                valor = uiState.ultimoRegistroNivel?.toString() ?: "—",
                rotulo = "último nível",
                modifier = Modifier.weight(1f),
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Progresso do ciclo",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "${(progressoCiclo * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            LinearProgressIndicator(
                progress = { progressoCiclo },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Text(
            text = if (uiState.estimulacoesPrevistas > 0) {
                "${uiState.estimulacoesFeitas} de ${uiState.estimulacoesPrevistas} estimulações previstas até agora foram registradas."
            } else {
                "As estimulações registradas aparecerão aqui ao longo do ciclo."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun RegistroHojeCard(
    ultimoNivel: Int?,
    ultimoQuando: String?,
    onRegistrar: () -> Unit,
) {
    AuricoCard {
        Text(
            text = "Como você está hoje?",
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = if (ultimoNivel != null) {
                "Seu último registro foi nível $ultimoNivel${ultimoQuando?.let { " · $it" } ?: ""}."
            } else {
                "Registre como está o sintoma acompanhado neste ciclo."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(
            onClick = onRegistrar,
            shape = RoundedCornerShape(AuricoDimens.CornerRadius),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Registrar agora")
        }
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

private fun saudacaoAtual(): String = when (LocalTime.now().hour) {
    in 5..11 -> "Bom dia"
    in 12..17 -> "Boa tarde"
    else -> "Boa noite"
}
