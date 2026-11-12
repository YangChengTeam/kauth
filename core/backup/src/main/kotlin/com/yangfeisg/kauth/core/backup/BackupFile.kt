package com.yangfeisg.kauth.core.backup

import com.yangfeisg.kauth.core.model.Account
import com.yangfeisg.kauth.core.otp.OtpAuthUri
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.nio.BufferUnderflowException
import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * A password protected backup of accounts, for moving them to another device by hand.
 *
 * Layout: the magic `KAUTH`, a version byte, the PBKDF2 iteration count, salt and IV, then
 * AES-256-GCM ciphertext of one `otpauth://` URI per line. The header is authenticated too.
 * A plain text file of `otpauth://` URIs, as other apps export, can be read as well.
 */
object BackupFile {

    fun encrypt(accounts: List<Account>, password: CharArray): ByteArray {
        val random = SecureRandom()
        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        val iv = ByteArray(IV_BYTES).also(random::nextBytes)
        val header = ByteArrayOutputStream().also { bytes ->
            DataOutputStream(bytes).run {
                write(MAGIC)
                writeByte(VERSION)
                writeInt(ITERATIONS)
                write(salt)
                write(iv)
            }
        }.toByteArray()
        val plainText = accounts.joinToString("\n", transform = OtpAuthUri::format).toByteArray()
        val cipher = cipher(Cipher.ENCRYPT_MODE, password, salt, ITERATIONS, iv).apply { updateAAD(header) }
        return header + cipher.doFinal(plainText)
    }

    /** Whether [bytes] are an encrypted backup, which needs a password to [decrypt]. */
    fun isEncrypted(bytes: ByteArray): Boolean =
        bytes.size >= MAGIC.size && bytes.copyOfRange(0, MAGIC.size).contentEquals(MAGIC)

    /**
     * The accounts in [bytes]. [password] is only needed when they are [isEncrypted].
     *
     * @throws WrongPasswordException if the password does not open the backup.
     * @throws InvalidBackupException if the bytes are not a backup this app can read.
     */
    fun decrypt(bytes: ByteArray, password: CharArray? = null): List<Account> {
        if (!isEncrypted(bytes)) return parse(String(bytes)).ifEmpty { throw InvalidBackupException() }
        requireNotNull(password) { "An encrypted backup needs a password" }
        val header = ByteBuffer.wrap(bytes, MAGIC.size, bytes.size - MAGIC.size)
        val plainText = try {
            if (header.get().toInt() != VERSION) throw InvalidBackupException()
            val iterations = header.getInt()
            if (iterations !in 1..MAX_ITERATIONS) throw InvalidBackupException()
            val salt = ByteArray(SALT_BYTES).also(header::get)
            val iv = ByteArray(IV_BYTES).also(header::get)
            val headerSize = header.position()
            cipher(Cipher.DECRYPT_MODE, password, salt, iterations, iv).run {
                updateAAD(bytes, 0, headerSize)
                doFinal(bytes, headerSize, bytes.size - headerSize)
            }
        } catch (_: AEADBadTagException) {
            throw WrongPasswordException()
        } catch (_: BufferUnderflowException) {
            throw InvalidBackupException()
        }
        return parse(String(plainText))
    }

    /** The default file name for a backup made on [date] (`yyyy-MM-dd`). */
    fun fileName(date: String) = "kauth-backup-$date.$EXTENSION"

    const val EXTENSION = "kauth"

    private fun parse(text: String): List<Account> =
        text.lineSequence().map(String::trim).filter(String::isNotEmpty).mapNotNull(OtpAuthUri::parse).toList()

    private fun cipher(mode: Int, password: CharArray, salt: ByteArray, iterations: Int, iv: ByteArray): Cipher {
        val spec = PBEKeySpec(password, salt, iterations, KEY_BITS)
        val key = try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
        return Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(mode, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, iv))
        }
    }

    private val MAGIC = "KAUTH".toByteArray()
    private const val VERSION = 1
    private const val ITERATIONS = 210_000
    private const val MAX_ITERATIONS = 10_000_000
    private const val SALT_BYTES = 16
    private const val IV_BYTES = 12
    private const val KEY_BITS = 256
    private const val TAG_BITS = 128
}

class WrongPasswordException : Exception("Wrong backup password")

class InvalidBackupException : Exception("Not a backup file")
