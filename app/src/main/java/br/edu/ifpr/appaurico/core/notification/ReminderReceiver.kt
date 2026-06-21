package br.edu.ifpr.appaurico.core.notification

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import br.edu.ifpr.appaurico.MainActivity
import br.edu.ifpr.appaurico.domain.model.ReminderType
import br.edu.ifpr.appaurico.ui.navigation.Routes

/**
 * Recebe o alarme do [ReminderScheduler] e emite a notificacao do lembrete.
 * Ao tocar a notificacao, o app abre na tela certa (estimulacao ou agenda).
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val tipo = intent.getStringExtra(ReminderScheduler.EXTRA_TIPO)
            ?.let(ReminderType::valueOf)
            ?: ReminderType.ESTIMULACAO

        when (tipo) {
            ReminderType.ESTIMULACAO -> {
                Notificacoes.notificarEstimulacao(context, abrirApp(context, Routes.REMINDER, REQ_ABRIR_ESTIMULACAO))
                // Estimulacao se repete: reagenda o mesmo horario para o proximo dia.
                val hora = intent.getIntExtra(ReminderScheduler.EXTRA_HORA, -1)
                if (hora >= 0) ReminderScheduler(context.applicationContext).agendarEstimulacaoSlot(hora)
            }

            ReminderType.RETORNO -> {
                Notificacoes.notificarRetorno(context, abrirApp(context, Routes.AGENDA, REQ_ABRIR_RETORNO))
            }
        }
    }

    private fun abrirApp(context: Context, rota: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP,
            )
            putExtra(MainActivity.EXTRA_ROTA, rota)
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private companion object {
        const val REQ_ABRIR_ESTIMULACAO = 9001
        const val REQ_ABRIR_RETORNO = 9002
    }
}
