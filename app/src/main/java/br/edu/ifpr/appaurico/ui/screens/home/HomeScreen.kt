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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import br.edu.ifpr.appaurico.R
import br.edu.ifpr.appaurico.ui.components.AuricoCard
import br.edu.ifpr.appaurico.ui.theme.AuricoDimens

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

        ProximaEstimulacaoCard(
            momento = uiState.proximaEstimulacao,
            onEstimularAgora = onEstimularAgora,
        )

        CicloCard(uiState = uiState)

        Button(
            onClick = onRegistrar,
            shape = RoundedCornerShape(AuricoDimens.CornerRadius),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Registrar como me sinto agora")
        }

        TextButton(
            onClick = onVisaoProfissional,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Visão do profissional")
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
            modifier = Modifier.size(40.dp),
        )
        Spacer(modifier = Modifier.size(12.dp))
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.weight(1f))
        // Acesso discreto aos ajustes de demonstração (no-op em release).
        DemoSettingsButton(onClick = onAbrirAjustes)
    }
}

@Composable
private fun ProximaEstimulacaoCard(
    momento: String,
    onEstimularAgora: () -> Unit,
) {
    AuricoCard {
        Text(
            text = "Próxima estimulação",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = momento.ifEmpty { "—" },
            style = MaterialTheme.typography.headlineSmall,
        )
        Button(
            onClick = onEstimularAgora,
            shape = RoundedCornerShape(AuricoDimens.CornerRadius),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Estimular agora")
        }
    }
}

@Composable
private fun CicloCard(uiState: HomeUiState) {
    AuricoCard {
        Text(
            text = "Ciclo atual",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "Dia ${uiState.diaCiclo} de ${uiState.duracaoCiclo}",
            style = MaterialTheme.typography.bodyLarge,
        )

        Text(
            text = "Adesão: ${uiState.adesaoPercentual}% " +
                "(${uiState.estimulacoesFeitas} de ${uiState.estimulacoesPrevistas})",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LinearProgressIndicator(
            progress = { uiState.adesaoPercentual / 100f },
            modifier = Modifier.fillMaxWidth(),
        )

        val ultimo = uiState.ultimoRegistroNivel
        if (ultimo != null) {
            Text(
                text = "Último registro: nível $ultimo · ${uiState.ultimoRegistroQuando}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text(
                text = "Faça seu primeiro registro do sintoma.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
