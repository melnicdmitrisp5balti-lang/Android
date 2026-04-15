package com.parentalcontrol.app.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class PreferenceManager(context: Context) {

    private val prefs: SharedPreferences by lazy {
        createSecurePreferences(context)
    }

    private fun createSecurePreferences(context: Context): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                Constants.PREFS_SECURE,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.e(TAG, "EncryptedSharedPreferences unavailable, using fallback: ${e.message}")
            context.getSharedPreferences(Constants.PREFS_SECURE_FALLBACK, Context.MODE_PRIVATE)
        }
    }

    private val regularPrefs: SharedPreferences =
        context.getSharedPreferences(Constants.PREFS_REGULAR, Context.MODE_PRIVATE)

    companion object {
        private const val TAG = "PreferenceManager"
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

    fun saveChildConnectionCode(code: String) {
        prefs.edit().putString(Constants.PREFS_CHILD_CODE, code).apply()
    }

    fun getChildConnectionCode(): String? = prefs.getString(Constants.PREFS_CHILD_CODE, null)

    fun clearChildConnectionCode() {
        prefs.edit().remove(Constants.PREFS_CHILD_CODE).apply()
    }

    fun saveLastChildHost(host: String) {
        regularPrefs.edit().putString(Constants.PREFS_LAST_CHILD_HOST, host).apply()
    }

    fun getLastChildHost(): String? =
        regularPrefs.getString(Constants.PREFS_LAST_CHILD_HOST, null)
}
