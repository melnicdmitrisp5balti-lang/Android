package com.parentalcontrol.app.utils

import org.json.JSONObject

object SocketManager {

    fun createClientConnect(code: String): String = JSONObject()
        .put("type", Constants.MSG_CLIENT_CONNECT)
        .put("code", code)
        .toString()

    fun createServerOk(childName: String): String = JSONObject()
        .put("type", Constants.MSG_SERVER_OK)
        .put("status", "connected")
        .put("child_name", childName)
        .toString()

    fun createServerError(message: String): String = JSONObject()
        .put("type", Constants.MSG_SERVER_ERROR)
        .put("status", "error")
        .put("message", message)
        .toString()

    fun parse(raw: String): JSONObject = JSONObject(raw)
}
