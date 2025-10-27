package org.multipaz.samples.wallet.cmp

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform