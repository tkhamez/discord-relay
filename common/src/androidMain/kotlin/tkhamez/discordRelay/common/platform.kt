package tkhamez.discordRelay.common

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.core.content.ContextCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import tkhamez.discordRelay.lib.Config

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = Config.STORE_NAME)
private val CONFIG_API_BASE_URL = stringPreferencesKey("apiBaseUrl")
private val CONFIG_TOKEN = stringPreferencesKey("token")
private val CONFIG_IS_BOT_TOKEN = stringPreferencesKey("isBotToken")
private val CONFIG_CHANNEL_IDS = stringPreferencesKey("channelIds")
private val CONFIG_ONLY_MENTION_EVERYONE = stringPreferencesKey("onlyMentionEveryone")
private val CONFIG_WEBHOOK = stringPreferencesKey("webhook")

actual fun getUserLanguage(context: Any?): String {
    if (context !is Context) {
        return "en"
    }
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) { // >= 7 (Nougat), API 24
        context.resources.configuration.locales.get(0).toString().substring(0, 2)
    } else {
        @Suppress("DEPRECATION")
        context.resources.configuration.locale.toString().substring(0, 2)
    }
}

actual fun saveConfig(context: Any?, config: Config) {
    if (context !is Context) {
        return
    }
    CoroutineScope(Dispatchers.Default).launch {
        context.dataStore.edit { settings ->
            settings[CONFIG_API_BASE_URL] = config.apiBaseUrl
            settings[CONFIG_TOKEN] = config.token
            settings[CONFIG_IS_BOT_TOKEN] = if (config.isBotToken) "1" else "0"
            settings[CONFIG_CHANNEL_IDS] = config.channelIds
            settings[CONFIG_ONLY_MENTION_EVERYONE] = if (config.onlyMentionEveryone) "1" else "0"
            settings[CONFIG_WEBHOOK] = config.webhook
        }
    }
}

actual fun loadConfig(context: Any?, config: Config) {
    if (context !is Context) {
        return
    }
    val preferences = runBlocking { context.dataStore.data.first() }
    val apiBaseUrl = preferences[CONFIG_API_BASE_URL] ?: ""
    if (apiBaseUrl.isNotEmpty()) {
        config.apiBaseUrl = apiBaseUrl
    }
    config.token = preferences[CONFIG_TOKEN] ?: ""
    config.isBotToken = preferences[CONFIG_IS_BOT_TOKEN] == "1"
    config.channelIds = preferences[CONFIG_CHANNEL_IDS] ?: ""
    config.onlyMentionEveryone = preferences[CONFIG_ONLY_MENTION_EVERYONE] == "1"
    config.webhook = preferences[CONFIG_WEBHOOK] ?: ""
}

private val foregroundServiceClass = Class.forName("tkhamez.discordRelay.android.ForegroundService")

actual fun startGateway(context: Any?) {
    if (context !is Context) {
        return
    }
    val intent = Intent(context, foregroundServiceClass)
    ContextCompat.startForegroundService(context, intent)
}

actual fun stopGateway(context: Any?) {
    if (context !is Context) {
        return
    }
    val intent = Intent(context, foregroundServiceClass)
    context.stopService(intent)
}

@Composable
actual fun scrollbar(scrollState: ScrollState): Any? {
    return null
}

@Composable
actual fun getContext(): Any? {
    return LocalContext.current
}

@Composable
actual fun loadLogo(): Painter? {
    return painterResource(id = R.drawable.ic_launcher_foreground)
}
