package de.pbwear.ui

import androidx.compose.ui.graphics.Color
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

private val DE: Locale = Locale.GERMANY

fun money(value: Double, iso: String? = null): String {
    val nf = NumberFormat.getCurrencyInstance(DE)
    if (!iso.isNullOrBlank()) {
        try {
            nf.currency = Currency.getInstance(iso)
        } catch (e: IllegalArgumentException) {
            // unbekannter Code: Standard (EUR) behalten
        }
    }
    return nf.format(value)
}

/** Der Server liefert Prozentwerte bereits als Prozent (z. B. 1.5 = 1,5 %). */
fun percent(value: Double): String = String.format(DE, "%+.2f %%", value)

fun quantity(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else String.format(DE, "%.2f", value)

fun perfColor(value: Double): Color = if (value < 0.0) PB.Loss else PB.Gain
