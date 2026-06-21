package br.edu.ifpr.appaurico.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Controles de feira (somente debug): carregam ou limpam os dados de demonstração. */
@Composable
fun DemoControls(modifier: Modifier = Modifier) {
    val viewModel: DemoViewModel = hiltViewModel()
    val mensagem by viewModel.mensagem.collectAsStateWithLifecycle()

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "Demonstração (debug)",
            style = MaterialTheme.typography.labelMedium,
        )
        OutlinedButton(
            onClick = viewModel::carregar,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Carregar dados de demonstração")
        }
        OutlinedButton(
            onClick = viewModel::limpar,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Limpar dados")
        }
        mensagem?.let {
            Text(text = it, style = MaterialTheme.typography.bodySmall)
        }
    }
}
