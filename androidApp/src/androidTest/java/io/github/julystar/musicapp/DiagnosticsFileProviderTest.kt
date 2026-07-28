package io.github.julystar.musicapp

import androidx.core.content.FileProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DiagnosticsFileProviderTest {
    @Test
    fun diagnosticExportGetsContentUriAndUnrelatedFileIsRejected() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val authority = "${context.packageName}.diagnostics.fileprovider"
        val export = File(context.filesDir, "diagnostics/exports/provider-test.zip")
        val unrelated = File(context.filesDir, "provider-test-private.txt")
        try {
            checkNotNull(export.parentFile).mkdirs()
            export.writeBytes(byteArrayOf(1))
            unrelated.writeText("private")

            val uri = FileProvider.getUriForFile(context, authority, export)
            assertEquals("content", uri.scheme)
            assertEquals(authority, uri.authority)
            assertThrows(IllegalArgumentException::class.java) {
                FileProvider.getUriForFile(context, authority, unrelated)
            }
        } finally {
            export.delete()
            unrelated.delete()
        }
    }
}
