package tkhamez.discordRelay.common

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import tkhamez.discordRelay.lib.Config


// For both

expect fun getUserLanguage(context: Any?): String

expect fun saveConfig(context: Any?, config: Config)

expect fun loadConfig(context: Any?, config: Config)

expect fun startGateway(context: Any?)

expect fun stopGateway(context: Any?)


// For Desktop only

@Composable
expect fun scrollbar(scrollState: ScrollState): Any?


// For Android only

@Composable
expect fun getContext(): Any?

expect fun loadLogo(): Painter?
