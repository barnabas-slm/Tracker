package com.example.tracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [CounterList::class, Counter::class, CounterGroup::class, CustomOrderEntity::class],
    version = 3,
    exportSchema = false
)
abstract class TrackerDatabase : RoomDatabase() {
    abstract fun counterListDao(): CounterListDao
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

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 1. Create the counter_lists table and seed the Default list
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `counter_lists` " +
                    "(`id` TEXT NOT NULL, `name` TEXT NOT NULL, `position` INTEGER NOT NULL, PRIMARY KEY(`id`))"
                )
                database.execSQL("INSERT INTO `counter_lists` VALUES ('default', 'Default', 0)")

                // 2. Add listId to counters and counter_groups (all existing rows → 'default')
                database.execSQL("ALTER TABLE `counters` ADD COLUMN `listId` TEXT NOT NULL DEFAULT 'default'")
                database.execSQL("ALTER TABLE `counter_groups` ADD COLUMN `listId` TEXT NOT NULL DEFAULT 'default'")

                // 3. Recreate custom_order with a TEXT primary key (listId) instead of INTEGER (rowId)
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `custom_order_new` " +
                    "(`listId` TEXT NOT NULL, `order` TEXT NOT NULL, PRIMARY KEY(`listId`))"
                )
                database.execSQL(
                    "INSERT INTO `custom_order_new` (`listId`, `order`) " +
                    "SELECT 'default', `order` FROM `custom_order` WHERE rowId = 0"
                )
                database.execSQL("DROP TABLE `custom_order`")
                database.execSQL("ALTER TABLE `custom_order_new` RENAME TO `custom_order`")
            }
        }

        fun getInstance(context: Context): TrackerDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    TrackerDatabase::class.java,
                    "tracker.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build().also { INSTANCE = it }
            }
    }
}
