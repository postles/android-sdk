package com.postles.android

import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.postles.android.network.NetworkManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TopicsTest {
    private val gson: Gson = GsonBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .create()

    @Test
    fun decodesTopicChannels() {
        val response = gson.fromJson(
            """
            {
              "channels": [
                {
                  "channel": "text",
                  "label": "All Text Messages",
                  "master": {
                    "subscription_id": 1,
                    "name": "All Text Messages",
                    "channel": "text",
                    "kind": "channel",
                    "is_opt_in": false,
                    "state": "unsubscribed"
                  },
                  "topics": [
                    {
                      "subscription_id": 11,
                      "name": "Daily Recap",
                      "channel": "text",
                      "kind": "topic",
                      "is_opt_in": true,
                      "state": "not_opted_in"
                    }
                  ],
                  "paused": true,
                  "can_resubscribe": false,
                  "resubscribe_text_number": "+1 312 555 0100"
                }
              ]
            }
            """.trimIndent(),
            TopicChannelsResponse::class.java
        )

        val channel = response.channels.single()
        assertEquals(TopicKind.CHANNEL, channel.master?.kind)
        assertEquals(TopicState.NOT_OPTED_IN, channel.topics.single().state)
        assertTrue(channel.topics.single().isOptIn)
        assertTrue(channel.paused)
        assertFalse(channel.canResubscribe)
        assertEquals("+1 312 555 0100", channel.resubscribeTextNumber)
    }

    @Test
    fun defaultsLegacyRowsToTopicAndNotOptInFalse() {
        val topic = gson.fromJson(
            """
            {
              "subscription_id": 12,
              "name": "Product updates",
              "channel": "email",
              "state": "subscribed"
            }
            """.trimIndent(),
            Topic::class.java
        )

        assertEquals(TopicKind.TOPIC, topic.kind)
        assertFalse(topic.isOptIn)
    }

    @Suppress("DEPRECATION")
    @Test
    fun mapsNotOptedInToLegacyUnsubscribedState() {
        val topic = Topic(
            subscriptionId = 13,
            name = "Daily Recap",
            channel = "text",
            kind = TopicKind.TOPIC,
            isOptIn = true,
            state = TopicState.NOT_OPTED_IN
        )

        assertEquals(SubscriptionState.UNSUBSCRIBED, topic.toSubscriptionPreference().state)
    }

    @Test
    fun mapsNotOptedInUpdatesToUnsubscribed() {
        val update = TopicUpdate(13, TopicState.NOT_OPTED_IN)

        assertEquals(
            """{"subscription_id":13,"state":"unsubscribed"}""",
            gson.toJson(update)
        )
    }

    @Test
    fun parsesLockedResubscribeError() {
        val network = NetworkManager(Config("public-key", "https://example.com"))
        val error = network.parseError(
            422,
            """{"status":"error","error":"Text messages can only be turned back on by replying START from your phone.","code":4004}"""
        )

        assertEquals(422, error.status)
        assertEquals(4004, error.code)
        assertTrue(error.isTopicResubscribeLocked)
        assertEquals(
            "Text messages can only be turned back on by replying START from your phone.",
            error.message
        )
    }
}
