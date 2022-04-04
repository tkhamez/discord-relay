# Discord Relay

## Changelog

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

Initial app for Linux, macOS, Windows (GUI and console) and Android with ability to relay from several channels to one
channel.

Known issues:
- Android sometimes "pauses" the WebSocket connection.
