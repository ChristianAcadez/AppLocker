package com.christianacadez.applocker.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [AppGroup::class, GroupMember::class, GroupRule::class, UnlockEvent::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun groupDao(): GroupDao
    abstract fun unlockEventDao(): UnlockEventDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `unlock_events` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`packageName` TEXT NOT NULL, " +
                            "`timestamp` INTEGER NOT NULL, " +
                            "`extraMinutes` INTEGER NOT NULL)"
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `app_groups` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`name` TEXT NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `group_members` (" +
                            "`target` TEXT NOT NULL, `kind` TEXT NOT NULL, " +
                            "`groupId` INTEGER NOT NULL, PRIMARY KEY(`target`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `group_rules` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`groupId` INTEGER NOT NULL, `startMinute` INTEGER NOT NULL, " +
                            "`endMinute` INTEGER NOT NULL, `limitMinutes` INTEGER NOT NULL)"
                )
                // Cada límite antiguo pasa a ser un grupo de una app con una franja de todo el día
                db.execSQL("INSERT INTO app_groups (name) SELECT packageName FROM app_limits")
                db.execSQL("INSERT INTO group_members (target, kind, groupId) SELECT name, 'APP', id FROM app_groups")
                db.execSQL(
                    "INSERT INTO group_rules (groupId, startMinute, endMinute, limitMinutes) " +
                            "SELECT g.id, 0, 1440, l.minutes FROM app_groups g " +
                            "JOIN app_limits l ON l.packageName = g.name"
                )
                db.execSQL(
                    "UPDATE app_groups SET name = CASE name " +
                            "WHEN 'com.facebook.katana' THEN 'Facebook' " +
                            "WHEN 'com.instagram.android' THEN 'Instagram' " +
                            "WHEN 'com.zhiliaoapp.musically' THEN 'TikTok' " +
                            "WHEN 'com.android.chrome' THEN 'Chrome' " +
                            "WHEN 'com.brave.browser' THEN 'Brave' " +
                            "ELSE name END"
                )
                db.execSQL("DROP TABLE app_limits")
            }
        }

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "applocker.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { instance = it }
            }
    }
}