package com.yangfeisg.kauth.core.otp

import com.yangfeisg.kauth.core.model.Account
import com.yangfeisg.kauth.core.model.OtpAlgorithm
import com.yangfeisg.kauth.core.model.OtpType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OtpAuthUriTest {
    @Test
    fun `parses a time based uri with defaults`() {
        assertEquals(
            Account(issuer = "Example", name = "alice@example.com", secret = "JBSWY3DPEHPK3PXP"),
            OtpAuthUri.parse("otpauth://totp/Example:alice@example.com?secret=JBSWY3DPEHPK3PXP&issuer=Example"),
        )
    }

    @Test
    fun `parses every parameter and an encoded label`() {
        assertEquals(
            Account(
                issuer = "Big Corp",
                name = "bob+work@example.com",
                secret = "JBSWY3DPEHPK3PXP",
                type = OtpType.HOTP,
                algorithm = OtpAlgorithm.SHA256,
                digits = 8,
                counter = 7,
            ),
            OtpAuthUri.parse(
                "otpauth://hotp/Big%20Corp:%20bob+work@example.com" +
                    "?secret=JBSWY3DPEHPK3PXP&algorithm=sha256&digits=8&counter=7",
            ),
        )
    }

    @Test
    fun `rejects anything that is not a usable otpauth uri`() {
        assertNull(OtpAuthUri.parse("https://example.com"))
        assertNull(OtpAuthUri.parse("otpauth://totp/Example:alice"))
        assertNull(OtpAuthUri.parse("otpauth://totp/Example:alice?secret=not-base32!"))
        assertNull(OtpAuthUri.parse("otpauth://push/Example:alice?secret=JBSWY3DPEHPK3PXP"))
        assertNull(OtpAuthUri.parse("otpauth://totp/Example:alice?secret=JBSWY3DPEHPK3PXP&digits=12"))
        assertNull(OtpAuthUri.parse("just some text"))
    }

    @Test
    fun `formats accounts that parse back unchanged`() {
        val accounts = listOf(
            Account(issuer = "Big Corp", name = "bob+work@example.com", secret = "JBSWY3DPEHPK3PXP"),
            Account(issuer = "a:b", name = "c:d", secret = "JBSWY3DPEHPK3PXP", algorithm = OtpAlgorithm.SHA512, period = 60),
            Account(name = "未命名 账号", secret = "JBSWY3DPEHPK3PXP", type = OtpType.HOTP, digits = 8, counter = 42),
        )
        for (account in accounts) {
            assertEquals(account, OtpAuthUri.parse(OtpAuthUri.format(account)))
        }
    }
}
