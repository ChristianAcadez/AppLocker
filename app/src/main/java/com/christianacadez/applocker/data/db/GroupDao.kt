package com.christianacadez.applocker.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import androidx.room.Update

@Dao
abstract class GroupDao {

    @Query("SELECT * FROM group_members")
    abstract fun observeMembers(): Flow<List<GroupMember>>

    @Query("SELECT * FROM group_rules")
    abstract fun observeRules(): Flow<List<GroupRule>>

    @Query("SELECT groupId FROM group_members WHERE target = :target")
    abstract suspend fun groupIdOf(target: String): Long?

    @Insert
    abstract suspend fun insertGroup(group: AppGroup): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertMember(member: GroupMember)

    @Insert
    abstract suspend fun insertRule(rule: GroupRule)

    @Query("DELETE FROM group_rules WHERE groupId = :groupId")
    abstract suspend fun deleteRules(groupId: Long)

    @Query("DELETE FROM group_members WHERE groupId = :groupId")
    abstract suspend fun deleteMembers(groupId: Long)

    @Query("DELETE FROM app_groups WHERE id = :groupId")
    abstract suspend fun deleteGroup(groupId: Long)

    @Query("SELECT * FROM app_groups ORDER BY name COLLATE NOCASE")
    abstract fun observeGroups(): Flow<List<AppGroup>>

    @Update
    abstract suspend fun updateGroup(group: AppGroup)

    /** Crea (id = 0) o actualiza un grupo, reemplazando sus apps y reglas en una sola transacción. */
    @Transaction
    open suspend fun saveGroup(
        id: Long,
        name: String,
        members: List<GroupMember>,
        rules: List<GroupRule>
    ) {
        val groupId = if (id == 0L) {
            insertGroup(AppGroup(name = name))
        } else {
            updateGroup(AppGroup(id = id, name = name))
            id
        }
        deleteMembers(groupId)
        deleteRules(groupId)
        members.forEach { insertMember(it.copy(groupId = groupId)) }
        rules.forEach { insertRule(it.copy(id = 0, groupId = groupId)) }
    }

    @Transaction
    open suspend fun deleteGroupCompletely(groupId: Long) {
        deleteRules(groupId)
        deleteMembers(groupId)
        deleteGroup(groupId)
    }

}