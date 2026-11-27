package com.example.androidproject1.service.scanner

import android.content.Context
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/** What a scan source reports: the outcome of one scan, before it is turned into a [ScanResult]. */
public interface ScanSource {

    public fun start(onCode: (String?, Int) -> Unit, onCancel: () -> Unit, onFailure: (Throwable) -> Unit)
}

/** [ScanSource] over Google's code scanner in Play services. */
internal class GmsScanSource(private val context: Context) : ScanSource {

    override fun start(onCode: (String?, Int) -> Unit, onCancel: () -> Unit, onFailure: (Throwable) -> Unit) {
        val options = GmsBarcodeScannerOptions.Builder().allowManualInput().build()
        GmsBarcodeScanning.getClient(context, options).startScan()
            .addOnSuccessListener { onCode(it.rawValue, it.format) }
            .addOnCanceledListener(onCancel)
            .addOnFailureListener(onFailure)
    }
}

/**
 * [CodeScanner] over a [ScanSource]: a code is [ScanResult.Code], backing out [ScanResult.Cancelled]
 * and anything that goes wrong — including a code with no text — [ScanResult.Unavailable].
 */
public class PlayCodeScanner(private val source: ScanSource) : CodeScanner {

    public constructor(context: Context) : this(GmsScanSource(context))

    override suspend fun scan(): ScanResult = suspendCancellableCoroutine { continuation ->
        fun finish(result: ScanResult) {
            if (continuation.isActive) continuation.resume(result)
        }
        try {
            source.start(
                onCode = { value, format ->
                    finish(
                        if (value.isNullOrEmpty()) ScanResult.Unavailable else ScanResult.Code(value, formatOf(format)),
                    )
                },
                onCancel = { finish(ScanResult.Cancelled) },
                onFailure = { finish(ScanResult.Unavailable) },
            )
        } catch (_: Exception) {
            finish(ScanResult.Unavailable)
        }
    }

    internal companion object {

        fun formatOf(format: Int): CodeFormat = when (format) {
            Barcode.FORMAT_QR_CODE -> CodeFormat.Qr
            Barcode.FORMAT_EAN_13 -> CodeFormat.Ean13
            Barcode.FORMAT_EAN_8 -> CodeFormat.Ean8
            Barcode.FORMAT_UPC_A -> CodeFormat.UpcA
            Barcode.FORMAT_UPC_E -> CodeFormat.UpcE
            Barcode.FORMAT_CODE_128 -> CodeFormat.Code128
            Barcode.FORMAT_CODE_39 -> CodeFormat.Code39
            else -> CodeFormat.Other
        }
    }
}
