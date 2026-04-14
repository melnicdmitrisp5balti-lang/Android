package com.parentalcontrol.app.data.repository

import android.content.Context
import android.util.Base64
import com.parentalcontrol.app.utils.Constants
import com.parentalcontrol.app.utils.PreferenceManager
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class AuthRepository(context: Context) {

    private val prefs = PreferenceManager(context)

    companion object {
        private const val PBKDF2_ITERATIONS = Constants.PBKDF2_ITERATIONS
        private const val KEY_LENGTH = Constants.KEY_LENGTH
        private const val ALGORITHM = "PBKDF2WithHmacSHA256"
        private const val SALT_LENGTH = 16
        private const val SEPARATOR = ":"
    }

    fun isPasswordSet(): Boolean = prefs.getPassword() != null

    fun setPassword(password: String) {
        val salt = ByteArray(SALT_LENGTH).also { SecureRandom().nextBytes(it) }
        val hash = deriveKey(password, salt)
        val stored = Base64.encodeToString(salt, Base64.NO_WRAP) +
                SEPARATOR +
                Base64.encodeToString(hash, Base64.NO_WRAP)
        prefs.savePassword(stored)
    }

    fun verifyPassword(password: String): Boolean {
        val stored = prefs.getPassword() ?: return false
        val parts = stored.split(SEPARATOR)
        if (parts.size != 2) return false
        val salt = Base64.decode(parts[0], Base64.NO_WRAP)
        val expectedHash = Base64.decode(parts[1], Base64.NO_WRAP)
        val actualHash = deriveKey(password, salt)
        return actualHash.contentEquals(expectedHash)
    }

    fun clearPassword() {
        prefs.clearPassword()
    }

    private fun deriveKey(password: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH)
        return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).encoded
    }
}
