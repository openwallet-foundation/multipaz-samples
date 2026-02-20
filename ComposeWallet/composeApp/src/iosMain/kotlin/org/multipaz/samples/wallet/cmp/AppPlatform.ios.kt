package org.multipaz.samples.wallet.cmp

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.darwin.Darwin
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.multipaz.storage.Storage
import org.multipaz.storage.ios.IosStorage
import platform.Foundation.NSFileManager

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual object AppPlatform {

    @OptIn(
        DelicateCoroutinesApi::class,
        ExperimentalForeignApi::class,
        ExperimentalCoroutinesApi::class
    )
    actual val storage: Storage = IosStorage(
        storageFileUrl = NSFileManager.defaultManager.containerURLForSecurityApplicationGroupIdentifier(
            groupIdentifier = "group.org.multipaz.samples.wallet.cmp.sharedgroup"
        )!!.URLByAppendingPathComponent("storageNoBackup.db")!!,
        excludeFromBackup = true
    )

    // TODO: replace with "/redirect/" for consistency with Android once we configure it on the server.
    actual val redirectPath: String = "/landing/"

    actual val httpClientEngineFactory: HttpClientEngineFactory<*> by lazy {
        Darwin
    }
}
