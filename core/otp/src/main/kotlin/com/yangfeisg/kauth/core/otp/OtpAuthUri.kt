package com.yangfeisg.kauth.core.otp

import com.yangfeisg.kauth.core.model.Account
import com.yangfeisg.kauth.core.model.OtpAlgorithm
import com.yangfeisg.kauth.core.model.OtpType
import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder

/**
 * The `otpauth://` Key URI format carried by setup QR codes:
 * `otpauth://totp/Issuer:user@example.com?secret=BASE32&issuer=Issuer&digits=6&period=30`.
 */
object OtpAuthUri {

    /** The account [text] describes, or null if it is not a usable otpauth URI. */
    fun parse(text: String): Account? = runCatching {
        val uri = URI(text.trim())
        if (!uri.scheme.equals("otpauth", ignoreCase = true)) return null
        val type = when (uri.host?.lowercase()) {
            "totp" -> OtpType.TOTP
            "hotp" -> OtpType.HOTP
            else -> return null
        }
        val params = uri.rawQuery.orEmpty().split('&').mapNotNull { pair ->
            val key = pair.substringBefore('=', "")
            if (key.isEmpty()) null else decode(key).lowercase() to decode(pair.substringAfter('='))
        }.toMap()

        val secret = params["secret"]?.takeIf(Base32::isValid) ?: return null
        // A literal "+" in the label is a plus sign, not a form-encoded space.
        val label = decode(uri.rawPath.orEmpty().removePrefix("/").replace("+", "%2B"))
        val labelIssuer = label.substringBefore(':', "").trim()
        val digits = params["digits"]?.toIntOrNull() ?: Account.DEFAULT_DIGITS
        val period = params["period"]?.toIntOrNull() ?: Account.DEFAULT_PERIOD
        if (digits !in 6..8 || period <= 0) return null

        Account(
            issuer = params["issuer"]?.trim().orEmpty().ifEmpty { labelIssuer },
            name = label.substringAfter(':').trim(),
            secret = secret,
            type = type,
            algorithm = params["algorithm"]
                ?.let { name -> OtpAlgorithm.entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: return null }
                ?: OtpAlgorithm.SHA1,
            digits = digits,
            period = period,
            counter = params["counter"]?.toLongOrNull() ?: 0,
        )
    }.getOrNull()

    /** The URI that [parse] turns back into [account], less its id. */
    fun format(account: Account): String = with(account) {
        // The label's issuer loses any colon, so the first colon still ends it when parsed.
        val label = "${issuer.replace(":", "")}:$name"
        val params = buildList {
            add("secret" to Base32.normalize(secret))
            if (issuer.isNotEmpty()) add("issuer" to issuer)
            add("algorithm" to algorithm.name)
            add("digits" to digits.toString())
            when (type) {
                OtpType.TOTP -> add("period" to period.toString())
                OtpType.HOTP -> add("counter" to counter.toString())
            }
        }
        "otpauth://${type.name.lowercase()}/${encode(label)}?" +
            params.joinToString("&") { (key, value) -> "$key=${encode(value)}" }
    }

    private fun decode(value: String) = URLDecoder.decode(value, Charsets.UTF_8)

    // Spaces as %20: a "+" in the label would read back as a plus sign.
    private fun encode(value: String) = URLEncoder.encode(value, Charsets.UTF_8).replace("+", "%20")
}
