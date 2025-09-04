package org.multipaz.get_started

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform