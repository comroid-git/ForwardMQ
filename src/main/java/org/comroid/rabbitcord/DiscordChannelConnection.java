package org.comroid.rabbitcord;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.external.JDAWebhookClient;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Delivery;
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
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.concurrent.CompletableFuture.failedFuture;
import static java.util.function.Predicate.not;

@Log
@Value
@RequiredArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class DiscordChannelConnection extends Component.Base {
    Config config;
    @NonFinal
    Connection connection;
    @NonFinal
    Channel channel;

    @Override
    @SneakyThrows
    protected void $initialize() {
        var factory = new ConnectionFactory();
        factory.setUri(config.amqpUri);
        connection = factory.newConnection();
        checkChannel();
    }

    @SneakyThrows
    private void checkChannel() {
        if (channel != null && channel.isOpen())
            return;

        channel = connection.createChannel();
        channel.exchangeDeclare("aurion.chat", "fanout");

        String queue = channel.queueDeclare().getQueue();
        channel.queueBind(queue, "aurion.chat", "");

        channel.basicConsume(queue, true, this::handleRabbitData, consumerTag -> {
        });
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

    public static final Pattern MessagePattern = Pattern.compile("(?<server>[a-zA-Z0-9]+)" +
            "\\s(?<displayname>\\[(?<rank>.+)]" +
            "\\s(?<username>\\w+)):" +
            "\\s(?<message>.+)");
    public void sendToDiscord(net.kyori.adventure.text.Component component) {
        var channel = RabbitCord.Instance.getJda()
                .getGuildById(config.guildId)
                .getTextChannelById(config.channelId);
        assert channel != null;
        var text = string(component).replaceAll("[§&]\\w", "");
        var matcher = MessagePattern.matcher(text);
        if (Debug.isDebug())
            text += "\n\n```json\n" + GsonComponentSerializer.gson().serializeToTree(component) + "\n```";
        if (!matcher.matches()) {
            channel.sendMessage(text).queue();
            return;
        }
        var server = matcher.group("server");
        var username = matcher.group("username");
        var displayname = matcher.group("displayname");
        var content = matcher.group("message");
        //noinspection ConstantValue,OptionalOfNullableMisuse
        Optional.ofNullable(config.webhookUrl)
                .filter(not(String::isBlank))
                .map(WebhookClient::withUrl)
                .map(CompletableFuture::completedFuture)
                .orElseGet(() -> channel.retrieveWebhooks().submit()
                        .thenCompose(ls -> ls.stream()
                                .filter(wh -> wh.getName().toLowerCase().contains("aurion"))
                                .findAny()
                                .map(CompletableFuture::completedFuture)
                                .orElseGet(()->failedFuture(new NoSuchElementException("No webhook present; creating one...")))
                                .exceptionallyCompose(e -> {
                                    log.log(Level.INFO, "Could not obtain webhook", e);
                                    return channel.createWebhook("AurionChat Link").submit();
                                }))
                        .thenApply(JDAWebhookClient::from))
                .thenCompose(wh -> wh.send(new WebhookMessageBuilder()
                        .setUsername(displayname + " on " + server)
                        .setAvatarUrl("https://mc-heads.net/avatar/" + username)
                        .setContent(content)
                        .build()))
                .exceptionally(e-> {
                    log.log(Level.SEVERE, "Unable to forward message", e);
                    return null;
                });
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
            if (!data.has("source") && data.get("channel").getAsString().equalsIgnoreCase(config.channelName))
                sendToDiscord(component);
        } catch (Throwable t) {
            log.log(Level.SEVERE, "Internal error", t);
        }
    }

    @lombok.Value
    public static class Config implements DataNode, UUIDContainer {
        long guildId;
        long channelId;
        @Nullable String webhookUrl = null;
        @Nullable String inviteUrl;
        String amqpUri;
        String exchange;
        String channelName;

        public Config(@Alias("guildId") long guildId,
                      @Alias("channelId") long channelId,
                      //@Alias("webhookUrl") String webhookUrl,
                      @Alias("inviteUrl") String inviteUrl,
                      @Alias("amqpUri") String amqpUri,
                      @Alias("exchange") String exchange,
                      @Alias("exchange") String channelName) {
            this.guildId = guildId;
            this.channelId = channelId;
            //this.webhookUrl = webhookUrl;
            this.inviteUrl = inviteUrl;
            this.amqpUri = amqpUri;
            this.exchange = exchange;
            this.channelName = channelName;
        }

        @Ignore
        @Override
        public UUID getUuid() {
            return new UUID(guildId, channelId);
        }
    }
}
