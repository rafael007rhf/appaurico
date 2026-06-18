package br.edu.ifpr.appaurico.data.repository

import br.edu.ifpr.appaurico.data.local.dao.SymptomLogDao
import br.edu.ifpr.appaurico.data.local.entity.SymptomLogEntity
import br.edu.ifpr.appaurico.domain.model.SymptomLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Unica porta de acesso aos registros de sintoma. Expoe domain models, nunca entities. */
class SymptomRepository(
    private val dao: SymptomLogDao,
) {
    fun observarRegistros(): Flow<List<SymptomLog>> =
        dao.observarTodos().map { lista -> lista.map(SymptomLogEntity::toDomain) }

    suspend fun registrar(log: SymptomLog): Long =
        dao.inserir(log.toEntity())
}

private fun SymptomLogEntity.toDomain() = SymptomLog(
    id = id,
    dataHora = dataHora,
    nivel = nivel,
    nota = nota,
)

private fun SymptomLog.toEntity() = SymptomLogEntity(
    id = id,
    dataHora = dataHora,
    nivel = nivel,
    nota = nota,
)
