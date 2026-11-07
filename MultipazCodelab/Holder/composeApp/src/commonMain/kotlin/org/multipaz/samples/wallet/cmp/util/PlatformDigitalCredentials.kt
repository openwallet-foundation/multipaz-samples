package org.multipaz.samples.wallet.cmp.util

import org.multipaz.digitalcredentials.DigitalCredentials

expect fun shouldRegisterDigitalCredentialsInCommonModule(): Boolean

expect suspend fun getDigitalCredentials(): DigitalCredentials
