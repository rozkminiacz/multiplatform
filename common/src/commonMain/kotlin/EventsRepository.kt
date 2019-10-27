package org.common

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.url
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

internal expect val ApplicationDispatcher: CoroutineDispatcher

class EventsRepository {
    val client: HttpClient = HttpClient()

    fun getTestMessage(callback: (String) -> Unit) {
        GlobalScope.apply {
            launch(ApplicationDispatcher) {
                val result = client.get<String>(host = "localhost", scheme = "http", port = 8080, path = "/api/test")

                callback(result)
            }
        }
    }
}