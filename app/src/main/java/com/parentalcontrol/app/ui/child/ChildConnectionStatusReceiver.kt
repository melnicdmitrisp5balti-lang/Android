package com.parentalcontrol.app.ui.child

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.parentalcontrol.app.utils.Constants

class ChildConnectionStatusReceiver(
    private val onStatus: (String, Boolean) -> Unit
) : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        val status = intent?.getStringExtra(Constants.EXTRA_CONNECTION_STATUS) ?: return
        val parentConnected = intent.getBooleanExtra(Constants.EXTRA_PARENT_CONNECTED, false)
        onStatus(status, parentConnected)
    }
}
