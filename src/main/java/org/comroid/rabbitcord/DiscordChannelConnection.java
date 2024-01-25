package org.comroid.rabbitcord;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rabbitmq.client.*;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.experimental.NonFinal;
import lombok.extern.java.Log;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.comroid.annotations.Alias;
import org.comroid.annotations.Ignore;
import org.comroid.api.attr.UUIDContainer;
import org.comroid.api.data.seri.DataNode;
import org.comroid.api.func.util.Debug;
import org.comroid.api.tree.Component;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.logging.Level;
import java.util.stream.Collectors;

@Log
@Value
@RequiredArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class DiscordChannelConnection extends Component.Base {
    Config config;
    @NonFinal Connection connection;
    @NonFinal Channel channel;

    @Override
    @SneakyThrows
    protected void $initialize() {
        var factory = new ConnectionFactory();
        factory.setUri(config.amqpUri);
        connection = factory.newConnection();
    }

    @SneakyThrows
    private void checkChannel() {
        if (channel.isOpen())
            return;

        channel = connection.createChannel();
        channel.exchangeDeclare("aurion.chat", "fanout");

        String queue = channel.queueDeclare().getQueue();
        channel.queueBind(queue, "aurion.chat", "");

        channel.basicConsume(queue, true, this::handleRabbitData, consumerTag -> {});
    }

    @Override
    @SneakyThrows
    protected void $terminate() {
        channel.close();
        connection.close();
    }

    public String string(net.kyori.adventure.text.Component comp) {
        if (comp instanceof TextComponent text)
            return text.content() + comp.children().stream()
                    .map(this::string)
                    .collect(Collectors.joining());
        return comp.toString();
    }

    public void sendToDiscord(net.kyori.adventure.text.Component component) {
        RabbitCord.Instance.getJda()
                .getGuildById(config.guildId)
                .getTextChannelById(config.channelId)
                .sendMessage(string(component)
                        .replaceAll("[§&]\\w", "")
                +(Debug.isDebug()?"\n\n```json\n"+GsonComponentSerializer.gson().serializeToTree(component)+"\n```":""))
                .queue();
    }

    @SneakyThrows
    public void sendToRabbit(net.kyori.adventure.text.Component component) {
        var json = new JsonObject();

        json.addProperty("type", "chat");
        json.addProperty("source", "discord");
        json.addProperty("channel", "global");
        json.addProperty("message", GsonComponentSerializer.gson().serializeToTree(component).toString());

        checkChannel();
        channel.basicPublish(config.exchange, "", null, json.toString().getBytes());
    }

    private void handleRabbitData(String $, Delivery content) {
        try {
            var data = new JsonParser().parse(new String(content.getBody(), StandardCharsets.UTF_8)).getAsJsonObject();
            var component = GsonComponentSerializer.gson().deserialize(data.get("message").getAsString());
            if (!data.has("source"))
                sendToDiscord(component);
        } catch (Throwable t) {
            log.log(Level.SEVERE, "Internal error", t);
        }
    }

    @lombok.Value
    public static class Config implements DataNode, UUIDContainer {
        long guildId;
        long channelId;
        String amqpUri;
        String exchange;

        public Config(@Alias("guildId") long guildId,
                      @Alias("channelId") long channelId,
                      @Alias("amqpUri") String amqpUri,
                      @Alias("exchange") String exchange) {
            this.guildId = guildId;
            this.channelId = channelId;
            this.amqpUri = amqpUri;
            this.exchange = exchange;
        }

        @Ignore
        @Override
        public UUID getUuid() {
            return new UUID(guildId, channelId);
        }
    }
}
