package br.edu.ifpr.appaurico.platform

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "platform_treatment", indices = [Index("accountId")])
data class StoredTreatment(@PrimaryKey val id: String, val accountId: String, val professionalId: String, val title: String, val status: String, val startsOn: String, val endsOn: String, val timezone: String, val version: Int, val snapshot: String, val sharingActive: Boolean = true)
@Entity(tableName = "platform_occurrence", indices = [Index("accountId", "treatmentId")])
data class Occurrence(@PrimaryKey val id: String, val accountId: String, val treatmentId: String, val scheduledAt: Long, val status: String, val notified: Boolean = false)
@Entity(tableName = "patient_event", indices = [Index("accountId", "treatmentId")])
data class PatientEvent(@PrimaryKey val id: String, val accountId: String, val treatmentId: String, val kind: String, val payload: String, val occurredAt: Long, val syncState: String)
@Entity(tableName = "sync_outbox", indices = [Index("accountId", "state")])
data class OutboxOperation(@PrimaryKey val id: String, val accountId: String, val eventId: String, val payload: String, val state: String = "pending", val error: String? = null)
@Entity(tableName = "sync_position")
data class SyncPosition(@PrimaryKey val accountId: String, val cursor: String)

@Dao
interface PlatformDao {
    @Query("SELECT * FROM patient_event WHERE id=:id") suspend fun eventById(id: String): PatientEvent?
    @Query("SELECT * FROM platform_treatment WHERE accountId=:accountId ORDER BY startsOn DESC") fun treatments(accountId: String): Flow<List<StoredTreatment>>
    @Query("SELECT * FROM platform_treatment WHERE accountId=:accountId") suspend fun allTreatments(accountId: String): List<StoredTreatment>
    @Query("SELECT * FROM platform_treatment WHERE id=:id AND accountId=:accountId") suspend fun treatment(accountId: String, id: String): StoredTreatment?
    @Upsert suspend fun saveTreatment(value: StoredTreatment)
    @Query("SELECT * FROM platform_occurrence WHERE accountId=:accountId ORDER BY scheduledAt") fun occurrences(accountId: String): Flow<List<Occurrence>>
    @Query("SELECT * FROM platform_occurrence WHERE accountId=:accountId ORDER BY scheduledAt") suspend fun allOccurrences(accountId: String): List<Occurrence>
    @Upsert suspend fun saveOccurrence(value: Occurrence)
    @Query("UPDATE platform_occurrence SET notified=1 WHERE id=:id") suspend fun notified(id: String)
    @Query("SELECT * FROM patient_event WHERE accountId=:accountId ORDER BY occurredAt") fun events(accountId: String): Flow<List<PatientEvent>>
    @Query("SELECT * FROM patient_event WHERE accountId=:accountId") suspend fun allEvents(accountId: String): List<PatientEvent>
    @Upsert suspend fun saveEvent(value: PatientEvent)
    @Query("UPDATE patient_event SET syncState=:state WHERE id=:id AND accountId=:accountId") suspend fun eventState(accountId: String, id: String, state: String)
    @Insert suspend fun enqueue(value: OutboxOperation)
    @Query("SELECT * FROM sync_outbox WHERE accountId=:accountId AND state='pending' ORDER BY rowid LIMIT 50") suspend fun pending(accountId: String): List<OutboxOperation>
    @Query("UPDATE sync_outbox SET state=:state,error=:error WHERE id=:id AND accountId=:accountId") suspend fun operationState(accountId: String, id: String, state: String, error: String?)
    @Query("SELECT * FROM sync_outbox WHERE accountId=:accountId AND state='rejected'") fun failures(accountId: String): Flow<List<OutboxOperation>>
    @Query("SELECT cursor FROM sync_position WHERE accountId=:accountId") suspend fun cursor(accountId: String): String?
    @Upsert suspend fun saveCursor(value: SyncPosition)
    @Query("UPDATE platform_treatment SET sharingActive=1 WHERE accountId=:accountId AND professionalId=:professionalId") suspend fun restoreSharing(accountId: String, professionalId: String)
    @Query("UPDATE platform_treatment SET sharingActive=0 WHERE accountId=:accountId AND professionalId=:professionalId") suspend fun revoke(accountId: String, professionalId: String)
}

object PlatformMigration {
    val STATEMENTS = listOf(
        "CREATE TABLE IF NOT EXISTS platform_treatment (id TEXT NOT NULL PRIMARY KEY, accountId TEXT NOT NULL, professionalId TEXT NOT NULL, title TEXT NOT NULL, status TEXT NOT NULL, startsOn TEXT NOT NULL, endsOn TEXT NOT NULL, timezone TEXT NOT NULL, version INTEGER NOT NULL, snapshot TEXT NOT NULL, sharingActive INTEGER NOT NULL)",
        "CREATE INDEX IF NOT EXISTS index_platform_treatment_accountId ON platform_treatment(accountId)",
        "CREATE TABLE IF NOT EXISTS platform_occurrence (id TEXT NOT NULL PRIMARY KEY, accountId TEXT NOT NULL, treatmentId TEXT NOT NULL, scheduledAt INTEGER NOT NULL, status TEXT NOT NULL, notified INTEGER NOT NULL)",
        "CREATE INDEX IF NOT EXISTS index_platform_occurrence_accountId_treatmentId ON platform_occurrence(accountId,treatmentId)",
        "CREATE TABLE IF NOT EXISTS patient_event (id TEXT NOT NULL PRIMARY KEY, accountId TEXT NOT NULL, treatmentId TEXT NOT NULL, kind TEXT NOT NULL, payload TEXT NOT NULL, occurredAt INTEGER NOT NULL, syncState TEXT NOT NULL)",
        "CREATE INDEX IF NOT EXISTS index_patient_event_accountId_treatmentId ON patient_event(accountId,treatmentId)",
        "CREATE TABLE IF NOT EXISTS sync_outbox (id TEXT NOT NULL PRIMARY KEY, accountId TEXT NOT NULL, eventId TEXT NOT NULL, payload TEXT NOT NULL, state TEXT NOT NULL, error TEXT)",
        "CREATE INDEX IF NOT EXISTS index_sync_outbox_accountId_state ON sync_outbox(accountId,state)",
        "CREATE TABLE IF NOT EXISTS sync_position (accountId TEXT NOT NULL PRIMARY KEY, cursor TEXT NOT NULL)",
    )
    val FROM_2_TO_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) { STATEMENTS.forEach(db::execSQL) }
    }
}
