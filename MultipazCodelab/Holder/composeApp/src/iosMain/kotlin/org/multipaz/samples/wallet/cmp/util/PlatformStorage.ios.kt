package org.multipaz.samples.wallet.cmp.util

import org.multipaz.storage.Storage
import org.multipaz.storage.ios.IosStorage
import platform.Foundation.NSBundle
import platform.Foundation.NSFileManager

private const val IOS_APP_GROUP_IDENTIFIER = "group.org.multipaz.hanlu.testapp.sharedgroup"

actual fun createWalletStorage(): Storage =
    IosStorage(
        storageFileUrl =
            NSFileManager.defaultManager
                .containerURLForSecurityApplicationGroupIdentifier(
                    groupIdentifier =
                        NSBundle.mainBundle.objectForInfoDictionaryKey("AppGroupID") as? String
                            ?: error("Missing AppGroupID in Info.plist"),
                )!!
                .URLByAppendingPathComponent("storageNoBackup.db")!!,
        excludeFromBackup = true,
    )
