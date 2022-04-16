package tkhamez.discordRelay.lib

object Config {
    const val STORE_NAME = "tkhamez-discord-relay"

    const val apiBaseUrlDefault = "https://discord.com/api"

    var apiBaseUrl = apiBaseUrlDefault

    var token = ""

    var isBotToken = false

    var channelIds = mutableListOf<String>()

    var onlyMentionEveryone = mutableListOf<Boolean>()

    var webhook = ""

    /**
     * Gateway response cache
     */
    var gatewayResponse: HttpResponseGateway? = null

    /**
     * Gateway response cache for bots
     */
    var gatewayResponseBot: MutableMap<String, HttpResponseGateway> = mutableMapOf()

    /**
     * Cache of channel IDs and names with guild.
     */
    val guildChannels = mutableListOf<GuildProperty>()

    /**
     * Cache of role IDs and names with guild.
     */
    val guildRoles = mutableListOf<GuildProperty>()

    /**
     * Make sure that the onlyMentionEveryone list has the same size as the channelIds list.
     */
    fun sanitize() {
        if (channelIds.size > onlyMentionEveryone.size) {
            repeat(channelIds.size - onlyMentionEveryone.size) {
                onlyMentionEveryone.add(true)
            }
        } else if (onlyMentionEveryone.size > channelIds.size) {
            repeat(onlyMentionEveryone.size - channelIds.size) {
                onlyMentionEveryone.removeLast()
            }
        }
    }

    /**
     * Return list of channel IDs without additional text at the end.
     */
    fun channelIds(): MutableList<String> {
        val ids = mutableListOf<String>()
        for (value in channelIds) {
            val pos = value.indexOf(' ')
            if (pos > 0) {
                ids.add(value.substring(0, pos))
            } else {
                ids.add(value)
            }
        }
        return ids
    }
}
