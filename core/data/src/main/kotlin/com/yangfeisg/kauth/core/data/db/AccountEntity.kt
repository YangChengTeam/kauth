package com.yangfeisg.kauth.core.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.yangfeisg.kauth.core.model.OtpAlgorithm
import com.yangfeisg.kauth.core.model.OtpType

@Entity(tableName = "accounts")
internal data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val issuer: String,
    val name: String,
    val encryptedSecret: String,
    val type: OtpType,
    val algorithm: OtpAlgorithm,
    val digits: Int,
    val period: Int,
    val counter: Long,
)
