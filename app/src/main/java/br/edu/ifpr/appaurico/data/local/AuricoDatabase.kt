package br.edu.ifpr.appaurico.data.local

import br.edu.ifpr.appaurico.platform.*
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import br.edu.ifpr.appaurico.data.local.dao.ReminderDao
import br.edu.ifpr.appaurico.data.local.dao.StimulationDao
import br.edu.ifpr.appaurico.data.local.dao.SymptomLogDao
import br.edu.ifpr.appaurico.data.local.entity.ReminderEntity
import br.edu.ifpr.appaurico.data.local.entity.StimulationEntity
import br.edu.ifpr.appaurico.data.local.entity.SymptomLogEntity

@Database(
    entities = [SymptomLogEntity::class, ReminderEntity::class, StimulationEntity::class, StoredTreatment::class, Occurrence::class, PatientEvent::class, OutboxOperation::class, SyncPosition::class],
    version = 3,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AuricoDatabase : RoomDatabase() {
    abstract fun platformDao(): PlatformDao

    abstract fun symptomLogDao(): SymptomLogDao

    abstract fun reminderDao(): ReminderDao

    abstract fun stimulationDao(): StimulationDao
}
