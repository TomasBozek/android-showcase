package com.example.androidproject1.service.scanner.test

import com.example.androidproject1.service.scanner.CodeFormat
import com.example.androidproject1.service.scanner.CodeScanner
import com.example.androidproject1.service.scanner.ScanResult

/**
 * A [CodeScanner] that answers [result] — a fixed code by default — and counts the scans. It is
 * what `dev` binds where there is no camera to point at a barcode.
 */
class FakeScanner(var result: ScanResult = ScanResult.Code(CODE, CodeFormat.Ean13)) : CodeScanner {

    var scans = 0
        private set

    override suspend fun scan(): ScanResult {
        scans++
        return result
    }

    companion object {

        const val CODE = "5901234123457"
    }
}
