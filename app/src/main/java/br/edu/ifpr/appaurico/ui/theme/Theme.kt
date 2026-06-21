package br.edu.ifpr.appaurico.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AuricoColorScheme = lightColorScheme(
    primary = VerdeFloresta,
    onPrimary = Color.White,
    secondary = Sage,
    onSecondary = Color.White,
    tertiary = Mostarda,
    onTertiary = Color.White,
    background = FundoSage,
    onBackground = Texto,
    surface = FundoSage,
    onSurface = Texto,
    surfaceVariant = Linhas,
    onSurfaceVariant = Texto,
    outline = Linhas,
    error = AlertaSintomaAlto,
    onError = Color.White,
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
        content = content,
    )
}
