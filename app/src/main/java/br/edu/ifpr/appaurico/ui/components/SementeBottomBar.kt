package br.edu.ifpr.appaurico.ui.components

import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import br.edu.ifpr.appaurico.ui.navigation.TopLevelDestination

// Placeholder sem icones por ora: a lib de icones (material-icons) ainda nao foi
// adicionada ao projeto. Trocar por icones quando definirmos o set visual.
@Composable
fun SementeBottomBar(
    rotaAtual: String?,
    onNavegar: (String) -> Unit,
) {
    NavigationBar {
        TopLevelDestination.entries.forEach { destino ->
            NavigationBarItem(
                selected = rotaAtual == destino.route,
                onClick = { onNavegar(destino.route) },
                icon = { Text(destino.label) },
            )
        }
    }
}
