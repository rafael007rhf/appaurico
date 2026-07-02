package br.edu.ifpr.appaurico.ui.screens.log

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.unit.dp
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
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = AuricoDimens.ScreenPadding, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(AuricoDimens.BlockSpacing),
    ) {
        Text(
            text = "Como está sua ansiedade agora?",
            style = MaterialTheme.typography.headlineSmall,
        )

        SymptomScale(
            valorSelecionado = uiState.nivelSelecionado,
            onValorSelecionado = onNivelSelecionado,
        )

        OutlinedTextField(
            value = uiState.nota,
            onValueChange = onNotaChange,
            label = { Text("Nota (opcional)") },
            shape = RoundedCornerShape(AuricoDimens.CornerRadius),
            modifier = Modifier.fillMaxWidth(),
        )

        Button(
            onClick = onSalvar,
            enabled = uiState.nivelSelecionado != null && !uiState.salvando,
            shape = RoundedCornerShape(AuricoDimens.CornerRadius),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Salvar registro")
        }

        if (uiState.salvo) {
            Text(
                text = "Registro salvo",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
