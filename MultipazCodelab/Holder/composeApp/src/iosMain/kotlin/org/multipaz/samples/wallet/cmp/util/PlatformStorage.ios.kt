package org.multipaz.samples.wallet.cmp.util

import org.multipaz.storage.Storage
import org.multipaz.storage.ios.IosStorage
import platform.Foundation.NSFileManager

private const val IOS_APP_GROUP_IDENTIFIER = "group.org.multipaz.hanlu.testapp.sharedgroup"

actual fun createWalletStorage(): Storage =
    IosStorage(
        storageFileUrl =
            NSFileManager.defaultManager
                .containerURLForSecurityApplicationGroupIdentifier(
                    groupIdentifier = IOS_APP_GROUP_IDENTIFIER,
                )!!
                .URLByAppendingPathComponent("storageNoBackup.db")!!,
        excludeFromBackup = true,
    )
