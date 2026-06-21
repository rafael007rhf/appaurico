package br.edu.ifpr.appaurico.data.repository

import br.edu.ifpr.appaurico.data.local.dao.StimulationDao
import br.edu.ifpr.appaurico.data.local.entity.StimulationEntity
import br.edu.ifpr.appaurico.domain.model.Stimulation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Unica porta de acesso as estimulacoes realizadas. Expoe domain models, nunca entities. */
class StimulationRepository(
    private val dao: StimulationDao,
) {
    fun observarRealizadas(): Flow<List<Stimulation>> =
        dao.observarTodas().map { lista -> lista.map(StimulationEntity::toDomain) }

    suspend fun registrar(dataHora: Long): Long =
        dao.inserir(StimulationEntity(dataHora = dataHora))
}

private fun StimulationEntity.toDomain() = Stimulation(
    id = id,
    dataHora = dataHora,
)
