package com.parentalcontrol.app.utils

object Constants {

    // Database
    const val DB_NAME = "parental_control_db"

    // SharedPreferences
    const val PREFS_SECURE = "secure_prefs"
    const val PREFS_SECURE_FALLBACK = "secure_prefs_fallback"
    const val PREFS_REGULAR = "app_prefs"

    // Password requirements
    const val MIN_PASSWORD_LENGTH = 8
    const val PBKDF2_ITERATIONS = 120_000
    const val KEY_LENGTH = 256

    // Notification
    const val MONITORING_CHANNEL_ID = "monitoring_channel"
    const val MONITORING_NOTIFICATION_ID = 1001

    // Service actions
    const val ACTION_START_CAMERA = "action_start_camera"
    const val ACTION_START_AUDIO = "action_start_audio"
    const val ACTION_STOP = "action_stop"

    // Log actions
    const val LOG_CAMERA_START = "CAMERA_START"
    const val LOG_CAMERA_STOP = "CAMERA_STOP"
    const val LOG_AUDIO_START = "AUDIO_START"
    const val LOG_AUDIO_STOP = "AUDIO_STOP"
    const val LOG_AUTH_SUCCESS = "AUTH_SUCCESS"
    const val LOG_PASSWORD_SET = "PASSWORD_SET"
}
