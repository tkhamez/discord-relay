# Discord Relay

An application for Linux, macOS, Windows (each GUI and console) and Android to forward Discord messages from 
one Discord server to another.

## Setup

Discord Bot:
- Create a new application at https://discord.com/developers/applications.
- Add bot - *don't* make it public.
- Copy the bot token for configuring this app.
- Go to OAuth2 -> URL Generator, at "Scopes" check `bot` and then the following bot permissions:
  `Read Messages/View Channels`, `Read Message History`.
- Copy the URL and open it in the browser to add the bot to your server.
- Add bot to the channels that it should read.

Webhook:
- Create a webhook on the target server for a channel: Edit channel -> Integrations -> Create Webhook.
- Copy the webhook URL for the configuration of this application.

## Notes

- If a message has been edited, it will currently not be relayed again.
- Messages in threads will currently not be relayed.
- The number of embeds that can be relayed is limited to 8.
- Only embeds of the "rich" type were tested, without video or provider fields.
- The channel and guild name in the footer of the relayed message does not contain commas or colons, they are replaced 
  with a space if any are present.
- The channel ID fields in the UI can contain additional text at the end separated by a space.
- There's a limit of 1000 connections (excluding resumes) withing 24 hours. The bot token will be invalidated if this 
  limit is exceeded. The app tries to keep track of this.
- Read [this](https://support.discord.com/hc/en-us/articles/115002192352) and 
  [this](https://discord.com/guidelines#respect-discord) if you consider using a user token.
- Android: This app can significantly affect battery life as it has to constantly send data to Discord to maintain
  the WebSocket connection.
- The app icon is from https://www.appiconsios.com.

The configuration values for the desktop and Android apps are stored in different locations depending on the operating
system:
- Linux: ~/.java/.userPrefs/tkhamez-discord-relay/prefs.xml
- Windows: Registry \HKEY_CURRENT_USER\SOFTWARE\JavaSoft\Prefs\tkhamez-discord-relay
- macOS: ~/Library/Preferences/com.apple.java.util.prefs.plist
- Android: In the Jetpack DataStore. They are deleted when the application is uninstalled.

The console app is configured with environment variables:
- `DISCORD_RELAY_API_BASE_URL` Optional, default: https://discord.com/api
- `DISCORD_RELAY_TOKEN` Required
- `DISCORD_RELAY_IS_BOT_TOKEN` 1 or 0, optional, default: 1
- `DISCORD_RELAY_CHANNEL_IDS` Comma separated list of channel IDs
- `DISCORD_RELAY_ONLY_MENTION_EVERYONE` Comma separated list of 1 or 0, position according to the channels 
  in DISCORD_RELAY_CHANNEL_IDS, optional, default: 1
- `DISCORD_RELAY_WEBHOOK`

If the app crashes on macOS, try:
```
SKIKO_RENDER_API=SOFTWARE ./DiscordRelay.app/Contents/MacOS/DiscordRelay
```

## Build

- Install OpenJDK 17.

### Desktop

Run on each platform:
```shell
./gradlew createDistributable
# desktop/build/compose/binaries/main/app/

./gradlew packageUberJarForCurrentOS
# desktop/build/compose/jars/
```

### Android

- Install Android SDK.
- Copy `local.properties.dist` to `local.properties` and adjust path or set `ANDROID_HOME` environment variable.

```shell
./gradlew android:build
# android/build/outputs/apk/
```

Create key store:
```shell
keytool -genkey -v -keystore misc/key-store.jks -keyalg RSA -keysize 2048 -validity 9131 -alias key0
(keytool -v -list -keystore misc/key-store.jks)
```

Sign:
```shell
zipalign -p -f -v 4 android/build/outputs/apk/release/android-release-unsigned.apk android/build/discord-relay.apk
apksigner sign --ks misc/key-store.jks android/build/discord-relay.apk
(apksigner verify -v android/build/discord-relay.apk)
(keytool -printcert -jarfile android/build/discord-relay.apk)
```

### Console

```shell
./gradlew console:build
# console/build/distributions/
```

### GitHub Workflow Configuration

- Base64 encode keystore: `base64 -w 0 misc/key-store.jks > misc/key-store.jks.base64.txt`.
- Create an action secret `KEY_STORE` with that value.
- Create an action secret `KEY_STORE_PASS` with the password for the key store.
