package io.github.julystar.musicapp.platform

import java.net.HttpURLConnection
import java.net.URI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal actual suspend fun fetchRemoteImageBytes(
    url: String,
    maxBytes: Long,
): ByteArray? = withContext(Dispatchers.IO) {
    fetchJvmRemoteImageBytes(url, maxBytes)
}

private fun fetchJvmRemoteImageBytes(url: String, maxBytes: Long): ByteArray? {
    val connection = URI(url).toURL().openConnection() as? HttpURLConnection ?: return null
    return try {
        connection.connectTimeout = 8_000
        connection.readTimeout = 8_000
        connection.instanceFollowRedirects = true
        connection.setRequestProperty("Accept", "image/*")
        if (connection.responseCode !in 200..299) return null
        if (connection.contentLengthLong > maxBytes) return null
        connection.inputStream.use { input ->
            val output = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(8 * 1024)
            var total = 0L
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                total += count
                if (total > maxBytes) return null
                output.write(buffer, 0, count)
            }
            output.toByteArray()
        }
    } catch (_: Exception) {
        null
    } finally {
        connection.disconnect()
    }
}
