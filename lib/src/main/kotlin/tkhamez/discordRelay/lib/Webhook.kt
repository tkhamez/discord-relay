package tkhamez.discordRelay.lib

import com.google.gson.Gson
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.*
import io.ktor.client.features.json.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.BlockingQueue

private val httpClient = HttpClient(CIO) {
    install(JsonFeature)
}

/**
 * https://discord.com/developers/docs/resources/webhook
 * https://discord.com/developers/docs/resources/channel
 */
class Webhook(private val messages: BlockingQueue<String>) {

    suspend fun sendMessage(messageReceived: WsReceiveMessageCreate) {
        if (Config.webhook.isEmpty()) {
            withContext(Dispatchers.IO) { messages.put(ResString.missingWebhook) }
            return
        }

        var content: String = messageReceived.content ?: ""
        if (content.isEmpty()) {
            content = loadContent(messageReceived)
        }

        val user = messageReceived.author?.username + "#" + messageReceived.author?.discriminator
        val time = messageReceived.timestamp?.replace("T", " ")?.substring(0, 19)
        val footer = DiscordMessageEmbed(
            footer = DiscordMessageEmbedFooter(
                text = "Original message - Author: $user, Date: $time UTC, " +
                    "Guild: ${messageReceived.guild_id}, Channel: ${messageReceived.channel_id}"
            )
        )

        val embeds = buildEmbedsFromMessage(messageReceived)
        embeds.add(footer)

        val payload = HttpRequestMessage(
            username = messageReceived.member?.nick,
            content = content,
            embeds = embeds,
        )

        log("HTTP request: POST ${Config.webhook}:")
        log(Gson().toJson(payload))
        try {
            httpClient.request<String>(Config.webhook) {
                method = HttpMethod.Post
                header("Content-Type", "application/json")
                body = payload
            }
        } catch (e: ResponseException) {
            withContext(Dispatchers.IO) { messages.put("${ResString.relayFailed}: ${e.message}") }
            log(e.message + " - Body: " + e.response.content.readRemaining().readText())
            return
        }

        withContext(Dispatchers.IO) { messages.put(ResString.messageRelayed) }
    }

    private suspend fun loadContent(messageReceived: WsReceiveMessageCreate): String {
        /*val url0 = "${Config.apiBaseUrl}/channels/${messageReceived.channel_id}/messages/${messageReceived.id}"
        try {
            val result0 = httpClient.get<String>(url0) { header("Authorization", Config.token) }
            println(result0)
        } catch (e: ResponseException) {
            println(e.message)
        }*/

        val url = "${Config.apiBaseUrl}/channels/${messageReceived.channel_id}/messages?limit=10"
        log("HTTP request: GET $url")

        val result: List<HttpResponseChannelMessage>
        try {
            result = httpClient.get(url) {
                header("Authorization", (if (Config.isBotToken) "Bot " else "") + Config.token)
            }
        } catch (e: ResponseException) {
            log(e.message + " - Body: " + e.response.content.readRemaining().readText())
            return ""
        }

        for (message in result) {
            if (message.id == messageReceived.id) {
                return message.content ?: ""
            }
        }

        return ""
    }

    private fun buildEmbedsFromMessage(messageReceived: WsReceiveMessageCreate): MutableList<DiscordMessageEmbed> {
        val result = mutableListOf<DiscordMessageEmbed>()

        // Include up to 8 embeds
        messageReceived.embeds?.forEachIndexed { index, embed ->
            if (index < 7) {
                result.add(embed)
            }
        }

        // Add all attachments to one embed
        val attachmentsFields = mutableListOf<DiscordMessageEmbedField>()
        messageReceived.attachments?.forEach {
            attachmentsFields.add(DiscordMessageEmbedField(name = it.filename, value = it.url))
        }
        if (attachmentsFields.size > 0) {
            result.add(DiscordMessageEmbed(title = "Attachments", fields = attachmentsFields))
        }

        return result
    }
}
