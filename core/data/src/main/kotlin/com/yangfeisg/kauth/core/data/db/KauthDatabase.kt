package com.yangfeisg.kauth.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [AccountEntity::class], version = 1, exportSchema = false)
internal abstract class KauthDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
}
