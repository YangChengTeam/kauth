package com.yangfeisg.kauth.core.data

import com.yangfeisg.kauth.core.model.Account
import kotlinx.coroutines.flow.Flow

/** The accounts on this device. Everything stays local; there is no server. */
interface AccountRepository {
    /** All accounts, oldest first. */
    val accounts: Flow<List<Account>>

    suspend fun add(account: Account)

    /** Adds [accounts], skipping any already here. Returns how many were added. */
    suspend fun addAll(accounts: List<Account>): Int

    suspend fun rename(id: Long, issuer: String, name: String)

    /** Moves a counter based account on to its next code. */
    suspend fun incrementCounter(id: Long)

    suspend fun delete(id: Long)
}
