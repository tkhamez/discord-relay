package tkhamez.discordRelay.console

object ResString {

    var setEnvVars = "Please set all required environment variables."
        private set

    private var language = "en"

    fun setLanguage(locale: String) {
        language = locale
        update()
    }

    private fun update() {
        if (language == "de") {
            setEnvVars = "Bitte setzen Sie alle erforderlichen Umgebungsvariablen."
        }
    }
}
