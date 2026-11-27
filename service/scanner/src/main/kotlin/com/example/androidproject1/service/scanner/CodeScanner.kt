package com.example.androidproject1.service.scanner

/** What a code is, where the scanner says. */
public enum class CodeFormat { Qr, Ean13, Ean8, UpcA, UpcE, Code128, Code39, Other }

/** How a scan ended. */
public sealed interface ScanResult {

    /** A code was read. [value] is its text exactly as encoded. */
    public data class Code(val value: String, val format: CodeFormat = CodeFormat.Other) : ScanResult

    /** The user closed the scanner without reading anything. Not an error. */
    public data object Cancelled : ScanResult

    /** It could not be shown: no Play services, an old one, the module not yet downloaded, or a camera fault. Ask for the code to be typed. */
    public data object Unavailable : ScanResult
}

/**
 * Read one barcode or QR code. Google's code scanner draws its own screen, so this needs **no
 * camera permission** and no key — but it needs Play services, and a device without them answers
 * [ScanResult.Unavailable].
 */
public interface CodeScanner {

    public suspend fun scan(): ScanResult
}

/**
 * A scanner that reads the same code every time, with no camera and no Play services — for a build
 * with nothing to point at a barcode (`dev`, an emulator), so a flow that starts from a scan can be
 * walked by hand. The default is a valid EAN-13.
 */
public class FixedCodeScanner(
    private val value: String = "5901234123457",
    private val format: CodeFormat = CodeFormat.Ean13,
) : CodeScanner {

    override suspend fun scan(): ScanResult = ScanResult.Code(value, format)
}
