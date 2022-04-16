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

/**
 * Send when the connection is closed and there will be no attempt to reconnect.
 */
const val EVENT_CLOSED = 1

private const val OP_DISPATCH = 0
private const val OP_HEARTBEAT = 1
private const val OP_IDENTIFY = 2
private const val OP_RESUME = 6
private const val OP_INVALID_SESSION = 9
private const val OP_HELLO = 10
private const val OP_HEARTBEAT_ACK = 11
private const val EVENT_READY = "READY"
private const val EVENT_GUILD_CREATE = "GUILD_CREATE"
private const val EVENT_RESUMED = "RESUMED"
private const val EVENT_MESSAGE_CREATE = "MESSAGE_CREATE"
//private const val EVENT_THREAD_CREATE = "THREAD_CREATE"

private const val CLOSE_REASON_MESSAGE_PROTOCOL_ERROR = "Protocol error"
private const val CLOSE_REASON_MESSAGE_NORMAL = "Normal"

private val httpClient = HttpClient(CIO) {
    install(WebSockets)
    install(JsonFeature)
}

/**
 * https://discord.com/developers/docs/topics/gateway
 * https://discord.com/developers/docs/topics/opcodes-and-status-codes
 * https://discord.com/developers/docs/topics/threads
 */
class Gateway {
    private val closeCodesReconnect = setOf<Short>(4000, 4001, 4002, 4003, 4005, 4007, 4008, 4009)
    private val closeCodesStop = setOf<Short>(4004, 4010, 4011, 4012, 4013, 4014)

    private val retryTimeSec: Long = 20

    private var sessionStartLimitRemaining = 1000
    private var sessionStartCount = 0

    private var lastConnectionAttempt: Long = 0
    private var connectingOrConnected = false

    private var gatewayResponse: HttpResponseGateway? = null
    private var webSocketSession: DefaultWebSocketSession? = null

    private var connectionJob: Job? = null

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
    private var mayQueueRetry = false
    private var isClosed = true

    fun init() {
        if (connectingOrConnected) {
            messagesSend(ResString.alreadyConnectingOrConnected)
            return
        }

        if (lastConnectionAttempt + 5000 > System.currentTimeMillis()) {
            // It's actually HttpResponseGateway.session_start_limit.max_concurrency times per 5 seconds,
            // which was always 1 for me so far.
            messagesSend(ResString.waitSeconds)
            return
        }

        connectingOrConnected = true

        fullConnect()
    }

    fun close() {
        cancelRetryJob(true)
        if (webSocketSession?.isActive == true) {
            CoroutineScope(Dispatchers.IO).launch { close(CloseReason.Codes.NORMAL, CLOSE_REASON_MESSAGE_NORMAL) }
        } else {
            connectingOrConnected = false
        }
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

    private fun fullConnect() {
        CoroutineScope(Dispatchers.IO).launch {
            if (!fetchGatewayUrl()) {
                connectingOrConnected = false
                // need to send this for Android to stop the service
                eventsSend(EVENT_CLOSED)
                return@launch
            }

            // Connect
            lastConnectionAttempt = System.currentTimeMillis()
            gatewayResponse?.url?.let { connect(it) }
        }
    }

    /**
     * Get gateway URL and metadata
     */
    private suspend fun fetchGatewayUrl(): Boolean {
        messagesSend(ResString.requestingGatewayURL)

        if (sessionStartCount + 1 >= sessionStartLimitRemaining) {
            Config.gatewayResponse = null
            Config.gatewayResponseBot = mutableMapOf()
            sessionStartCount = 0
        }

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
                messagesSend("${ResString.error}: ${t.message.toString()}")
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
            messagesSend(ResString.failedGatewayURL)
            return false
        }

        if (Config.isBotToken) {
            Config.gatewayResponseBot[Config.token] = gatewayResponseTemp
        } else {
            Config.gatewayResponse = gatewayResponseTemp
        }

        sessionStartLimitRemaining = gatewayResponseTemp.session_start_limit?.remaining ?: 1000
        if (sessionStartLimitRemaining < 1) {
            val resetAfter = (gatewayResponseTemp.session_start_limit?.reset_after ?: 0) / 1000 / 60
            messagesSend(ResString.sessionStartLimitReached.replace("$1", resetAfter.toString()))
            return false
        }

        return true
    }

    private fun connect(url: String) {
        connectingOrConnected = true // here too for resume()

        // Cancelling the connectionJob job below can lead to an exception which would call queueHandleClose()
        // which results in an endless loop.
        mayQueueRetry = false

        cancelRetryJob(false)
        heartbeatJob?.cancel()
        webSocketSession?.cancel()
        connectionJob?.cancel()

        messagesSend(ResString.connecting)

        connectionJob = CoroutineScope(Dispatchers.IO).launch {
            // Delays a bit to make sure that the exception has been thrown before a new connection
            // attempt is allowed to be queued.
            delay(1000L)
            mayQueueRetry = true

            try {
                clientClosed = false
                identifySent = false
                isClosed = false
                val fullUrl = "$url?v=9"
                log("WS url: $fullUrl")
                httpClient.webSocket(fullUrl) {
                    log("WS connected")
                    webSocketSession = this
                    messagesSend(ResString.connected)
                    readMessages() // this will keep it running
                }
            } catch (t: Throwable) {
                isClosed = true
                messagesSend(ResString.errorConnection + t.message)
                if (mayQueueRetry) {
                    queueHandleClose()
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
                        messagesSend("${ResString.messageError}: ${t.message}")
                    }
                }
            }
        } catch (t: Throwable) {
            messagesSend(ResString.errorReceiving + t.message)
        }
    }

    private fun handleClose(reason: CloseReason? = null) {
        isClosed = true
        cancelRetryJob(false)

        val closeCode: Short = reason?.code ?: 0
        val closeMessage: String = reason?.message ?: ""

        if (closeCode > 0) { // not when trying to reconnect after a delay
            val who = if (clientClosed) ResString.client else ResString.server
            messagesSend("${ResString.disconnected} ($who, $closeCode $closeMessage).")
        }

        if (
            !closeCodesStop.contains(closeCode) &&
            (!clientClosed || tryResumeConnection || closeCodesReconnect.contains(closeCode))
        ) {
            resumeTheConnection()
        } else if (!tryResumeConnection) {
            sessionId = null
            connectingOrConnected = false
            eventsSend(EVENT_CLOSED)
        }
        tryResumeConnection = false
    }

    private suspend fun close(code: CloseReason.Codes, reason: String = "") {
        clientClosed = true
        if (!tryResumeConnection && retryJob?.isActive == true) {
            messagesSend(ResString.reconnectAttemptCanceled)
        }

        cancelRetryJob(false)
        if (tryResumeConnection) {
            queueHandleClose() // handleClose() will not be called without an internet connection
        }

        webSocketSession?.close(CloseReason(code, reason))

        connectingOrConnected = false
        log("WS closed ($code)")
    }

    private fun queueHandleClose() {
        messagesSend(ResString.reconnectAttempt.replace("$1", retryTimeSec.toString()))
        trySendResume = false
        cancelRetryJob(false)
        retryJob = CoroutineScope(Dispatchers.IO).launch {
            delay(retryTimeSec * 1000)
            handleClose()
        }
    }

    private fun cancelRetryJob(withMessage: Boolean) {
        if (withMessage && retryJob?.isActive == true) {
            messagesSend(ResString.reconnectAttemptCanceled)
        }
        retryJob?.cancel()
    }

    private fun resumeTheConnection() {
        trySendResume = true
        gatewayResponse?.url?.let {
            if (sessionStartCount + 1 >= sessionStartLimitRemaining) {
                fullConnect()
            } else {
                connect(it)
            }
        } ?: run {
            connectingOrConnected = false
            messagesSend(ResString.cannotResume)
            eventsSend(EVENT_CLOSED)
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
        val message = gson.fromJson(messageText, WebSocketReceiveMessage::class.java)
        if (message.op == OP_HELLO) {
            val messageHello = gson.fromJson(gson.toJson(message.d), WebSocketReceiveHello::class.java)
            if (messageHello.heartbeat_interval == null) {
                messagesSend(ResString.invalidHello)
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
                val messageReady = gson.fromJson(gson.toJson(message.d), WebSocketReceiveReady::class.java)
                if (messageReady.session_id != null) {
                    sessionId = messageReady.session_id
                    messagesSend(ResString.ready)
                    storeGuildInfo(messageReady.guilds)
                } else {
                    messagesSend(ResString.readyError)
                    tryResumeConnection = true
                    close(CloseReason.Codes.PROTOCOL_ERROR, CLOSE_REASON_MESSAGE_PROTOCOL_ERROR)
                }
            } else if (message.t == EVENT_GUILD_CREATE) {
                val messageGuildCreate = gson.fromJson(gson.toJson(message.d), DiscordGuild::class.java)
                storeGuildInfo(listOf(messageGuildCreate))
            } else if (message.t == EVENT_RESUMED) {
                messagesSend(ResString.connectionResumed)
            } else if (message.t == EVENT_MESSAGE_CREATE) {
                val messageMessageCreate = gson.fromJson(gson.toJson(message.d), ChannelMessage::class.java)
                relayMessage(messageMessageCreate)
            }
        }
    }

    private fun startHeartbeats(heartbeatInterval: Long) {
        heartbeatJob?.cancel()
        heartbeatLastAck = 0
        var heartbeatLastSend: Long = 0
        heartbeatJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive && webSocketSession?.isActive == true) {
                // Wait first
                var sleep = heartbeatInterval
                val waitInterval = 200L
                while (isActive && webSocketSession?.isActive == true && sleep > 0) {
                    delay(waitInterval.coerceAtMost(sleep))
                    sleep -= waitInterval
                }

                if (webSocketSession?.isActive != true) {
                    break
                }

                // Check last acknowledge
                if (heartbeatLastSend > heartbeatLastAck) {
                    // probably failed or "zombied" connection, terminate and resume
                    tryResumeConnection = true
                    messagesSend("${ResString.closing} (${CloseReason.Codes.PROTOCOL_ERROR})")
                    close(CloseReason.Codes.PROTOCOL_ERROR, CLOSE_REASON_MESSAGE_PROTOCOL_ERROR)
                    break
                }

                // Send heartbeat
                sendHeartbeat()
                heartbeatLastSend = System.currentTimeMillis()
            }

            if (!clientClosed && !isClosed && retryJob?.isActive != true) {
                // e.g. when the internet connection was lost the socket is not active but the close
                // event did not fire. Calling close() now doesn't work.
                messagesSend(ResString.connectionLost)
                resumeTheConnection()
            }
        }
    }

    private suspend fun sendHeartbeat() {
        val gson = GsonBuilder().serializeNulls().create()
        send(gson.toJson(WebSocketSendHeartbeat(op = OP_HEARTBEAT, d = sequenceNumber)))
    }

    private suspend fun sendIdentify() {
        sessionStartCount ++
        val wait = identifyLastSend + 5000 - System.currentTimeMillis()
        if (wait > 0) {
            log("'Identify' too fast, wait: $wait ms")
            delay(wait)
        }
        identifyLastSend = System.currentTimeMillis()
        val identify = WebSocketSendIdentify(
            op = OP_IDENTIFY,
            d = WebSocketSendIdentifyData(
                token = Config.token,
                intents = 513, // GUILDS (1) and GUILD_MESSAGES (512)
                properties = WebSocketSendIdentifyDataProperties("Unknown", "discord-relay", "discord-relay"),
            ),
        )
        send(Gson().toJson(identify))
    }

    private suspend fun sendResume() {
        if (sessionId == null) {
            sendIdentify()
            return
        }
        val resume = WebSocketSendResume(
            op = OP_RESUME,
            d = WebSocketSendResumeData(
                token = Config.token,
                session_id = sessionId ?: "",
                seq = sequenceNumber,
            )
        )
        send(Gson().toJson(resume))
    }

    private fun storeGuildInfo(guilds: List<DiscordGuild>?) {
        if (guilds == null) {
            return
        }

        for (guild in guilds) {
            if (guild.unavailable == true) {
                continue
            }

            val tmpGuild = DiscordGuild(id = guild.id, name = guild.name)

            // Channels
            if (guild.channels != null) {
                for (channel in guild.channels) {
                    if (Config.channelIds().contains(channel.id)) {
                        Config.guildChannels.add(GuildProperty(id = channel.id, name = channel.name, guild = tmpGuild))
                    }
                }
            }

            // Roles
            if (guild.roles != null) {
                for (role in guild.roles) {
                    Config.guildRoles.add(GuildProperty(id = role.id, name = role.name, guild = tmpGuild))
                }
            }

        } // foreach
    }

    private fun relayMessage(message: ChannelMessage) {
        val channelIndex = Config.channelIds().indexOf(message.channel_id)
        val onlyMentionEveryone = Config.onlyMentionEveryone.getOrNull(channelIndex) ?: true
        if (channelIndex == -1 || (onlyMentionEveryone && message.mention_everyone != true)) {
            return
        }

        messagesSend(ResString.messageReceived)
        relayQueueSend(message)
    }
}
