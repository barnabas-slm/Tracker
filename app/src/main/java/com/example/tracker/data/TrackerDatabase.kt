package com.example.tracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Counter::class, CounterGroup::class, CustomOrderEntity::class], version = 2, exportSchema = false)
abstract class TrackerDatabase : RoomDatabase() {
    abstract fun counterDao(): CounterDao
    abstract fun counterGroupDao(): CounterGroupDao
    abstract fun customOrderDao(): CustomOrderDao

    companion object {
        @Volatile private var INSTANCE: TrackerDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE counters ADD COLUMN color INTEGER DEFAULT NULL")
            }
        }

        fun getInstance(context: Context): TrackerDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    TrackerDatabase::class.java,
                    "tracker.db"
                ).addMigrations(MIGRATION_1_2).build().also { INSTANCE = it }
            }
    }
}


