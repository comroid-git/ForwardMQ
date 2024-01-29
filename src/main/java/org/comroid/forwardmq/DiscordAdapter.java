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
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.interactions.commands.Command.Choice;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageCreateAction;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.dv8tion.jda.api.utils.Compression;
import org.comroid.api.Polyfill;
import org.comroid.api.attr.IntegerAttribute;
import org.comroid.api.attr.Named;
import org.comroid.api.data.seri.DataNode;
import org.comroid.api.func.util.Command;
import org.comroid.api.func.util.Event;
import org.comroid.api.func.util.Streams;
import org.comroid.forwardmq.dto.Config;
import org.comroid.forwardmq.entity.DataFlow;
import org.comroid.forwardmq.entity.proto.adapter.discord.ProtoAdapter$DiscordChannel;
import org.comroid.forwardmq.entity.proto.adapter.rabbit.ProtoAdapter$Rabbit;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.List;
import java.util.UUID;
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
        this.config = config;
        this.jda = JDABuilder.create(config.getDiscordToken(), GatewayIntent.getIntents(GatewayIntent.ALL_INTENTS))
                .setCompression(Compression.ZLIB)
                .addEventListeners((EventListener) bus::publish)
                .build()
                .awaitReady();
        var adp = new Command.Ada
        this.cmdr = new Command.Manager(jda, this);

        bus.flatMap(SlashCommandInteractionEvent.class).listen()
                .subscribeData(event -> cmdr.execute(event.getName(), event, event.getUser(), event.getGuild(), event.getChannel()));
        bus.flatMap(CommandAutoCompleteInteractionEvent.class).listen()
                .subscribeData(event -> {
                    var option = event.getFocusedOption();
                    event.replyChoices(cmdr.autoComplete(event.getName(), option.getName(), option.getValue())
                                    .map(e -> new Choice(e.getKey(), e.getValue()))
                                    .toList())
                            .queue();
                });
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
    @SneakyThrows
    public void link(SlashCommandInteractionEvent event, @Command.Arg String amqpUri, @Command.Arg String exchangeName, @Command.Arg LinkType type) {
        final var dfm = bean(DataFlowManager.class);
        var amqpUri = new URI(event.getOption("amqp-uri").getAsString());
        var exchangeName = event.getOption("exchange-name").getAsString();

        var ac2dc = dfm.getProcessorRepo_js().findById(UUID.fromString(""/*todo*/)).orElseThrow(); // rectifier
        var dc2ac = dfm.getProcessorRepo_js().findById(UUID.fromString(""/*todo*/)).orElseThrow(); // inverter
        var discord = new ProtoAdapter$DiscordChannel(event.getChannelIdLong());
        var rabbit = new ProtoAdapter$Rabbit(amqpUri, exchangeName, null, null);
        var d2r = new DataFlow(discord, rabbit, List.of(dc2ac));
        var r2d = new DataFlow(rabbit, discord, List.of(ac2dc));

        dfm.getAdapterRepo_dc().save(discord);
        dfm.getAdapterRepo_mq().save(rabbit);
        dfm.getFlowRepo().save(d2r);
        dfm.getFlowRepo().save(r2d);

        dfm.init(d2r);
        dfm.init(r2d);
    }
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

    public enum LinkType implements Named, IntegerAttribute {
        AurionChat
    }
}
