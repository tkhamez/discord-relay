package tkhamez.discordRelay.console

import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking
import tkhamez.discordRelay.lib.*
import kotlin.system.exitProcess

fun main() {
    val language = System.getProperty("user.language")
    tkhamez.discordRelay.lib.ResString.setLanguage(language)
    ResString.setLanguage(language)

    println("\n### ${tkhamez.discordRelay.lib.ResString.appName} ###\n")

    if (!readConfig()) {
        println(ResString.setEnvVars)
        exitProcess(0)
    }

    getGateway().init()
    runBlocking {
        while (isActive) {
            log(messagesReceive())
        }
    }
}

private fun readConfig(): Boolean {
    val token = System.getenv("DISCORD_RELAY_TOKEN")
    if (token !is String) {
        return false
    }

    val apiBaseUrl = System.getenv("DISCORD_RELAY_API_BASE_URL")
    if (apiBaseUrl?.isNotEmpty() == true) {
        Config.apiBaseUrl = apiBaseUrl
    }
    Config.token = token
    Config.isBotToken = System.getenv("DISCORD_RELAY_IS_BOT_TOKEN") == "1"
    Config.channelIds = System.getenv("DISCORD_RELAY_CHANNEL_IDS") + ""
    Config.onlyMentionEveryone = System.getenv("DISCORD_RELAY_ONLY_MENTION_EVERYONE") == "1"
    Config.webhook = System.getenv("DISCORD_RELAY_WEBHOOK") + ""

    return true
}
