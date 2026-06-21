package br.edu.ifpr.appaurico.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stimulation")
data class StimulationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val dataHora: Long,
)
