package org.comroid.forwardmq;

import lombok.*;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageCreateAction;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.dv8tion.jda.api.utils.Compression;
import org.comroid.annotations.Description;
import org.comroid.api.Polyfill;
import org.comroid.api.attr.Named;
import org.comroid.api.data.seri.DataNode;
import org.comroid.api.func.util.Command;
import org.comroid.api.func.util.Event;
import org.comroid.api.func.util.Streams;
import org.comroid.forwardmq.dto.system.Config;
import org.comroid.forwardmq.entity.DataFlow;
import org.comroid.forwardmq.entity.proto.adapter.discord.ProtoAdapter$DiscordChannel;
import org.comroid.forwardmq.entity.proto.adapter.rabbit.ProtoAdapter$Rabbit;
import org.comroid.forwardmq.entity.proto.processor.ProtoProcessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.net.URI;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.comroid.forwardmq.util.ApplicationContextProvider.bean;

@Value
public class DiscordAdapter implements Command.Handler {
    Event.Bus<GenericEvent> bus;
    Command.Manager cmdr;
    Config config;
    JDA jda;

    @SneakyThrows
    public DiscordAdapter(Config config) {
        this.bus = new Event.Bus<>("Discord Event Bus");
        this.config = config;
        this.jda = JDABuilder.create(config.getDiscordToken(), GatewayIntent.getIntents(GatewayIntent.ALL_INTENTS))
                .setCompression(Compression.ZLIB)
                .addEventListeners((EventListener) bus::publish)
                .build()
                .awaitReady();
        this.cmdr = new Command.Manager(this);
        cmdr.new Adapter$JDA(jda);
        cmdr.initialize();
    }

    @SneakyThrows
    @Command(permission = "16", ephemeral = true)
    @Description("Link this channel to the specified AMQP Exchange")
    public void link(SlashCommandInteractionEvent event,
                     @Command.Arg @Description("The AMQP URL to connect to") String amqpUri,
                     @Command.Arg(autoFill = {"aurion.chat"}) @Description("The AMQP exchange name to connect to") String exchangeName,
                     @Command.Arg @Description("The data scheme to refer to") LinkType type) {
        final var dfm = bean(DataFlowManager.class);
        var ac2dc = dfm.getProcessorRepo_$$()
                .findByName("aurion2discord")
                .orElseThrow(() -> new NoSuchElementException("Missing processor: aurion2discord"));
        var dc2ac = dfm.getProcessorRepo_$$()
                .findByName("discord2aurion")
                .orElseThrow(() -> new NoSuchElementException("Missing processor: discord2aurion"));
        var discord = new ProtoAdapter$DiscordChannel(event.getChannelIdLong());
        var rabbit = new ProtoAdapter$Rabbit(new URI(amqpUri), exchangeName, null, null);
        var d2r = new DataFlow(discord, rabbit, List.of(dc2ac));
        var r2d = new DataFlow(rabbit, discord, List.of(ac2dc));

        Stream.of(discord, rabbit, d2r, r2d).forEach(e -> e
                .setDisplayName(e.getClass().getSimpleName() + " for discord channel " + event.getChannel().getName()));

        dfm.getAdapterRepo_dc().save(discord);
        dfm.getAdapterRepo_mq().save(rabbit);
        dfm.getFlowRepo().save(d2r);
        dfm.getFlowRepo().save(r2d);

        dfm.init(d2r);
        dfm.init(r2d);
    }

    @Command(permission = "16", ephemeral = true)
    @Description("See AMQP channel status")
    public void status() {/*todo*/}

    @Override
    public void handleResponse(Command.Delegate cmd, @NotNull Object response, Object... args) {
        final var e = Stream.of(args)
                .flatMap(Streams.cast(SlashCommandInteractionEvent.class))
                .findAny()
                .orElseThrow();
        final var user = Stream.of(args)
                .flatMap(Streams.cast(User.class))
                .findAny()
                .orElseThrow();
        if (response instanceof CompletableFuture)
            e.deferReply().setEphemeral(cmd.ephemeral())
                    .submit()
                    .thenCombine(((CompletableFuture<?>) response), (hook, resp) -> {
                        WebhookMessageCreateAction<net.dv8tion.jda.api.entities.Message> req;
                        if (resp instanceof EmbedBuilder)
                            req = hook.sendMessageEmbeds(embed((EmbedBuilder) resp, user).build());
                        else req = hook.sendMessage(String.valueOf(resp));
                        return req.submit();
                    })
                    .thenCompose(Function.identity())
                    .exceptionally(Polyfill.exceptionLogger());
        else {
            ReplyCallbackAction req;
            if (response instanceof EmbedBuilder)
                req = e.replyEmbeds(embed((EmbedBuilder) response, user).build());
            else req = e.reply(String.valueOf(response));
            req.setEphemeral(cmd.ephemeral()).submit();
        }
    }

    private EmbedBuilder embed(EmbedBuilder base, net.dv8tion.jda.api.entities.User user) {
        return base.setAuthor(user.getName(), "https://forwardmq.comroid.org", user.getAvatarUrl());
    }

    public Event.Listener<Message> listen(final long channelId, Event.Bus<DataNode> source) {
        return bus.flatMap(MessageReceivedEvent.class)
                .filterData(e -> e.getChannel().getIdLong() == channelId)
                .mapData(MessageReceivedEvent::getMessage)
                .mapData(msg -> Message.builder()
                        .content(msg.getContentStripped())
                        .author(Author.builder()
                                .name(msg.getAuthor().getName())
                                .effectiveName(msg.getAuthor().getEffectiveName())
                                .avatarUrl(msg.getAuthor().getAvatarUrl())
                                .color(Optional.ofNullable(msg.getMember())
                                        .map(Member::getColor)
                                        .orElse(null))
                                .build())
                        .build())
                .listen()
                .subscribeData(source::publish);
    }

    public enum LinkType implements Named {
        AurionChat
    }

    @Value
    @Builder
    public static class Message implements DataNode {
        String content;
        @Nullable Author author;
        @Singular List<EmbedBuilder> embeds;
        @Singular List<String> attachmentUrls;

        @Override
        public String toSerializedString() {
            return content + attachmentUrls.stream().collect(Collectors.joining(" ", " ", ""));
        }
    }

    @Value
    @Builder
    public static class Author implements DataNode {
        String name;
        String effectiveName;
        String avatarUrl;
        @Nullable Color color;

        @Override
        public String toSerializedString() {
            return effectiveName;
        }
    }
}
