package com.yangfeisg.kauth.core.data

import com.yangfeisg.kauth.core.data.crypto.SecretCipher
import com.yangfeisg.kauth.core.data.db.AccountDao
import com.yangfeisg.kauth.core.data.db.AccountEntity
import com.yangfeisg.kauth.core.model.Account
import com.yangfeisg.kauth.core.otp.Base32
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** Room backed accounts; the secret column only ever holds [SecretCipher] output. */
@Singleton
internal class OfflineAccountRepository @Inject constructor(
    private val dao: AccountDao,
    private val cipher: SecretCipher,
) : AccountRepository {

    override val accounts: Flow<List<Account>> = dao.observeAll()
        .map { entities -> entities.map { it.toModel() } }
        .flowOn(Dispatchers.Default)

    override suspend fun add(account: Account) {
        val entity = withContext(Dispatchers.Default) { account.toEntity() }
        dao.insert(entity)
    }

    override suspend fun addAll(accounts: List<Account>): Int {
        val existing = this.accounts.first().mapTo(mutableSetOf()) { it.identity() }
        val entities = withContext(Dispatchers.Default) {
            accounts.filter { existing.add(it.identity()) }.map { it.toEntity() }
        }
        dao.insertAll(entities)
        return entities.size
    }

    override suspend fun rename(id: Long, issuer: String, name: String) = dao.rename(id, issuer, name)

    override suspend fun incrementCounter(id: Long) = dao.incrementCounter(id)

    override suspend fun delete(id: Long) = dao.delete(id)

    /** Two accounts are the same one if they agree on everything but id and counter. */
    private fun Account.identity() = copy(id = 0, secret = Base32.normalize(secret), counter = 0)

    private fun AccountEntity.toModel() = Account(
        id = id,
        issuer = issuer,
        name = name,
        secret = cipher.decrypt(encryptedSecret),
        type = type,
        algorithm = algorithm,
        digits = digits,
        period = period,
        counter = counter,
    )

    private fun Account.toEntity() = AccountEntity(
        issuer = issuer,
        name = name,
        encryptedSecret = cipher.encrypt(secret),
        type = type,
        algorithm = algorithm,
        digits = digits,
        period = period,
        counter = counter,
    )
}
