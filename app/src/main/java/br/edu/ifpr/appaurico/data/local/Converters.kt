package br.edu.ifpr.appaurico.data.local

import androidx.room.TypeConverter
import br.edu.ifpr.appaurico.domain.model.ReminderType

/** Conversores do Room para tipos que ele nao persiste nativamente. */
class Converters {

    @TypeConverter
    fun fromReminderType(tipo: ReminderType): String = tipo.name

    @TypeConverter
    fun toReminderType(valor: String): ReminderType = ReminderType.valueOf(valor)
}
