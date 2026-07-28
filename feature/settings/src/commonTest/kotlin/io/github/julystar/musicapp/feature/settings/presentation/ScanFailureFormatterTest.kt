package io.github.julystar.musicapp.feature.settings.presentation

import kotlin.test.Test
import kotlin.test.assertEquals

class ScanFailureFormatterTest {

    @Test
    fun `decodes remote path and explains metadata byte budget failures`() {
        val failure = "/%E6%88%91%E7%9A%84%E9%9F%B3%E4%B9%90/" +
            "%E7%8E%8B%E5%BF%83%E5%87%8C%20-%20%E7%9D%AB%E6%AF%9B%E5%BC%AF%E5%BC%AF.flac" +
            ": metadata error: metadata scan exceeded byte budget (4194304)"

        val display = failure.toScanFailureDisplay()

        assertEquals("王心凌 - 睫毛弯弯.flac", display.fileName)
        assertEquals("/我的音乐", display.directory)
        assertEquals(ScanFailureReason.ByteBudget(4L * 1024 * 1024), display.reason)
    }

    @Test
    fun `shows readable reason for missing metadata`() {
        val display = "/Music/Missing.flac：元数据读取无返回结果".toScanFailureDisplay()

        assertEquals("Missing.flac", display.fileName)
        assertEquals("/Music", display.directory)
        assertEquals(ScanFailureReason.MissingMetadata, display.reason)
    }

    @Test
    fun `shows readable reason for unsupported container`() {
        val display = "/Music/Track.ape: metadata error: unsupported container".toScanFailureDisplay()

        assertEquals("Track.ape", display.fileName)
        assertEquals("/Music", display.directory)
        assertEquals(ScanFailureReason.UnsupportedContainer, display.reason)
    }

    @Test
    fun `shows readable reason for webdav server read failures`() {
        val failure = "/%E6%88%91%E7%9A%84%E9%9F%B3%E4%B9%90/" +
            "%E6%9D%A8%E5%8D%83%E5%AC%85%20-%20%E5%92%96%E5%95%A1%E5%9B%A0.flac" +
            "：range source failed: HTTP status server error (500 Internal Server Error) for url " +
            "(http://192.168.50.100:5244/dav/%E6%88%91%E7%9A%84%E9%9F%B3%E4%B9%90/" +
            "%E6%9D%A8%E5%8D%83%E5%AC%85%20-%20%E5%92%96%E5%95%A1%E5%9B%A0.flac)"

        val display = failure.toScanFailureDisplay()

        assertEquals("杨千嬅 - 咖啡因.flac", display.fileName)
        assertEquals("/我的音乐", display.directory)
        assertEquals(ScanFailureReason.RemoteRead("500 Internal Server Error"), display.reason)
    }
}
