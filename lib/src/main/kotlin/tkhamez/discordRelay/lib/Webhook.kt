package tkhamez.discordRelay.lib

import com.google.gson.Gson
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.*
import io.ktor.client.features.json.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val httpClient = HttpClient(CIO) {
    install(JsonFeature)
}

/**
 * https://discord.com/developers/docs/resources/webhook
 * https://discord.com/developers/docs/resources/channel
 * https://discord.com/developers/docs/topics/rate-limits
 */
class Webhook(private val messages: Channel<String>) {

    private var lastException: ResponseException? = null
    private var lastRequest: Long = 0
    private var requestsQueued: Long = 0

    suspend fun sendMessage(channelMessage: ChannelMessage) {
        if (Config.webhook.isEmpty()) {
            messagesSend(ResString.missingWebhook)
            return
        }

        if ((channelMessage.content ?: "").isEmpty()) {
            loadContent(channelMessage)
        }

        val payload = HttpSendWebhook(
            username = channelMessage.member?.nick,
            content = channelMessage.content,
            embeds = buildEmbedsFromMessage(channelMessage),
        )

        log("HTTP request: POST ${Config.webhook}:")
        log(Gson().toJson(payload))

        val result = request<String>(
            Config.webhook,
            mapOf("Content-Type" to "application/json"),
            HttpMethod.Post,
            payload,
        )
        if (result == null) {
            messagesSend("${ResString.relayFailed}: ${lastException?.message}")
        }

        messagesSend(ResString.messageRelayed)
    }

    private suspend fun loadContent(channelMessage: ChannelMessage) {
        val url = "${Config.apiBaseUrl}/channels/${channelMessage.channel_id}/messages?limit=10"
        log("HTTP request: GET $url")

        val result = request<List<ChannelMessage>>(
            url,
            mapOf("Authorization" to (if (Config.isBotToken) "Bot " else "") + Config.token)
        ) ?: return

        for (message in result) {
            if (message.id == channelMessage.id) {
                channelMessage.content = message.content
                channelMessage.embeds = message.embeds
                channelMessage.attachments = message.attachments
                return
            }
        }
    }

    private suspend inline fun <reified T> request(
        url: String,
        headers: Map<String, String>? = null,
        httpMethod: HttpMethod = HttpMethod.Get,
        requestBody: Any? = null,
    ): T? {
        // Very simple rate limit, max 1 request per second
        if (lastRequest + (1000 * requestsQueued) + 1000 > System.currentTimeMillis()) {
            requestsQueued++
            log("Rate limit hit, requestsQueued: $requestsQueued")
            delay(1000L * requestsQueued)
        }
        lastRequest = System.currentTimeMillis()
        requestsQueued = (requestsQueued - 1).coerceAtLeast(0)

        val result: T
        try {
            result = httpClient.request(url) {
                method = httpMethod
                if (requestBody != null) body = requestBody
                headers {
                    if (headers != null) {
                        for (header in headers) {
                            append(header.key, header.value)
                        }
                    }
                }
            }
        } catch (e: ResponseException) {
            lastException = e
            log(e.message + " - Body: " + e.response.content.readRemaining().readText())
            return null
        }

        log("HTTP received: $result")

        return result
    }

    private fun buildEmbedsFromMessage(channelMessage: ChannelMessage): MutableList<ChannelMessageEmbed> {
        val embeds = mutableListOf<ChannelMessageEmbed>()

        // Include up to 8 embeds
        channelMessage.embeds?.forEachIndexed { index, embed ->
            if (index < 7) {
                embeds.add(embed)
            }
        }

        // Add all attachments to one embed
        val attachmentsFields = mutableListOf<ChannelMessageEmbedField>()
        channelMessage.attachments?.forEach {
            attachmentsFields.add(ChannelMessageEmbedField(name = it.filename, value = it.url))
        }
        if (attachmentsFields.size > 0) {
            embeds.add(ChannelMessageEmbed(title = "Attachments", fields = attachmentsFields))
        }

        // Footer
        val user = channelMessage.author?.username + "#" + channelMessage.author?.discriminator
        val time = channelMessage.timestamp?.replace("T", " ")?.substring(0, 19)
        var channelName = ""
        var guildName = ""
        Config.channels.forEach {
            if (it.id == channelMessage.channel_id) {
                channelName = it.name.toString().replace(',', ' ').replace(':', ' ')
                guildName = it.guild?.name.toString().replace(',', ' ').replace(':', ' ')
            }
        }
        val footer = ChannelMessageEmbed(
            footer = ChannelMessageEmbedFooter(
                text = "Original message - Author: $user, Date: $time UTC, " +
                    "Channel: ${channelMessage.channel_id} $channelName, " +
                    "Guild: ${channelMessage.guild_id} $guildName"
            )
        )
        embeds.add(footer)

        return embeds
    }

    private fun messagesSend(message: String) {
        CoroutineScope(Dispatchers.Default).launch { messages.send(message) }
    }
}
