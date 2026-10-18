package com.wwwescape.devicehousekeeping.data.scan

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JunkRulesTest {

    private val now = 1_800_000_000_000L
    private val day = 24L * 60 * 60 * 1000
    private val old = now - 2 * day
    private val fresh = now - 60_000

    private fun classify(name: String, top: String? = "someapp", size: Long = 100, modified: Long = old) =
        JunkRules.classifyFile(name, top, size, modified, now, minAgeMillis = day)

    private fun categoryOf(name: String, top: String? = "someapp", size: Long = 100, modified: Long = old) =
        (classify(name, top, size, modified) as? FileVerdict.Junk)?.category

    @Test
    fun `temp files need to be older than the minimum age`() {
        assertEquals(JunkCategory.TEMP_FILES, categoryOf("upload.tmp"))
        assertEquals(JunkCategory.TEMP_FILES, categoryOf("video.mp4.crdownload", top = "download"))
        assertNull(classify("upload.tmp", modified = fresh))
    }

    @Test
    fun `logs are never matched inside user content folders`() {
        assertEquals(JunkCategory.LOGS, categoryOf("app.log"))
        assertEquals(JunkCategory.LOGS, categoryOf("app.log.3"))
        assertEquals(JunkCategory.LOGS, categoryOf("heap.hprof"))
        assertNull(classify("app.log", top = "documents"))
        assertNull(classify("app.log", top = "download"))
        assertNull(classify("app.log", top = "dcim"))
    }

    @Test
    fun `ordinary user files are never matched`() {
        listOf("IMG_0001.jpg", "notes.txt", "song.mp3", "backup.zip", "catalog.db", "logbook.pdf", "report.partial.docx")
            .forEach { assertNull(it, classify(it)) }
    }

    @Test
    fun `foreign metadata files match regardless of age`() {
        assertEquals(JunkCategory.FOREIGN_FILES, categoryOf("Thumbs.db", modified = fresh))
        assertEquals(JunkCategory.FOREIGN_FILES, categoryOf(".DS_Store", top = "dcim"))
        assertEquals(JunkCategory.FOREIGN_FILES, categoryOf("._IMG_0001.jpg", size = 4096))
    }

    @Test
    fun `large dot-underscore files are not treated as AppleDouble`() {
        assertNull(classify("._IMG_0001.jpg", size = 5_000_000))
    }

    @Test
    fun `mediastore trash is review and carries its expiry`() {
        val verdict = classify(".trashed-1800500000-IMG_1.jpg", top = "dcim", modified = fresh) as FileVerdict.Junk
        assertEquals(JunkCategory.TRASH, verdict.category)
        assertEquals(Safety.REVIEW, verdict.category.safety)
        assertEquals(1_800_500_000_000L, (verdict.detail as ItemDetail.Trashed).expiresAt)
    }

    @Test
    fun `pending files are only offered once abandoned`() {
        // Expiry in the future and just written: an active download.
        assertNull(classify(".pending-1800500000-movie.mp4", top = "download", modified = fresh))
        // Expiry passed: MediaStore itself would remove it.
        assertEquals(JunkCategory.TEMP_FILES, categoryOf(".pending-1700000000-movie.mp4", top = "download", modified = fresh))
    }

    @Test
    fun `apks are deferred to installed-version comparison`() {
        assertEquals(FileVerdict.ApkCandidate, classify("app-release.apk", top = "download", modified = fresh))
    }

    @Test
    fun `protected folders cover roots and user folders`() {
        assertTrue(JunkRules.isProtectedDir(""))
        assertTrue(JunkRules.isProtectedDir("/DCIM"))
        assertTrue(JunkRules.isProtectedDir("Android/data/"))
        assertFalse(JunkRules.isProtectedDir("DCIM/.thumbnails"))
        assertFalse(JunkRules.isProtectedDir("Android/media/com.gone.app"))
    }

    @Test
    fun `package name detection`() {
        assertTrue(JunkRules.looksLikePackageName("com.example.app"))
        assertFalse(JunkRules.looksLikePackageName(".nomedia"))
        assertFalse(JunkRules.looksLikePackageName("WhatsApp"))
        assertFalse(JunkRules.looksLikePackageName("com..bad"))
    }
}
