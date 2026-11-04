package br.edu.ifpr.appaurico.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import br.edu.ifpr.appaurico.ui.navigation.TopLevelDestination

@Composable
fun AuricoBottomBar(
    rotaAtual: String?,
    onNavegar: (String) -> Unit,
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
    ) {
        TopLevelDestination.entries.forEach { destino ->
            val selecionado = rotaAtual == destino.route
            NavigationBarItem(
                selected = selecionado,
                onClick = { onNavegar(destino.route) },
                icon = {
                    Box(
                        modifier = Modifier
                            .size(if (selecionado) 10.dp else 7.dp)
                            .clip(CircleShape)
                            .background(
                                if (selecionado) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outline
                                },
                            ),
                    )
                },
                label = { Text(destino.label) },
                alwaysShowLabel = true,
            )
        }
    }
}
