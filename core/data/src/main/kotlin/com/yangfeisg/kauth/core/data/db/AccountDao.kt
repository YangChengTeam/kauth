package com.yangfeisg.kauth.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
internal interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY id")
    fun observeAll(): Flow<List<AccountEntity>>

    @Insert
    suspend fun insert(account: AccountEntity)

    @Insert
    suspend fun insertAll(accounts: List<AccountEntity>)

    @Query("UPDATE accounts SET issuer = :issuer, name = :name WHERE id = :id")
    suspend fun rename(id: Long, issuer: String, name: String)

    @Query("UPDATE accounts SET counter = counter + 1 WHERE id = :id")
    suspend fun incrementCounter(id: Long)

    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun delete(id: Long)
}
