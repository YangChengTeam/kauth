package com.yangfeisg.kauth.core.model

enum class OtpType { TOTP, HOTP }

enum class OtpAlgorithm { SHA1, SHA256, SHA512 }

/** One 2-step verification account. [secret] is the shared key, Base32 encoded. */
data class Account(
    val id: Long = 0,
    val issuer: String = "",
    val name: String,
    val secret: String,
    val type: OtpType = OtpType.TOTP,
    val algorithm: OtpAlgorithm = OtpAlgorithm.SHA1,
    val digits: Int = DEFAULT_DIGITS,
    /** Seconds a time based code stays valid. */
    val period: Int = DEFAULT_PERIOD,
    /** Moving factor of a counter based code. */
    val counter: Long = 0,
) {
    val displayName: String
        get() = when {
            issuer.isBlank() -> name
            name.isBlank() -> issuer
            else -> "$issuer ($name)"
        }

    companion object {
        const val DEFAULT_DIGITS = 6
        const val DEFAULT_PERIOD = 30
    }
}
