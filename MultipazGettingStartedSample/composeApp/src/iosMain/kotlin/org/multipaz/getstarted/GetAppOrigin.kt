package org.multipaz.getstarted

import platform.Foundation.NSBundle

actual fun getAppToAppOrigin(): String {
    // On iOS, use the bundle identifier as the app origin
    // This uniquely identifies the app and is the iOS equivalent
    // of using the signing certificate on Android
    return NSBundle.mainBundle.bundleIdentifier ?: "unknown.bundle.id"
}