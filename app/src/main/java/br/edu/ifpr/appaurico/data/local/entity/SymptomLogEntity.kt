package br.edu.ifpr.appaurico.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "symptom_log")
data class SymptomLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val dataHora: Long,
    val nivel: Int,
    val nota: String?,
)
