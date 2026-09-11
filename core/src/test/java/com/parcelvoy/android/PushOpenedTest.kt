package com.parcelvoy.android

import android.os.Looper.getMainLooper
import androidx.core.os.bundleOf
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.Shadows.shadowOf
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PushOpenedTest {

    private lateinit var server: MockWebServer
    private lateinit var parcelvoy: Parcelvoy

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        server.enqueue(MockResponse(code = 204))

        parcelvoy = Parcelvoy.initialize(
            app = RuntimeEnvironment.getApplication(),
            apiKey = "pk_test",
            urlEndpoint = server.url("/").toString().removeSuffix("/")
        )
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun `sends one GET to the open url in the payload`() {
        val openUrl = server.url("/o?m=abc&h=def").toString()

        parcelvoy.pushOpened(bundleOf(Constants.PARCELVOY_KEY to "true", Constants.OPEN_URL_KEY to openUrl))
        shadowOf(getMainLooper()).idle()

        val request = server.takeRequest(5, TimeUnit.SECONDS)
        assertNotNull(request)
        assertEquals("GET", request!!.method)
        assertEquals("/o?m=abc&h=def", request.target)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `sends nothing for a payload without an open url`() {
        parcelvoy.pushOpened(bundleOf(Constants.PARCELVOY_KEY to "true", "url" to "myapp://home"))
        shadowOf(getMainLooper()).idle()

        assertNull(server.takeRequest(1, TimeUnit.SECONDS))
        assertEquals(0, server.requestCount)
    }
}
