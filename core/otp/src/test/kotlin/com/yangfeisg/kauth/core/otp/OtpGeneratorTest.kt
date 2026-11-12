package com.yangfeisg.kauth.core.otp

import com.yangfeisg.kauth.core.model.Account
import com.yangfeisg.kauth.core.model.OtpAlgorithm
import com.yangfeisg.kauth.core.model.OtpType
import org.junit.Assert.assertEquals
import org.junit.Test

class OtpGeneratorTest {
    private val sha1Secret = "12345678901234567890".toByteArray()

    @Test
    fun `hotp matches RFC 4226 test vectors`() {
        val expected = listOf("755224", "287082", "359152", "969429", "338314")
        assertEquals(expected, expected.indices.map { OtpGenerator.hotp(sha1Secret, it.toLong()) })
    }

    @Test
    fun `totp matches RFC 6238 test vectors`() {
        fun totp(seconds: Long, secret: String, algorithm: OtpAlgorithm) =
            OtpGenerator.hotp(secret.toByteArray(), seconds / 30, digits = 8, algorithm = algorithm)

        assertEquals("94287082", totp(59, "12345678901234567890", OtpAlgorithm.SHA1))
        assertEquals("07081804", totp(1111111109, "12345678901234567890", OtpAlgorithm.SHA1))
        assertEquals("46119246", totp(59, "12345678901234567890123456789012", OtpAlgorithm.SHA256))
        assertEquals(
            "90693936",
            totp(59, "1234567890123456789012345678901234567890123456789012345678901234", OtpAlgorithm.SHA512),
        )
    }

    @Test
    fun `generate uses the time step for time based and the counter for counter based accounts`() {
        val key = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ"
        val totp = Account(name = "a", secret = key, digits = 8)
        val hotp = Account(name = "a", secret = key, type = OtpType.HOTP, counter = 1)

        assertEquals("94287082", OtpGenerator.generate(totp, timeMillis = 59_000))
        assertEquals("287082", OtpGenerator.generate(hotp, timeMillis = 59_000))
    }
}
