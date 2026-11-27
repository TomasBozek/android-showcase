package com.example.androidproject1.feature.trips.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [TripEntity::class, DestinationEntity::class],
    version = 2,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class TripsDatabase : RoomDatabase() {

    abstract fun tripDao(): TripDao

    abstract fun destinationDao(): DestinationDao

    companion object {

        const val NAME = "trips.db"

        /** Adds the departure time. Nothing to backfill: a trip saved before it had none. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `trips` ADD COLUMN `departureTime` TEXT")
            }
        }

        /**
         * Every migration this database has, in one place because two things read it: the builder
         * in `TripsModule` and `TripsDatabaseMigrationTest`. A `version` bump ships its migration
         * and its entry here in the same commit — the test fails otherwise, which is the point.
         * Declared after the migrations it names: a companion initialises top down.
         */
        val MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_1_2)
    }
}
