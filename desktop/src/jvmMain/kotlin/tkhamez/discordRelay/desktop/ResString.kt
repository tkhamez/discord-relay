package tkhamez.discordRelay.desktop

import tkhamez.discordRelay.common.getUserLanguage

object ResString {

    var menu = "Menu"
        private set
    var minimizeTray = "Minimize to System Tray"
        private set
    var exit = "Exit"
        private set
    var restore = "Restore"
        private set

    init {
        if (getUserLanguage(null) == "de") {
            menu = "Menü"
            minimizeTray = "In System-Tray minimieren"
            exit = "Beenden"
            restore = "Wiederherstellen"
        }
    }
}
