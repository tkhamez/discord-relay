package tkhamez.discordRelay.desktop

import tkhamez.discordRelay.common.App
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.*

fun main() = application {
    var isVisible by remember { mutableStateOf(true) }
    val icon = painterResource("app-icon.png")

    Window(
        onCloseRequest = ::exitApplication,
        visible = isVisible,
        title = tkhamez.discordRelay.lib.ResString.appName,
        icon = icon,
    ) {
        MaterialTheme {
            App()
        }

        MenuBar {
            Menu(ResString.menu, mnemonic = 'M') {
                Item(ResString.minimizeTray, onClick = { isVisible = false })
                Item(ResString.exit, onClick = ::exitApplication)
            }
        }
    }

    if (!isVisible) {
        Tray(
            icon = icon,
            tooltip = tkhamez.discordRelay.lib.ResString.appName,
            onAction = { isVisible = true },
            menu = {
                Item(ResString.restore, onClick = { isVisible = true })
                Item(ResString.exit, onClick = ::exitApplication)
            },
        )
    }
}
