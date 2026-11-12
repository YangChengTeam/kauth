package com.yangfeisg.kauth.core.otp

import com.yangfeisg.kauth.core.model.Account
import com.yangfeisg.kauth.core.model.OtpAlgorithm
import com.yangfeisg.kauth.core.model.OtpType
import java.nio.ByteBuffer
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/** One-time passwords: HOTP (RFC 4226) and TOTP (RFC 6238). */
object OtpGenerator {

    /** The code [account] shows at [timeMillis] (the time is ignored for counter based accounts). */
    fun generate(account: Account, timeMillis: Long): String {
        val movingFactor = when (account.type) {
            OtpType.TOTP -> timeMillis / 1000 / account.period
            OtpType.HOTP -> account.counter
        }
        return hotp(Base32.decode(account.secret), movingFactor, account.digits, account.algorithm)
    }

    fun hotp(
        secret: ByteArray,
        counter: Long,
        digits: Int = Account.DEFAULT_DIGITS,
        algorithm: OtpAlgorithm = OtpAlgorithm.SHA1,
    ): String {
        val macName = "Hmac${algorithm.name}"
        val hash = Mac.getInstance(macName).run {
            init(SecretKeySpec(secret, macName))
            doFinal(ByteBuffer.allocate(Long.SIZE_BYTES).putLong(counter).array())
        }
        // Dynamic truncation: the low nibble of the last byte picks 31 bits out of the hash.
        val offset = hash.last().toInt() and 0x0f
        val binary = ByteBuffer.wrap(hash, offset, Int.SIZE_BYTES).int and Int.MAX_VALUE
        var modulus = 1
        repeat(digits) { modulus *= 10 }
        return (binary % modulus).toString().padStart(digits, '0')
    }
}
