package com.example.hostelfix.data

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PasswordHasher {
    private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val ITERATIONS = 100_000
    private const val KEY_LENGTH_BITS = 256
    private const val EXPECTED_HASH_BYTES = KEY_LENGTH_BITS / 8
    private const val SALT_BYTES = 16

    fun hashPassword(plainPassword: String): String {
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        val chars = plainPassword.toCharArray()
        val derived = pbkdf2(chars, salt, ITERATIONS, KEY_LENGTH_BITS)
        val saltB64 = Base64.encodeToString(salt, Base64.NO_WRAP)
        val hashB64 = Base64.encodeToString(derived, Base64.NO_WRAP)
        return "$saltB64:$hashB64"
    }

    fun verifyPassword(plainPassword: String, storedValue: String): Boolean {
        val parts = storedValue.split(":")
        if (parts.size != 2 || parts[0].isBlank() || parts[1].isBlank()) return false

        return try {
            val salt = Base64.decode(parts[0], Base64.NO_WRAP)
            val expectedHash = Base64.decode(parts[1], Base64.NO_WRAP)
            if (salt.size != SALT_BYTES || expectedHash.size != EXPECTED_HASH_BYTES) {
                return false
            }

            val chars = plainPassword.toCharArray()
            val actualHash = pbkdf2(chars, salt, ITERATIONS, KEY_LENGTH_BITS)
            MessageDigest.isEqual(expectedHash, actualHash)
        } catch (_: Exception) {
            false
        }
    }

    private fun pbkdf2(chars: CharArray, salt: ByteArray, iterations: Int, keyLengthBits: Int): ByteArray {
        val spec = PBEKeySpec(chars, salt, iterations, keyLengthBits)
        return try {
            SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
            chars.fill('\u0000')
        }
    }
}