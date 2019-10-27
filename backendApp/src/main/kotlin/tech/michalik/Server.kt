package tech.michalik

import io.ktor.application.Application
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CORS
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.features.DefaultHeaders
import io.ktor.gson.gson
import io.ktor.http.*
import io.ktor.response.cacheControl
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.response.respondTextWriter
import io.ktor.routing.get
import io.ktor.routing.route
import io.ktor.routing.routing
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.broadcast
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.delay

fun Application.main() {
    install(DefaultHeaders)
    // This uses use the logger to log every call (request/response)
    install(CallLogging)
    install(CORS)
    {
        method(HttpMethod.Options)
        method(HttpMethod.Get)
        method(HttpMethod.Post)
        method(HttpMethod.Delete)
        header(HttpHeaders.XForwardedProto)
        header(HttpHeaders.Authorization)
        anyHost()
        host("localhost:3000")
        allowCredentials = true
        allowNonSimpleContentTypes = true
        maxAge = java.time.Duration.ofDays(1)
    }
    install(ContentNegotiation) {
        gson()
    }

    val channel = produce { // this: ProducerScope<SseEvent> ->
        var n = 0
        while (true) {
            send(SseEvent("demo$n"))
            delay(1000)
            n++
        }
    }.broadcast()

    routing {
        get("/api"){
            // http app
            val events = channel.openSubscription()
            try {
                call.respondSse(events)
            } finally {
                events.cancel()
            }
        }
        get(("/api/test")){
            call.respond(HttpStatusCode.OK to mapOf("status" to "success"))
        }
        route("/"){
            get {
                call.respondText(
                        """
                        <html>
                            <head></head>
                            <body>
                                <ul id="events">
                                </ul>
                                <script type="text/javascript">
                                    var source = new EventSource('/api');
                                    var eventsUl = document.getElementById('events');

                                    function logEvent(text) {
                                        var li = document.createElement('li')
                                        li.innerText = text;
                                        eventsUl.appendChild(li);
                                    }

                                    source.addEventListener('message', function(e) {
                                        logEvent('message:' + e.data);
                                    }, false);

                                    source.addEventListener('open', function(e) {
                                        logEvent('open');
                                    }, false);

                                    source.addEventListener('error', function(e) {
                                        if (e.readyState == EventSource.CLOSED) {
                                            logEvent('closed');
                                        } else {
                                            logEvent('error');
                                            console.log(e);
                                        }
                                    }, false);
                                </script>
                            </body>
                        </html>
                    """.trimIndent(),
                        contentType = ContentType.Text.Html
                )
            }
        }
    }
}

data class SseEvent(val data: String, val event: String? = null, val id: String? = null)

suspend fun ApplicationCall.respondSse(events: ReceiveChannel<SseEvent>) {
    response.cacheControl(CacheControl.NoCache(null))
    respondTextWriter(contentType = ContentType.Text.EventStream) {
        for (event in events) {
            if (event.id != null) {
                write("id: ${event.id}\n")
            }
            if (event.event != null) {
                write("event: ${event.event}\n")
            }
            for (dataLine in event.data.lines()) {
                write("data: $dataLine\n")
            }
            write("\n")
            flush()
        }
    }
}