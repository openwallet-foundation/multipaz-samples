package org.multipaz.getstarted

import android.content.pm.PackageManager
import org.multipaz.context.applicationContext
import org.multipaz.digitalcredentials.getAppOrigin

@Suppress("DEPRECATION")
actual fun getAppToAppOrigin(): String {
    val packageInfo = applicationContext.packageManager
        .getPackageInfo(applicationContext.packageName, PackageManager.GET_SIGNATURES)

    val signatures = packageInfo.signatures
    if (signatures.isNullOrEmpty()) {
        throw IllegalStateException("No signatures found for package ${applicationContext.packageName}")
    }
    return getAppOrigin(signatures[0].toByteArray())
}

actual fun isAndroid(): Boolean = true