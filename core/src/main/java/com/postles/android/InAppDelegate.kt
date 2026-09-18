package com.postles.android

enum class InAppDisplayState {
    SHOW,
    SKIP,
    CONSUME
}

interface InAppDelegate {

    val autoShow: Boolean
        get() = true

    val useDarkMode: Boolean
        get() = false

    fun onNew(notification: PostlesNotification): InAppDisplayState {
        return InAppDisplayState.SHOW
    }

    fun handle(action: InAppAction, context: Map<String, Any>, notification: PostlesNotification)

    fun onError(error: Throwable) {
        // Default empty implementation
    }

    fun onNotificationShown(notification: PostlesNotification) {
        // Default empty implementation
    }
}
