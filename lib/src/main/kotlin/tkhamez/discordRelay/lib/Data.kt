package tkhamez.discordRelay.lib

data class HttpResponseGateway(
    val url: String? = null,
    val shards: String? = null,
    val session_start_limit: HttpResponseGatewaySession? = null,
)
data class HttpResponseGatewaySession(
    val total: Int? = null,
    val remaining: Int? = null,
    val reset_after: Int? = null,
    val max_concurrency: Int? = null,
)

data class HttpResponseChannelMessage(
    val id: String? = null,
    val content: String? = null,
)

data class HttpRequestMessage(
    val username: String? = null,
    val content: String? = null,
    val embeds: List<HttpRequestMessageEmbeds>? = null,
)
data class HttpRequestMessageEmbeds(
    val color: String? = null,
    val footer: HttpRequestMessageEmbedsText? = null,
)
data class HttpRequestMessageEmbedsText(
    val text: String? = null,
)

data class WsReceive(
    val t: Any? = null, // gateway event name
    val op: Int? = null, // Opcode
    val s: Int? = null, // Sequence number
    val d: Any? = null // data
)

data class WsReceiveHello(val heartbeat_interval: Long? = null)

data class WsReceiveReady(val session_id: String? = null)

data class WsReceiveMessageCreate(
    val timestamp: String? = null,
    val mention_everyone: Boolean? = null,
    val member: WsReceiveMessageCreateMember? = null,
    val id: String? = null,
    val content: String? = null,
    val channel_id: String? = null,
    val author: WsReceiveMessageCreateAuthor? = null,
    val guild_id: String? = null,
)
data class WsReceiveMessageCreateMember(val nick: String? = null)
data class WsReceiveMessageCreateAuthor(val username: String? = null, val discriminator: String? = null)

data class WsSendHeartbeat(val op: Int, val d: Int?)

data class WsSendIdentify(val op: Int, val d: WsSendIdentifyData)
data class WsSendIdentifyData(val token: String, val intents: Int, val properties: WsSendIdentifyDataProperties)
data class WsSendIdentifyDataProperties(val `$os`: String, val `$browser`: String, val `$device`: String)

data class WsSendResume(val op: Int, val d: WsSendResumeData)
data class WsSendResumeData(val token: String, val session_id: String, val seq: Int?)
