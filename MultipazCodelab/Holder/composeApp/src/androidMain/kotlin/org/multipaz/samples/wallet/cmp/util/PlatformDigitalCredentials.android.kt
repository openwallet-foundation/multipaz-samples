package org.multipaz.samples.wallet.cmp.util

import org.multipaz.digitalcredentials.DigitalCredentials
import org.multipaz.digitalcredentials.DigitalCredentialsCredman

actual fun shouldRegisterDigitalCredentialsInCommonModule(): Boolean = true

actual suspend fun getDigitalCredentials(): DigitalCredentials {
    return DigitalCredentialsCredman.create()
}
