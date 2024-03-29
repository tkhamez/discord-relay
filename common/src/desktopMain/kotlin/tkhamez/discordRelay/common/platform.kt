package tkhamez.discordRelay.common

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import tkhamez.discordRelay.lib.Config
import tkhamez.discordRelay.lib.getGateway
import java.util.prefs.Preferences

private val prefs: Preferences = Preferences.userRoot().node(Config.STORE_NAME)
private const val CONFIG_API_BASE_URL = "apiBaseUrl"
private const val CONFIG_TOKEN = "token"
private const val CONFIG_IS_BOT_TOKEN = "isBotToken"
private const val CONFIG_CHANNEL_IDS = "channelIds"
private const val CONFIG_ONLY_MENTION_EVERYONE = "onlyMentionEveryone"
private const val CONFIG_WEBHOOK = "webhook"

actual fun getUserLanguage(context: Any?): String {
    return System.getProperty("user.language") ?: "en"
}

actual fun saveConfig(context: Any?, config: Config) {
    prefs.put(CONFIG_API_BASE_URL, config.apiBaseUrl)
    prefs.put(CONFIG_TOKEN, config.token)
    prefs.put(CONFIG_IS_BOT_TOKEN, if (config.isBotToken) "1" else "0")
    prefs.put(CONFIG_CHANNEL_IDS, config.channelIds.joinToString(","))
    val onlyMentionEveryone = mutableListOf<String>()
    config.onlyMentionEveryone.forEach {
        onlyMentionEveryone.add(if (it) "1" else "0")
    }
    prefs.put(CONFIG_ONLY_MENTION_EVERYONE, onlyMentionEveryone.joinToString(","))
    prefs.put(CONFIG_WEBHOOK, config.webhook)
}

actual fun loadConfig(context: Any?, config: Config) {
    val apiBaseUrl = prefs.get(CONFIG_API_BASE_URL, "")
    if (apiBaseUrl.isNotEmpty()) {
        config.apiBaseUrl = apiBaseUrl
    }
    config.token = prefs.get(CONFIG_TOKEN, "")
    config.isBotToken = prefs.get(CONFIG_IS_BOT_TOKEN, "1") == "1"
    config.channelIds = prefs.get(CONFIG_CHANNEL_IDS, "").split(",").toMutableList()
    config.onlyMentionEveryone.clear()
    prefs.get(CONFIG_ONLY_MENTION_EVERYONE, "1").split(",").forEach {
        config.onlyMentionEveryone.add(it == "1")
    }
    config.webhook = prefs.get(CONFIG_WEBHOOK, "")
}

actual fun startGateway(context: Any?) {
    getGateway().init()
}

actual fun stopGateway(context: Any?) {
    getGateway().close()
}

@Composable
actual fun scrollbar(scrollState: ScrollState): Any? = Box {
    return VerticalScrollbar(
        modifier = Modifier
            .fillMaxHeight()
            .align(Alignment.CenterEnd), // does not work here
        adapter = rememberScrollbarAdapter(scrollState)
    )
}

@Composable
actual fun getContext(): Any? {
    return null
}

@Composable
actual fun loadLogo(): Painter? {
    return null
}
