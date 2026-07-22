package org.multipaz.getstarted

import kotlinx.io.bytestring.ByteString
import org.multipaz.compose.mdoc.CombinedNfcService
import org.multipaz.compose.mdoc.NfcApduService
import org.multipaz.nfc.Nfc

class GetStartedNfcService : CombinedNfcService() {
    override fun buildServices(): Map<ByteString, NfcApduService> {
        return mapOf(
            Nfc.NDEF_APPLICATION_ID to NdefService(this, ::sendResponseApdu),
        )
    }
}
