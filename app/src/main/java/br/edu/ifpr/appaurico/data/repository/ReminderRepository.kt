package br.edu.ifpr.appaurico.data.repository

import br.edu.ifpr.appaurico.data.local.dao.ReminderDao
import br.edu.ifpr.appaurico.data.local.entity.ReminderEntity
import br.edu.ifpr.appaurico.domain.model.Reminder
import br.edu.ifpr.appaurico.domain.model.ReminderType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Unica porta de acesso aos lembretes. Expoe domain models, nunca entities. */
class ReminderRepository(
    private val dao: ReminderDao,
) {
    fun observarLembretes(): Flow<List<Reminder>> =
        dao.observarTodos().map { lista -> lista.map(ReminderEntity::toDomain) }

    suspend fun salvar(reminder: Reminder): Long =
        dao.inserir(reminder.toEntity())

    /** Liga/desliga o lembrete de um tipo, criando-o se ainda nao existir. */
    suspend fun definir(tipo: ReminderType, ativo: Boolean, horario: Long) {
        val existente = dao.porTipo(tipo)
        val atualizado = existente?.copy(ativo = ativo, horario = horario)
            ?: ReminderEntity(tipo = tipo, horario = horario, ativo = ativo)
        dao.inserir(atualizado)
    }
}

private fun ReminderEntity.toDomain() = Reminder(
    id = id,
    tipo = tipo,
    horario = horario,
    ativo = ativo,
)

private fun Reminder.toEntity() = ReminderEntity(
    id = id,
    tipo = tipo,
    horario = horario,
    ativo = ativo,
)
