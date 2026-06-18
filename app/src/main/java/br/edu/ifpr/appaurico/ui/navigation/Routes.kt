package br.edu.ifpr.appaurico.ui.navigation

/** Rotas de navegacao do app. */
object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val REMINDER = "reminder"
    const val LOG = "log"
    const val EVOLUTION = "evolution"
    const val AGENDA = "agenda"
    const val PROFESSIONAL = "professional"
}

/** Destinos exibidos na bottom bar (home, log, evolution, agenda). */
enum class TopLevelDestination(
    val route: String,
    val label: String,
) {
    HOME(Routes.HOME, "Início"),
    LOG(Routes.LOG, "Registrar"),
    EVOLUTION(Routes.EVOLUTION, "Evolução"),
    AGENDA(Routes.AGENDA, "Agenda"),
}
