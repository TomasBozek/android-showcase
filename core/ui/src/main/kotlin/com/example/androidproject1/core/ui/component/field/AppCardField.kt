package com.example.androidproject1.core.ui.component.field

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.example.androidproject1.core.ui.R
import com.example.androidproject1.core.ui.common.ComponentPreview
import com.example.androidproject1.core.ui.common.DigitsTransformation
import com.example.androidproject1.core.ui.common.ThemedComponentPreview
import com.example.androidproject1.core.ui.common.grouped
import com.example.androidproject1.core.ui.theme.AppTheme
import java.time.YearMonth

/**
 * A payment card: the number, the expiry and the security code, as three [AppTextField]s that
 * each carry their autofill hint — `CreditCardNumber`, `CreditCardExpirationDate`,
 * `CreditCardSecurityCode` — so the platform can fill all three from a saved card.
 *
 * Only digits are kept, and no more than the brand allows: sixteen for Visa and Mastercard,
 * fifteen for Amex, whose code is four. The number is drawn in groups of four, the expiry as
 * `MM/YY`, the code as dots; the brand the first digits name sits at the end of the number.
 *
 * **It checks what it can without a server** — the Luhn digit once the number is whole, and an
 * expiry that is a real month not already over ([today] is the month it compares against, a
 * parameter so a test can hold it still). Each is the field's error, in words; a card the bank
 * declines is the screen's alert, not this. [AppCardFieldState.isComplete] is what a pressed
 * Continue checks. [onDone] runs from the code's `Done` key, the last of the three.
 *
 * [numberTestTag], [expiryTestTag] and [cvcTestTag] are the three inputs' ids, so a flow types
 * into each by id — `checkoutPayment_cardNumberField`, `…_expiryField`, `…_cvcField`.
 */
@Composable
fun AppCardField(
    value: AppCardFieldState,
    onValueChange: (AppCardFieldState) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    today: YearMonth = remember { YearMonth.now() },
    onDone: (() -> Unit)? = null,
    numberTestTag: String? = null,
    expiryTestTag: String? = null,
    cvcTestTag: String? = null,
) {
    val numberTransformation = remember { DigitsTransformation(::grouped) }
    val expiryTransformation = remember { DigitsTransformation(::expiryShown) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.stack.sm),
    ) {
        AppTextField(
            value = value.number,
            onValueChange = { typed ->
                val digits = typed.filter(Char::isDigit)
                // The brand is read off what was typed, so a 3 that turns the card into an Amex
                // shortens the number the same keystroke.
                val length = AppCardFieldState(number = digits).numberLength
                onValueChange(value.copy(number = digits.take(length)))
            },
            label = stringResource(R.string.app_card_number),
            errorText = value.numberProblem()?.let { stringResource(it.message()) },
            enabled = enabled,
            keyboardType = KeyboardType.Number,
            suffix = value.brand?.label,
            imeAction = ImeAction.Next,
            contentType = ContentType.CreditCardNumber,
            visualTransformation = numberTransformation,
            modifier = Modifier.fillMaxWidth().then(numberTestTag?.let { Modifier.testTag(it) } ?: Modifier),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.inline.md)) {
            AppTextField(
                value = value.expiry,
                onValueChange = { typed ->
                    onValueChange(value.copy(expiry = typed.filter(Char::isDigit).take(EXPIRY_LENGTH)))
                },
                label = stringResource(R.string.app_card_expiry),
                placeholder = stringResource(R.string.app_card_expiry_hint),
                errorText = value.expiryProblem(today)?.let { stringResource(it.message()) },
                enabled = enabled,
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next,
                contentType = ContentType.CreditCardExpirationDate,
                visualTransformation = expiryTransformation,
                modifier = Modifier.weight(1f).then(expiryTestTag?.let { Modifier.testTag(it) } ?: Modifier),
            )
            AppTextField(
                value = value.code,
                onValueChange = { typed ->
                    onValueChange(value.copy(code = typed.filter(Char::isDigit).take(value.codeLength)))
                },
                label = stringResource(R.string.app_card_code),
                enabled = enabled,
                keyboardType = KeyboardType.NumberPassword,
                imeAction = ImeAction.Done,
                onImeAction = onDone,
                contentType = ContentType.CreditCardSecurityCode,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.weight(1f).then(cvcTestTag?.let { Modifier.testTag(it) } ?: Modifier),
            )
        }
    }
}

/**
 * What [AppCardField] holds: the card's digits and nothing else. [number] is up to the brand's
 * length, [expiry] is `MMYY`, [code] is the three or four digits on the back — the grouping and
 * the slash are drawn, never stored, so what a caller sends is what it reads.
 *
 * The rules a view model checks when Continue is pressed live here rather than in the field, so a
 * screen and its test ask the same question: [isComplete]. The button itself stays enabled.
 */
@Immutable
data class AppCardFieldState(
    val number: String = "",
    val expiry: String = "",
    val code: String = "",
) {

    /** The brand the first digits name, or null while they name none. */
    val brand: CardBrand? get() = CardBrand.of(number)

    /** How many digits the number takes: the brand's, else the common sixteen. */
    val numberLength: Int get() = brand?.numberLength ?: CARD_NUMBER_LENGTH

    /** How many digits the code takes: four on the front of an Amex, three on the back of the rest. */
    val codeLength: Int get() = brand?.codeLength ?: CARD_CODE_LENGTH

    /** What is wrong with the number once all of it is typed; nothing while it is still being typed. */
    fun numberProblem(): CardProblem? =
        if (number.length == numberLength && !luhnValid(number)) CardProblem.InvalidNumber else null

    /** What is wrong with the expiry once both halves are typed. A card is good to the end of its month. */
    fun expiryProblem(today: YearMonth = YearMonth.now()): CardProblem? {
        if (expiry.length < EXPIRY_LENGTH) return null
        val month = expiry.take(2).toInt()
        if (month !in 1..MONTHS) return CardProblem.InvalidExpiry
        val expires = YearMonth.of(CENTURY + expiry.drop(2).toInt(), month)
        return if (expires < today) CardProblem.PastExpiry else null
    }

    /** Every part typed in full and none of them wrong — what a pressed Continue checks. */
    fun isComplete(today: YearMonth = YearMonth.now()): Boolean =
        number.length == numberLength &&
            numberProblem() == null &&
            expiry.length == EXPIRY_LENGTH &&
            expiryProblem(today) == null &&
            code.length == codeLength

    companion object {

        val PREVIEW = AppCardFieldState(number = "4242424242424242", expiry = "1230", code = "123")
    }
}

/** The brands a number's first digits give away, and how long each one's number and code are. */
enum class CardBrand(val label: String, val numberLength: Int, val codeLength: Int) {
    Visa("Visa", CARD_NUMBER_LENGTH, CARD_CODE_LENGTH),
    Mastercard("Mastercard", CARD_NUMBER_LENGTH, CARD_CODE_LENGTH),
    Amex("Amex", AMEX_NUMBER_LENGTH, AMEX_CODE_LENGTH),
    ;

    companion object {

        /** Visa is 4; Mastercard 51–55 or 2221–2720; Amex 34 or 37. Null while the digits name none. */
        fun of(digits: String): CardBrand? = when {
            digits.startsWith("4") -> Visa
            digits.startsWith("34") || digits.startsWith("37") -> Amex
            digits.take(2).toIntOrNull() in 51..55 -> Mastercard
            digits.length >= 4 && digits.take(4).toInt() in 2221..2720 -> Mastercard
            else -> null
        }
    }
}

/** Why a card cannot be charged, as far as the field can tell without asking anyone. */
enum class CardProblem { InvalidNumber, InvalidExpiry, PastExpiry }

/**
 * The Luhn check: every second digit from the right doubled, the digits of the lot summed, and the
 * sum a multiple of ten. It catches one mistyped digit and most swapped pairs — not a real card.
 */
fun luhnValid(digits: String): Boolean {
    if (digits.isEmpty() || digits.any { !it.isDigit() }) return false
    val sum = digits.reversed().mapIndexed { index, char ->
        val digit = char.digitToInt()
        if (index % 2 == 1) (digit * 2).let { if (it > 9) it - 9 else it } else digit
    }.sum()
    return sum % 10 == 0
}

private const val CARD_NUMBER_LENGTH = 16
private const val CARD_CODE_LENGTH = 3
private const val AMEX_NUMBER_LENGTH = 15
private const val AMEX_CODE_LENGTH = 4
internal const val EXPIRY_LENGTH = 4
private const val MONTHS = 12
private const val CENTURY = 2000

/** `0826` as `08/26`, the slash appearing once the month is typed. */
private fun expiryShown(digits: String): String =
    if (digits.length > 2) digits.take(2) + "/" + digits.drop(2) else digits

private fun CardProblem.message(): Int = when (this) {
    CardProblem.InvalidNumber -> R.string.app_card_number_invalid
    CardProblem.InvalidExpiry -> R.string.app_card_expiry_invalid
    CardProblem.PastExpiry -> R.string.app_card_expiry_past
}

@ComponentPreview
@Composable
private fun Preview() = ThemedComponentPreview {
    val today = YearMonth.of(2026, 9)
    AppCardField(value = AppCardFieldState(), onValueChange = {}, today = today)
    AppCardField(value = AppCardFieldState.PREVIEW, onValueChange = {}, today = today)
    AppCardField(
        value = AppCardFieldState(number = "378282246310005", expiry = "0127", code = "1234"),
        onValueChange = {},
        today = today,
    )
    AppCardField(
        value = AppCardFieldState(number = "4242424242424241", expiry = "0826", code = "12"),
        onValueChange = {},
        today = today,
    )
}
