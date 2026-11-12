package com.yangfeisg.kauth.core.backup

import com.yangfeisg.kauth.core.model.Account
import com.yangfeisg.kauth.core.model.OtpType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupFileTest {
    private val accounts = listOf(
        Account(issuer = "Example", name = "alice@example.com", secret = "JBSWY3DPEHPK3PXP"),
        Account(name = "counter", secret = "GEZDGNBVGY3TQOJQ", type = OtpType.HOTP, digits = 8, counter = 3),
    )

    @Test
    fun `restores what it backed up with the same password`() {
        val bytes = BackupFile.encrypt(accounts, "correct horse".toCharArray())
        assertTrue(BackupFile.isEncrypted(bytes))
        assertEquals(accounts, BackupFile.decrypt(bytes, "correct horse".toCharArray()))
    }

    @Test
    fun `keeps the secrets out of the file`() {
        val text = String(BackupFile.encrypt(accounts, "pw".toCharArray()), Charsets.ISO_8859_1)
        assertFalse(text.contains("JBSWY3DPEHPK3PXP"))
        assertFalse(text.contains("alice"))
    }

    @Test
    fun `rejects a wrong password`() {
        val bytes = BackupFile.encrypt(accounts, "correct horse".toCharArray())
        assertThrows(WrongPasswordException::class.java) { BackupFile.decrypt(bytes, "wrong".toCharArray()) }
    }

    @Test
    fun `rejects a tampered file`() {
        val bytes = BackupFile.encrypt(accounts, "pw".toCharArray())
        bytes[bytes.size - 1] = (bytes.last() + 1).toByte()
        assertThrows(WrongPasswordException::class.java) { BackupFile.decrypt(bytes, "pw".toCharArray()) }
    }

    @Test
    fun `reads a plain list of otpauth uris without a password`() {
        val text = "otpauth://totp/Example:alice@example.com?secret=JBSWY3DPEHPK3PXP&issuer=Example\n\nnot a uri\n"
        assertEquals(listOf(accounts[0]), BackupFile.decrypt(text.toByteArray()))
    }

    @Test
    fun `rejects files that are not backups`() {
        assertThrows(InvalidBackupException::class.java) { BackupFile.decrypt("hello".toByteArray()) }
        assertThrows(InvalidBackupException::class.java) { BackupFile.decrypt("KAUTH".toByteArray(), "pw".toCharArray()) }
    }
}
