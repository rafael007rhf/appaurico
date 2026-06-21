package br.edu.ifpr.appaurico.core.demo

import br.edu.ifpr.appaurico.core.cycle.Ciclo
import br.edu.ifpr.appaurico.core.notification.ReminderScheduler
import br.edu.ifpr.appaurico.data.local.dao.ReminderDao
import br.edu.ifpr.appaurico.data.local.dao.StimulationDao
import br.edu.ifpr.appaurico.data.local.dao.SymptomLogDao
import br.edu.ifpr.appaurico.data.local.entity.StimulationEntity
import br.edu.ifpr.appaurico.data.local.entity.SymptomLogEntity
import br.edu.ifpr.appaurico.data.repository.ReminderRepository
import br.edu.ifpr.appaurico.domain.model.ReminderType
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import kotlin.math.roundToInt

/**
 * Popula o banco com dados plausiveis para a demonstracao ao vivo. Existe apenas no
 * source set de debug: nao entra no build de release e nunca roda sozinho — so e
 * acionado pelos controles de feira na Home.
 *
 * [carregar] e idempotente: cada bloco so insere se a respectiva tabela estiver vazia,
 * entao tocar o botao mais de uma vez nao duplica registros.
 */
class DemoSeeder @Inject constructor(
    private val symptomLogDao: SymptomLogDao,
    private val stimulationDao: StimulationDao,
    private val reminderDao: ReminderDao,
    private val reminderRepository: ReminderRepository,
    private val reminderScheduler: ReminderScheduler,
) {
    private val zona: ZoneId = ZoneId.systemDefault()

    suspend fun carregar() {
        val agora = LocalDateTime.now()
        seedSintomas(agora)
        seedEstimulacoes(agora)
        seedLembretes()
        reminderScheduler.reagendarTodos(reminderRepository.observarLembretes().first())
    }

    suspend fun limpar() {
        symptomLogDao.limparTudo()
        stimulationDao.limparTudo()
        reminderDao.limparTudo()
        reminderScheduler.cancelarTodos()
    }

    /**
     * Sete registros, um por dia nos ultimos 7 dias (o ultimo cai em hoje), comecando alto e
     * caindo para mostrar melhora. E o historico recente do sintoma, independente do dia do
     * ciclo (que abre num dia intermediario), entao nenhum registro cai no futuro.
     */
    private suspend fun seedSintomas(agora: LocalDateTime) {
        if (symptomLogDao.contar() > 0) return
        val hoje = agora.toLocalDate()
        NIVEIS.forEachIndexed { i, nivel ->
            val dia = hoje.minusDays((NIVEIS.lastIndex - i).toLong())
            symptomLogDao.inserir(
                SymptomLogEntity(
                    dataHora = dia.atTime(HORA_REGISTRO).millis(),
                    nivel = nivel,
                    nota = notaDoDia(i),
                ),
            )
        }
    }

    /** Marca ~86% dos horarios previstos do ciclo ate agora como estimulacoes feitas. */
    private suspend fun seedEstimulacoes(agora: LocalDateTime) {
        if (stimulationDao.contar() > 0) return
        val slots = slotsPrevistos(agora)
        val previstas = slots.size
        if (previstas == 0) return
        val feitas = (previstas * ADESAO_ALVO).roundToInt()
        // Distribui as faltas uniformemente (estilo Bresenham): mantem exatamente
        // [feitas] de [previstas] slots, espacando as ausencias ao longo do ciclo.
        slots.forEachIndexed { i, millis ->
            val mantido = (i * feitas) / previstas != ((i + 1) * feitas) / previstas
            if (mantido) stimulationDao.inserir(StimulationEntity(dataHora = millis))
        }
    }

    /** Lembretes padrao ativos: estimulacao nos horarios do ciclo e sessao de retorno. */
    private suspend fun seedLembretes() {
        reminderRepository.definir(ReminderType.ESTIMULACAO, ativo = true, horario = Ciclo.proximaEstimulacaoMillis())
        reminderRepository.definir(ReminderType.RETORNO, ativo = true, horario = Ciclo.retornoMillis())
    }

    /** Horarios previstos do inicio do ciclo ate [agora], em epoch millis (mesma regra do Ciclo). */
    private fun slotsPrevistos(agora: LocalDateTime): List<Long> {
        val slots = mutableListOf<Long>()
        var dia = Ciclo.inicio(agora.toLocalDate())
        while (!dia.isAfter(agora.toLocalDate())) {
            Ciclo.HORARIOS.forEach { hora ->
                val momento = dia.atTime(LocalTime.of(hora, 0))
                if (!momento.isAfter(agora)) slots.add(momento.millis())
            }
            dia = dia.plusDays(1)
        }
        return slots
    }

    private fun notaDoDia(indice: Int): String? = when (indice) {
        0 -> "Início do acompanhamento"
        NIVEIS.lastIndex -> "Bem melhor hoje"
        else -> null
    }

    private fun LocalDateTime.millis(): Long = atZone(zona).toInstant().toEpochMilli()

    private companion object {
        /** Niveis do sintoma, um por dia nos ultimos 7 dias (mais antigo -> hoje). */
        val NIVEIS = listOf(8, 7, 6, 6, 5, 5, 4)
        val HORA_REGISTRO: LocalTime = LocalTime.of(9, 0)
        const val ADESAO_ALVO = 0.857f
    }
}
