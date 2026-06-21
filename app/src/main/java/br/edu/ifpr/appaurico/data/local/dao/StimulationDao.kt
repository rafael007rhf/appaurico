package br.edu.ifpr.appaurico.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import br.edu.ifpr.appaurico.data.local.entity.StimulationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StimulationDao {

    @Insert
    suspend fun inserir(estimulacao: StimulationEntity): Long

    @Query("SELECT * FROM stimulation ORDER BY dataHora ASC")
    fun observarTodas(): Flow<List<StimulationEntity>>

    @Query("SELECT COUNT(*) FROM stimulation")
    suspend fun contar(): Int

    @Query("DELETE FROM stimulation")
    suspend fun limparTudo()
}
