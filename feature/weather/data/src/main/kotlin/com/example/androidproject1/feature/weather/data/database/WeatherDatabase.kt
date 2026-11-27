package com.example.androidproject1.feature.weather.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        PlaceEntity::class,
        ForecastEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class WeatherDatabase : RoomDatabase() {

    abstract fun weatherDao(): WeatherDao

    companion object {

        const val NAME = "weather.db"

        /**
         * Adds the order the user arranges. The backfill is the whole point: before this,
         * the list was `savedAt DESC`, so the first positions are that same order — a user who
         * updates sees the list they left, and only then moves a row.
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `places` ADD COLUMN `position` INTEGER NOT NULL DEFAULT 0")
                // One statement, numbering from 0 down the old order. The correlated count is
                // over the whole table rather than a window function: SQLite has had those since
                // 3.25, but the version is the device's, and minSdk 29 ships 3.28 on some ROMs
                // and a patched 3.22 on others.
                db.execSQL(
                    "UPDATE `places` SET `position` = (" +
                        "SELECT COUNT(*) FROM `places` AS earlier " +
                        "WHERE earlier.savedAt > places.savedAt " +
                        "OR (earlier.savedAt = places.savedAt AND earlier.id < places.id))",
                )
            }
        }

        /**
         * Every migration this database has, in one place because two things read it: the builder
         * in `WeatherModule` and `WeatherDatabaseMigrationTest`. A `version` bump ships its
         * migration and its entry here in the same commit — the test fails otherwise.
         * Declared after the migrations it names: a companion initialises top down.
         */
        val MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_1_2)
    }
}
