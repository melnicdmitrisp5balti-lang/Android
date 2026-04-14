package com.parentalcontrol.app.data.repository

import android.content.Context
import com.parentalcontrol.app.utils.PreferenceManager
import java.security.MessageDigest

class AuthRepository(context: Context) {

    private val prefs = PreferenceManager(context)

    fun isPasswordSet(): Boolean = prefs.getPassword() != null

    fun setPassword(password: String) {
        prefs.savePassword(hashPassword(password))
    }

    fun verifyPassword(password: String): Boolean {
        val stored = prefs.getPassword() ?: return false
        return stored == hashPassword(password)
    }

    fun clearPassword() {
        prefs.clearPassword()
    }

    private fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(password.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }
}
