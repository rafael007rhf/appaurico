package br.edu.ifpr.appaurico.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val AuricoColorScheme = lightColorScheme(
    primary = VerdeFloresta,
    onPrimary = Color.White,
    secondary = Sage,
    onSecondary = Color.White,
    tertiary = Mostarda,
    onTertiary = Color.White,
    background = FundoSage,
    onBackground = Texto,
    // Cards e superficies elevadas usam um tom claro para destacar-se do fundo sage.
    surface = Superficie,
    onSurface = Texto,
    surfaceVariant = Superficie,
    onSurfaceVariant = TextoSuave,
    outline = Linhas,
    outlineVariant = Linhas,
    error = AlertaSintomaAlto,
    onError = Color.White,
)

// Cantos arredondados padronizados: cards e demais superficies em 16dp.
private val AuricoShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(16.dp),
)

// Tema unico e estavel: sem dynamic color, para preservar a identidade da marca
// (o app e demonstrado ao vivo numa feira).
@Composable
fun AppauricoTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = AuricoColorScheme,
        typography = Typography,
        shapes = AuricoShapes,
        content = content,
    )
}
