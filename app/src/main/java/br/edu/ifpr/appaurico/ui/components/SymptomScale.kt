package br.edu.ifpr.appaurico.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp

/**
 * Escala de 0 a 10 para o paciente indicar a intensidade do sintoma.
 * Cada item e um alvo de toque de 48dp e expoe o estado de selecao para acessibilidade.
 */
@Composable
fun SymptomScale(
    valorSelecionado: Int?,
    onValorSelecionado: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cores = MaterialTheme.colorScheme
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        (0..10).forEach { nivel ->
            val selecionado = nivel == valorSelecionado
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (selecionado) cores.primary else cores.surfaceVariant)
                    .selectable(
                        selected = selecionado,
                        onClick = { onValorSelecionado(nivel) },
                    )
                    .semantics {
                        stateDescription = if (selecionado) "Selecionado" else "Não selecionado"
                    },
            ) {
                Text(
                    text = nivel.toString(),
                    color = if (selecionado) cores.onPrimary else cores.onSurface,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}
