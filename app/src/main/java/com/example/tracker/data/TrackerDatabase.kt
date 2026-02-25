package com.example.tracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Counter::class, CounterGroup::class, CustomOrderEntity::class], version = 1, exportSchema = false)
abstract class TrackerDatabase : RoomDatabase() {
    abstract fun counterDao(): CounterDao
    abstract fun counterGroupDao(): CounterGroupDao
    abstract fun customOrderDao(): CustomOrderDao

    companion object {
        @Volatile private var INSTANCE: TrackerDatabase? = null

        fun getInstance(context: Context): TrackerDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    TrackerDatabase::class.java,
                    "tracker.db"
                ).build().also { INSTANCE = it }
            }
    }
}


