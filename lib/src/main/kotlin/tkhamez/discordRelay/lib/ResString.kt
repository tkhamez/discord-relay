package tkhamez.discordRelay.lib

object ResString {
    const val appName = "Discord Relay"
    const val server = "Server"
    const val client = "Client"

    var waitSeconds = "Please wait 5 seconds between connection attempts."
        private set
    var alreadyConnectingOrConnected = "The connection is already being established or already exists."
        private set
    var requestingGatewayURL = "Requesting gateway URL ..."
        private set
    var error = "Error: "
        private set
    var failedGatewayURL = "Failed to read gateway URL."
        private set
    var sessionStartLimitReached = "Session start limit reached, try again in $1 minutes."
        private set
    var connecting = "Connecting ..."
        private set
    var connected = "Connected."
        private set
    var closing = "Closing the connection ..."
        private set
    var disconnected = "Disconnected"
        private set
    var errorConnection = "Connection error: "
        private set
    var reconnectAttempt = "Reconnect attempt in $1 seconds."
        private set
    var reconnectAttemptCanceled = "Reconnect attempt canceled."
        private set
    var errorReceiving = "Error while receiving: "
        private set
    var messageError = "Could not handle message: "
        private set
    var cannotResume = "Cannot resume connection."
        private set
    var connectionLost = "Connection lost."
        private set
    var connectionResumed = "Connection resumed."
        private set
    var invalidHello = "Invalid data from HELLO message."
        private set
    var ready = "Ready to receive messages."
        private set
    var readyError = "Received invalid READY event."
        private set
    var messageReceived = "Message received."
        private set
    var missingWebhook = "Missing webhook, cannot relay message."
        private set
    var relayFailed = "Relaying of the message has failed."
        private set
    var messageRelayed = "Message relayed."
        private set

    private var language = "en"

    fun setLanguage(locale: String) {
        language = locale
        update()
    }

    private fun update() {
        if (language == "de") {
            waitSeconds = "Bitte warten Sie 5 Sekunden zwischen den Verbindungsversuchen."
            alreadyConnectingOrConnected = "Die Verbindung wird bereits aufgebaut oder besteht bereits."
            requestingGatewayURL = "Abfragen der Gateway-URL ..."
            error = "Fehler: "
            failedGatewayURL = "Gateway-URL konnte nicht gelesen werden."
            sessionStartLimitReached = "Sitzungsstartlimit erreicht, versuchen Sie es in $1 Minuten erneut."
            connecting = "Verbinden ..."
            connected = "Verbunden."
            closing = "Schließen der Verbindung ..."
            disconnected = "Getrennt"
            errorConnection = "Verbindungsfehler: "
            reconnectAttempt = "Erneuter Verbindungsversuch in $1 Sekunden."
            reconnectAttemptCanceled = "Erneuter Verbindungsversuch abgebrochen."
            errorReceiving = "Fehler beim Empfang: "
            messageError = "Die Nachricht konnte nicht verarbeitet werden: "
            cannotResume = "Die Verbindung kann nicht wiederhergestellt werden."
            connectionLost = "Verbindung verloren."
            connectionResumed = "Verbindung wiederhergestellt."
            invalidHello = "Ungültige Daten aus der HELLO-Nachricht."
            ready = "Bereit zum Empfang von Nachrichten."
            readyError = "Es wurde ein ungültiges READY-Ereignis empfangen."
            messageReceived = "Nachricht empfangen."
            missingWebhook = "Webhook fehlt, kann Nachricht nicht weiterleiten."
            relayFailed = "Die Weiterleitung der Nachricht ist fehlgeschlagen."
            messageRelayed = "Nachricht wurde weitergeleitet."
        }
    }
}
