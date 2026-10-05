package com.bk.quoteskmp

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform