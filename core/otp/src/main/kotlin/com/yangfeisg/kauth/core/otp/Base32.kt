package com.yangfeisg.kauth.core.otp

/** RFC 4648 Base32, as used for setup keys. */
object Base32 {
    private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"

    /**
     * Decodes a setup key. Case, spaces, hyphens and padding are ignored, since keys are
     * often shown grouped ("abcd efgh ...").
     *
     * @throws IllegalArgumentException if the key is empty or has a character outside the alphabet.
     */
    fun decode(text: String): ByteArray {
        val clean = normalize(text)
        require(clean.isNotEmpty()) { "Empty key" }
        val out = ByteArray(clean.length * 5 / 8)
        var buffer = 0
        var bits = 0
        var index = 0
        for (char in clean) {
            val value = ALPHABET.indexOf(char)
            require(value >= 0) { "Invalid Base32 character: $char" }
            buffer = (buffer shl 5) or value
            bits += 5
            if (bits >= 8) {
                bits -= 8
                out[index++] = (buffer shr bits).toByte()
                buffer = buffer and ((1 shl bits) - 1)
            }
        }
        require(out.isNotEmpty()) { "Key too short" }
        return out
    }

    /** Encodes [bytes] as a setup key, without padding. */
    fun encode(bytes: ByteArray): String = buildString {
        var buffer = 0
        var bits = 0
        for (byte in bytes) {
            buffer = (buffer shl 8) or (byte.toInt() and 0xFF)
            bits += 8
            while (bits >= 5) {
                bits -= 5
                append(ALPHABET[(buffer shr bits) and 31])
            }
            buffer = buffer and ((1 shl bits) - 1)
        }
        if (bits > 0) append(ALPHABET[(buffer shl (5 - bits)) and 31])
    }

    /** The canonical form of a typed key: upper case, without grouping or padding. */
    fun normalize(text: String): String = text.filterNot { it.isWhitespace() || it == '-' }.trimEnd('=').uppercase()

    fun isValid(text: String): Boolean = runCatching { decode(text) }.isSuccess
}
