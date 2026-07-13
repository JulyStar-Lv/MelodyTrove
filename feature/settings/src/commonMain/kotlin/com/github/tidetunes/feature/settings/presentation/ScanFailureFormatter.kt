package com.github.tidetunes.feature.settings.presentation

import com.github.tidetunes.service.librarysync.domain.LibrarySyncFailure

internal data class ScanFailureDisplay(
    val fileName: String?,
    val directory: String?,
    val reason: ScanFailureReason,
)

internal sealed interface ScanFailureReason {
    data object Unknown : ScanFailureReason

    data class RemoteRead(val httpStatus: String?) : ScanFailureReason

    data class ByteBudget(val limitBytes: Long?) : ScanFailureReason

    data object UnsupportedContainer : ScanFailureReason

    data object MissingMetadata : ScanFailureReason

    data class MetadataError(val detail: String) : ScanFailureReason

    data class Raw(val detail: String) : ScanFailureReason
}

internal fun LibrarySyncFailure.toScanFailureDisplay(): ScanFailureDisplay =
    message.toScanFailureDisplay()

internal fun String.toScanFailureDisplay(): ScanFailureDisplay {
    val (rawPath, rawReason) = splitFailureMessage()
    val decodedPath = rawPath
        ?.percentDecodeUtf8()
        ?.trim()
        ?.takeIf(String::isNotBlank)
    return ScanFailureDisplay(
        fileName = decodedPath?.fileNameOrNull(),
        directory = decodedPath?.directoryOrNull(),
        reason = rawReason.toScanFailureReason(),
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

private fun String.toScanFailureReason(): ScanFailureReason {
    val reason = trim().percentDecodeUtf8()
    if (reason.isBlank()) return ScanFailureReason.Unknown

    val lowercase = reason.lowercase()
    return when {
        "range source failed" in lowercase && "http status" in lowercase -> {
            val httpStatus = HttpStatusRegex.find(reason)
                ?.groupValues
                ?.getOrNull(1)
            ScanFailureReason.RemoteRead(httpStatus)
        }

        "metadata scan exceeded byte budget" in lowercase -> {
            val limit = ByteBudgetRegex.find(reason)
                ?.groupValues
                ?.getOrNull(1)
                ?.toLongOrNull()
            ScanFailureReason.ByteBudget(limit)
        }

        "unsupported container" in lowercase -> ScanFailureReason.UnsupportedContainer
        "元数据读取无返回结果" in reason -> ScanFailureReason.MissingMetadata
        lowercase.startsWith("metadata error:") -> {
            ScanFailureReason.MetadataError(reason.substringAfter(':').trim())
        }

        else -> ScanFailureReason.Raw(reason)
    }
}

private val ByteBudgetRegex = Regex("""byte budget \((\d+)\)""")
private val HttpStatusRegex = Regex("""HTTP status .*?\(([^)]+)\)""", RegexOption.IGNORE_CASE)

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
