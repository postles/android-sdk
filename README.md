[![](https://jitpack.io/v/parcelvoy/android-sdk.svg)](https://jitpack.io/#parcelvoy/android-sdk)

<p align="center">
  <img width="400" alt="Parcelvoy Logo" src=".github/assets/logo-light.png#gh-light-mode-only" />
  <img width="400" alt="Parcelvoy Logo" src=".github/assets/logo-dark.png#gh-dark-mode-only" />
</p>

# Parcelvoy Android SDK

## Installation
Installing the Parcelvoy Android SDK will provide you with user identification, deeplink unwrapping and basic tracking functionality. The Android SDK is available through jitpack or through manual installation.

### Version Information
- The Parcelvoy Android SDK supports SDK 21+

### Install the SDK
In your **build.gradle** add:
```
dependencies {
    implementation 'com.github.parcelvoy:android-sdk:0.1.7'
}
```

## Usage
### Initialize
Before using any methods, the library must be initialized with an API key and URL endpoint.

Initialize the library:
```kotlin
val analytics = Parcelvoy.initialize(context, YOUR_API_KEY, YOUR_URL_ENDPOINT)
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
In order to send push notifications to a given device you need to register for notifications and then register the device with Parcelvoy. You can do so by using the `register` method. If a user does not grant access to send notifications, you can also call this method without a token to register device characteristics.
```kotlin
analytics.register(
    token = token,
    appBuild = BuildConfig.VERSION_CODE,
    appVersion = BuildConfig.VERSION_NAME
)
```

### Subscription Preferences
Read and modify a user's subscription preferences directly through SDK methods — no UI is included, so you can build your own preference center (or manage preferences programmatically). `getSubscriptions` returns the project's public subscriptions along with the current user's state for each. Use `subscribe`/`unsubscribe` to toggle a single subscription, or `setSubscription` to set an explicit state. The user must be identified first (via `identify`). All of these are `suspend` functions (call them from a coroutine) and return a `Result`.
```kotlin
lifecycleScope.launch {
    // Read the current preferences
    analytics.getSubscriptions().onSuccess { page ->
        page.results.forEach { preference ->
            Log.d("Postles", "${preference.name} (${preference.channel}): ${preference.state}")
        }
    }

    // Toggle a preference
    analytics.unsubscribe(subscriptionId = 123)
    analytics.subscribe(subscriptionId = 123)

    // Or set an explicit state
    analytics.setSubscription(subscriptionId = 123, state = SubscriptionState.UNSUBSCRIBED)
}
```

### Push Notification Opens
Notifications sent through the send API carry a signed open URL in their data payload. Pass that payload to `pushOpened` when the user taps the notification and Parcelvoy records that the message was opened. Payloads without the key are ignored.

First put the push data on the intent your notification opens, so the activity can read it back:

```kotlin
val intent = Intent(applicationContext, MainActivity::class.java).putExtras(bundle)
val pendingIntent = PendingIntent.getActivity(
    applicationContext, 101, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_ONE_SHOT
)
```

Then read it in both places a tap can arrive. A tap that launches the app from cold goes to `onCreate`; a tap while it is already running goes to `onNewIntent`. Handle only one and the more common case records nothing:

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    intent.extras?.let { analytics.pushOpened(it) }
}

override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)

    intent.extras?.let { analytics.pushOpened(it) }
}
```

### Deeplink Navigation
To allow for click tracking links in emails can be click-wrapped in a Parcelvoy url that then needs to be unwrapped for navigation purposes. For information on setting this up on your platform, please see our [deeplink documentation](https://docs.parcelvoy.com/advanced/deeplinking).

Parcelvoy includes a method which checks to see if a given URL is a Parcelvoy URL and if so, unwraps the url, triggers the unwrapped URL and calls the Parcelvoy API to register that the URL was executed.

To start using deeplinking in your app, add your Parcelvoy deployment URL in your activity `intent-filter`. Example in the sample project [dere](samples/kotlin-android-app/src/main/AndroidManifest.xml).

Next, you'll need to update your apps code to support unwrapping the Parcelvoy URLs that open your app. To do so, use the `getUriRedirect(universalLink)` method. In your app delegate's `onNewIntent(intent)` method, unwrap the URL and pass it to the handler:

```kotlin
override fun onNewIntent(intent: Intent?) {
    super.onNewIntent(intent)

    val uri = intent?.data
    if (uri != null) {
        val redirect = analytics.getUriRedirect(uri)
    }
}
```

Parcelvoy links will now be automatically read and opened in your application.

## Example

Explore our [example project](samples/kotlin-android-app) which includes basic usage.
