package tkhamez.discordRelay.lib

import com.google.gson.Gson
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.*
import io.ktor.client.features.json.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*

private val httpClient = HttpClient(CIO) {
    install(JsonFeature)
}

/**
 * https://discord.com/developers/docs/resources/webhook
 * https://discord.com/developers/docs/resources/channel
 * https://discord.com/developers/docs/topics/rate-limits
 */
class Webhook {
    private var lastException: ResponseException? = null
    private var lastRequest: Long = 0

    private var job: Job? = null

    fun launchJob() {
        if (job?.isActive == true) {
            return
        }
        job = CoroutineScope(Dispatchers.Default).launch {
            while (isActive) {
                val message = relayQueueReceive()
                sendMessage(message)
            }
        }
    }

    suspend fun cancelAndJoinJob() {
        job?.cancelAndJoin()
        job = null
    }

    private suspend fun sendMessage(channelMessage: ChannelMessage) {
        if (Config.webhook.isEmpty()) {
            messagesSend(ResString.missingWebhook)
            return
        }

        if ((channelMessage.content ?: "").isEmpty()) {
            loadContent(channelMessage)
        }

        val payload = HttpSendWebhook(
            username = channelMessage.member?.nick ?: channelMessage.author?.username,
            avatar_url = if (channelMessage.author?.avatar != null) {
                    "https://cdn.discordapp.com/avatars/${channelMessage.author.id}/${channelMessage.author.avatar}"
                } else {
                    null
                },
            content = replaceRoleIdInMention(channelMessage.content ?: ""),
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
        // Very simple rate limit for Webhook execute, max 1 request every 2 seconds.
        if (httpMethod == HttpMethod.Post) {
            val delay = 2000L
            if (lastRequest + delay > System.currentTimeMillis()) {
                log("Rate limit hit.")
                delay(delay)
            }
            lastRequest = System.currentTimeMillis()
        }

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

    private fun replaceRoleIdInMention(content: String): String {
        var result = content
        val roleIds = Regex("""<@&(\d+)>""").findAll(content)
            .map { it.groups[1]!!.value }
            .toList()
        for (roleId in roleIds) {
            for (role in Config.guildRoles) {
                if (role.id == roleId) {
                    result = result.replace("<@&$roleId>", "`@${role.name}`")
                    break
                }
            }
        }
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

        // Add footer
        var user = channelMessage.author?.username
        if ((channelMessage.author?.discriminator ?: "0000") != "0000") {
            user += "#" + channelMessage.author?.discriminator
        }
        if (channelMessage.author?.bot == true) {
            user += " (bot)"
        }
        val time = channelMessage.timestamp?.replace("T", " ")?.substring(0, 19)
        var channelName = ""
        var guildName = ""
        Config.guildChannels.forEach {
            if (it.id == channelMessage.channel_id) {
                channelName = it.name.toString().replace(',', ' ').replace(':', ' ')
                guildName = it.guild?.name.toString().replace(',', ' ').replace(':', ' ')
                return@forEach
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
}
