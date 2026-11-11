package com.christianacadez.applocker.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface UnlockEventDao {

    @Insert
    suspend fun insert(event: UnlockEvent)

    @Query("SELECT * FROM unlock_events WHERE timestamp >= :since")
    suspend fun since(since: Long): List<UnlockEvent>
}