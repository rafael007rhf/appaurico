package br.edu.ifpr.appaurico.platform

import android.content.Context
import androidx.room.withTransaction
import androidx.work.*
import br.edu.ifpr.appaurico.data.local.AuricoDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PatientRepository @Inject constructor(val database: AuricoDatabase, val session: PatientSession, @ApplicationContext private val context: Context) {
    val dao = database.platformDao()
    suspend fun recordSymptom(account: String, treatmentId: String, symptomId: String, level: Int, note: String, correctionOf: String? = null) {
        val data = JSONObject().put("id", UUID.randomUUID().toString()).put("treatmentId", treatmentId).put("symptomId", symptomId)
            .put("level", level).put("note", note.trim().ifBlank { null }).put("occurredAt", Instant.now().toString())
        correctionOf?.let { data.put("correctionOf", it) }
        persist(account, treatmentId, "symptom", data)
    }
    suspend fun recordStimulation(account: String, occurrence: Occurrence) {
        require(occurrence.accountId == account && occurrence.status == "scheduled" && occurrence.scheduledAt <= System.currentTimeMillis())
        persist(account, occurrence.treatmentId, "stimulation", JSONObject().put("id", UUID.randomUUID().toString())
            .put("treatmentId", occurrence.treatmentId).put("scheduledId", occurrence.id).put("occurredAt", Instant.now().toString()))
    }
    private suspend fun persist(account: String, treatmentId: String, kind: String, data: JSONObject) {
        database.withTransaction {
            val treatment = dao.treatment(account, treatmentId) ?: error("Tratamento indisponível")
            val today = LocalDate.now(ZoneId.of(treatment.timezone))
            require(treatment.status == "active" && treatment.sharingActive && today >= LocalDate.parse(treatment.startsOn) && today <= LocalDate.parse(treatment.endsOn)) { "O tratamento não está ativo para novos registros." }
            if (kind == "symptom") {
                val symptoms = JSONObject(treatment.snapshot).getJSONArray("symptoms")
                val symptom = (0 until symptoms.length()).map(symptoms::getJSONObject).firstOrNull { it.getString("id") == data.getString("symptomId") } ?: error("Sintoma indisponível")
                require(data.getInt("level") in symptom.getInt("min_value")..symptom.getInt("max_value"))
                require(data.optString("note").length <= 2000)
            } else {
                val already = dao.allEvents(account).any { it.kind == "stimulation" && JSONObject(it.payload).optString("scheduledId") == data.getString("scheduledId") }
                if (already) return@withTransaction
            }
            val operationId = UUID.randomUUID().toString()
            val operation = JSONObject().put("operationId", operationId).put("kind", kind).put("data", data)
            dao.saveEvent(PatientEvent(data.getString("id"), account, treatmentId, kind, data.toString(), System.currentTimeMillis(), "pending"))
            dao.enqueue(OutboxOperation(operationId, account, data.getString("id"), operation.toString()))
        }
        scheduleSync()
    }
    fun scheduleSync() {
        val work = OneTimeWorkRequestBuilder<PatientSyncWorker>().setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS).build()
        WorkManager.getInstance(context).enqueueUniqueWork("patient-sync", ExistingWorkPolicy.APPEND_OR_REPLACE, work)
    }
    fun schedulePeriodicSync() {
        WorkManager.getInstance(context).enqueueUniquePeriodicWork("patient-sync-periodic", ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<PatientSyncWorker>(15, TimeUnit.MINUTES).setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).build())
    }
    suspend fun synchronize(account: String) {
        if (account == "local") return
        // Pull authority changes (including revocations) before sending new patient events.
        pull(account)
        while (true) {
            val pending = dao.pending(account)
            if (pending.isEmpty()) break
            val result = session.requestFor(account, "/sync/push", "POST", JSONObject().put("deviceId", session.deviceId()).put("operations", JSONArray(pending.map { JSONObject(it.payload) })))
            val results = result.getJSONArray("results")
            database.withTransaction {
                for (i in 0 until results.length()) {
                    val ack = results.getJSONObject(i)
                    val op = pending.firstOrNull { it.id == ack.getString("operationId") } ?: continue
                    if (ack.getString("status") == "acknowledged") {
                        dao.operationState(account, op.id, "acknowledged", null)
                        dao.eventState(account, op.eventId, "synced")
                        val canonical = ack.getJSONObject("record")
                        if (canonical.getString("id") != op.eventId) dao.eventState(account, op.eventId, "duplicate")
                        importEvent(account, JSONObject(op.payload).getString("kind"), canonical)
                    } else {
                        dao.operationState(account, op.id, "rejected", ack.optString("code"))
                        dao.eventState(account, op.eventId, "error")
                    }
                }
            }
            if (results.length() == 0) error("O servidor não confirmou os registros.")
        }
        pull(account)
    }
    private suspend fun pull(account: String) {
        do {
            val result = session.requestFor(account, "/sync/pull?cursor=${dao.cursor(account) ?: "0"}")
            database.withTransaction {
                val changes = result.getJSONArray("changes")
                for (i in 0 until changes.length()) {
                    val change = changes.getJSONObject(i); val payload = change.getJSONObject("payload")
                    when (change.getString("kind")) {
                        "treatment" -> {
                            val t = payload.getJSONObject("treatment")
                            val id = t.getString("id")
                            val existing = dao.treatment(account, id)
                            dao.saveTreatment(StoredTreatment(id, account, t.getString("professional_id"), t.getString("title"), t.getString("status"), t.getString("starts_on"), t.getString("ends_on"), t.getString("timezone"), t.getInt("version"), payload.toString(), existing?.sharingActive ?: true))
                            val previous = dao.allOccurrences(account).associateBy { it.id }
                            val slots = payload.getJSONArray("occurrences")
                            for (j in 0 until slots.length()) {
                                val slot = slots.getJSONObject(j); val slotId = slot.getString("id")
                                dao.saveOccurrence(Occurrence(slotId, account, id, Instant.parse(slot.getString("scheduled_at")).toEpochMilli(), slot.getString("status"), previous[slotId]?.notified ?: false))
                            }
                        }
                        "symptom", "stimulation" -> importEvent(account, change.getString("kind"), payload)
                        "link" -> if (payload.optString("status") == "revoked") dao.revoke(account, payload.getString("professionalId")) else if (payload.has("professionalId")) dao.restoreSharing(account, payload.getString("professionalId"))
                    }
                }
                dao.saveCursor(SyncPosition(account, result.getString("cursor")))
            }
        } while (result.getBoolean("hasMore"))
    }
    private suspend fun importEvent(account: String, kind: String, record: JSONObject) {
        val payload = JSONObject().put("id", record.getString("id")).put("treatmentId", record.getString("treatment_id")).put("occurredAt", record.getString("occurred_at"))
        if (kind == "symptom") payload.put("symptomId", record.getString("symptom_id")).put("level", record.getInt("level")).put("note", record.optString("note", "")).put("correctionOf", record.optString("correction_of", ""))
        else payload.put("scheduledId", record.getString("scheduled_id"))
        dao.saveEvent(PatientEvent(record.getString("id"), account, record.getString("treatment_id"), kind, payload.toString(), Instant.parse(record.getString("occurred_at")).toEpochMilli(), "synced"))
    }
}
