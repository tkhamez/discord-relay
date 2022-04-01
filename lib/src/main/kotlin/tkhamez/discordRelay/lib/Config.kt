package tkhamez.discordRelay.lib

object Config {
    const val STORE_NAME = "tkhamez-discord-relay"

    const val apiBaseUrlDefault = "https://discord.com/api"

    var apiBaseUrl = apiBaseUrlDefault

    var token = ""

    var isBotToken = false

    var channelIds = ""

    var onlyMentionEveryone = false

    var webhook = ""

    /**
     * Gateway response cache
     */
    var gatewayResponse: HttpResponseGateway? = null

    /**
     * Gateway response cache for bots
     */
    var gatewayResponseBot: MutableMap<String, HttpResponseGateway> = mutableMapOf()

    val channels = mutableListOf<GuildChannel>()

    fun channelIdList(): List<String> {
        return channelIds.split(',')
    }
}
