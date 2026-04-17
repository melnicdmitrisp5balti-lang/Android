package com.parentalcontrol.app.utils

object Constants {

    // Database
    const val DB_NAME = "parental_control_db"

    // SharedPreferences
    const val PREFS_SECURE = "secure_prefs"
    const val PREFS_SECURE_FALLBACK = "secure_prefs_fallback"
    const val PREFS_REGULAR = "app_prefs"
    const val PREFS_CHILD_CODE = "child_connection_code"
    const val PREFS_LAST_CHILD_HOST = "last_child_host"
    const val PREFS_LAST_CONNECTION_CODE = "last_connection_code"

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
    const val LOG_CONNECTION_ATTEMPT = "CONNECTION_ATTEMPT"
    const val LOG_CONNECTION_SUCCESS = "CONNECTION_SUCCESS"
    const val LOG_CONNECTION_REJECTED = "CONNECTION_REJECTED"

    // Socket
    const val DEFAULT_SOCKET_PORT = 5050
    const val DEFAULT_MJPEG_PORT = 8080
    const val MJPEG_STREAM_PATH = "/video.mjpeg"
    const val SOCKET_TIMEOUT_MS = 30 * 60 * 1000L
    const val SOCKET_CONNECT_TIMEOUT_MS = 1_200
    const val SOCKET_HANDSHAKE_TIMEOUT_MS = 3_000
    const val STREAM_RECONNECT_DELAY_MS = 1_500L
    const val MAX_MJPEG_RECONNECT_ATTEMPTS = 3
    const val SOCKET_SCAN_CHUNK_SIZE = 24
    const val SOCKET_SCAN_HOST_MIN = 2
    const val SOCKET_SCAN_HOST_MAX = 220
    const val MSG_CLIENT_CONNECT = "CLIENT_CONNECT"
    const val MSG_SERVER_OK = "SERVER_OK"
    const val MSG_SERVER_ERROR = "SERVER_ERROR"
    const val MSG_VIDEO_STREAM = "VIDEO_STREAM"
    const val MSG_AUDIO_STREAM = "AUDIO_STREAM"
    const val MSG_ACTIVITY_LOG = "ACTIVITY_LOG"

    // Child server broadcast
    const val ACTION_CHILD_CONNECTION_STATUS = "action_child_connection_status"
    const val EXTRA_CONNECTION_STATUS = "extra_connection_status"
    const val EXTRA_PARENT_CONNECTED = "extra_parent_connected"

    // Cloud signaling (Firebase Realtime Database)
    // Replace with your Firebase project's Realtime Database URL:
    // e.g. "https://my-project-default-rtdb.firebaseio.com"
    const val FIREBASE_DATABASE_URL = ""

    // WebRTC STUN / TURN servers
    // Public Google STUN server — no credentials required.
    const val STUN_SERVER_URI = "stun:stun.l.google.com:19302"
    // Replace with your TURN server credentials for NAT traversal across
    // restrictive networks (e.g. a Twilio TURN or self-hosted COTURN).
    const val TURN_SERVER_URI = ""
    const val TURN_USERNAME = ""
    const val TURN_CREDENTIAL = ""
}
