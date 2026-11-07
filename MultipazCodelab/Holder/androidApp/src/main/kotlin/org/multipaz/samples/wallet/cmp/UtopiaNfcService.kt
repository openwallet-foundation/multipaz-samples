package org.multipaz.samples.wallet.cmp

import kotlinx.io.bytestring.ByteString
import org.multipaz.mdoc.nfc.MdocNfcDataTransferService
import org.multipaz.nfc.Nfc
import org.multipaz.nfc.NfcApduRouter
import org.multipaz.nfc.NfcApduService

class UtopiaNfcService : NfcApduRouter() {
    override fun buildServices(): Map<ByteString, NfcApduService> {
        return mapOf(
            Nfc.NDEF_APPLICATION_ID to NdefService(),
            Nfc.ISO_MDOC_NFC_DATA_TRANSFER_APPLICATION_ID to MdocNfcDataTransferService(),
        )
    }
}
