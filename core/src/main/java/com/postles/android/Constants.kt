package com.postles.android

import java.text.SimpleDateFormat
import java.util.*

class Constants {
    companion object {
        const val POSTLES_KEY: String = "postles"
        // Pre-rename scheme; existing templates still use it
        const val LEGACY_PARCELVOY_KEY: String = "parcelvoy"
        const val IN_APP_CHECK_MESSAGE_KEY: String = "check_in_app_messages"
        const val IN_APP_FETCH_THROTTLE_MS: Long = 30_000

        val iso8601DateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }
}