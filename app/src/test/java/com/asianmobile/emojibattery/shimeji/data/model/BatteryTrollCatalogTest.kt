package com.asianmobile.emojibattery.shimeji.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BatteryTrollCatalogTest {
    @Test
    fun distributionGate_acceptsAnApprovedCatalogInEveryBuild() {
        assertTrue(
            BatteryTrollDistributionPolicy.isDistributionAllowed(
                status = BatteryTrollDistributionStatus.APPROVED,
                isDebugBuild = false
            )
        )
        assertTrue(
            BatteryTrollDistributionPolicy.isDistributionAllowed(
                status = BatteryTrollDistributionStatus.APPROVED,
                isDebugBuild = true
            )
        )
    }

    @Test
    fun distributionGate_blocksAnUnapprovedCatalogOnlyInRelease() {
        assertFalse(
            BatteryTrollDistributionPolicy.isDistributionAllowed(
                status = BatteryTrollDistributionStatus.REVIEW_REQUIRED,
                isDebugBuild = false
            )
        )
        assertTrue(
            BatteryTrollDistributionPolicy.isDistributionAllowed(
                status = BatteryTrollDistributionStatus.REVIEW_REQUIRED,
                isDebugBuild = true
            )
        )
    }

    @Test
    fun entry_readsFullFirstAndEmptyLast() {
        val entry = entry()

        assertEquals(BATTERY_TROLL_LEVEL_COUNT, entry.emojiPaths.size)
        assertEquals(BATTERY_TROLL_LEVEL_COUNT, entry.batteryPaths.size)
        assertEquals("emoji/TROLL_1_1.webp", entry.emojiPathAt(0))
        assertEquals("emoji/TROLL_1_5.webp", entry.emojiPathAt(BATTERY_TROLL_LEVEL_COUNT - 1))
        assertEquals("battery/TROLL_1_3.webp", entry.batteryPathAt(2))
    }

    @Test
    fun entry_clampsAnOutOfRangeLevelIndex() {
        val entry = entry()

        assertEquals("emoji/TROLL_1_1.webp", entry.emojiPathAt(-1))
        assertEquals("battery/TROLL_1_5.webp", entry.batteryPathAt(99))
    }

    @Test
    fun snapshot_findsTrollsByIdAndStartsEmpty() {
        val snapshot = BatteryTrollCatalogSnapshot(trolls = listOf(entry()))

        assertEquals(1, snapshot.findTroll(1)?.id)
        assertNull(snapshot.findTroll(9))
        assertTrue(BatteryTrollCatalogSnapshot().trolls.isEmpty())
        assertTrue(BatteryTrollCatalogSnapshot().isLoading)
    }

    private fun entry(): BatteryTrollEntry = BatteryTrollEntry(
        id = 1,
        name = "Spider Hero",
        slug = "troll_1",
        order = 0,
        entitlement = BatteryTrollEntitlement.FREE,
        batteryOrientation = BatteryTrollBatteryOrientation.LANDSCAPE,
        thumbnailPath = "thumb/TROLL_1.webp",
        emojiPaths = (1..BATTERY_TROLL_LEVEL_COUNT).map { "emoji/TROLL_1_$it.webp" },
        batteryPaths = (1..BATTERY_TROLL_LEVEL_COUNT).map { "battery/TROLL_1_$it.webp" }
    )
}
