package com.postles.android

import com.google.gson.JsonParser
import com.postles.android.network.NetworkManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EventTest {
    private val network = NetworkManager(Config(apiKey = "pk_test", urlEndpoint = "https://postles.test"))

    private fun sentEvent(geo: EventLocation?) = JsonParser.parseString(
        network.gson.toJson(listOf(Event("opened", "anon-1", null, emptyMap(), geo)))
    ).asJsonArray.single().asJsonObject

    @Test
    fun geoIsOmittedNullOrSentAsGiven() {
        assertFalse(sentEvent(null).has("geo"))

        val cleared = sentEvent(EventLocation.NoLocation)
        assertTrue(cleared.has("geo"))
        assertTrue(cleared.get("geo").isJsonNull)

        val located = sentEvent(EventLocation.Location(Geo(country = "US", region = "IL", city = "Chicago")))
        assertEquals(
            JsonParser.parseString("""{"country":"US","region":"IL","city":"Chicago"}"""),
            located.get("geo"),
        )
    }
}
