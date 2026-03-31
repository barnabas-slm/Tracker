package com.example.tracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CounterListDao {
    @Query("SELECT * FROM counter_lists ORDER BY position")
    fun getAllFlow(): Flow<List<CounterList>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(list: CounterList)

    @Query("DELETE FROM counter_lists WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface CounterDao {
    @Query("SELECT * FROM counters")
    fun getAllFlow(): Flow<List<Counter>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(counter: Counter)

    @Query("DELETE FROM counters WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM counters WHERE listId = :listId")
    suspend fun deleteByListId(listId: String)

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

    @Query("DELETE FROM counter_groups WHERE listId = :listId")
    suspend fun deleteByListId(listId: String)
}

@Dao
interface CustomOrderDao {
    @Query("SELECT `order` FROM custom_order WHERE listId = :listId")
    suspend fun getOrderForList(listId: String): String?

    @Query("SELECT * FROM custom_order")
    suspend fun getAllOrders(): List<CustomOrderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveOrder(entity: CustomOrderEntity)

    @Query("DELETE FROM custom_order WHERE listId = :listId")
    suspend fun deleteByListId(listId: String)
}
