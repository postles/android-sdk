# Postles Android SDK
[![](https://jitpack.io/v/postles/android-sdk.svg)](https://jitpack.io/#postles/android-sdk)

## Installation
Installing the Postles Android SDK will provide you with user identification, deeplink unwrapping and basic tracking functionality. The Android SDK is available through jitpack or through manual installation.

### Version Information
- The Postles Android SDK supports SDK 21+

### Install the SDK
In your **build.gradle** add:
```
dependencies {
    implementation 'com.github.postles:android-sdk:1.2.0'
}
```

## Usage
### Initialize
Before using any methods, the library must be initialized with an API key and URL endpoint.

Initialize the library:
```kotlin
val analytics = Postles.initialize(context, YOUR_API_KEY, YOUR_URL_ENDPOINT)
```

### Identify
You can handle the user identity of your users by using the `identify` method. This method works in combination either/or associate a given user to your internal user ID (`external_id`) or to associate attributes (traits) to the user. By default all events and traits are associated with an anonymous ID until a user is identified with an `external_id`. From that point moving forward, all updates to the user and events will be associated to your provider identifier.
```kotlin
analytics.identify(
    id = USER_ID,
    traits = mapOf(
        "first_name" to "John",
        "last_name" to "Doe"
    )
)
```

### Events
If you want to trigger journey and list updates off of things a user does within your app, you can pass up those events by using the `track` method.
```kotlin
analytics.track(
    event = "Application Opened",
    properties = mapOf("property" to true)
)
```

### Register Device
In order to send push notifications to a given device you need to register for notifications and then register the device with Postles. You can do so by using the `register` method. If a user does not grant access to send notifications, you can also call this method without a token to register device characteristics.
```kotlin
analytics.register(
    token = token,
    appBuild = BuildConfig.VERSION_CODE,
    appVersion = BuildConfig.VERSION_NAME
)
```

### Topics
The SDK provides the data and methods for building a preference center. It does not include UI. Identify the user before calling these `suspend` functions.

`getTopicChannels` groups each channel master with its topics. Show topic toggles when a channel has more than one topic or any opt-in topic. If a master is paused, keep its topic values disabled and do not submit them. A locked master can be turned off in the app, but the user must text START to turn it back on.

```kotlin
private var loadedChannels: List<TopicChannel> = emptyList()

fun loadTopicPreferences() = lifecycleScope.launch {
    analytics.getTopicChannels().onSuccess { channels ->
        loadedChannels = channels
        channels.forEach { channel ->
            val showTopics = channel.topics.size > 1 || channel.topics.any { it.isOptIn }
            val locked = channel.paused && !channel.canResubscribe

            if (locked) {
                showStartNotice(channel.resubscribeTextNumber)
            } else {
                showMasterToggle(channel.master)
            }

            if (showTopics) {
                showTopicToggles(
                    topics = channel.topics,
                    selected = { it.state == TopicState.SUBSCRIBED },
                    enabled = !channel.paused
                )
            }
        }
    }.onFailure(::showError)
}

fun saveTopicPreferences() {
    val updates = loadedChannels.flatMap { channel ->
        buildList {
            channel.master
                ?.takeUnless { channel.paused && !channel.canResubscribe }
                ?.let { add(TopicUpdate(it.subscriptionId, selectedState(it))) }

            if (!channel.paused) {
                channel.topics.forEach {
                    add(TopicUpdate(it.subscriptionId, selectedState(it)))
                }
            }
        }
    }

    lifecycleScope.launch {
        analytics.setTopics(updates).onFailure { error ->
            if (error is PostlesException && error.isTopicResubscribeLocked) {
                val textNumber = loadedChannels
                    .firstOrNull { it.channel == "text" }
                    ?.resubscribeTextNumber
                showStartNotice(textNumber)
            } else {
                showError(error)
            }
        }
    }
}

fun selectedState(topic: Topic): TopicState =
    if (isTopicSelected(topic)) TopicState.SUBSCRIBED else TopicState.UNSUBSCRIBED
```

`NOT_OPTED_IN` means the user has not made a choice for an opt-in topic. Render it unchecked. When the user taps Save, map the control's current value to `SUBSCRIBED` or `UNSUBSCRIBED` as `selectedState` does above. If a `TopicUpdate` is constructed with `NOT_OPTED_IN`, it serializes that state as `UNSUBSCRIBED`, so the read-only state is never sent to the server.

The previous names remain available as deprecated aliases:

| Previous name | Replacement |
|---|---|
| `SubscriptionState` | `TopicState` |
| `SubscriptionPreference` | `Topic` |
| `SubscriptionUpdate` | `TopicUpdate` |
| `getSubscriptions()` | `getTopics()` |
| `setSubscription()` | `setTopic()` |
| `subscribe()` | `subscribeTopic()` |
| `unsubscribe()` | `unsubscribeTopic()` |

The legacy API reports `not_opted_in` as `SubscriptionState.UNSUBSCRIBED`.

### Deeplink Navigation
To allow for click tracking links in emails can be click-wrapped in a Postles url that then needs to be unwrapped for navigation purposes. For information on setting this up on your platform, please see our [deeplink documentation](https://docs.postles.com/advanced/deeplinking).

Postles includes a method which checks to see if a given URL is a Postles URL and if so, unwraps the url, triggers the unwrapped URL and calls the Postles API to register that the URL was executed.

To start using deeplinking in your app, add your Postles deployment URL in your activity `intent-filter`. Example in the sample project [dere](samples/kotlin-android-app/src/main/AndroidManifest.xml).

Next, you'll need to update your apps code to support unwrapping the Postles URLs that open your app. To do so, use the `getUriRedirect(universalLink)` method. In your app delegate's `onNewIntent(intent)` method, unwrap the URL and pass it to the handler:

```kotlin
override fun onNewIntent(intent: Intent?) {
    super.onNewIntent(intent)

    val uri = intent?.data
    if (uri != null) {
        val redirect = analytics.getUriRedirect(uri)
    }
}
```

Postles links will now be automatically read and opened in your application.

## Example

Explore our [example project](samples/kotlin-android-app) which includes basic usage.
