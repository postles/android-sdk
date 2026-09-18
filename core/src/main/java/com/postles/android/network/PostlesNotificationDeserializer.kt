package com.postles.android.network

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.postles.android.AlertNotification
import com.postles.android.BannerNotification
import com.postles.android.HtmlNotification
import com.postles.android.NotificationContent // Assuming this is a sealed interface or common base class
import com.postles.android.NotificationType
import com.postles.android.PostlesNotification
import java.lang.reflect.Type
import java.util.Date

class PostlesNotificationDeserializer : JsonDeserializer<PostlesNotification> {

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): PostlesNotification {
        val jsonObject = json.asJsonObject
        val contentTypeString = jsonObject.get("content_type").asString
        val contentType = try {
            NotificationType.valueOf(contentTypeString.trim().uppercase())
        } catch (e: IllegalArgumentException) {
            throw JsonParseException("Unknown notification content type: $contentTypeString", e)
        }

        val contentJson = jsonObject.getAsJsonObject("content")
        val notificationContent: NotificationContent = context.deserialize(
            contentJson,
            NotificationContent::class.java
        )
        return PostlesNotification(
            id = jsonObject.get("id")?.asLong ?: 0L,
            contentType = contentType,
            content = notificationContent,
            readAt = context.deserialize<Date>(jsonObject.get("read_at"), Date::class.java),
            expiresAt = context.deserialize<Date>(jsonObject.get("expires_at"), Date::class.java),
        )
    }
}
