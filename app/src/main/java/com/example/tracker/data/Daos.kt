package com.example.tracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CounterDao {
    @Query("SELECT * FROM counters")
    fun getAllFlow(): Flow<List<Counter>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(counter: Counter)

    @Query("DELETE FROM counters WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM counters")
    suspend fun deleteAll()
}

@Dao
interface CounterGroupDao {
    @Query("SELECT * FROM counter_groups")
    fun getAllFlow(): Flow<List<CounterGroup>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(group: CounterGroup)

    @Query("DELETE FROM counter_groups WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface CustomOrderDao {
    @Query("SELECT `order` FROM custom_order WHERE rowId = 0")
    suspend fun getOrderList(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveOrder(entity: CustomOrderEntity)
}




