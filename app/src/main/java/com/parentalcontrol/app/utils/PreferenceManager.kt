package com.parentalcontrol.app.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class PreferenceManager(context: Context) {

    private val prefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                "secure_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            context.getSharedPreferences("fallback_prefs", Context.MODE_PRIVATE)
        }
    }

    private val regularPrefs: SharedPreferences =
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_PASSWORD = "parent_password"
        private const val KEY_WARNING_ACCEPTED = "warning_accepted"
        private const val KEY_IS_AUTHENTICATED = "is_authenticated"
    }

    fun savePassword(hashedPassword: String) {
        prefs.edit().putString(KEY_PASSWORD, hashedPassword).apply()
    }

    fun getPassword(): String? = prefs.getString(KEY_PASSWORD, null)

    fun clearPassword() {
        prefs.edit().remove(KEY_PASSWORD).apply()
    }

    fun setWarningAccepted(accepted: Boolean) {
        regularPrefs.edit().putBoolean(KEY_WARNING_ACCEPTED, accepted).apply()
    }

    fun isWarningAccepted(): Boolean =
        regularPrefs.getBoolean(KEY_WARNING_ACCEPTED, false)

    fun setAuthenticated(authenticated: Boolean) {
        regularPrefs.edit().putBoolean(KEY_IS_AUTHENTICATED, authenticated).apply()
    }

    fun isAuthenticated(): Boolean =
        regularPrefs.getBoolean(KEY_IS_AUTHENTICATED, false)
}
