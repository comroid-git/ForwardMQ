package org.comroid.forwardmq;

import lombok.SneakyThrows;
import lombok.Value;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageCreateAction;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.dv8tion.jda.api.utils.Compression;
import org.comroid.api.Polyfill;
import org.comroid.api.data.seri.DataNode;
import org.comroid.api.func.util.Command;
import org.comroid.api.func.util.Event;
import org.comroid.api.func.util.Streams;
import org.comroid.forwardmq.dto.Config;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
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
        this.cmdr = new Command.Manager(this);
        this.config = config;
        this.jda = JDABuilder.create(config.getDiscordToken(), GatewayIntent.getIntents(GatewayIntent.ALL_INTENTS))
                .setCompression(Compression.ZLIB)
                .addEventListeners((EventListener) bus::publish)
                .build()
                .awaitReady();

        bus.flatMap(SlashCommandInteractionEvent.class).listen()
                .subscribeData(event -> cmdr.execute(event.getName(), event, event.getUser(), event.getGuild(), event.getChannel()));
        jda.updateCommands().addCommands(
                Commands.slash("link", "Link this channel to the specified AMQP Exchange")
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_CHANNEL))
                        .addOption(OptionType.STRING, "amqp-uri", "The AMQP URL to connect to", true)
                        .addOption(OptionType.STRING, "exchange-name", "The AMQP exchange name to connect to", true)
                        .addOption(OptionType.STRING, "data-scheme", "The data scheme to refer to", true, true),
                Commands.slash("status", "See AMQP channel status")
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_CHANNEL))
        ).queue();
    }

    @Command
    public void link() {/*todo*/}
    @Command
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
                        WebhookMessageCreateAction<Message> req;
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

    private EmbedBuilder embed(EmbedBuilder base, User user) {
        return base.setAuthor(user.getName(), "https://forwardmq.comroid.org", user.getAvatarUrl());
    }

    public Event.Listener<DataNode> listen(final long channelId, Event.Bus<DataNode> source) {
        return bus.flatMap(MessageReceivedEvent.class)
                .filterData(e -> e.getChannel().getIdLong() == channelId)
                .mapData(MessageReceivedEvent::getMessage)
                .mapData(DataNode::of)
                .listen()
                .subscribeData(source);
    }
}
