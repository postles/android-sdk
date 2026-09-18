package com.postles.example

import android.app.Application
import android.util.Log
import android.widget.Toast
import com.postles.android.InAppAction
import com.postles.android.InAppDelegate
import com.postles.android.InAppDisplayState
import com.postles.android.Postles
import com.postles.android.PostlesNotification

class MainApplication : Application(), InAppDelegate {

    override fun onCreate() {
        super.onCreate()

        // TODO: Enter API Key and URL
        val apiKey = "" // like: pk_fdfbi282ec65-4a4f-b9ef-6f6979905523
        val urlEndpoint = "" // like: https://postles.company.com/api
        analytics = Postles.initialize(
            app = this,
            apiKey = apiKey,
            urlEndpoint = urlEndpoint,
            isDebug = true,
            inAppDelegate = this
        )
    }

    override val autoShow: Boolean = true

    override val useDarkMode: Boolean = false

    override fun onNew(notification: PostlesNotification): InAppDisplayState {
        Log.d(LOG_TAG, "onNew: $notification")
        return InAppDisplayState.SHOW
    }

    override fun handle(
        action: InAppAction,
        context: Map<String, Any>,
        notification: PostlesNotification
    ) {
        Log.d(LOG_TAG, "handle: $action, context: $context, notification: $notification")
        Toast.makeText(this, "Action: $action", Toast.LENGTH_SHORT).show()
    }

    override fun onError(error: Throwable) {
        Log.e(LOG_TAG, "onError: $error")
        super.onError(error)
    }

    companion object {
        private const val LOG_TAG = "MainApplication"
        
        lateinit var analytics: Postles
    }
}