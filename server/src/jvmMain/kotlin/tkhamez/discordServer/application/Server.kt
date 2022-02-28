package tkhamez.discordServer.application

import io.ktor.application.*
import io.ktor.http.cio.websocket.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.websocket.*
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

private var lastDisconnectCode: CloseReason.Codes? = null

private val lastHeartbeat = mutableMapOf<String, Long>()

/**
 * Mockup of the Discord API and WebSocket server.
 */
fun main() {
    embeddedServer(Netty, port = 8080) {
        install(WebSockets)

        routing {
            get("/api/gateway") {
                val host = call.request.host()
                call.respondText("{\"url\": \"ws://$host:8080\"}")
            }
            get("/api/gateway/bot") {
                val host = call.request.host()
                val s = "{\"total\":1000,\"remaining\":980,\"reset_after\":59255492,\"max_concurrency\":1}"
                call.respondText("{\"url\":\"ws://$host:8080\",\"shards\":1,\"session_start_limit\":$s}")
            }
            webSocket("/") {
                val id = UUID.randomUUID().toString()
                lastHeartbeat[id] = 0

                sendMessage("{\"op\":10,\"d\":{\"heartbeat_interval\":40100}}", id) // Hello
                //sendMessage("{\"op\":10,\"d\":{\"heartbeat_interval\":5000}}")

                // The error shown below is a bug https://youtrack.jetbrains.com/issue/KTIJ-19704, it works
                CoroutineScope(Dispatchers.Default).launch {
                    checkHeartbeat(id)
                }

                for (frame in incoming) {
                    frame as? Frame.Text ?: continue
                    handleMessage(frame.readText(), id)
                }

                val reason = this.closeReason.await()
                lastDisconnectCode = reason?.knownReason
                log("$id $reason")
            }
        }

    }.start(wait = true)
}

private suspend fun DefaultWebSocketServerSession.checkHeartbeat(id: String) {
    while (true) {
        delay(60000)
        if ((lastHeartbeat[id] ?: 0) < System.currentTimeMillis() - 50000) {
            close(CloseReason(1000, ""))
        }
    }
}

private suspend fun DefaultWebSocketServerSession.handleMessage(receivedText: String, id: String) {
    log("$id received: $receivedText")
    if (receivedText.equals("{invalid}", ignoreCase = true)) {
        close(CloseReason(4002, "Error while decoding payload."))
    } else if (receivedText == "heartbeat") {
        sendMessage("{\"op\":1}", id) // request Heartbeat
    } else if (receivedText == "invalid_ready") {
        sendMessage("{\"op\":0,\"t\":\"READY\",\"s\":2,\"d\":{}}", id) // invalid Ready
    } else if (receivedText.contains("\"op\":1")) { // Heartbeat
        lastHeartbeat[id] = System.currentTimeMillis()
        sendMessage("{\"op\":11}", id) // Heartbeat ACK
    } else if (receivedText.contains("\"op\":2")) { // Identify
        val d = "{\"session_id\":\"123abc\"}"
        sendMessage("{\"op\":0,\"t\":\"READY\",\"s\":2,\"d\":$d}", id) // Ready
    } else if (receivedText.contains("\"op\":6")) { // Resume
        if (lastDisconnectCode == CloseReason.Codes.GOING_AWAY) {
            sendMessage("{\"op\":9}", id) // invalid session
        } else {
            sendMessage("{\"op\":0,\"t\":\"RESUMED\",\"s\":8}", id) // Resumed
        }
    } else {
        val d = "{" +
            "\"timestamp\":\"2022-04-08T23:11:07.123000+00:00\"," +
            "\"mention_everyone\":false," +
            "\"member\":{\"nick\": \"Nickname\"}," +
            "\"id\":\"987654321\"," +
            "\"content\":\"$receivedText\"," +
            "\"channel_id\":\"1234\"," +
            "\"author\":{\"username\":\"User\",\"discriminator\":\"0456\"}," +
            "\"guild_id\":\"789456123\"" +
            "}"
        sendMessage("{\"op\":0,\"t\":\"MESSAGE_CREATE\",\"d\":$d}", id) // message create
    }
}

private suspend fun DefaultWebSocketServerSession.sendMessage(text: String, id: String) {
    log("$id send: $text")
    send(text)
}

@Suppress("SimpleDateFormat")
private fun log(message: Any?) {
    val now = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(Date())
    println("$now $message")
}
