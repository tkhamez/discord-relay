# Discord Relay

## Changelog

### next

- Fix: Added a queue for messages so that they always arrive in the correct order.
- Fix: A maximum of 1 message is sent every 2 seconds to avoid being rate-limited.
- Fix (Android): If the display has been rotated, the settings are no longer reset.

### v1.3.0, 2022-04-09

Features:
- Each channel now has its own "@everyone" setting. `DISCORD_RELAY_ONLY_MENTION_EVERYONE` is now a 
  comma separated list.

Relayed message:
- The footer of the message now shows whether the author is a bot.
- Messages now use the original avatar, if available.
- Messages now use the author's username if the member's nickname is not available.
- Message embeds now also include the color and the footer icon of the original message.
- @deleted-role is now replaced with `@Role name` in the message content.

UI:
- The new channel ID fields can contain additional text at the end separated by a space.
- General UI improvements.
- Fix: The time of a message is now the time it was created, not the time it was displayed.

Console client:
- Change: Exit the console program if the websocket connection cannot be restored.
- Added an example systemd service unit file for the console program.

### v1.2.1, 2022-04-04

- Fix for an endless disconnect/reconnect loop.
- Android: Do not show toast twice on exit.

### v1.2.0, 2022-04-03

- Added channel and guild name to the footer of the relayed message.
- Messages in the GUI of the app are now selectable.
- Reduced minimum width of left column.
- Generally improved connection handling, hopefully.
- Added rate limits for the Webhook.
- Fix: Embeds and attachments were not always relayed.
- Fix: Gateway was sometimes not properly closed.

### v1.1.0, 2022-03-30

- Android: Prevent the WebSocket connection from sleeping via wake lock. This can significantly affect battery life.
- The app now attempts to reconnect after the Internet connection was lost or similar errors occurred.
- The relayed message now includes links to all attachments from the original message.
- The relayed message now includes up to 8 embeds from the original message.

### v1.0.0, 2022-03-26

Initial app for Linux, macOS, Windows (each GUI and console) and Android with ability to relay from several
channels to one channel.

Known issues:
- Android sometimes "pauses" the WebSocket connection.
