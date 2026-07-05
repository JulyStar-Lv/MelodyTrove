package com.github.tidetunes.feature.settings.presentation

import com.github.tidetunes.service.librarysync.domain.LibrarySyncFailure

internal data class ScanFailureDisplay(
    val fileName: String,
    val directory: String?,
    val reason: String,
)

internal fun LibrarySyncFailure.toScanFailureDisplay(): ScanFailureDisplay =
    message.toScanFailureDisplay()

internal fun String.toScanFailureDisplay(): ScanFailureDisplay {
    val (rawPath, rawReason) = splitFailureMessage()
    val decodedPath = rawPath
        ?.percentDecodeUtf8()
        ?.trim()
        ?.takeIf(String::isNotBlank)
    return ScanFailureDisplay(
        fileName = decodedPath?.fileNameOrNull() ?: "未知文件",
        directory = decodedPath?.directoryOrNull(),
        reason = rawReason.toReadableReason(),
    )
}

private fun String.splitFailureMessage(): Pair<String?, String> {
    val metadataSeparator = ": metadata error: "
    val metadataIndex = indexOf(metadataSeparator)
    if (metadataIndex >= 0) {
        return take(metadataIndex) to drop(metadataIndex + metadataSeparator.length)
    }

    val chineseSeparatorIndex = indexOf('：')
    if (chineseSeparatorIndex >= 0) {
        return take(chineseSeparatorIndex) to drop(chineseSeparatorIndex + 1)
    }

    val separatorIndex = indexOf(": ")
    if (separatorIndex >= 0) {
        return take(separatorIndex) to drop(separatorIndex + 2)
    }

    return null to this
}

private fun String.fileNameOrNull(): String? =
    substringAfterLast('/').takeIf(String::isNotBlank)

private fun String.directoryOrNull(): String? {
    if ('/' !in this) return null
    val directory = substringBeforeLast('/', missingDelimiterValue = "")
    return directory.ifBlank {
        if (startsWith('/')) "/" else null
    }
}

private fun String.toReadableReason(): String {
    val reason = trim().percentDecodeUtf8()
    if (reason.isBlank()) return "未知错误"

    val lowercase = reason.lowercase()
    return when {
        "range source failed" in lowercase && "http status" in lowercase -> {
            val httpStatus = HttpStatusRegex.find(reason)
                ?.groupValues
                ?.getOrNull(1)
                ?.let(::readableHttpStatus)
            if (httpStatus != null) {
                "远程文件读取失败：服务器返回 $httpStatus"
            } else {
                "远程文件读取失败：服务器返回错误"
            }
        }

        "metadata scan exceeded byte budget" in lowercase -> {
            val limit = ByteBudgetRegex.find(reason)
                ?.groupValues
                ?.getOrNull(1)
                ?.toLongOrNull()
                ?.toReadableBytes()
            buildString {
                append("元数据读取失败：文件标签或内嵌封面超过扫描读取上限")
                if (limit != null) append("（$limit）")
            }
        }

        "unsupported container" in lowercase -> "元数据读取失败：不支持的音频容器或文件格式"
        "元数据读取无返回结果" in reason -> "元数据读取失败：未读取到可用元数据"
        lowercase.startsWith("metadata error:") -> {
            "元数据读取失败：${reason.removePrefix("metadata error:").trim()}"
        }

        else -> reason
    }
}

private val ByteBudgetRegex = Regex("""byte budget \((\d+)\)""")
private val HttpStatusRegex = Regex("""HTTP status .*?\(([^)]+)\)""", RegexOption.IGNORE_CASE)

private fun readableHttpStatus(status: String): String =
    when {
        status.startsWith("500 ") -> "HTTP 500（服务器内部错误）"
        status.startsWith("404 ") -> "HTTP 404（文件不存在）"
        status.startsWith("401 ") -> "HTTP 401（未授权）"
        status.startsWith("403 ") -> "HTTP 403（无访问权限）"
        else -> "HTTP $status"
    }

private fun Long.toReadableBytes(): String {
    val megabyte = 1024L * 1024L
    return if (this % megabyte == 0L) {
        "${this / megabyte} MB"
    } else {
        "$this bytes"
    }
}

private fun String.percentDecodeUtf8(): String {
    if ('%' !in this) return this

    val output = StringBuilder(length)
    val bytes = mutableListOf<Byte>()

    fun flushBytes() {
        if (bytes.isEmpty()) return
        output.append(bytes.toByteArray().decodeToString())
        bytes.clear()
    }

    var index = 0
    while (index < length) {
        val char = this[index]
        if (char == '%' && index + 2 < length) {
            val high = hexValue(this[index + 1])
            val low = hexValue(this[index + 2])
            if (high != null && low != null) {
                bytes += ((high shl 4) + low).toByte()
                index += 3
                continue
            }
        }

        flushBytes()
        output.append(char)
        index += 1
    }

    flushBytes()
    return output.toString()
}

private fun hexValue(char: Char): Int? =
    when (char) {
        in '0'..'9' -> char - '0'
        in 'a'..'f' -> char - 'a' + 10
        in 'A'..'F' -> char - 'A' + 10
        else -> null
    }
