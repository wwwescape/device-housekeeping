package com.wwwescape.devicehousekeeping.data.schedule

import com.wwwescape.devicehousekeeping.data.scan.JunkCategory
import com.wwwescape.devicehousekeeping.data.scan.JunkItem
import com.wwwescape.devicehousekeeping.data.scan.Safety
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoCleanTest {

    private fun item(category: JunkCategory) = JunkItem(
        path = "/storage/emulated/0/x/${category.name}",
        category = category,
        sizeBytes = 1,
        lastModified = 0,
        isDirectory = false,
        volumeRoot = "/storage/emulated/0",
    )

    @Test
    fun `unattended runs never include review categories`() {
        val all = JunkCategory.entries.map(::item)
        val picked = autoCleanable(all)
        assertTrue(picked.all { it.category.safety == Safety.SAFE })
        assertEquals(JunkCategory.entries.count { it.safety == Safety.SAFE }, picked.size)
    }
}
