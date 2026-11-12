package com.yangfeisg.kauth.core.otp

import com.yangfeisg.kauth.core.model.Account
import com.yangfeisg.kauth.core.model.OtpAlgorithm
import com.yangfeisg.kauth.core.model.OtpType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GoogleAuthMigrationTest {
    // Second of two codes in export 12345: a TOTP account, an HOTP one named "Issuer:user", and an MD5 one.
    private val uri = "otpauth-migration://offline?data=Ci4KCkhlbGxvId6tvu8SEWFsaWNlQGV4YW1wbGUuY29tGgdFeGFtcGxlIAE" +
        "oATACCiIKCkhlbGxvId6tvu8SDEJpZyBDb3JwOmJvYiACKAIwATgHChcKCkhlbGxvId6tvu8SA29sZCAEKAEwAhABGAIgASi5YA%3D%3D"

    @Test
    fun `parses every account a batch carries`() {
        assertEquals(
            GoogleAuthMigration.Batch(
                accounts = listOf(
                    Account(issuer = "Example", name = "alice@example.com", secret = "JBSWY3DPEHPK3PXP"),
                    Account(
                        issuer = "Big Corp",
                        name = "bob",
                        secret = "JBSWY3DPEHPK3PXP",
                        type = OtpType.HOTP,
                        algorithm = OtpAlgorithm.SHA256,
                        digits = 8,
                        counter = 7,
                    ),
                ),
                skipped = 1,
                index = 1,
                size = 2,
                id = 12345,
            ),
            GoogleAuthMigration.parse(uri),
        )
    }

    @Test
    fun `reads unencoded base64 too`() {
        assertEquals(2, GoogleAuthMigration.parse(uri.replace("%3D", "="))?.accounts?.size)
    }

    @Test
    fun `rejects anything that is not a readable migration uri`() {
        assertFalse(GoogleAuthMigration.isMigration("otpauth://totp/x?secret=JBSWY3DPEHPK3PXP"))
        assertTrue(GoogleAuthMigration.isMigration(uri))
        assertNull(GoogleAuthMigration.parse("otpauth://totp/x?secret=JBSWY3DPEHPK3PXP"))
        assertNull(GoogleAuthMigration.parse("otpauth-migration://offline"))
        assertNull(GoogleAuthMigration.parse("otpauth-migration://offline?data=not*base64"))
        assertNull(GoogleAuthMigration.parse("otpauth-migration://offline?data=CgE%3D"))
    }

    @Test
    fun `base32 encoding round trips`() {
        for (key in listOf("JBSWY3DPEHPK3PXP", "GEZDGNBVGY3TQOJQ", "MZXW6YQ", "AE")) {
            assertEquals(key, Base32.encode(Base32.decode(key)))
        }
    }
}
