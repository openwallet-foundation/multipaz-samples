package org.multipaz.samples.wallet.cmp

import kotlinx.io.bytestring.ByteString
import org.multipaz.compose.mdoc.CombinedNfcService
import org.multipaz.compose.mdoc.NfcApduService
import org.multipaz.nfc.Nfc

class UtopiaNfcService : CombinedNfcService() {
    override fun buildServices(): Map<ByteString, NfcApduService> {
        return mapOf(
            Nfc.NDEF_APPLICATION_ID to NdefService(this, ::sendResponseApdu),
        )
    }
}
