package br.edu.ifpr.appaurico.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import br.edu.ifpr.appaurico.domain.model.ReminderType

@Entity(tableName = "reminder")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val tipo: ReminderType,
    val horario: Long,
    val ativo: Boolean,
)
