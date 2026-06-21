package br.edu.ifpr.appaurico.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import br.edu.ifpr.appaurico.R

/**
 * Ponto unico de notificacoes do app: cria o canal e emite o aviso de estimulacao.
 * Acionado pelo ReminderReceiver quando um alarme dispara.
 */
object Notificacoes {

    /** Canal usado por todos os lembretes (estimulacao e retorno). */
    const val CANAL_LEMBRETES_ID = "lembretes"

    /** Id fixo da notificacao de estimulacao: uma nova substitui a anterior. */
    const val NOTIFICACAO_ESTIMULACAO_ID = 1001

    /** Id fixo da notificacao de retorno. */
    const val NOTIFICACAO_RETORNO_ID = 1002

    /** Cria o canal de notificacao. Idempotente: chamar de novo apenas atualiza. */
    fun criarCanal(context: Context) {
        val canal = NotificationChannel(
            CANAL_LEMBRETES_ID,
            "Lembretes",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Avisos para estimular as sementes e a sessao de retorno."
        }
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(canal)
    }

    /**
     * Emite o aviso de estimulacao. Quem chama deve garantir a permissao
     * POST_NOTIFICATIONS (Android 13+); sem ela o sistema apenas ignora o aviso.
     *
     * @param contentIntent acao ao tocar a notificacao (abrir o app na tela certa).
     */
    fun notificarEstimulacao(context: Context, contentIntent: PendingIntent? = null) {
        emitir(
            context = context,
            id = NOTIFICACAO_ESTIMULACAO_ID,
            titulo = "Hora de estimular as sementes",
            texto = "Pressione cada semente por cerca de 30 segundos.",
            contentIntent = contentIntent,
        )
    }

    /** Emite o aviso da sessao de retorno. Mesmas regras de permissao da estimulacao. */
    fun notificarRetorno(context: Context, contentIntent: PendingIntent? = null) {
        emitir(
            context = context,
            id = NOTIFICACAO_RETORNO_ID,
            titulo = "Sessao de retorno",
            texto = "Voce tem retorno marcado. Toque para ver a agenda.",
            contentIntent = contentIntent,
        )
    }

    private fun emitir(
        context: Context,
        id: Int,
        titulo: String,
        texto: String,
        contentIntent: PendingIntent?,
    ) {
        val notificacao = NotificationCompat.Builder(context, CANAL_LEMBRETES_ID)
            .setSmallIcon(R.drawable.ic_notificacao)
            .setContentTitle(titulo)
            .setContentText(texto)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .apply { contentIntent?.let(::setContentIntent) }
            .build()

        NotificationManagerCompat.from(context).notify(id, notificacao)
    }
}
