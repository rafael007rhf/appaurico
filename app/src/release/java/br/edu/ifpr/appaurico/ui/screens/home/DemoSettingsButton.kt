package br.edu.ifpr.appaurico.ui.screens.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Sem acesso aos ajustes de demonstração no build de release. */
@Composable
fun DemoSettingsButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    // Intencionalmente vazio: os controles de demonstração só existem em debug.
}
