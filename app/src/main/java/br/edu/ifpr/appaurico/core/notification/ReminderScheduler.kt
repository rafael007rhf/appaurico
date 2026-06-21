package br.edu.ifpr.appaurico.core.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import br.edu.ifpr.appaurico.core.cycle.Ciclo
import br.edu.ifpr.appaurico.domain.model.Reminder
import br.edu.ifpr.appaurico.domain.model.ReminderType
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject

/**
 * Agenda os lembretes no AlarmManager.
 *
 * - ESTIMULACAO: um alarme exato por horario do ciclo ([Ciclo.HORARIOS]), reagendado
 *   para o dia seguinte assim que dispara (o receiver chama [agendarEstimulacaoSlot]).
 * - RETORNO: um alarme unico no momento configurado.
 *
 * Quando o app nao tem permissao de alarme exato, cai para alarme inexato (o sistema
 * pode atrasar um pouco), mantendo o lembrete funcional.
 */
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    /** Cancela tudo e reagenda apenas os lembretes ativos. Usado no inicio e ao alterar a agenda. */
    fun reagendarTodos(lembretes: List<Reminder>) {
        cancelarTodos()
        lembretes.filter { it.ativo }.forEach(::agendar)
    }

    private fun agendar(lembrete: Reminder) {
        when (lembrete.tipo) {
            ReminderType.ESTIMULACAO -> Ciclo.HORARIOS.forEach(::agendarEstimulacaoSlot)
            ReminderType.RETORNO -> agendar(
                requestCode = REQ_RETORNO,
                quandoMillis = lembrete.horario,
                tipo = ReminderType.RETORNO,
                hora = null,
            )
        }
    }

    /** (Re)agenda a estimulacao de um horario do dia para a proxima ocorrencia futura. */
    fun agendarEstimulacaoSlot(hora: Int) {
        agendar(
            requestCode = REQ_ESTIMULACAO_BASE + hora,
            quandoMillis = proximaOcorrenciaMillis(hora),
            tipo = ReminderType.ESTIMULACAO,
            hora = hora,
        )
    }

    fun cancelarTodos() {
        Ciclo.HORARIOS.forEach { hora ->
            alarmManager.cancel(
                pendingIntentAlarme(REQ_ESTIMULACAO_BASE + hora, ReminderType.ESTIMULACAO, hora),
            )
        }
        alarmManager.cancel(pendingIntentAlarme(REQ_RETORNO, ReminderType.RETORNO, null))
    }

    private fun agendar(requestCode: Int, quandoMillis: Long, tipo: ReminderType, hora: Int?) {
        val pi = pendingIntentAlarme(requestCode, tipo, hora)
        if (podeAgendarExato()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, quandoMillis, pi)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, quandoMillis, pi)
        }
    }

    private fun pendingIntentAlarme(requestCode: Int, tipo: ReminderType, hora: Int?): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(EXTRA_TIPO, tipo.name)
            hora?.let { putExtra(EXTRA_HORA, it) }
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun proximaOcorrenciaMillis(hora: Int): Long {
        val agora = LocalDateTime.now()
        var alvo = agora.toLocalDate().atTime(hora, 0)
        if (!alvo.isAfter(agora)) alvo = alvo.plusDays(1)
        return alvo.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    fun podeAgendarExato(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }

    /** Intent para o usuario liberar alarmes exatos nas configuracoes, ou null se ja liberados. */
    fun intentPermissaoAlarmeExato(): Intent? =
        if (!podeAgendarExato()) {
            Intent(
                Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                Uri.fromParts("package", context.packageName, null),
            )
        } else {
            null
        }

    companion object {
        const val EXTRA_TIPO = "extra_tipo"
        const val EXTRA_HORA = "extra_hora"

        private const val REQ_ESTIMULACAO_BASE = 100
        private const val REQ_RETORNO = 200
    }
}
