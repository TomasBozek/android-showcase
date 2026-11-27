package com.example.androidproject1.feature.inventory.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ItemEntity::class, OutboxEntity::class],
    version = 3,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class InventoryDatabase : RoomDatabase() {

    abstract fun itemDao(): ItemDao

    abstract fun outboxDao(): OutboxDao

    companion object {

        const val NAME = "inventory.db"

        /** Adds the barcode. Nothing to backfill: an item saved before it had none. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `items` ADD COLUMN `barcode` TEXT")
            }
        }

        /** Adds the outbox. Nothing to backfill: every write before it went nowhere. */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `outbox` (`itemId` TEXT NOT NULL, `op` TEXT NOT NULL, " +
                        "`failed` INTEGER NOT NULL, `enqueuedAt` INTEGER NOT NULL, PRIMARY KEY(`itemId`))",
                )
            }
        }

        /**
         * Every migration this database has, in one place because two things read it: the builder
         * in `InventoryModule` and `InventoryDatabaseMigrationTest`. A `version` bump ships its
         * migration and its entry here in the same commit — the test fails otherwise. Declared
         * after the migrations it names: a companion initialises top down.
         */
        val MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_1_2, MIGRATION_2_3)
    }
}
