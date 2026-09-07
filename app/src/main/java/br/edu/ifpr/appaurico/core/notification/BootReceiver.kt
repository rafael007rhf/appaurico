package br.edu.ifpr.appaurico.core.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import br.edu.ifpr.appaurico.data.repository.ReminderRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var reminderRepository: ReminderRepository
    @Inject lateinit var reminderScheduler: ReminderScheduler

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val lembretes = reminderRepository.observarLembretes().first()
                reminderScheduler.reagendarTodos(lembretes)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
