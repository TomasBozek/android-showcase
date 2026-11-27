package com.example.androidproject1.core.ui.component.field

import android.telephony.PhoneNumberUtils
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.example.androidproject1.core.ui.R
import com.example.androidproject1.core.ui.common.ComponentPreview
import com.example.androidproject1.core.ui.common.DigitsTransformation
import com.example.androidproject1.core.ui.common.ThemedComponentPreview
import com.example.androidproject1.core.ui.component.content.AppText
import com.example.androidproject1.core.ui.component.content.TextRole
import com.example.androidproject1.core.ui.component.overlay.AppPickerSheet
import com.example.androidproject1.core.ui.theme.AppTheme
import java.util.Locale

/**
 * A phone number and the country it is dialled in. The country sits inside the field's frame, as
 * its flag and dial code, and opens an [AppPickerSheet] of every region the field knows, named in
 * the reader's language; the number is typed on the phone keyboard and drawn formatted for that
 * region — `777 123 456` in Czechia, `(202) 555-0123` in the US — by the platform's own
 * `PhoneNumberUtils`, while the value keeps the digits alone.
 *
 * `ContentType.PhoneNumber` lets the platform fill it. What a server wants is
 * [AppPhoneFieldState.international], `+420777123456`; changing the country keeps the digits.
 * [regionTestTag] is the id every row of the sheet carries.
 */
@Composable
fun AppPhoneField(
    value: AppPhoneFieldState,
    onValueChange: (AppPhoneFieldState) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    errorText: String? = null,
    enabled: Boolean = true,
    imeAction: ImeAction = ImeAction.Default,
    onImeAction: (() -> Unit)? = null,
    regionTestTag: String? = null,
) {
    val locale = LocalConfiguration.current.locales[0]
    val regions = remember(locale) { phoneRegions(locale) }
    var picking by rememberSaveable { mutableStateOf(false) }
    val formatting = remember(value.region) {
        DigitsTransformation { digits ->
            if (digits.isEmpty()) digits else PhoneNumberUtils.formatNumber(digits, value.region) ?: digits
        }
    }
    val current = regions.firstOrNull { it.iso == value.region }
    val chooseRegion = stringResource(R.string.app_phone_region)

    AppTextField(
        value = value.number,
        onValueChange = { typed -> onValueChange(value.copy(number = typed.filter(Char::isDigit).take(MAX_DIGITS))) },
        label = label,
        errorText = errorText,
        enabled = enabled,
        keyboardType = KeyboardType.Phone,
        imeAction = imeAction,
        onImeAction = onImeAction,
        contentType = ContentType.PhoneNumber,
        visualTransformation = formatting,
        modifier = modifier,
        leading = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.inline.xs),
                modifier = Modifier
                    .defaultMinSize(
                        minWidth = AppTheme.density.minTouchTarget,
                        minHeight = AppTheme.density.minTouchTarget,
                    )
                    .clickable(enabled = enabled, role = Role.DropdownList) { picking = true }
                    .semantics {
                        contentDescription =
                            "$chooseRegion, ${current?.name ?: value.region} ${value.dialCode}"
                    },
            ) {
                AppText(text = "${flag(value.region)} ${value.dialCode}", role = TextRole.Body)
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = AppTheme.colors.textSecondary,
                    modifier = Modifier.size(AppTheme.icons.sm),
                )
            }
        },
    )

    if (picking) {
        AppPickerSheet(
            options = regions.map { "${flag(it.iso)} ${it.name} (+${it.dialCode})" },
            selectedIndex = regions.indexOfFirst { it.iso == value.region },
            onSelect = { index -> onValueChange(value.copy(region = regions[index].iso)) },
            onDismiss = { picking = false },
            title = chooseRegion,
            optionTestTag = regionTestTag,
        )
    }
}

/**
 * What [AppPhoneField] holds: an ISO 3166 [region] and the national [number], digits only. The
 * formatting is drawn and the dial code is looked up, so neither is ever stored twice.
 */
@Immutable
data class AppPhoneFieldState(
    val region: String = defaultPhoneRegion(),
    val number: String = "",
) {

    /** The region's calling code with its plus, `+420`; empty for a region the field does not know. */
    val dialCode: String get() = DIAL_CODES[region]?.let { "+$it" }.orEmpty()

    /** The number as a server takes it, E.164: `+420777123456`. Empty while no number is typed. */
    val international: String get() = if (number.isEmpty()) "" else dialCode + number.trimStart('0')

    companion object {

        val PREVIEW = AppPhoneFieldState(region = "CZ", number = "777123456")
    }
}

/** A region the field offers: its ISO code, its name in the reader's language and its calling code. */
@Immutable
data class PhoneRegion(val iso: String, val name: String, val dialCode: String)

/** Every region the field knows, named in [locale] and sorted by that name. */
fun phoneRegions(locale: Locale): List<PhoneRegion> =
    DIAL_CODES.map { (iso, code) ->
        PhoneRegion(iso, Locale.Builder().setRegion(iso).build().getDisplayCountry(locale), code)
    }
        .sortedBy { it.name }

/** The device's own region when the field knows it, else the US. */
fun defaultPhoneRegion(): String = Locale.getDefault().country.takeIf { it in DIAL_CODES } ?: "US"

/** The region's flag: its two letters as regional-indicator symbols, which a font draws as one flag. */
private fun flag(iso: String): String =
    if (iso.length !=
        2
    ) {
        iso
    } else {
        iso.uppercase().map { Character.toChars(FLAG_OFFSET + it.code) }.joinToString("") { String(it) }
    }

private const val FLAG_OFFSET = 0x1F1E6 - 'A'.code

/** E.164 allows fifteen digits including the calling code; a national number never needs more. */
private const val MAX_DIGITS = 15

/**
 * Calling codes (ITU-T E.164) for the regions the field offers. `Locale` names a region but has no
 * dialling code, and the framework keeps its table private, so the table is here: Europe whole,
 * and the rest of the world where a shop is likely to ship.
 */
private val DIAL_CODES: Map<String, String> = mapOf(
    "AD" to "376", "AE" to "971", "AL" to "355", "AM" to "374", "AR" to "54", "AT" to "43",
    "AU" to "61", "AZ" to "994", "BA" to "387", "BE" to "32", "BG" to "359", "BR" to "55",
    "BY" to "375", "CA" to "1", "CH" to "41", "CL" to "56", "CN" to "86", "CO" to "57",
    "CY" to "357", "CZ" to "420", "DE" to "49", "DK" to "45", "EE" to "372", "EG" to "20",
    "ES" to "34", "FI" to "358", "FR" to "33", "GB" to "44", "GE" to "995", "GR" to "30",
    "HK" to "852", "HR" to "385", "HU" to "36", "ID" to "62", "IE" to "353", "IL" to "972",
    "IN" to "91", "IS" to "354", "IT" to "39", "JP" to "81", "KE" to "254", "KR" to "82",
    "KZ" to "7", "LI" to "423", "LT" to "370", "LU" to "352", "LV" to "371", "MA" to "212",
    "MC" to "377", "MD" to "373", "ME" to "382", "MK" to "389", "MT" to "356", "MX" to "52",
    "MY" to "60", "NG" to "234", "NL" to "31", "NO" to "47", "NZ" to "64", "PE" to "51",
    "PH" to "63", "PL" to "48", "PT" to "351", "RO" to "40", "RS" to "381", "SA" to "966",
    "SE" to "46", "SG" to "65", "SI" to "386", "SK" to "421", "SM" to "378", "TH" to "66",
    "TR" to "90", "TW" to "886", "UA" to "380", "US" to "1", "VN" to "84", "ZA" to "27",
)

@ComponentPreview
@Composable
private fun Preview() = ThemedComponentPreview {
    AppPhoneField(value = AppPhoneFieldState(region = "CZ"), onValueChange = {}, label = "Phone")
    AppPhoneField(value = AppPhoneFieldState.PREVIEW, onValueChange = {}, label = "Phone")
    AppPhoneField(
        value = AppPhoneFieldState(region = "US", number = "2025550123"),
        onValueChange = {},
        label = "Phone",
    )
    AppPhoneField(
        value = AppPhoneFieldState(region = "GB", number = "79"),
        onValueChange = {},
        label = "Phone",
        errorText = "Too short to be a number",
    )
}
