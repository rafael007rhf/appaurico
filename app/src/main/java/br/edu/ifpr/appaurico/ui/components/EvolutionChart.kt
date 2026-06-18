package br.edu.ifpr.appaurico.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import br.edu.ifpr.appaurico.ui.screens.evolution.EvolutionPoint

private const val NIVEL_MAXIMO = 10f

/**
 * Grafico de linha da evolucao do sintoma.
 * Eixo vertical de 0 a 10; uma linha liga os registros em ordem e o registro mais
 * recente (hoje) aparece destacado em mostarda.
 */
@Composable
fun EvolutionChart(
    pontos: List<EvolutionPoint>,
    modifier: Modifier = Modifier,
) {
    val cores = MaterialTheme.colorScheme
    val corLinha = cores.secondary
    val corPonto = cores.primary
    val corHoje = cores.tertiary
    val corEixo = cores.outline

    Row(modifier = modifier.height(220.dp)) {
        Column(
            modifier = Modifier.fillMaxHeight().padding(end = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.End,
        ) {
            Text("10", style = MaterialTheme.typography.labelSmall)
            Text("5", style = MaterialTheme.typography.labelSmall)
            Text("0", style = MaterialTheme.typography.labelSmall)
        }

        Canvas(modifier = Modifier.fillMaxHeight().fillMaxWidth()) {
            val padV = 8.dp.toPx()
            val alturaUtil = size.height - 2 * padV

            fun y(nivel: Int): Float = padV + (1f - nivel / NIVEL_MAXIMO) * alturaUtil
            fun x(indice: Int): Float =
                if (pontos.size <= 1) size.width / 2f
                else indice * (size.width / (pontos.size - 1))

            // Linhas de grade em 0, 5 e 10.
            listOf(0, 5, 10).forEach { nivel ->
                drawLine(
                    color = corEixo,
                    start = Offset(0f, y(nivel)),
                    end = Offset(size.width, y(nivel)),
                    strokeWidth = 1.dp.toPx(),
                )
            }

            if (pontos.isEmpty()) return@Canvas

            // Linha ligando os registros.
            if (pontos.size > 1) {
                val caminho = Path().apply {
                    moveTo(x(0), y(pontos[0].nivel))
                    pontos.forEachIndexed { i, ponto -> lineTo(x(i), y(ponto.nivel)) }
                }
                drawPath(
                    path = caminho,
                    color = corLinha,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx()),
                )
            }

            // Pontos; o ultimo (hoje) em destaque.
            pontos.forEachIndexed { i, ponto ->
                val ehHoje = i == pontos.lastIndex
                drawCircle(
                    color = if (ehHoje) corHoje else corPonto,
                    radius = if (ehHoje) 7.dp.toPx() else 4.dp.toPx(),
                    center = Offset(x(i), y(ponto.nivel)),
                )
            }
        }
    }
}
