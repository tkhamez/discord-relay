package tkhamez.discordRelay.lib

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue

private var messages: BlockingQueue<String> = LinkedBlockingQueue()

private var gateway = Gateway(messages, LinkedBlockingQueue(), Webhook(messages))

@Suppress("SimpleDateFormat")
fun log(message: Any?) {
    val now = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(Date())
    println("$now $message")
}

suspend fun messagesTake(): String {
    return withContext(Dispatchers.IO) {
        messages.take()
    }
}

fun getGateway(): Gateway {
    return gateway
}
