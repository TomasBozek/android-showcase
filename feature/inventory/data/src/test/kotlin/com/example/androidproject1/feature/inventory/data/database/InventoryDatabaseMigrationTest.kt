package com.example.androidproject1.feature.inventory.data.database

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// A name of its own, so this never opens the database the app or the DAO test uses.
private const val DATABASE_NAME = "inventory-migration-test.db"

/**
 * Every committed schema of the inventory database migrates into the current one.
 *
 * `InventoryDatabaseTest` builds the current schema from scratch, which is what a fresh install
 * does; a user who already has the app runs the migration instead — and an item is not refetched
 * from anywhere, so a version bump that forgets its migration takes what they wrote down with it.
 * Version 2 added the barcode.
 *
 * Nothing here is written per version. The current version is read off the compiled database and
 * the old ones off the committed schemas, so bumping `version` without writing the migration fails
 * this test rather than needing someone to remember to extend it.
 */
@RunWith(RobolectricTestRunner::class)
class InventoryDatabaseMigrationTest {

    // Robolectric rather than an instrumentation test: the schemas are on the unit test's assets
    // — see `convention.android.room` — so `./gradlew test` covers this and CI needs no emulator.
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        InventoryDatabase::class.java,
    )

    /** What the entities compile to today. `@Database` is not a runtime annotation, so it is read
     * off a database rather than off the class. */
    private val currentVersion: Int
        get() {
            val database = Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(),
                InventoryDatabase::class.java,
            ).allowMainThreadQueries().build()
            return try {
                database.openHelper.readableDatabase.version
            } finally {
                database.close()
            }
        }

    /** The versions under `schemas/`, which Room exports on every compile. */
    private val committedVersions: List<Int>
        get() {
            val directory = requireNotNull(InventoryDatabase::class.qualifiedName)
            val assets = InstrumentationRegistry.getInstrumentation().context.assets
            return assets.list(directory).orEmpty()
                .mapNotNull { it.removeSuffix(".json").toIntOrNull() }
                .sorted()
        }

    @Test
    fun `the current version's schema is committed`() {
        // Room exports it on compile, so this only fails when the directory was left out of the
        // commit — and then the migration below has nothing to migrate from on anyone else's
        // machine.
        assertEquals(currentVersion, committedVersions.lastOrNull())
    }

    @Test
    fun `every committed version migrates to the current schema`() {
        val versions = committedVersions
        assertTrue("no committed schema to migrate from", versions.isNotEmpty())

        for (from in versions.dropLast(1)) {
            helper.createDatabase(DATABASE_NAME, from).close()
            // validateDroppedTables: a migration that leaves the old table behind still answers
            // every query correctly and is still a bug — the data is in two places from then on.
            helper.runMigrationsAndValidate(
                DATABASE_NAME,
                currentVersion,
                true,
                *InventoryDatabase.MIGRATIONS,
            ).close()
        }
    }

    @Test
    fun `the oldest committed schema still opens`() {
        // Creating it is the assertion: MigrationTestHelper validates what it opened against the
        // committed JSON, so a schema file that no longer describes a database Room can build
        // fails here rather than on somebody's phone.
        helper.createDatabase(DATABASE_NAME, committedVersions.first()).close()
    }

    @Test
    fun `an item from before the barcode keeps everything and reads no code`() {
        helper.createDatabase(DATABASE_NAME, 1).apply {
            execSQL(
                "INSERT INTO items (id, name, category, condition, quantity, priceMinor, acquiredOn, insured, tags, " +
                    "owner, imageUrl, notes) VALUES ('item-drill', 'Cordless drill', 'Tools', 'Good', 1, 249000, " +
                    "'2024-03-16', 0, 'Lent', 'Jana Nováková', NULL, 'Lent to Petr')",
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(
            DATABASE_NAME,
            currentVersion,
            true,
            *InventoryDatabase.MIGRATIONS,
        )

        migrated.query("SELECT name, quantity, notes, barcode FROM items WHERE id = 'item-drill'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Cordless drill", cursor.getString(0))
            assertEquals(1, cursor.getInt(1))
            assertEquals("Lent to Petr", cursor.getString(2))
            assertTrue("no barcode was ever set", cursor.isNull(3))
        }
        migrated.close()
    }
}
