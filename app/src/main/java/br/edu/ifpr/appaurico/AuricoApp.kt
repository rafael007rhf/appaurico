package br.edu.ifpr.appaurico

import android.app.Application
import br.edu.ifpr.appaurico.core.notification.Notificacoes
import br.edu.ifpr.appaurico.core.notification.ReminderScheduler
import br.edu.ifpr.appaurico.data.repository.ReminderRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class AuricoApp : Application() {

    @Inject lateinit var reminderRepository: ReminderRepository
    @Inject lateinit var reminderScheduler: ReminderScheduler

    override fun onCreate() {
        super.onCreate()
        Notificacoes.criarCanal(this)

        // Mantem o AlarmManager sincronizado com o estado persistido sempre que o app inicia.
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            val lembretes = reminderRepository.observarLembretes().first()
            reminderScheduler.reagendarTodos(lembretes)
        }
    }
}
