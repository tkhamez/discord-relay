package tkhamez.discordRelay.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.*
import tkhamez.discordRelay.lib.*
import java.text.SimpleDateFormat
import java.util.*

private var mainScope = MainScope()

private var messagesCoroutine: Job? = null
private var eventsCoroutine: Job? = null
private var messagesTextInitial = ""
private var sendDummyMessage = false

private const val LAYOUT_NARROW = 1
private const val LAYOUT_WIDE = 2
private const val CONTENT_COL1 = 1
private const val CONTENT_COL2 = 2
private const val CONTENT_CONFIG = 11
private const val CONTENT_GATEWAY = 12
private const val CONTENT_MESSAGES = 21

@Composable
fun App() {
    val androidContext = getContext()
    val language = getUserLanguage(androidContext)
    tkhamez.discordRelay.lib.ResString.setLanguage(language)
    ResString.setLanguage(language)
    loadConfig(androidContext, Config)

    startEventListener(androidContext)

    // Text for column 2, needs to be outside BoxWithConstraints.
    var messagesText by remember { mutableStateOf(messagesTextInitial) }
    if (messagesCoroutine?.isActive != true) {
        messagesCoroutine = mainScope.launch {
            while (true) {
                val message = messagesTake()
                val now = SimpleDateFormat("HH:mm:ss").format(Date())
                messagesText = "$now $message\n$messagesText"
                messagesTextInitial = messagesText // to restore text after destroy
            }
        }
    }

    BoxWithConstraints {
        val width = maxWidth
        Column {

            // Add headline with logo for Android
            if (androidContext != null) {
                val annotatedString = buildAnnotatedString {
                    appendInlineContent(id = "imageId")
                    append("  " + tkhamez.discordRelay.lib.ResString.appName)
                }
                val inlineContentMap = mapOf(
                    "imageId" to InlineTextContent(Placeholder(20.sp, 20.sp, PlaceholderVerticalAlign.TextCenter)) {
                        loadLogo()?.let {
                            Image(
                                painter = it,
                                modifier = Modifier.fillMaxSize(),
                                contentDescription = "",
                            )
                        }
                    }
                )
                Text(
                    annotatedString,
                    inlineContent = inlineContentMap,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .padding(4.dp, 4.dp, 4.dp, 0.dp)
                        .fillMaxWidth()
                        .wrapContentSize(Alignment.Center)
                )
            }

            if (width < 720.dp) {
                Column {
                    scrollableColumn(CONTENT_COL1, LAYOUT_NARROW, androidContext, messagesText)
                    scrollableColumn(CONTENT_COL2, LAYOUT_NARROW, androidContext, messagesText)
                }
            } else {
                Row {
                    scrollableColumn(CONTENT_COL1, LAYOUT_WIDE, androidContext, messagesText)
                    scrollableColumn(CONTENT_COL2, LAYOUT_WIDE, androidContext, messagesText)
                }
            }
        }
    }

    // For some reason the first message is (sometimes) not shown after:
    // - the app was started
    // - the app was destroyed (via back button or by turning the display)
    // - the app was resumed
    if (sendDummyMessage) {
        getGateway().putDummyMessage()
        sendDummyMessage = false
    }
}

/**
 * Only called on Android.
 */
fun appOnDestroy() {
    // Make sure that the coroutine is started again.
    messagesCoroutine?.cancel()

    // Reset cache or the displayed URL may be different from the used URL.
    Config.gatewayResponse = null
    Config.gatewayResponseBot = mutableMapOf()

    sendDummyMessage = true
}

private fun startEventListener(context: Any?) {
    if (eventsCoroutine?.isActive == true) {
        return
    }
    val gateway = getGateway()
    eventsCoroutine = mainScope.launch {
        while (true) {
            val event = gateway.eventsTake()
            if (event == EVENT_CLOSED || event == EVENT_CLOSE) {
                stopGateway(context)
            }
        }
    }
}

@Composable
private fun scrollableColumn(content: Int, layout: Int, context: Any?, messages: String) {
    Box(
        modifier = Modifier.fillMaxHeight(if (content == CONTENT_COL1 && layout == LAYOUT_NARROW) 0.7f else 1f)
    ) {
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxWidth(if (content == CONTENT_COL1 && layout == LAYOUT_WIDE) 0.5f else 1f)
                .padding(4.dp)
                .verticalScroll(scrollState)
        ) {
            when (content) {
                CONTENT_COL1 -> {
                    card(CONTENT_GATEWAY, context)
                    card(CONTENT_CONFIG, context)
                    //debugButtons()
                }
                CONTENT_COL2 -> {
                    card(CONTENT_MESSAGES, messages = messages, backgroundColor = Color.LightGray)
                }
            }
        }
        scrollbar(scrollState)
    }
}

@Composable
private fun card(content: Int, context: Any? = null, messages: String? = null, backgroundColor: Color = Color.White) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(4.dp),
        elevation = 4.dp
    ) {
        Column(
            modifier = Modifier.background(backgroundColor).padding(4.dp)
        ) {
            when (content) {
                CONTENT_CONFIG -> { configuration(context) }
                CONTENT_GATEWAY -> { gateway(context) }
                CONTENT_MESSAGES -> { messages(messages.toString()) }
            }
        }
    }
}

@Composable
private fun cardHeadline(test: String) {
    Text(
        test,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(0.dp, 0.dp, 0.dp, 4.dp)
    )
}

@Composable
private fun configuration(context: Any?) {
    var apiBaseUrl by remember { mutableStateOf(Config.apiBaseUrl) }
    fun updateApiBaseUrl(url: String) {
        apiBaseUrl = url
        Config.apiBaseUrl = url
        Config.gatewayResponse = null
        Config.gatewayResponseBot = mutableMapOf()
    }

    cardHeadline(ResString.configuration)

    TextField(
        value = apiBaseUrl,
        onValueChange = { updateApiBaseUrl(it) },
        label = { Text(ResString.apiBaseUrl) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    Button(
        onClick = { updateApiBaseUrl(Config.apiBaseUrlDefault) },
        modifier = Modifier.defaultMinSize(minWidth = 1.dp, minHeight = 1.dp),
        contentPadding = PaddingValues(4.dp, 4.dp)
    ) {
        Text(ResString.resetURL, fontSize = 10.sp)
    }

    var token by remember { mutableStateOf(Config.token) }
    TextField(
        value = token,
        onValueChange = {
            token = it
            Config.token = it
        },
        label = { Text(ResString.token) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    val isBotToken = remember { mutableStateOf(Config.isBotToken) }
    Row {
        Checkbox(
            checked = isBotToken.value,
            onCheckedChange = {
                isBotToken.value = it
                Config.isBotToken = it
            },
        )
        Text(ResString.isBotToken, Modifier.padding(12.dp))
    }

    var channelIds by remember { mutableStateOf(Config.channelIds) }
    TextField(
        value = channelIds,
        onValueChange = {
            channelIds = it
            Config.channelIds = it
        },
        label = { Text(ResString.channelIds) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    val onlyMentionEveryone = remember { mutableStateOf(Config.onlyMentionEveryone) }
    Row {
        Checkbox(
            checked = onlyMentionEveryone.value,
            onCheckedChange = {
                onlyMentionEveryone.value = it
                Config.onlyMentionEveryone = it
            },
        )
        Text(ResString.onlyMentionEveryone, Modifier.padding(12.dp))
    }

    var webhook by remember { mutableStateOf(Config.webhook) }
    TextField(
        value = webhook,
        onValueChange = {
            webhook = it
            Config.webhook = it
        },
        label = { Text(ResString.webhook) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )

    Row {
        Button(
            onClick = { saveConfig(context, Config) }
        ) {
            Text(ResString.save)
        }
        Button(
            modifier = Modifier.padding(4.dp, 0.dp, 0.dp, 0.dp),
            onClick = {
                loadConfig(context, Config)
                apiBaseUrl = Config.apiBaseUrl
                token = Config.token
                isBotToken.value = Config.isBotToken
                channelIds = Config.channelIds
                onlyMentionEveryone.value = Config.onlyMentionEveryone
                webhook = Config.webhook
            }
        ) {
            Text(ResString.load)
        }
    }
}

@Composable
private fun gateway(context: Any?) {
    cardHeadline(ResString.gateway)

    Row {
        Button(
            onClick = { startGateway(context) },
        ) {
            Text(ResString.connect)
        }
        Button(
            modifier = Modifier.padding(4.dp, 0.dp, 0.dp, 0.dp),
            onClick = { stopGateway(context) },
        ) {
            Text(ResString.disconnect)
        }
    }
}

@Composable
private fun messages(messages: String) {
    Text(
        messages,
        modifier = Modifier.fillMaxWidth()
    )
}

@Suppress("unused")
@Composable
private fun debugButtons() {
    val gateway = getGateway()
    Button(onClick = { gateway.testCloseResumeOK() }) { Text("D close resume OK") }
    Button(onClick = { gateway.testCloseResumeNOK() }) { Text("D close resume NOK") }
    Button(onClick = { gateway.testResume() }) { Text("D resume") }
    Button(onClick = { gateway.testSend("message") }) { Text("D request: send message") }
    Button(onClick = { gateway.testSend("heartbeat") }) { Text("D request: request heartbeat") }
    Button(onClick = { gateway.testSend("invalid_ready") }) { Text("D request: invalid ready") }
    Button(onClick = { gateway.testSend("{invalid}") }) { Text("D send invalid data") }
}
