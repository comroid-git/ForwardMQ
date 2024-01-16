# Minecraft Chat to Discord Link
Integrates discord channels with RabbitMQ traffic by the scheme of [AurionChat](https://github.com/Mineaurion/Aurionchat)

## Add to your Discord Server
- Invite the Discord bot using [this link](https://discord.com/oauth2/authorize?client_id=1134863874626179193&scope=bot%20applications.commands&permissions=939912257) (not all permissions required for this functionality; bot is also used for [MCSD](https://github.com/comroid-git/mc-server-hub))
- In a channel that you have `Manage Channel` permissions on, execute the command `/amqp-link` providing both parameters
- For a link using [AurionChat](https://github.com/Mineaurion/Aurionchat), your `exchange-name` parameter will most likely have to be `aurion.chat`
- Check channel status with `/amqp`
- Executing `/amqp-unlink` will remove the linkage
