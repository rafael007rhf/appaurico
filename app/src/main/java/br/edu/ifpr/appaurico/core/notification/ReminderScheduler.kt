package br.edu.ifpr.appaurico.core.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import br.edu.ifpr.appaurico.BuildConfig
import br.edu.ifpr.appaurico.core.cycle.Ciclo
import br.edu.ifpr.appaurico.domain.model.Reminder
import br.edu.ifpr.appaurico.domain.model.ReminderType
import br.edu.ifpr.appaurico.platform.PatientRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject

class ReminderScheduler @Inject constructor(@ApplicationContext private val context: Context, private val repository: PatientRepository) {
    private val manager = context.getSystemService(AlarmManager::class.java)
    suspend fun reagendarTodos(lembretes: List<Reminder>) {
        cancelarTodos()
        if (BuildConfig.DEMO_MODE) {
            if (lembretes.any { it.ativo && it.tipo == ReminderType.ESTIMULACAO }) Ciclo.HORARIOS.forEach(::agendarEstimulacaoSlot)
            return
        }
        if (!repository.session.reminders.first()) return
        val account = repository.session.account.first()
        val treatments = repository.dao.allTreatments(account).filter { it.status == "active" && it.sharingActive }.map { it.id }.toSet()
        val now = System.currentTimeMillis()
        val next = repository.dao.allOccurrences(account).firstOrNull { it.treatmentId in treatments && it.status == "scheduled" && !it.notified && it.scheduledAt > now } ?: return
        val intent = Intent(context, ReminderReceiver::class.java).putExtra(EXTRA_OCCURRENCE, next.id).putExtra(EXTRA_ACCOUNT, account)
        manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.scheduledAt, pending(300, intent))
    }
    fun agendarEstimulacaoSlot(hora: Int) {
        if (!BuildConfig.DEMO_MODE) return
        val now = LocalDateTime.now(); var target = now.toLocalDate().atTime(hora, 0)
        if (!target.isAfter(now)) target = target.plusDays(1)
        manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, target.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(), pending(100 + hora, Intent(context, ReminderReceiver::class.java).putExtra(EXTRA_HORA, hora)))
    }
    private fun pending(code: Int, intent: Intent) = PendingIntent.getBroadcast(context, code, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    fun cancelarTodos() {
        // Also removes pending intents left by the original application's fixed schedule.
        (100..123).plus(listOf(200, 300)).forEach { manager.cancel(pending(it, Intent(context, ReminderReceiver::class.java))) }
    }
    fun podeAgendarExato() = false
    fun intentPermissaoAlarmeExato(): Intent? = null
    companion object {
        const val EXTRA_TIPO = "extra_tipo"
        const val EXTRA_HORA = "extra_hora"
        const val EXTRA_OCCURRENCE = "occurrence"
        const val EXTRA_ACCOUNT = "account"
    }
}
