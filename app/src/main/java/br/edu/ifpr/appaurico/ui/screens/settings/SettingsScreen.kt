package br.edu.ifpr.appaurico.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.edu.ifpr.appaurico.ui.screens.home.DemoControls
import br.edu.ifpr.appaurico.ui.theme.AuricoDimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onVoltar: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ajustes") },
                navigationIcon = {
                    TextButton(onClick = onVoltar) {
                        Text("Voltar")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AuricoDimens.ScreenPadding, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(AuricoDimens.BlockSpacing),
        ) {
            // Controles de feira: presentes apenas no build de debug (no-op em release).
            DemoControls(modifier = Modifier.fillMaxWidth())

            Text(
                text = "Estes controles servem para preparar a demonstração e " +
                    "não fazem parte do uso normal do app.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
