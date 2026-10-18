package de.pbwear.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Typography
import de.pbwear.R

/** Farben (Original-App-Palette, Dark Mode) und Schrift aus der Original-App. */
object PB {
    val Red = Color(0xFFEE0000)        // @color/primary
    val DarkRed = Color(0xFFB30000)
    val Green = Color(0xFF04B728)      // @color/green
    val Loss = Color(0xFFFF5A5A)       // helleres Rot für Text auf Schwarz
    val Gain = Color(0xFF3DDC63)       // helleres Grün für Text auf Schwarz
    val Gray = Color(0xFF2B2B2B)       // Flächen (Chips, Eingabefelder)
    val Text = Color.White
    val HintGray = Color(0xFF8A8A8A)
    val TextGray = Color(0xFFB5B5B5)
}

val SparkasseFont = FontFamily(
    Font(R.font.sparkasse_regular, FontWeight.Normal),
    Font(R.font.sparkasse_bold, FontWeight.Bold),
)

@Composable
fun PBTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colors = Colors(
            primary = PB.Red,
            primaryVariant = PB.DarkRed,
            secondary = PB.Red,
            secondaryVariant = PB.DarkRed,
            background = Color.Black,
            surface = PB.Gray,
            error = PB.Loss,
            onPrimary = Color.White,
            onSecondary = Color.White,
            onBackground = Color.White,
            onSurface = Color.White,
            onSurfaceVariant = PB.TextGray,
            onError = Color.Black,
        ),
        typography = Typography(defaultFontFamily = SparkasseFont),
    ) {
        // Dark Mode: reines Schwarz (spart Akku auf OLED)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {
            content()
        }
    }
}
