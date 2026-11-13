package com.christianacadez.applocker.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

object MemberKind {
    const val APP = "APP"
    const val DOMAIN = "DOMAIN"   // para el manga (paso 11)
}

@Entity(tableName = "app_groups")
data class AppGroup(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)

/** La clave primaria es el objetivo: una app solo puede estar en un grupo. */
@Entity(tableName = "group_members")
data class GroupMember(
    @PrimaryKey val target: String,
    val kind: String,
    val groupId: Long
)

/** Franja: minutos desde las 00:00. Si end < start cruza la medianoche; 1440 = 24:00. */
@Entity(tableName = "group_rules")
data class GroupRule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val groupId: Long,
    val startMinute: Int,
    val endMinute: Int,
    val limitMinutes: Int
)