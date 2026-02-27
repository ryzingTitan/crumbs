package com.ryzingtitan.crumbs.wearable

import android.content.Intent
import android.net.Uri
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

class WearMessageListenerService : WearableListenerService() {
    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path == "/crumbs/open-url") {
            val url = String(messageEvent.data, Charsets.UTF_8)
            startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addCategory(Intent.CATEGORY_BROWSABLE)
                }
            )
        }
    }
}
