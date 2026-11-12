package com.yangfeisg.kauth.core.otp

import com.yangfeisg.kauth.core.model.Account
import com.yangfeisg.kauth.core.model.OtpAlgorithm
import com.yangfeisg.kauth.core.model.OtpType
import java.net.URI
import java.net.URLDecoder
import java.util.Base64

/**
 * The `otpauth-migration://offline?data=...` QR codes Google Authenticator shows under
 * "Transfer accounts". `data` is a Base64 encoded protobuf `MigrationPayload`. Many accounts
 * are split over several codes, each one a batch of the same export.
 */
object GoogleAuthMigration {

    /** One QR code's worth of an export. */
    data class Batch(
        val accounts: List<Account>,
        /** Accounts in the code that this app cannot use, such as MD5 ones. */
        val skipped: Int,
        val index: Int,
        val size: Int,
        /** Shared by every code of the same export. */
        val id: Int,
    )

    fun isMigration(text: String): Boolean = text.trim().startsWith("otpauth-migration:", ignoreCase = true)

    /** The batch [text] carries, or null if it is not a readable migration URI. */
    fun parse(text: String): Batch? = runCatching {
        if (!isMigration(text)) return null
        val data = URI(text.trim()).rawQuery.orEmpty().split('&')
            .firstOrNull { it.substringBefore('=') == "data" }
            ?.substringAfter('=')
            ?: return null
        // A "+" left unencoded would decode to a space, but in Base64 it can only be a plus.
        val bytes = Base64.getDecoder().decode(URLDecoder.decode(data, Charsets.UTF_8).replace(' ', '+'))

        val accounts = mutableListOf<Account>()
        var entries = 0
        var index = 0
        var size = 1
        var id = 0
        ProtoReader(bytes).forEachField { field, value ->
            when (field) {
                1 -> {
                    entries++
                    parseAccount(value as ByteArray)?.let(accounts::add)
                }
                3 -> size = (value as Long).toInt()
                4 -> index = (value as Long).toInt()
                5 -> id = (value as Long).toInt()
            }
        }
        if (entries == 0) return null
        Batch(accounts, skipped = entries - accounts.size, index = index, size = size.coerceAtLeast(1), id = id)
    }.getOrNull()

    private fun parseAccount(bytes: ByteArray): Account? {
        var secret = ByteArray(0)
        var name = ""
        var issuer = ""
        var algorithm = 0L
        var digits = 0L
        var type = 0L
        var counter = 0L
        ProtoReader(bytes).forEachField { field, value ->
            when (field) {
                1 -> secret = value as ByteArray
                2 -> name = (value as ByteArray).decodeToString()
                3 -> issuer = (value as ByteArray).decodeToString()
                4 -> algorithm = value as Long
                5 -> digits = value as Long
                6 -> type = value as Long
                7 -> counter = value as Long
            }
        }
        if (secret.isEmpty()) return null
        // The name is often the whole otpauth label, "Issuer:user".
        val labelIssuer = name.substringBefore(':', "").trim()
        return Account(
            issuer = issuer.trim().ifEmpty { labelIssuer },
            name = name.substringAfter(':').trim(),
            secret = Base32.encode(secret),
            type = when (type) {
                1L -> OtpType.HOTP
                0L, 2L -> OtpType.TOTP
                else -> return null
            },
            algorithm = when (algorithm) {
                0L, 1L -> OtpAlgorithm.SHA1
                2L -> OtpAlgorithm.SHA256
                3L -> OtpAlgorithm.SHA512
                else -> return null
            },
            digits = when (digits) {
                0L, 1L -> 6
                2L -> 8
                else -> return null
            },
            counter = counter,
        )
    }
}

/** Just enough protobuf wire format to walk a message's fields. */
private class ProtoReader(private val bytes: ByteArray) {
    private var position = 0

    /** Calls [action] with each field's number and its value: a Long for varints, a ByteArray for the rest. */
    fun forEachField(action: (field: Int, value: Any) -> Unit) {
        while (position < bytes.size) {
            val tag = readVarint()
            val field = (tag ushr 3).toInt()
            val value: Any = when ((tag and 7).toInt()) {
                0 -> readVarint()
                1 -> readBytes(8)
                2 -> readBytes(readVarint().toInt())
                5 -> readBytes(4)
                else -> throw IllegalArgumentException("Unsupported wire type in tag $tag")
            }
            action(field, value)
        }
    }

    private fun readVarint(): Long {
        var result = 0L
        var shift = 0
        while (true) {
            require(shift < 64 && position < bytes.size) { "Malformed varint" }
            val byte = bytes[position++].toInt()
            result = result or ((byte and 0x7F).toLong() shl shift)
            if (byte and 0x80 == 0) return result
            shift += 7
        }
    }

    private fun readBytes(length: Int): ByteArray {
        require(length >= 0 && position + length <= bytes.size) { "Truncated field" }
        return bytes.copyOfRange(position, position + length).also { position += length }
    }
}
