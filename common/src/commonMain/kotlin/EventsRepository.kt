package org.common

import io.ktor.client.HttpClient
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.request.get
import io.ktor.client.response.HttpResponse
import io.ktor.client.response.readText
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.Decoder
import kotlinx.serialization.Serializable
import kotlinx.serialization.StringTaggedDecoder
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json

internal expect val ApplicationDispatcher: CoroutineDispatcher

class EventsRepository {
    val client: HttpClient = HttpClient() {
        install(JsonFeature) {
            serializer = KotlinxSerializer()
        }
    }

    @UnstableDefault
    fun getTestMessage(callback: (String) -> Unit) {
        GlobalScope.apply {
            launch(ApplicationDispatcher) {
                val response = client.get<HttpResponse>(host = "localhost", scheme = "http", port = 8080, path = "/api/test")

                val result = response.readText()
                val event = Json.parse(Event.serializer(), result)


                callback(event.toString())
            }
        }
    }
}

@Serializable
data class Event(val status: String, val message: String? = null)