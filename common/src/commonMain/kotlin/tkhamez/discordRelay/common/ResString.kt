package tkhamez.discordRelay.common

object ResString {
    const val token = "Token"
    const val gateway = "Gateway"
    const val webhook = "Webhook"

    var configuration = "Configuration"
        private set
    var apiBaseUrl = "API base URL"
        private set
    var resetURL = "Reset URL"
        private set
    var isBotToken = "Is a bot token"
        private set
    var channelIds = "Channel IDs (comma separated)"
        private set
    var onlyMentionEveryone = "Only @everyone, @here"
        private set
    var save = "Save"
        private set
    var load = "Load"
        private set
    var connect = "Connect"
        private set
    var disconnect = "Disconnect"
        private set

    private var language = "en"

    fun setLanguage(locale: String) {
        language = locale
        update()
    }

    private fun update() {
        if (language == "de") {
            configuration = "Konfiguration"
            apiBaseUrl = "API-Basis-URL"
            resetURL = "URL zurücksetzen"
            isBotToken = "Ist ein Bot-Token"
            channelIds = "Kanal IDs (kommasepariert)"
            onlyMentionEveryone = "Nur @everyone, @here"
            save = "Speichern"
            load = "Laden"
            connect = "Verbinden"
            disconnect = "Trennen"
        }
    }
}
