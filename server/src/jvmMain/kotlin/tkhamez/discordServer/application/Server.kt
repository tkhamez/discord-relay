package tkhamez.discordServer.application

import io.ktor.application.*
import io.ktor.http.*
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
private val ignoreNextHeartbeat = mutableMapOf<String, Boolean>()
private const val heartbeatInterval: Long = 40100
private const val heartbeatCheckInterval: Long = 60100
private var remaining = 980

/**
 * Mockup of the Discord API and WebSocket server.
 */
fun main() {
    embeddedServer(Netty, port = 8080) {
        install(WebSockets)

        routing {
            get("/api/gateway") {
                val host = call.request.host()
                val data = "{\"url\": \"ws://$host:8080\"}"
                log("HTTP send: $data")
                call.respondText(data, contentType = ContentType.Application.Json)
            }
            get("/api/gateway/bot") {
                val host = call.request.host()
                val s = "{\"total\":1000,\"remaining\":$remaining,\"reset_after\":59255492,\"max_concurrency\":1}"
                val data = "{\"url\":\"ws://$host:8080\",\"shards\":1,\"session_start_limit\":$s}"
                log("HTTP send: $data")
                call.respondText(data, contentType = ContentType.Application.Json)
            }
            get("/api/channels/1234/messages") {
                val data = "[" + getMessageJson("the empty message") + "]"
                log("HTTP send: $data")
                call.respondText(data, contentType = ContentType.Application.Json)
            }
            webSocket("/") {
                val id = UUID.randomUUID().toString()
                lastHeartbeat[id] = 0

                sendMessage("{\"op\":10,\"d\":{\"heartbeat_interval\":$heartbeatInterval}}", id) // Hello

                // The error shown below is a bug https://youtrack.jetbrains.com/issue/KTIJ-19704, it works
                val checkHeartbeatJob = CoroutineScope(Dispatchers.IO).launch {
                    checkHeartbeat(id)
                }

                /*CoroutineScope(Dispatchers.IO).launch {
                    delay(4000)
                    //close(CloseReason(4000, ""))
                    close(CloseReason(4014, ""))
                }*/

                for (frame in incoming) {
                    frame as? Frame.Text ?: continue
                    handleMessage(frame.readText(), id)
                }

                checkHeartbeatJob.cancel()
                val reason = this.closeReason.await()
                lastDisconnectCode = reason?.knownReason
                log("$id $reason")
            }
        }

    }.start(wait = true)
}

private suspend fun DefaultWebSocketServerSession.checkHeartbeat(id: String) {
    var check = true
    while (isActive && check) {
        delay(heartbeatCheckInterval)
        log("$id check heartbeat")
        if ((lastHeartbeat[id] ?: 0) < System.currentTimeMillis() - heartbeatCheckInterval) {
            close(CloseReason(1000, ""))
            log("$id closed (no heartbeat)")
            check = false
        }
    }
}

private suspend fun DefaultWebSocketServerSession.handleMessage(receivedText: String, id: String) {
    log("$id received: $receivedText")
    if (receivedText.equals("{invalid}", ignoreCase = true)) {
        close(CloseReason(4002, "Error while decoding payload."))
    } else if (receivedText == "heartbeat") {
        sendMessage("{\"op\":1}", id) // request Heartbeat
    } else if (receivedText == "heartbeat-no-ack") {
        ignoreNextHeartbeat[id] = true
    } else if (receivedText == "invalid_ready") {
        sendMessage("{\"op\":0,\"t\":\"READY\",\"s\":2,\"d\":{}}", id) // invalid Ready
    } else if (receivedText == "close-no-resume") {
        close(CloseReason(4004, ""))
    } else if (receivedText == "close-1001") {
        close(CloseReason(1001, "WebSocket requesting client reconnect vom Server."))
    } else if (receivedText.contains("\"op\":1")) { // Heartbeat
        lastHeartbeat[id] = System.currentTimeMillis()
        if (ignoreNextHeartbeat[id] != true) {
            sendMessage("{\"op\":11}", id) // Heartbeat ACK
        }
        ignoreNextHeartbeat[id] = false
    } else if (receivedText.contains("\"op\":2")) { // Identify
        remaining --
        val d1 = "{\"session_id\":\"123abc\"}"
        sendMessage("{\"op\":0,\"t\":\"READY\",\"s\":1,\"d\":$d1}", id) // Ready
        val d2 = "{\"id\":\"789456123\", \"name\": \"Guild, Test\", \"unavailable\": false, " +
            "\"channels\": [{\"id\": \"1234\", \"name\": \"Test Channel\"}], " +
            "\"roles\": [{\"id\": \"4567\", \"name\": \"Test Role\"}, {\"id\": \"7890\", \"name\": \"Role2\"}]}"
        sendMessage("{\"op\":0,\"t\":\"GUILD_CREATE\",\"s\":2,\"d\":$d2}", id) // Ready
    } else if (receivedText.contains("\"op\":6")) { // Resume
        if (lastDisconnectCode == CloseReason.Codes.GOING_AWAY) {
            sendMessage("{\"op\":9}", id) // invalid session
        } else {
            sendMessage("{\"op\":0,\"t\":\"RESUMED\",\"s\":8}", id) // Resumed
        }
    } else if (receivedText == "message" || receivedText == "message-empty") {
        val content = if (receivedText == "message") "<@&4567>the message <@2656> <@&7890>" else ""
        val d = getMessageJson(content)
        sendMessage("{\"op\":0,\"t\":\"MESSAGE_CREATE\",\"d\":$d}", id) // message create
    }
}

private fun getMessageJson(content: String): String {
    return "{" +
        "\"timestamp\":\"2022-04-08T23:11:07.123000+00:00\"," +
        "\"mention_everyone\":false," +
        "\"member\":{\"nick\": \"Nickname\"}," +
        "\"id\":\"987654321\"," +
        "\"content\":\"$content\"," +
        "\"channel_id\":\"1234\"," +
        "\"author\":{\"username\":\"User\",\"discriminator\":\"0456\"}," +
        "\"guild_id\":\"789456123\"" +
    "}"
}

private suspend fun DefaultWebSocketServerSession.sendMessage(text: String, id: String) {
    log("$id send: $text")
    send(text)
}

private fun log(message: Any?) {
    val now = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())
    println("$now $message")
}
