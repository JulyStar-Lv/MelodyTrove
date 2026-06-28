package com.github.tidetunes.core.utils

fun decodeUrlComponent(value: String): String {
    val decoded = StringBuilder(value.length)
    var index = 0

    while (index < value.length) {
        if (value[index] != '%' || index + 2 >= value.length) {
            decoded.append(value[index])
            index += 1
            continue
        }

        val bytes = mutableListOf<Byte>()
        while (index + 2 < value.length && value[index] == '%') {
            val byte = value.substring(index + 1, index + 3).toIntOrNull(16) ?: break
            bytes += byte.toByte()
            index += 3
        }

        if (bytes.isEmpty()) {
            decoded.append('%')
            index += 1
        } else {
            decoded.append(bytes.toByteArray().decodeToString())
        }
    }

    return decoded.toString()
}
