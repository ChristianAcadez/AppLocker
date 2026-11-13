package com.christianacadez.applocker.data

import android.content.Context
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

sealed interface PinResult {
    data object Ok : PinResult
    data class Wrong(val attemptsLeft: Int) : PinResult
    data class LockedOut(val secondsLeft: Long) : PinResult
}

class PinStore(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences("pin_store", Context.MODE_PRIVATE)

    fun hasPin(): Boolean = prefs.contains(KEY_HASH)

    fun setPin(pin: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        prefs.edit()
            .putString(KEY_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString(KEY_HASH, Base64.encodeToString(hash(pin, salt), Base64.NO_WRAP))
            .putInt(KEY_FAILS, 0)
            .putLong(KEY_LOCKED_UNTIL, 0L)
            .apply()
    }

    fun verify(pin: String): PinResult {
        val now = System.currentTimeMillis()
        val lockedUntil = prefs.getLong(KEY_LOCKED_UNTIL, 0L)
        if (now < lockedUntil) return PinResult.LockedOut((lockedUntil - now + 999) / 1000)

        val saltB64 = prefs.getString(KEY_SALT, null) ?: return PinResult.Wrong(0)
        val hashB64 = prefs.getString(KEY_HASH, null) ?: return PinResult.Wrong(0)
        val salt = Base64.decode(saltB64, Base64.NO_WRAP)
        val expected = Base64.decode(hashB64, Base64.NO_WRAP)

        if (MessageDigest.isEqual(hash(pin, salt), expected)) {
            prefs.edit().putInt(KEY_FAILS, 0).apply()
            return PinResult.Ok
        }

        val fails = prefs.getInt(KEY_FAILS, 0) + 1
        if (fails >= MAX_ATTEMPTS) {
            prefs.edit()
                .putInt(KEY_FAILS, 0)
                .putLong(KEY_LOCKED_UNTIL, now + LOCKOUT_MS)
                .apply()
            return PinResult.LockedOut(LOCKOUT_MS / 1000)
        }
        prefs.edit().putInt(KEY_FAILS, fails).apply()
        return PinResult.Wrong(MAX_ATTEMPTS - fails)
    }

    private fun hash(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, 256)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
    }

    companion object {
        const val MIN_LENGTH = 4
        const val MAX_LENGTH = 8
        private const val MAX_ATTEMPTS = 5
        private const val LOCKOUT_MS = 5 * 60 * 1000L
        private const val ITERATIONS = 60_000
        private const val KEY_SALT = "salt"
        private const val KEY_HASH = "hash"
        private const val KEY_FAILS = "fails"
        private const val KEY_LOCKED_UNTIL = "locked_until"
    }
}