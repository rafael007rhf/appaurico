package br.edu.ifpr.appaurico.ui.screens.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * Acesso discreto aos ajustes de demonstração (somente debug).
 * Uma engrenagem pequena desenhada com Canvas, sem depender de material-icons.
 */
@Composable
fun DemoSettingsButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val cor = MaterialTheme.colorScheme.onSurfaceVariant
    IconButton(
        onClick = onClick,
        modifier = modifier.semantics { contentDescription = "Abrir ajustes de demonstração" },
    ) {
        Canvas(modifier = Modifier.size(20.dp)) {
            val centro = center
            val raioExterno = size.minDimension / 2f
            val raioAnel = raioExterno * 0.62f
            val larguraTraco = raioExterno * 0.18f

            // Dentes da engrenagem
            for (i in 0 until 8) {
                val angulo = Math.toRadians(i * 45.0)
                val inicio = Offset(
                    centro.x + (raioAnel * cos(angulo)).toFloat(),
                    centro.y + (raioAnel * sin(angulo)).toFloat(),
                )
                val fim = Offset(
                    centro.x + (raioExterno * cos(angulo)).toFloat(),
                    centro.y + (raioExterno * sin(angulo)).toFloat(),
                )
                drawLine(cor, inicio, fim, strokeWidth = larguraTraco)
            }

            drawCircle(cor, radius = raioAnel, style = Stroke(width = larguraTraco))
            drawCircle(cor, radius = raioAnel * 0.35f)
        }
    }
}
