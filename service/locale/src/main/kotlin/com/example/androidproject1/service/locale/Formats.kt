package com.example.androidproject1.service.locale

import com.example.androidproject1.service.core.domain.format.TemperatureUnit
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.time.Duration

/**
 * The forms a number takes when a person reads it.
 *
 * A role, in the same sense as a colour role or a type role: a screen asks for `money` rather than
 * reaching for `NumberFormat`, so a price is written the same way in the catalog, the cart and the
 * receipt, and a locale that groups with a space and decimals with a comma gets both without
 * anyone remembering to ask. What this replaced was two copies of the same `Price.kt`, one in the
 * catalog and one in the cart, each with its own rounding.
 *
 * Everything is derived from [locale], and nothing here is a `@Composable` — the same instance
 * formats a string for a screen, a test or a notification. Read it from composition with
 * `LocalFormats.current`; construct one directly in a test.
 *
 * **Tabular figures are not this class's job.** Lining a column of prices up is a type role —
 * `TextRole.Numeric` — because it is about the glyphs, not the value.
 */
public class Formats(public val locale: Locale = Locale.getDefault()) {

    /**
     * A price held in **minor units** — 124850 becomes `1 248,50 Kč`, `$1,248.50`, and so on.
     *
     * Minor units rather than a `Double` because money is counted, not measured, and the scale
     * comes from the currency rather than a hardcoded 2: not every currency has two decimal
     * places. A negative amount keeps its minus sign; no locale here writes a debt in brackets.
     */
    public fun money(minorUnits: Long): String {
        val format = NumberFormat.getCurrencyInstance(locale)
        return format.format(BigDecimal.valueOf(minorUnits, format.fractionDigits))
    }

    /**
     * The same amount with the fraction dropped — `1 249 Kč`, `$1,249`.
     *
     * For a badge, a chart axis or a summary row, where two decimal places are noise. Rounds half
     * up, so it is a display form and never the number an invoice is built from.
     */
    public fun moneyShort(minorUnits: Long): String {
        val format = NumberFormat.getCurrencyInstance(locale)
        val digits = format.fractionDigits
        format.maximumFractionDigits = 0
        format.minimumFractionDigits = 0
        format.roundingMode = RoundingMode.HALF_UP
        return format.format(BigDecimal.valueOf(minorUnits, digits))
    }

    /** A weight held in grams, written in kilograms to the gram: 420 becomes `0,420 kg`. */
    public fun weight(grams: Long): String {
        val format = NumberFormat.getNumberInstance(locale).apply {
            minimumFractionDigits = GRAM_DIGITS
            maximumFractionDigits = GRAM_DIGITS
        }
        return "${format.format(BigDecimal.valueOf(grams, GRAM_DIGITS))} $KILOGRAM"
    }

    /** A count, grouped the way the locale groups: 1248 becomes `1 248`. */
    public fun quantity(count: Long): String = NumberFormat.getIntegerInstance(locale).format(count)

    /**
     * A fraction as a percentage: `0.21` becomes `21 %` or `21%`, whichever the locale writes.
     *
     * A fraction rather than an already-multiplied number, so that a caller cannot pass 21 and get
     * 2 100 %.
     */
    public fun percent(fraction: Double): String = NumberFormat.getPercentInstance(locale).format(fraction)

    /**
     * A temperature held in **degrees Celsius**, written in [unit] to the whole degree:
     * `21 °C`, `70 °F`.
     *
     * Celsius on the way in because that is what the host sends and what the cache holds; the
     * unit is a preference, converted here rather than asked of the server, so switching it
     * costs a multiplication instead of a round trip and works with no network at all.
     *
     * A whole degree because a forecast is not a measurement to the tenth, and the figure and
     * its symbol are joined by a non-breaking space so a row that wraps never leaves `°C`
     * stranded on the next line.
     */
    public fun temperature(celsius: Double, unit: TemperatureUnit): String {
        val format = NumberFormat.getIntegerInstance(locale).apply { roundingMode = RoundingMode.HALF_UP }
        // Zero rather than -0: at -0.2 °C the integer instance writes "-0", which reads as a
        // scale nobody uses. The rounded value decides, not the sign of the input.
        val rounded = format.format(unit.fromCelsius(celsius)).let { if (it == NEGATIVE_ZERO) "0" else it }
        return "$rounded$NON_BREAKING_SPACE${unit.symbol}"
    }

    /** A wall-clock time in the locale's short form: `14:05`, `2:05 PM`. */
    public fun time(time: LocalTime): String =
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale).format(time)

    /** A date in the locale's medium form: `9. 9. 2026`, `Sep 9, 2026`. */
    public fun date(date: LocalDate): String =
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale).format(date)

    /**
     * A month as the header of a group: the standalone name and the year — `September
     * 2026`, `září 2026`. Standalone (`LLLL`) because a header is not inside a sentence: the
     * declined form (`MMMM`) is for dates in running text, and would make May `května` rather than
     * `květen`.
     */
    public fun month(month: YearMonth): String =
        DateTimeFormatter.ofPattern("LLLL yyyy", locale).format(month)

    /**
     * An elapsed or remaining span, largest two units: `2 h 10 m`, `45 m`, `1 d 3 h`, `0 m`.
     *
     * The unit letters are not translated. They are the same in every locale this template ships,
     * and a duration that reads `2 h 10 m` everywhere is easier to scan in a log or a list than
     * one whose shape changes with the language. Translate it the day a locale needs it, in a
     * `core_*` plural.
     */
    public fun duration(duration: Duration): String {
        val negative = duration.isNegative()
        val parts = duration.absoluteValue.toComponents { days, hours, minutes, seconds, _ ->
            listOf(days to "d", hours.toLong() to "h", minutes.toLong() to "m", seconds.toLong() to "s")
        }
        val from = parts.indexOfFirst { it.first > 0 }.takeIf { it >= 0 } ?: MINUTES_INDEX
        val text = parts.subList(from, minOf(from + UNITS_SHOWN, parts.size))
            .joinToString(" ") { (value, unit) -> "${quantity(value)} $unit" }
        return if (negative) "-$text" else text
    }

    /**
     * What the currency itself says the scale is, defaulting to 2 when the locale names no
     * currency at all — `Locale("cs")` without a country does exactly that.
     */
    private val NumberFormat.fractionDigits: Int
        get() = currency?.defaultFractionDigits?.coerceAtLeast(0) ?: DEFAULT_FRACTION_DIGITS

    private companion object {

        const val DEFAULT_FRACTION_DIGITS = 2
        const val GRAM_DIGITS = 3
        const val KILOGRAM = "kg"
        const val NON_BREAKING_SPACE = '\u00A0'
        const val NEGATIVE_ZERO = "-0"

        /** Where `duration` starts when every component is zero, so that it reads `0 m`. */
        const val MINUTES_INDEX = 2
        const val UNITS_SHOWN = 2
    }
}
