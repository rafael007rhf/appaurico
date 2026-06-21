package br.edu.ifpr.appaurico.core.cycle

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Configuracao e calculos do ciclo de estimulacao entre sessoes.
 *
 * Para o MVP os valores (inicio, duracao e horarios previstos) sao padroes locais,
 * isolados aqui para depois virarem configuraveis pelo paciente sem mexer na UI.
 */
object Ciclo {

    /** Duracao do ciclo em dias, do inicio ate o retorno. */
    const val DURACAO_DIAS = 7

    /** Horas do dia em que a estimulacao e prevista. */
    val HORARIOS = listOf(8, 12, 16, 20)

    /** Horario padrao da sessao de retorno (placeholder para a demonstracao). */
    private val HORARIO_RETORNO = LocalTime.of(9, 0)

    /**
     * Dia do ciclo em que a demonstracao abre. Intermediario de proposito: o paciente ainda
     * tem estimulacoes futuras, evitando a contradicao de "dia N de N" com proxima "amanha".
     */
    private const val DIA_ATUAL_DEMO = 5

    /** Dias ja decorridos do ciclo na abertura padrao (coloca hoje no [DIA_ATUAL_DEMO]). */
    private const val DIAS_DECORRIDOS_PADRAO = DIA_ATUAL_DEMO - 1

    /** Data de inicio do ciclo (padrao: [DIAS_DECORRIDOS_PADRAO] dias atras). */
    fun inicio(hoje: LocalDate = LocalDate.now()): LocalDate =
        hoje.minusDays(DIAS_DECORRIDOS_PADRAO.toLong())

    /** Dia atual dentro do ciclo, de 1 a [DURACAO_DIAS]. */
    fun diaAtual(agora: LocalDateTime = LocalDateTime.now()): Int {
        val decorridos = ChronoUnit.DAYS.between(inicio(agora.toLocalDate()), agora.toLocalDate())
        return (decorridos + 1).toInt().coerceIn(1, DURACAO_DIAS)
    }

    /** Proximo horario de estimulacao a partir de [agora] (hoje, ou o primeiro de amanha). */
    fun proximaEstimulacao(agora: LocalDateTime = LocalDateTime.now()): LocalDateTime {
        val hoje = agora.toLocalDate()
        val proximoHoje = HORARIOS
            .map { hoje.atTime(LocalTime.of(it, 0)) }
            .firstOrNull { it.isAfter(agora) }
        return proximoHoje ?: hoje.plusDays(1).atTime(LocalTime.of(HORARIOS.first(), 0))
    }

    /** Proxima estimulacao em epoch milissegundos, para registrar no lembrete. */
    fun proximaEstimulacaoMillis(zona: ZoneId = ZoneId.systemDefault()): Long =
        proximaEstimulacao().atZone(zona).toInstant().toEpochMilli()

    /** Quantidade de estimulacoes previstas do inicio do ciclo ate [agora]. */
    fun previstasAte(agora: LocalDateTime = LocalDateTime.now()): Int {
        var total = 0
        var dia = inicio(agora.toLocalDate())
        while (!dia.isAfter(agora.toLocalDate())) {
            HORARIOS.forEach { hora ->
                if (!dia.atTime(LocalTime.of(hora, 0)).isAfter(agora)) total++
            }
            dia = dia.plusDays(1)
        }
        return total
    }

    /** Inicio do ciclo em epoch milissegundos, para filtrar os registros do banco. */
    fun inicioMillis(zona: ZoneId = ZoneId.systemDefault()): Long =
        inicio().atStartOfDay(zona).toInstant().toEpochMilli()

    /** Data e hora da sessao de retorno: fim do ciclo a partir do inicio. */
    fun retorno(hoje: LocalDate = LocalDate.now()): LocalDateTime =
        inicio(hoje).plusDays(DURACAO_DIAS.toLong()).atTime(HORARIO_RETORNO)

    /** Sessao de retorno em epoch milissegundos, para agendar o alarme. */
    fun retornoMillis(zona: ZoneId = ZoneId.systemDefault()): Long =
        retorno().atZone(zona).toInstant().toEpochMilli()
}
