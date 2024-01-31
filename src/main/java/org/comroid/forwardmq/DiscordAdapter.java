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
import org.comroid.annotations.Description;
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
import org.jetbrains.annotations.Nullable;

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
        var ac2dc = dfm.getProcessorRepo_js().findById(UUID.fromString(""/*todo*/)).orElseThrow(); // rectifier
        var dc2ac = dfm.getProcessorRepo_js().findById(UUID.fromString(""/*todo*/)).orElseThrow(); // inverter
        var discord = new ProtoAdapter$DiscordChannel(event.getChannelIdLong());
        var rabbit = new ProtoAdapter$Rabbit(new URI(amqpUri), exchangeName, null, null);
        var d2r = new DataFlow(discord, rabbit, List.of(dc2ac));
        var r2d = new DataFlow(rabbit, discord, List.of(ac2dc));

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

    public enum LinkType implements Named {
        AurionChat
    }
}
