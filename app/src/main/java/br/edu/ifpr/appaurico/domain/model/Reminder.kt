package br.edu.ifpr.appaurico.domain.model

/**
 * Lembrete configurado pelo paciente.
 *
 * @param horario momento do lembrete em epoch milissegundos (alinhado ao AlarmManager).
 * @param ativo indica se o lembrete dispara notificacoes.
 */
data class Reminder(
    val id: Long = 0,
    val tipo: ReminderType,
    val horario: Long,
    val ativo: Boolean = true,
)
