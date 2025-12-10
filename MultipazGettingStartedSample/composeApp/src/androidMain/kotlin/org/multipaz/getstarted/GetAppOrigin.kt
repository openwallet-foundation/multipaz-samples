package org.multipaz.getstarted

import android.content.pm.PackageManager
import org.multipaz.context.applicationContext
import org.multipaz.digitalcredentials.getAppOrigin

@Suppress("DEPRECATION")
actual fun getAppToAppOrigin(): String {
    val packageInfo = applicationContext.packageManager
        .getPackageInfo(applicationContext.packageName, PackageManager.GET_SIGNATURES)
    return getAppOrigin(packageInfo.signatures!![0].toByteArray())
}