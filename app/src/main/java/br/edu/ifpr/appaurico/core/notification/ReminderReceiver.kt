package br.edu.ifpr.appaurico.core.notification

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import br.edu.ifpr.appaurico.BuildConfig
import br.edu.ifpr.appaurico.MainActivity
import br.edu.ifpr.appaurico.platform.PatientRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {
    @Inject lateinit var repository: PatientRepository
    @Inject lateinit var scheduler: ReminderScheduler
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                var allowed = BuildConfig.DEMO_MODE
                if (!BuildConfig.DEMO_MODE) {
                    val account = repository.session.account.first()
                    val id = intent.getStringExtra(ReminderScheduler.EXTRA_OCCURRENCE)
                    if (account != intent.getStringExtra(ReminderScheduler.EXTRA_ACCOUNT) || !repository.session.reminders.first()) return@launch
                    val slot = repository.dao.allOccurrences(account).firstOrNull { it.id == id } ?: return@launch
                    val t = repository.dao.treatment(account, slot.treatmentId) ?: return@launch
                    allowed = t.status == "active" && t.sharingActive && slot.status == "scheduled" && !slot.notified && slot.scheduledAt <= System.currentTimeMillis()
                    if (allowed) repository.dao.notified(slot.id)
                }
                if (allowed) {
                    val open = PendingIntent.getActivity(context, 9001, Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                    Notificacoes.notificarEstimulacao(context, open)
                }
                if (BuildConfig.DEMO_MODE) intent.getIntExtra(ReminderScheduler.EXTRA_HORA, -1).takeIf { it >= 0 }?.let(scheduler::agendarEstimulacaoSlot)
                else scheduler.reagendarTodos(emptyList())
            } finally { pending.finish() }
        }
    }
}
