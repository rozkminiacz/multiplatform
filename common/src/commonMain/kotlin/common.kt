package org.common

expect class Platform() {
    val platform: String
}

class Common {
    fun hello(): String = "Hello, ${Platform().platform}"
}
