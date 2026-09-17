package com.postles.android.network

import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.postles.android.Alias
import com.postles.android.Config
import com.postles.android.NotificationContent
import com.postles.android.PostlesNotification
import com.postles.android.PostlesException
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.coroutines.executeAsync
import okhttp3.logging.HttpLoggingInterceptor
import java.io.IOException
import java.net.URL
import java.util.Date

class NetworkManager(
    internal val config: Config,
) {

    private val gson: Gson = GsonBuilder()
        .serializeNulls()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .registerTypeAdapter(Date::class.java, DateAdapter())
        .registerTypeAdapter(PostlesNotification::class.java, PostlesNotificationDeserializer())
        .create()

    private val httpLoggingInterceptor: HttpLoggingInterceptor
        get() {
            val interceptor = HttpLoggingInterceptor()
            interceptor.level = if (config.isDebug) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
            return interceptor
        }

    private val client: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(httpLoggingInterceptor)
        .build()

    internal suspend inline fun <reified T> get(
        path: String,
        user: Alias,
        useBaseUri: Boolean = true,
    ): Result<T> {
        requireNotNull(user.externalId)

        val url = if (useBaseUri) URL("${config.urlEndpoint}/api/client/$path") else URL(path)
        val request = Request.Builder()
            .url(url)
            .get()
            .addHeaders(config)
            .addHeader("x-anonymous-id", user.anonymousId)
            .addHeader("x-external-id", user.externalId)
            .build()
        return execute(request)
    }

    internal suspend inline fun <reified T> put(
        path: String,
        body: Any,
        user: Alias? = null,
        useBaseUri: Boolean = true,
    ): Result<T> {
        val url = if (useBaseUri) URL("${config.urlEndpoint}/api/client/$path") else URL(path)
        val requestBody = gson.toJson(body).toRequestBody()
        val request = Request.Builder().url(url)
            .put(requestBody)
            .addHeaders(config)
            .apply {
                user?.let {
                    addHeader("x-anonymous-id", it.anonymousId)
                    it.externalId?.let { externalId -> addHeader("x-external-id", externalId) }
                }
            }
            .build()
        return execute(request)
    }

    internal suspend inline fun <reified T> post(
        path: String,
        body: Any,
        useBaseUri: Boolean = true,
    ): Result<T> {
        val url = if (useBaseUri) URL("${config.urlEndpoint}/api/client/$path") else URL(path)
        val requestBody = gson.toJson(body).toRequestBody()
        val request = Request.Builder().url(url)
            .post(requestBody)
            .addHeaders(config)
            .build()
        return execute(request)
    }

    private fun Request.Builder.addHeaders(config: Config): Request.Builder = this
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json")
            .addHeader("Authorization", "Bearer ${config.apiKey}")

    private suspend inline fun <reified T> execute(request: Request): Result<T> {
        try {
            val response = client.newCall(request).executeAsync()
            if (!response.isSuccessful) {
                return Result.failure(parseError(response.code, response.body.string()))
            }
            val typeToken = object : TypeToken<T>() {}

            // Handle cases where T might be Unit (for responses with no body expected)
            if (typeToken.type == Unit::class.java || typeToken.rawType == Nothing::class.java) {
                return Result.success(Unit as T)
            } else {
                val parsedObject: T = gson.fromJson(response.body.string(), typeToken.type)
                return Result.success(parsedObject)
            }
        } catch (e: IOException) {
            return Result.failure(e)
        }
    }

    internal fun parseError(status: Int, body: String): PostlesException {
        val error = runCatching { JsonParser.parseString(body).asJsonObject }.getOrNull()
        val message = error?.get("error")?.takeUnless { it.isJsonNull }?.asString
            ?: "Invalid status code $status"
        val code = error?.get("code")?.takeUnless { it.isJsonNull }?.asInt
        return PostlesException(status, code, message)
    }
}
