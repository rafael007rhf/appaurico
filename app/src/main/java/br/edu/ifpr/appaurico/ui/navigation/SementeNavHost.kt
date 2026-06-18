package br.edu.ifpr.appaurico.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import br.edu.ifpr.appaurico.ui.components.SementeBottomBar
import br.edu.ifpr.appaurico.ui.screens.agenda.AgendaScreen
import br.edu.ifpr.appaurico.ui.screens.evolution.EvolutionScreen
import br.edu.ifpr.appaurico.ui.screens.evolution.EvolutionViewModel
import br.edu.ifpr.appaurico.ui.screens.home.HomeScreen
import br.edu.ifpr.appaurico.ui.screens.log.LogScreen
import br.edu.ifpr.appaurico.ui.screens.log.LogViewModel
import br.edu.ifpr.appaurico.ui.screens.onboarding.OnboardingScreen
import br.edu.ifpr.appaurico.ui.screens.professional.ProfessionalScreen
import br.edu.ifpr.appaurico.ui.screens.reminder.ReminderScreen

private val rotasComBottomBar = TopLevelDestination.entries.map { it.route }.toSet()

@Composable
fun SementeNavHost(
    navController: NavHostController = rememberNavController(),
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val rotaAtual = backStackEntry?.destination?.route

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (rotaAtual in rotasComBottomBar) {
                SementeBottomBar(
                    rotaAtual = rotaAtual,
                    onNavegar = { rota -> navController.navegarParaTopLevel(rota) },
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.ONBOARDING,
            modifier = Modifier.padding(innerPadding),
        ) {
            sementeGraph(navController)
        }
    }
}

private fun NavGraphBuilder.sementeGraph(navController: NavHostController) {
    composable(Routes.ONBOARDING) {
        OnboardingScreen(
            onComecar = {
                navController.navigate(Routes.HOME) {
                    // Onboarding nao deve voltar na pilha apos comecar.
                    popUpTo(Routes.ONBOARDING) { inclusive = true }
                }
            },
        )
    }
    composable(Routes.HOME) {
        HomeScreen(
            onEstimularAgora = { navController.navigate(Routes.REMINDER) },
            onVisaoProfissional = { navController.navigate(Routes.PROFESSIONAL) },
        )
    }
    composable(Routes.REMINDER) { ReminderScreen() }
    composable(Routes.LOG) {
        val viewModel: LogViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        LogScreen(
            uiState = uiState,
            onNivelSelecionado = viewModel::onNivelSelecionado,
            onNotaChange = viewModel::onNotaChange,
            onSalvar = viewModel::salvar,
        )
    }
    composable(Routes.EVOLUTION) {
        val viewModel: EvolutionViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        EvolutionScreen(uiState = uiState)
    }
    composable(Routes.AGENDA) { AgendaScreen() }
    composable(Routes.PROFESSIONAL) { ProfessionalScreen() }
}

/** Navegacao entre destinos da bottom bar, preservando estado e sem empilhar duplicatas. */
private fun NavHostController.navegarParaTopLevel(rota: String) {
    navigate(rota) {
        popUpTo(Routes.HOME) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
