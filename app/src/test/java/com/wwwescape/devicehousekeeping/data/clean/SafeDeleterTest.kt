package com.wwwescape.devicehousekeeping.data.clean

import com.wwwescape.devicehousekeeping.data.scan.JunkCategory
import com.wwwescape.devicehousekeeping.data.scan.JunkItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class SafeDeleterTest {

    @get:Rule
    val temp = TemporaryFolder()

    private val volume: File by lazy { temp.newFolder("volume") }
    private val outside: File by lazy { temp.newFolder("outside") }

    private fun deleter(excluded: Set<String> = emptySet(), stillJunk: (JunkItem) -> Boolean = { true }) =
        SafeDeleter(listOf(volume.absolutePath), excluded, stillJunk)

    private fun fileItem(file: File, category: JunkCategory = JunkCategory.TEMP_FILES) = JunkItem(
        path = file.absolutePath,
        category = category,
        sizeBytes = file.length(),
        lastModified = file.lastModified(),
        isDirectory = false,
        volumeRoot = volume.absolutePath,
    )

    private fun dirItem(dir: File) = JunkItem(
        path = dir.absolutePath,
        category = JunkCategory.THUMBNAILS,
        sizeBytes = 0,
        lastModified = dir.lastModified(),
        isDirectory = true,
        volumeRoot = volume.absolutePath,
    )

    private fun write(file: File, bytes: Int = 10): File {
        file.parentFile?.mkdirs()
        file.writeBytes(ByteArray(bytes))
        return file
    }

    @Test
    fun `deletes an unchanged file and reports its size`() {
        val f = write(File(volume, "app/upload.tmp"), 123)
        val outcome = deleter().delete(fileItem(f))
        assertNull(outcome.skipped)
        assertEquals(123L, outcome.freedBytes)
        assertFalse(f.exists())
    }

    @Test
    fun `refuses a file that changed after the scan`() {
        val f = write(File(volume, "app/upload.tmp"), 10)
        val item = fileItem(f)
        write(f, 20)
        assertEquals(SkipReason.CHANGED, deleter().delete(item).skipped)
        assertTrue(f.exists())
    }

    @Test
    fun `refuses anything outside the scanned volumes`() {
        val f = write(File(outside, "important.tmp"))
        assertEquals(SkipReason.OUTSIDE_VOLUME, deleter().delete(fileItem(f)).skipped)
        assertTrue(f.exists())
    }

    @Test
    fun `refuses dot-dot escapes`() {
        val f = write(File(outside, "important.tmp"))
        val sneaky = fileItem(f).copy(path = volume.absolutePath + "/../outside/important.tmp")
        assertEquals(SkipReason.OUTSIDE_VOLUME, deleter().delete(sneaky).skipped)
        assertTrue(f.exists())
    }

    @Test
    fun `refuses protected top-level folders`() {
        val dcim = File(volume, "DCIM").apply { mkdirs() }
        write(File(dcim, "photo.jpg"))
        assertEquals(SkipReason.PROTECTED, deleter().delete(dirItem(dcim)).skipped)
        assertTrue(File(dcim, "photo.jpg").exists())
    }

    @Test
    fun `refuses excluded paths and their children`() {
        val f = write(File(volume, "keep/me.tmp"))
        val outcome = deleter(excluded = setOf(File(volume, "keep").absolutePath)).delete(fileItem(f))
        assertEquals(SkipReason.EXCLUDED, outcome.skipped)
        assertTrue(f.exists())
    }

    @Test
    fun `refuses items whose rule no longer holds`() {
        val f = write(File(volume, "app/upload.tmp"))
        assertEquals(SkipReason.NO_LONGER_JUNK, deleter(stillJunk = { false }).delete(fileItem(f)).skipped)
        assertTrue(f.exists())
    }

    @Test
    fun `missing items are skipped, not failed`() {
        val f = write(File(volume, "gone.tmp"))
        val item = fileItem(f)
        f.delete()
        assertEquals(SkipReason.MISSING, deleter().delete(item).skipped)
    }

    @Test
    fun `deletes a junk folder tree bottom-up`() {
        val thumbs = File(volume, "DCIM/.thumbnails")
        write(File(thumbs, "a.jpg"), 5)
        write(File(thumbs, "nested/b.jpg"), 7)
        val outcome = deleter().delete(dirItem(thumbs))
        assertNull(outcome.skipped)
        assertEquals(12L, outcome.freedBytes)
        assertEquals(2, outcome.deletedFiles)
        assertFalse(thumbs.exists())
        assertTrue(File(volume, "DCIM").exists())
    }
}
