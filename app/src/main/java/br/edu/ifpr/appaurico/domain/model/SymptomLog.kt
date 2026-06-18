package br.edu.ifpr.appaurico.domain.model

/**
 * Registro diario do sintoma feito pelo paciente.
 *
 * @param dataHora momento do registro em epoch milissegundos.
 * @param nivel intensidade do sintoma na escala 0..10.
 * @param nota observacao opcional do paciente.
 */
data class SymptomLog(
    val id: Long = 0,
    val dataHora: Long,
    val nivel: Int,
    val nota: String? = null,
)
