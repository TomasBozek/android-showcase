package com.example.androidproject1.feature.movies.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        MovieEntity::class,
        MovieDetailEntity::class,
        MoviePageEntity::class,
        MovieFavouriteEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
abstract class MoviesDatabase : RoomDatabase() {

    abstract fun moviesDao(): MoviesDao

    abstract fun movieFavouritesDao(): MovieFavouritesDao

    companion object {

        const val NAME = "movies.db"

        /** Adds the favourites. Nothing to backfill: nobody could heart a movie before it existed. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `movie_favourites` " +
                        "(`id` INTEGER NOT NULL, `favouritedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))",
                )
            }
        }

        /**
         * The list moved to TVmaze: a page marker says whether it was the last
         * rather than how many there are, and every cached row — the favourites with them — was a
         * the previous host's id, which names a different show on TVmaze, so the cache starts empty.
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE `movie_pages`")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `movie_pages` " +
                        "(`page` INTEGER NOT NULL, `isLast` INTEGER NOT NULL, `fetchedAt` INTEGER NOT NULL, PRIMARY KEY(`page`))",
                )
                db.execSQL("DELETE FROM `movies`")
                db.execSQL("DELETE FROM `movie_details`")
                db.execSQL("DELETE FROM `movie_favourites`")
            }
        }

        /**
         * Every migration this database has, in one place because two things read it: the builder
         * in `MoviesModule` and `MoviesDatabaseMigrationTest`. A `version` bump ships its migration
         * and its entry here in the same commit — the test fails otherwise, which is the point.
         * Declared after the migrations it names: a companion initialises top down.
         */
        val MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_1_2, MIGRATION_2_3)
    }
}
