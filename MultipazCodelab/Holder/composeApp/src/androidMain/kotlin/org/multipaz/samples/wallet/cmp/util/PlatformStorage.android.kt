package org.multipaz.samples.wallet.cmp.util

import org.multipaz.storage.Storage
import org.multipaz.util.Platform

actual fun createWalletStorage(): Storage = Platform.nonBackedUpStorage
