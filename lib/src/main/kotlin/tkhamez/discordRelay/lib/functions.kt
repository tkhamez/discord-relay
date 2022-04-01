package tkhamez.discordRelay.lib

import kotlinx.coroutines.channels.Channel
import java.text.SimpleDateFormat
import java.util.*

private var messages = Channel<String>(5000)
private var events = Channel<Int>(100)

private var gateway = Gateway(messages, events, Webhook(messages))

suspend fun messagesReceive(): String {
    return messages.receive()
}

suspend fun eventsReceive(): Int {
    return events.receive()
}

fun getGateway(): Gateway {
    return gateway
}

@Suppress("SimpleDateFormat")
fun log(message: Any?) {
    val now = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(Date())
    val msg = message.toString().replace(Config.token, "*****")
    println("$now $msg")
}
