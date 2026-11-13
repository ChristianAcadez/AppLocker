package com.christianacadez.applocker.data.repository

import com.christianacadez.applocker.data.db.AppGroup
import com.christianacadez.applocker.data.db.GroupDao
import com.christianacadez.applocker.data.db.GroupMember
import com.christianacadez.applocker.data.db.GroupRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class GroupDetails(
    val group: AppGroup,
    val members: List<GroupMember>,
    val rules: List<GroupRule>
)

class GroupRepository(private val dao: GroupDao) {

    fun observeAll(): Flow<List<GroupDetails>> =
        combine(dao.observeGroups(), dao.observeMembers(), dao.observeRules()) { groups, members, rules ->
            val membersByGroup = members.groupBy { it.groupId }
            val rulesByGroup = rules.groupBy { it.groupId }
            groups.map { group ->
                GroupDetails(
                    group = group,
                    members = membersByGroup[group.id].orEmpty(),
                    rules = rulesByGroup[group.id].orEmpty().sortedBy { it.startMinute }
                )
            }
        }

    suspend fun save(id: Long, name: String, members: List<GroupMember>, rules: List<GroupRule>) =
        dao.saveGroup(id, name, members, rules)

    suspend fun delete(groupId: Long) = dao.deleteGroupCompletely(groupId)
}