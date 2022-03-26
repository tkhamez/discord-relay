package tkhamez.discordRelay.lib

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.json.*
import io.ktor.client.features.websocket.*
import io.ktor.client.request.*
import io.ktor.http.cio.websocket.*
import kotlinx.coroutines.*
import java.util.concurrent.BlockingQueue

const val EVENT_CLOSED = 1

private const val OP_DISPATCH = 0
private const val OP_HEARTBEAT = 1
private const val OP_IDENTIFY = 2
private const val OP_RESUME = 6
private const val OP_INVALID_SESSION = 9
private const val OP_HELLO = 10
private const val OP_HEARTBEAT_ACK = 11
private const val EVENT_READY = "READY"
private const val EVENT_RESUMED = "RESUMED"
private const val EVENT_MESSAGE_CREATE = "MESSAGE_CREATE"

private const val CLOSE_REASON_MESSAGE_PROTOCOL_ERROR = "Protocol error"
private const val CLOSE_REASON_MESSAGE_NORMAL = "Normal"

private val httpClient = HttpClient(CIO) {
    install(WebSockets)
    install(JsonFeature)
}

/**
 * https://discord.com/developers/docs/topics/gateway
 * https://discord.com/developers/docs/topics/opcodes-and-status-codes
 */
class Gateway(
    private val messages: BlockingQueue<String>,
    private val events: BlockingQueue<Int>,
    private val webhook: Webhook
) {
    private val closeCodesReconnect = setOf<Short>(4000, 4001, 4002, 4003, 4005, 4007, 4008, 4009)
    private val closeCodesStop = setOf<Short>(4004, 4010, 4011, 4012, 4013, 4014)

    private var lastConnectionAttempt: Long = 0
    private var connectingOrConnected = false

    private var gatewayResponse: HttpResponseGateway? = null
    private var webSocketSession: DefaultWebSocketSession? = null

    private var sequenceNumber: Int? = null
    private var tryResumeConnection = false
    private var trySendResume = false
    private var clientClosed = false
    private var sessionId: String? = null
    private var lastSend: Long = 0

    private var identifySent = false
    private var identifyLastSend: Long = 0

    private var heartbeatLastAck: Long = 0
    private var heartbeatJob: Job? = null

    private var retryJob: Job? = null
    private var closed = false

    fun putDummyMessage() {
        messages.put("")
    }

    suspend fun eventsTake(): Int {
        return withContext(Dispatchers.IO) { events.take() }
    }

    suspend fun init(): Boolean {
        if (lastConnectionAttempt + 5000 > System.currentTimeMillis()) {
            // It's actually HttpResponseGateway.session_start_limit.max_concurrency times per 5 seconds,
            // which was always 1 for me so far.
            withContext(Dispatchers.IO) { messages.put(ResString.waitSeconds) }
            return false
        }

        // Move this up here, it can prevent a "ForegroundServiceDidNotStartInTimeException" on Android if you
        // click the connect button very fast several times without an internet connection.
        lastConnectionAttempt = System.currentTimeMillis()

        if (connectingOrConnected) {
            withContext(Dispatchers.IO) { messages.put(ResString.alreadyConnectingOrConnected) }
            return false
        }
        connectingOrConnected = true

        if (!fetchGatewayUrl()) {
            connectingOrConnected = false
            // need to send this for Android to stop the service
            withContext(Dispatchers.IO) { events.put(EVENT_CLOSED) }
            return false
        }

        // Connect
        gatewayResponse?.url?.let { connect(it) }

        return true
    }

    suspend fun close() {
        close(CloseReason.Codes.NORMAL, CLOSE_REASON_MESSAGE_NORMAL)
    }

    fun testCloseResumeOK() {
        tryResumeConnection = true
        clientClosed = true
        CoroutineScope(Dispatchers.IO).launch {
            webSocketSession?.close(CloseReason(CloseReason.Codes.PROTOCOL_ERROR, "")) // resume will work
        }
    }

    fun testCloseResumeNOK() {
        tryResumeConnection = true
        clientClosed = true
        CoroutineScope(Dispatchers.IO).launch {
            webSocketSession?.close(CloseReason(CloseReason.Codes.GOING_AWAY, "")) // resume will not work
        }
    }

    fun testStopHeartbeat() {
        heartbeatJob?.cancel()
    }

    fun testSend(text: String) {
        CoroutineScope(Dispatchers.IO).launch { send(text) }
    }

    /**
     * Get gateway URL and metadata
     */
    private suspend fun fetchGatewayUrl(): Boolean {
        withContext(Dispatchers.IO) { messages.put(ResString.requestingGatewayURL) }

        var gatewayResponseTemp =
            if (Config.isBotToken) Config.gatewayResponseBot[Config.token]
            else Config.gatewayResponse
        if (gatewayResponseTemp == null) {
            val path = if (Config.isBotToken) "/gateway/bot" else "/gateway"
            val url = Config.apiBaseUrl + path
            log("HTTP request: GET $url")
            val response: String
            try {
                response = httpClient.get(url) {
                    if (Config.isBotToken) header("Authorization", "Bot " + Config.token)
                }
            } catch (t: Throwable) {
                withContext(Dispatchers.IO) { messages.put("${ResString.error}: ${t.message.toString()}") }
                return false
            }
            log("HTTP response: $response")
            try {
                gatewayResponseTemp = Gson().fromJson(response, HttpResponseGateway::class.java)
            } catch (t: Throwable) {
                log(t.message)
            }
        }

        gatewayResponse = gatewayResponseTemp

        if (gatewayResponseTemp == null) {
            withContext(Dispatchers.IO) { messages.put(ResString.failedGatewayURL) }
            return false
        }

        if (Config.isBotToken) {
            Config.gatewayResponseBot[Config.token] = gatewayResponseTemp
        } else {
            Config.gatewayResponse = gatewayResponseTemp
        }

        if ((gatewayResponseTemp.session_start_limit?.remaining ?: 1) < 1) {
            val resetAfter = gatewayResponseTemp.session_start_limit?.reset_after
            withContext(Dispatchers.IO) {
                messages.put(ResString.sessionStartLimitReached.replace("$1", resetAfter.toString()))
            }
            return false
        }

        return true
    }

    private fun connect(url: String) {
        connectingOrConnected = true // here also for resume()

        if (webSocketSession?.isActive == true) {
            messages.put(ResString.alreadyConnected)
            return
        }
        messages.put(ResString.connecting)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                clientClosed = false
                identifySent = false
                closed = false
                httpClient.webSocket(url) {
                    log("WS connected")
                    webSocketSession = this
                    withContext(Dispatchers.IO) { messages.put(ResString.connected) }
                    readMessages()
                }
            } catch (t: Throwable) {
                val retryTimeSec: Long = 20
                withContext(Dispatchers.IO) { messages.put(ResString.errorConnection + t.message) }
                withContext(Dispatchers.IO) {
                    messages.put(ResString.reconnectAttempt.replace("$1", retryTimeSec.toString()))
                }
                trySendResume = false
                retryJob = CoroutineScope(Dispatchers.IO).launch {
                    delay(retryTimeSec * 1000)
                    handleClose()
                }
                return@launch
            }
            webSocketSession?.let {
                val reason = it.closeReason.await()
                handleClose(reason)
            }
        }
    }

    private suspend fun DefaultClientWebSocketSession.readMessages() {
        try {
            for (message in incoming) {
                message as? Frame.Text ?: continue
                val msg = message.readText()
                log("WS received: $msg")
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        handleMessage(msg)
                    } catch (t: Throwable) {
                        withContext(Dispatchers.IO) { messages.put("${ResString.messageError}: ${t.message}") }
                    }
                }
            }
        } catch (t: Throwable) {
            withContext(Dispatchers.IO) { messages.put(ResString.errorReceiving + t.message) }
        }
    }

    private suspend fun handleClose(reason: CloseReason? = null) {
        closed = true
        val closeCode: Short = reason?.code ?: 0
        val closeMessage: String = reason?.message ?: ""

        if (closeCode > 0) { // not when trying to reconnect after a delay
            val who = if (clientClosed) ResString.client else ResString.server
            withContext(Dispatchers.IO) {
                messages.put("${ResString.disconnected} ($who, $closeCode $closeMessage).")
            }
        }

        if (
            !closeCodesStop.contains(closeCode) &&
            (!clientClosed || tryResumeConnection || closeCodesReconnect.contains(closeCode))
        ) {
            resumeTheConnection()
        } else if (!tryResumeConnection) {
            sessionId = null
            connectingOrConnected = false
            withContext(Dispatchers.IO) { events.put(EVENT_CLOSED) }
        }
        tryResumeConnection = false
    }

    private suspend fun close(code: CloseReason.Codes, reason: String = "") {
        clientClosed = true
        if (!tryResumeConnection && retryJob?.isActive == true) {
            withContext(Dispatchers.IO) { messages.put(ResString.reconnectAttemptCanceled) }
        }
        retryJob?.cancel()
        webSocketSession?.close(CloseReason(code, reason))
        connectingOrConnected = false
        // It's not necessary to send EVENT_CLOSED here.
        log("WS closed ($code)")
    }

    private suspend fun resumeTheConnection() {
        trySendResume = true
        gatewayResponse?.url?.let {
            connect(it)
        } ?: run {
            connectingOrConnected = false
            withContext(Dispatchers.IO) { messages.put(ResString.cannotResume) }
            withContext(Dispatchers.IO) { events.put(EVENT_CLOSED) }
        }
    }

    private suspend fun send(message: String) {
        if (webSocketSession?.isActive == true) {
            // Rate Limit: "120 gateway commands every 60 seconds", so 2 every second on average
            val wait = lastSend + 500 - System.currentTimeMillis()
            if (wait > 0) {
                log("WS 'send' too fast, wait: $wait ms")
                delay(wait)
            }
            lastSend = System.currentTimeMillis()
            webSocketSession?.send(message)
            log("WS sent: $message")
        }
    }

    @Throws(JsonSyntaxException::class)
    private suspend fun handleMessage(messageText: String) {
        val gson = Gson()
        val message = gson.fromJson(messageText, WsReceive::class.java)
        if (message.op == OP_HELLO) {
            val messageHello = gson.fromJson(gson.toJson(message.d), WsReceiveHello::class.java)
            if (messageHello.heartbeat_interval == null) {
                withContext(Dispatchers.IO) { messages.put(ResString.invalidHello) }
                return
            }
            startHeartbeats(messageHello.heartbeat_interval)
            if (trySendResume) { // check resume first
                trySendResume = false
                sendResume()
            } else if (!identifySent) {
                sendIdentify()
                identifySent = true
            }
        } else if (message.op == OP_HEARTBEAT) {
            // Server requested a heartbeat, send immediately
            sendHeartbeat()
        } else if (message.op == OP_HEARTBEAT_ACK) {
            heartbeatLastAck = System.currentTimeMillis()
        } else if (message.op == OP_INVALID_SESSION) {
            // After a failed RESUME
            log("WS delay after failed 'resume' before 'identify'")
            delay((1000L..5000L).random())
            sendIdentify()
        } else if (message.op == OP_DISPATCH) {
            sequenceNumber = message.s
            if (message.t == EVENT_READY) {
                val messageReady = gson.fromJson(gson.toJson(message.d), WsReceiveReady::class.java)
                if (messageReady.session_id != null) {
                    sessionId = messageReady.session_id
                    withContext(Dispatchers.IO) { messages.put(ResString.ready) }
                } else {
                    withContext(Dispatchers.IO) { messages.put(ResString.readyError) }
                    tryResumeConnection = true
                    close(CloseReason.Codes.PROTOCOL_ERROR, CLOSE_REASON_MESSAGE_PROTOCOL_ERROR)
                }
            } else if (message.t == EVENT_RESUMED) {
                withContext(Dispatchers.IO) { messages.put(ResString.connectionResumed) }
            } else if (message.t == EVENT_MESSAGE_CREATE) {
                val messageMessageCreate = gson.fromJson(gson.toJson(message.d), WsReceiveMessageCreate::class.java)
                relayMessage(messageMessageCreate)
            }
        }
    }

    private fun startHeartbeats(heartbeatInterval: Long) {
        heartbeatJob?.cancel()
        heartbeatLastAck = 0
        var heartbeatLastSend: Long = 0
        heartbeatJob = CoroutineScope(Dispatchers.IO).launch {
            while (webSocketSession?.isActive == true) {
                // Wait first
                var sleep = heartbeatInterval
                while (webSocketSession?.isActive == true && sleep > 0) {
                    delay(200.toLong().coerceAtMost(sleep))
                    sleep -= 200
                }

                // Check last acknowledge
                if (heartbeatLastSend > heartbeatLastAck) {
                    // probably failed or "zombied" connection, terminate and resume
                    tryResumeConnection = true
                    close(CloseReason.Codes.PROTOCOL_ERROR, CLOSE_REASON_MESSAGE_PROTOCOL_ERROR)
                    break
                }

                // Send heartbeat
                sendHeartbeat()
                heartbeatLastSend = System.currentTimeMillis()
            }

            if (!clientClosed && !closed && retryJob?.isActive != true) {
                // e.g. when the internet connection was lost the socket is not active but the close
                // event did not fire. Calling close() now doesn't work.
                withContext(Dispatchers.IO) { messages.put(ResString.connectionLost) }
                resumeTheConnection()
            }
        }
    }

    private suspend fun sendHeartbeat() {
        val gson = GsonBuilder().serializeNulls().create()
        send(gson.toJson(WsSendHeartbeat(op = OP_HEARTBEAT, d = sequenceNumber)))
    }

    private suspend fun sendIdentify() {
        val wait = identifyLastSend + 5000 - System.currentTimeMillis()
        if (wait > 0) {
            log("'Identify' too fast, wait: $wait ms")
            delay(wait)
        }
        identifyLastSend = System.currentTimeMillis()
        val identify = WsSendIdentify(
            op = OP_IDENTIFY,
            d = WsSendIdentifyData(
                token = Config.token,
                intents = 512, // GUILD_MESSAGES - MESSAGE_CREATE, MESSAGE_UPDATE, MESSAGE_DELETE, MESSAGE_DELETE_BULK
                properties = WsSendIdentifyDataProperties("Unknown", "discord-relay", "discord-relay"),
            ),
        )
        send(Gson().toJson(identify))
    }

    private suspend fun sendResume() {
        if (sessionId == null) {
            sendIdentify()
            return
        }
        val resume = WsSendResume(
            op = OP_RESUME,
            d = WsSendResumeData(
                token = Config.token,
                session_id = sessionId ?: "",
                seq = sequenceNumber,
            )
        )
        send(Gson().toJson(resume))
    }

    private suspend fun relayMessage(messageMessageCreate: WsReceiveMessageCreate) {
        if (
            !Config.channelIdList().contains(messageMessageCreate.channel_id) ||
            (Config.onlyMentionEveryone && messageMessageCreate.mention_everyone != true)
        ) {
            return
        }

        withContext(Dispatchers.IO) { messages.put(ResString.messageReceived) }
        webhook.sendMessage(messageMessageCreate)
    }
}
