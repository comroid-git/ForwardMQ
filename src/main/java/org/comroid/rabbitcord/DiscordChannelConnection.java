package org.comroid.rabbitcord;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.experimental.NonFinal;
import org.comroid.annotations.Ignore;
import org.comroid.api.attr.UUIDContainer;
import org.comroid.api.tree.Component;

import java.net.URI;
import java.util.UUID;

@Value
@RequiredArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class DiscordChannelConnection extends Component.Base {
    Config config;
    @NonFinal Connection connection;
    @NonFinal Channel channel;

    @Override
    protected void $initialize() {
        super.$initialize();
    }

    @lombok.Value
    public static class Config implements UUIDContainer {
        long guildId;
        long channelId;
        URI amqpUri;

        @Ignore
        @Override
        public UUID getUuid() {
            return new UUID(guildId, channelId);
        }
    }
}
