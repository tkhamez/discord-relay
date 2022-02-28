package tkhamez.discordRelay.lib

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
        val payload = HttpRequestMessage(
            username = messageReceived.member?.nick,
            content = content,
            embeds = listOf(
                HttpRequestMessageEmbeds(
                    footer = HttpRequestMessageEmbedsText(
                        text = "Original message sent by $user on $time UTC in ${messageReceived.channel_id}"
                    )
                )
            ),
        )

        log("HTTP request: POST ${Config.webhook}")
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
}
