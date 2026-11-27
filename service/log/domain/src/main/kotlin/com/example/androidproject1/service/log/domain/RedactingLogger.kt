package com.example.androidproject1.service.log.domain

/**
 * A [Logger] that masks personal data before a line reaches [delegate]: an email address
 * becomes `<email>`, a bearer token or a JSON `…token` field `<token>`, a card number `<card>`.
 *
 * Bound outermost in `coreModule`, so whatever sits under it — logcat, the log file, the error
 * tracker's breadcrumbs — only ever sees the masked line. The message is still built lazily: a
 * line below the delegate's level is neither built nor scanned.
 *
 * A [Throwable] passes through as it is, so the tracker still groups a crash by its type; a
 * message that carries personal data belongs in the line, where it is masked, not in the exception.
 */
public class RedactingLogger(private val delegate: Logger) : Logger {

    override fun withTag(tag: String): Logger = RedactingLogger(delegate.withTag(tag))

    override fun d(throwable: Throwable?, message: () -> String): Unit = delegate.d(throwable) { redact(message()) }

    override fun w(throwable: Throwable?, message: () -> String): Unit = delegate.w(throwable) { redact(message()) }

    override fun e(throwable: Throwable?, message: () -> String): Unit = delegate.e(throwable) { redact(message()) }

    public companion object {

        private val EMAIL = Regex("""[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}""")
        private val BEARER = Regex("""(?i)(bearer\s+)[A-Za-z0-9._~+/=-]+""")
        private val JSON_TOKEN = Regex("""(?i)("[a-z_]*token"\s*:\s*")[^"]*(")""")

        // Thirteen to nineteen digits, optionally grouped by single spaces or dashes; only a run
        // that passes the Luhn check is masked, so an id or a timestamp of that length is kept.
        private val CARD = Regex("""(?<![\d-])\d(?:[ -]?\d){12,18}(?![\d-])""")

        /** [line] with every address, token and card number in it masked. */
        public fun redact(line: String): String = line
            .replace(BEARER) { it.groupValues[1] + "<token>" }
            .replace(JSON_TOKEN) { it.groupValues[1] + "<token>" + it.groupValues[2] }
            .replace(EMAIL, "<email>")
            .replace(CARD) { if (luhn(it.value.filter(Char::isDigit))) "<card>" else it.value }

        private fun luhn(digits: String): Boolean = digits.reversed()
            .mapIndexed { i, c ->
                val d = c - '0'
                if (i % 2 == 1) (d * 2).let { if (it > 9) it - 9 else it } else d
            }
            .sum() % 10 == 0
    }
}
