package org.multipaz.samples.wallet.cmp.util

import org.multipaz.digitalcredentials.DigitalCredentials
import org.multipaz.digitalcredentials.DigitalCredentialsIos

actual fun shouldRegisterDigitalCredentialsInCommonModule(): Boolean = false

actual suspend fun getDigitalCredentials(): DigitalCredentials {
    return DigitalCredentialsIos.create()
}
