package br.edu.ifpr.appaurico.ui.components

import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import br.edu.ifpr.appaurico.R

/**
 * Ponto auricular do diagrama.
 *
 * @param nome nome do ponto (ex.: "Shenmen").
 * @param posicao posicao relativa sobre o desenho da orelha, com x e y entre 0 e 1.
 */
data class PontoAuricular(
    val nome: String,
    val posicao: Offset,
)

/** Pontos padrao usados no diagrama, na ordem em que aparecem. */
val pontosAuricularesPadrao = listOf(
    PontoAuricular("Shenmen", Offset(0.547f, 0.319f)),
    PontoAuricular("Rim", Offset(0.576f, 0.494f)),
    PontoAuricular("Ansiedade", Offset(0.485f, 0.545f)),
    PontoAuricular("Simpatico", Offset(0.526f, 0.634f)),
)

private val CorDestaque = Color(0xFFD98A3D)
private val CorBorda = Color(0xFF2E4034)

// Proporcao do vector ic_orelha (viewport 680 x 470), mantida ao exibir a imagem.
private const val PROPORCAO_ORELHA = 680f / 470f

private val RaioPonto = 7.dp
private val RaioPontoDestacado = 10.dp
private val RaioAnelMax = 26.dp

/**
 * Orelha do diagrama: a imagem vem do vector [R.drawable.ic_orelha] e os pontos
 * auriculares sao sobrepostos em Compose, posicionados por fracao da area da imagem.
 * O ponto em [indiceDestacado] aparece em mostarda com um anel de pulso animado,
 * sinalizando qual ponto estimular agora.
 */
@Composable
fun EarDiagram(
    indiceDestacado: Int,
    modifier: Modifier = Modifier,
    pontos: List<PontoAuricular> = pontosAuricularesPadrao,
) {
    val reduzirMovimento = movimentoReduzido()

    // Anel de pulso continuo ao redor do ponto destacado; parado se o usuario
    // pediu menos animacoes no sistema.
    val transicao = rememberInfiniteTransition(label = "pulso")
    val pulso by transicao.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "pulso",
    )

    BoxWithConstraints(modifier = modifier.aspectRatio(PROPORCAO_ORELHA)) {
        Image(
            painter = painterResource(R.drawable.ic_orelha),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )

        val largura = maxWidth
        val altura = maxHeight
        pontos.forEachIndexed { indice, ponto ->
            val destacado = indice == indiceDestacado
            PontoMarcador(
                destacado = destacado,
                pulso = if (destacado && !reduzirMovimento) pulso else 0f,
                animarPulso = destacado && !reduzirMovimento,
                modifier = Modifier.offset(
                    x = largura * ponto.posicao.x - RaioAnelMax,
                    y = altura * ponto.posicao.y - RaioAnelMax,
                ),
            )
        }
    }
}

@Composable
private fun PontoMarcador(
    destacado: Boolean,
    pulso: Float,
    animarPulso: Boolean,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(RaioAnelMax * 2)) {
        val centro = Offset(size.width / 2f, size.height / 2f)

        if (animarPulso) {
            drawCircle(
                color = CorDestaque.copy(alpha = (1f - pulso) * 0.6f),
                radius = RaioPontoDestacado.toPx() + pulso * (RaioAnelMax - RaioPontoDestacado).toPx(),
                center = centro,
            )
        }

        if (destacado) {
            drawCircle(
                color = CorDestaque,
                radius = RaioPontoDestacado.toPx(),
                center = centro,
            )
        } else {
            drawCircle(color = Color.White, radius = RaioPonto.toPx(), center = centro)
            drawCircle(
                color = CorBorda,
                radius = RaioPonto.toPx(),
                center = centro,
                style = Stroke(width = 2.dp.toPx()),
            )
        }
    }
}

/** True quando o sistema esta com animacoes desligadas (acessibilidade / economia). */
@Composable
private fun movimentoReduzido(): Boolean {
    val context = LocalContext.current
    return Settings.Global.getFloat(
        context.contentResolver,
        Settings.Global.ANIMATOR_DURATION_SCALE,
        1f,
    ) == 0f
}
