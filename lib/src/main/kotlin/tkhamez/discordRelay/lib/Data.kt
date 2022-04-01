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

data class HttpSendWebhook(
    val username: String? = null,
    val content: String? = null,
    val embeds: List<ChannelMessageEmbed>? = null,
)

data class WebSocketReceiveMessage(
    val t: Any? = null, // gateway event name
    val op: Int? = null, // Opcode
    val s: Int? = null, // Sequence number
    val d: Any? = null, // data
)

data class WebSocketReceiveHello(val heartbeat_interval: Long? = null)

data class WebSocketReceiveReady(val session_id: String? = null, val guilds: List<DiscordGuild>? = null)

data class WebSocketSendHeartbeat(val op: Int, val d: Int?)

data class WebSocketSendIdentify(val op: Int, val d: WebSocketSendIdentifyData)
data class WebSocketSendIdentifyData(
    val token: String,
    val intents: Int,
    val properties: WebSocketSendIdentifyDataProperties,
)
data class WebSocketSendIdentifyDataProperties(val `$os`: String, val `$browser`: String, val `$device`: String)

data class WebSocketSendResume(val op: Int, val d: WebSocketSendResumeData)
data class WebSocketSendResumeData(val token: String, val session_id: String, val seq: Int?)

data class DiscordGuild(
    val unavailable: Boolean? = null,
    val id: String? = null,
    val name: String? = null,
    val channels: List<GuildChannel>? = null,
)

data class GuildChannel(
    val id: String? = null,
    val name: String? = null,
    val guild: DiscordGuild? = null,
)

data class ChannelMessage(
    val timestamp: String? = null,
    val mention_everyone: Boolean? = null,
    val member: ChannelMessageMember? = null,
    val id: String? = null,
    var content: String? = null,
    val channel_id: String? = null,
    val author: ChannelMessageAuthor? = null,
    val guild_id: String? = null,
    var attachments: List<ChannelMessageAttachment>? = null,
    var embeds: List<ChannelMessageEmbed>? = null,
)
data class ChannelMessageMember(val nick: String? = null)
data class ChannelMessageAuthor(val username: String? = null, val discriminator: String? = null)
data class ChannelMessageAttachment(val url: String? = null, val filename: String? = null)

data class ChannelMessageEmbed(
    val title: String? = null,
    val url: String? = null,
    val author: ChannelMessageEmbedAuthor? = null,
    val description: String? = null,
    val fields: List<ChannelMessageEmbedField>? = null,
    val image: ChannelMessageEmbedURL? = null,
    val thumbnail: ChannelMessageEmbedURL? = null,
    val footer: ChannelMessageEmbedFooter? = null,
)
data class ChannelMessageEmbedAuthor(val name: String? = null, val url: String? = null, val icon_url: String? = null)
data class ChannelMessageEmbedField(val name: String? = null, val value: String? = null, val inline: Boolean = false)
data class ChannelMessageEmbedURL(val url: String? = null)
data class ChannelMessageEmbedFooter(val text: String? = null)
