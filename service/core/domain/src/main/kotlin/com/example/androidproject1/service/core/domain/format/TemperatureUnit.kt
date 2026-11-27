package com.example.androidproject1.service.core.domain.format

/**
 * The two scales a temperature is read in.
 *
 * Here rather than beside `Formats`, which is where the role that writes one lives: a repository
 * in a feature's `domain` returns the unit the user picked, and a feature's `domain` is a
 * Kotlin/JVM module that cannot see an Android library. The conversion is the enum's own
 * so that nobody writes `× 9 / 5 + 32` twice.
 *
 * A forecast is stored and passed around in **degrees Celsius** — that is what the host sends and
 * what the cache holds — and converted once, on the way to the screen.
 */
public enum class TemperatureUnit {

    Celsius,
    Fahrenheit,
    ;

    /** [celsius] in this unit. Celsius returns it unchanged rather than through a no-op formula. */
    public fun fromCelsius(celsius: Double): Double = when (this) {
        Celsius -> celsius
        Fahrenheit -> celsius * FAHRENHEIT_RATIO + FAHRENHEIT_OFFSET
    }

    /** What follows the figure: `°C`, `°F`. Not translated — the symbols are the same everywhere. */
    public val symbol: String
        get() = when (this) {
            Celsius -> "°C"
            Fahrenheit -> "°F"
        }

    public companion object {

        /** What an app with no stored choice reads in. */
        public val DEFAULT: TemperatureUnit = Celsius

        private const val FAHRENHEIT_RATIO = 9.0 / 5.0
        private const val FAHRENHEIT_OFFSET = 32.0
    }
}
