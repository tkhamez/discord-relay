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

// https://discord.com/developers/docs/resources/webhook#execute-webhook-jsonform-params
data class HttpSendWebhook(
    val content: String? = null,
    val username: String? = null,
    val avatar_url: String? = null,
    val embeds: List<ChannelMessageEmbed>? = null,
)

data class WebSocketReceiveMessage(
    val t: Any? = null, // Gateway event name
    val op: Int? = null, // Opcode
    val s: Int? = null, // Sequence number
    val d: Any? = null, // Data
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

// https://discord.com/developers/docs/resources/guild#unavailable-guild-object
// https://discord.com/developers/docs/resources/guild#guild-object
data class DiscordGuild(
    val id: String? = null,
    val name: String? = null,
    val unavailable: Boolean? = null,
    val roles: List<GuildObject>? = null,
    val channels: List<GuildObject>? = null,
)

data class GuildObject(
    val id: String? = null,
    val name: String? = null,
    val guild: DiscordGuild? = null,
)

// https://discord.com/developers/docs/resources/channel#message-object
data class ChannelMessage(
    val id: String? = null,
    val channel_id: String? = null,
    val guild_id: String? = null,
    val author: ChannelMessageAuthor? = null,
    val member: ChannelMessageMember? = null,
    var content: String? = null,
    val timestamp: String? = null,
    val mention_everyone: Boolean? = null,
    var attachments: List<ChannelMessageAttachment>? = null,
    var embeds: List<ChannelMessageEmbed>? = null,
)
data class ChannelMessageAuthor(
    val id: String? = null,
    val username: String? = null,
    val discriminator: String? = null,
    val avatar: String? = null,
    val bot: Boolean? = null,
)
data class ChannelMessageMember(val nick: String? = null)
data class ChannelMessageAttachment(val url: String? = null, val filename: String? = null)

// https://discord.com/developers/docs/resources/channel#embed-object
data class ChannelMessageEmbed(
    val title: String? = null,
    val description: String? = null,
    val url: String? = null,
    val color: Int? = null,
    val footer: ChannelMessageEmbedFooter? = null,
    val image: ChannelMessageEmbedURL? = null,
    val thumbnail: ChannelMessageEmbedURL? = null,
    val author: ChannelMessageEmbedAuthor? = null,
    val fields: List<ChannelMessageEmbedField>? = null,
)
data class ChannelMessageEmbedFooter(val text: String? = null, val icon_url: String? = null)
data class ChannelMessageEmbedURL(val url: String? = null)
data class ChannelMessageEmbedAuthor(val name: String? = null, val url: String? = null, val icon_url: String? = null)
data class ChannelMessageEmbedField(val name: String? = null, val value: String? = null, val inline: Boolean = false)
