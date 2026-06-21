package br.edu.ifpr.appaurico.domain.model

/**
 * Estimulacao realizada pelo paciente (pressao na semente).
 *
 * @param dataHora momento em que o paciente marcou a estimulacao como feita, em epoch milissegundos.
 */
data class Stimulation(
    val id: Long = 0,
    val dataHora: Long,
)
