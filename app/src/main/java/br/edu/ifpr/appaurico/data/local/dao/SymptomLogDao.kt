package br.edu.ifpr.appaurico.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import br.edu.ifpr.appaurico.data.local.entity.SymptomLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SymptomLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(log: SymptomLogEntity): Long

    @Query("SELECT * FROM symptom_log ORDER BY dataHora ASC")
    fun observarTodos(): Flow<List<SymptomLogEntity>>
}
