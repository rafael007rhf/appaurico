package br.edu.ifpr.appaurico.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import br.edu.ifpr.appaurico.data.local.entity.ReminderEntity
import br.edu.ifpr.appaurico.domain.model.ReminderType
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(reminder: ReminderEntity): Long

    @Query("SELECT * FROM reminder ORDER BY horario ASC")
    fun observarTodos(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminder WHERE tipo = :tipo LIMIT 1")
    suspend fun porTipo(tipo: ReminderType): ReminderEntity?

    @Query("DELETE FROM reminder")
    suspend fun limparTudo()
}
