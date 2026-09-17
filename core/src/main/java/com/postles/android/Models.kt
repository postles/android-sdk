package com.postles.android

import android.os.Build
import android.os.Parcelable
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import com.postles.android.network.NotificationContentDeserializer
import kotlinx.parcelize.Parcelize
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.lang.reflect.Type
import java.util.Date

data class Config(
    val apiKey: String,
    val urlEndpoint: String,
    val inAppDelegate: InAppDelegate? = null,
    val isDebug: Boolean = false,
)

data class Identity(
    val anonymousId: String,
    val externalId: String?,
    val phone: String?,
    val email: String?,
    @SerializedName("data")
    val traits: Map<String, Any>
)

data class Alias(
    val anonymousId: String,
    val externalId: String?
)

data class Event(
    val name: String,
    val anonymousId: String,
    val externalId: String?,
    val properties: Map<String, Any>
)

data class Device(
    val anonymousId: String,
    val externalId: String?,
    val deviceId: String?,
    val token: String?,
    val os: String,
    val osVersion: String,
    val model: String,
    val appBuild: String,
    val appVersion: String,
    val sdkVersion: String,
) {

    constructor(
        anonymousId: String,
        externalId: String?,
        token: String?,
        deviceId: String,
        appBuild: Int,
        appVersion: String,
    ) : this(
        anonymousId = anonymousId,
        externalId = externalId,
        token = token,
        deviceId = deviceId,
        os = "Android",
        osVersion = Build.VERSION.RELEASE,
        model = Build.MODEL,
        appBuild = appBuild.toString(),
        appVersion = appVersion,
        sdkVersion = "1.0.0",
    )
}

data class Page<T>(
    val results: List<T>,
    val nextCursor: String?
)

enum class TopicState {
    @SerializedName("subscribed")
    SUBSCRIBED,

    @SerializedName("unsubscribed")
    UNSUBSCRIBED,

    @SerializedName("not_opted_in")
    NOT_OPTED_IN
}

enum class TopicKind {
    @SerializedName("channel")
    CHANNEL,

    @SerializedName("topic")
    TOPIC
}

@JsonAdapter(TopicDeserializer::class)
data class Topic(
    @SerializedName("subscription_id")
    val subscriptionId: Long,
    val name: String,
    val channel: String,
    val kind: TopicKind = TopicKind.TOPIC,
    @SerializedName("is_opt_in")
    val isOptIn: Boolean = false,
    val state: TopicState
)

class TopicDeserializer : JsonDeserializer<Topic> {
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Topic {
        val value = json.asJsonObject
        return Topic(
            subscriptionId = value.get("subscription_id").asLong,
            name = value.get("name").asString,
            channel = value.get("channel").asString,
            kind = value.get("kind")?.let { context.deserialize(it, TopicKind::class.java) } ?: TopicKind.TOPIC,
            isOptIn = value.get("is_opt_in")?.asBoolean ?: false,
            state = context.deserialize(value.get("state"), TopicState::class.java)
        )
    }
}

data class TopicChannel(
    val channel: String,
    val label: String,
    val master: Topic?,
    val topics: List<Topic>,
    val paused: Boolean,
    val canResubscribe: Boolean,
    val resubscribeTextNumber: String?
)

data class TopicUpdate(
    @SerializedName("subscription_id")
    val subscriptionId: Long,
    val state: TopicState
) {
    init {
        require(state != TopicState.NOT_OPTED_IN) {
            "Topic updates only accept SUBSCRIBED or UNSUBSCRIBED"
        }
    }
}

class PostlesException(
    val status: Int,
    val code: Int?,
    override val message: String
) : IOException(message) {
    val isTopicResubscribeLocked: Boolean
        get() = code == 4004
}

internal data class TopicChannelsResponse(
    val channels: List<TopicChannel>
)

@Deprecated(
    message = "Use TopicState. NOT_OPTED_IN is mapped to UNSUBSCRIBED by the deprecated APIs.",
    replaceWith = ReplaceWith("TopicState")
)
enum class SubscriptionState {
    @SerializedName("subscribed")
    SUBSCRIBED,

    @SerializedName("unsubscribed")
    UNSUBSCRIBED
}

@Deprecated(
    message = "Use Topic. NOT_OPTED_IN is mapped to SubscriptionState.UNSUBSCRIBED by the deprecated APIs.",
    replaceWith = ReplaceWith("Topic")
)
data class SubscriptionPreference(
    @SerializedName("subscription_id")
    val subscriptionId: Long,
    val name: String,
    val channel: String,
    val state: SubscriptionState
)

@Deprecated(
    message = "Use TopicUpdate. Topic identity is sent in request headers.",
    replaceWith = ReplaceWith("TopicUpdate(subscriptionId = TODO(), state = TopicState.UNSUBSCRIBED)")
)
data class SubscriptionUpdate(
    val anonymousId: String,
    val externalId: String?,
    val state: SubscriptionState
)

enum class NotificationType {
    @SerializedName("banner")
    BANNER,

    @SerializedName("alert")
    ALERT,

    @SerializedName("html")
    HTML
}

@JsonAdapter(NotificationContentDeserializer::class)
interface NotificationContent : Parcelable {
    val title: String
    val body: String
    val readOnShow: Boolean?
    val custom: Map<String, Any>?
    val context: Map<String, String>?
}

@Parcelize
data class BannerNotification(
    override val title: String,
    override val body: String,
    override val readOnShow: Boolean? = null,
    override val custom: Map<String, String>? = null,
    override val context: Map<String, String>? = null,
) : NotificationContent

@Parcelize
data class AlertNotification(
    override val title: String,
    override val body: String,
    val image: String?,
    override val readOnShow: Boolean? = null,
    override val custom: Map<String, String>? = null,
    override val context: Map<String, String>? = null,
) : NotificationContent

@Parcelize
data class HtmlNotification(
    override val title: String,
    override val body: String,
    val html: String,
    override val readOnShow: Boolean? = null,
    override val custom: Map<String, String>? = null,
    override val context: Map<String, String>? = null
) : NotificationContent

@Parcelize
data class PostlesNotification(
    val id: Long,
    val contentType: NotificationType,
    val content: NotificationContent,
    val readAt: Date?,
    val expiresAt: Date?
) : Parcelable

enum class InAppAction {
    DISMISS,
    CUSTOM,
}

class PostlesAction(
    val config: JSONObject
) {

    var userInput: String? = null

    val type: String?
        get() = config.optString("type")

    val data: String?
        get() = config.optString("data")

    fun isOfType(type: String): Boolean {
        return this.type != null && this.type == type
    }

    companion object {
        const val ACTION_TYPE_OPEN_URL: String = "openUrl"

        fun from(config: JSONObject?): PostlesAction? = config?.let { PostlesAction(it) }

        fun actionOpenUrl(url: String?): PostlesAction? =
            url?.let {
                val config = JSONObject()
                config.put("type", "openUrl")
                config.put("data", url)
                PostlesAction(config)
            }

        fun actionCustomAction(customActionName: String): PostlesAction? {
            val config = JSONObject()
            config.put("type", customActionName)
            return PostlesAction(config)
        }
    }
}
