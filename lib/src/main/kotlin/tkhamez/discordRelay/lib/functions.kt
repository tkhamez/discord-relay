package tkhamez.discordRelay.lib

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

private var messages = Channel<String>(5000)
private var events = Channel<Int>(100)

private var gateway = Gateway(Webhook())

fun messagesSend(message: String) {
    CoroutineScope(Dispatchers.Default).launch { messages.send(message) }
}

suspend fun messagesReceive(): String {
    val message = messages.receive()
    val now = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
    return "$now $message"
}

fun eventsSend(event: Int) {
    CoroutineScope(Dispatchers.Default).launch { events.send(event) }
}

suspend fun eventsReceive(): Int {
    return events.receive()
}

fun getGateway(): Gateway {
    return gateway
}

fun log(message: Any?) {
    val now = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())
    val msg = message.toString().replace(Config.token, "*****")
    println("$now $msg")
}
