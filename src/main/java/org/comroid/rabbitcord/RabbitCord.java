package org.comroid.rabbitcord;

import emoji4j.Emoji;
import emoji4j.EmojiUtils;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
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
import net.dv8tion.jda.internal.entities.emoji.UnicodeEmojiImpl;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import org.comroid.annotations.Alias;
import org.comroid.api.Polyfill;
import org.comroid.api.data.seri.DataNode;
import org.comroid.api.data.seri.Jackson;
import org.comroid.api.func.util.Command;
import org.comroid.api.func.util.Event;
import org.comroid.api.func.util.Streams;
import org.comroid.api.io.FileHandle;
import org.comroid.api.text.Markdown;
import org.comroid.api.text.TextDecoration;
import org.comroid.api.tree.Component;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.kyori.adventure.text.Component.text;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum RabbitCord implements Command.Handler {
    Instance;

    FileHandle DirBase;
    FileHandle DirChannels;
    FileHandle FileConfig;
    Config config;
    Event.Bus<GenericEvent> bus;
    Command.Manager cmdr;
    JDA jda;
    Map<UUID, DiscordChannelConnection> channels;

    {
        try {
            DirBase = new FileHandle("/srv/dcb/rabbitcord", true);
            DirChannels = DirBase.createSubDir("channels");
            FileConfig = DirBase.createSubFile("config.json");
            config = Jackson.JSON.parse(FileConfig.getContent(true))
                    .asObject()
                    .convert(Config.class);
            bus = new Event.Bus<>();
            cmdr = new Command.Manager(this);

            jda = JDABuilder.createLight(Objects.requireNonNull(config).token,
                            GatewayIntent.getIntents(GatewayIntent.ALL_INTENTS))
                    .setCompression(Compression.ZLIB)
                    .addEventListeners((EventListener) bus::accept)
                    .build()
                    .awaitReady();
            jda.updateCommands().addCommands(
                    Commands.slash("link", "Link this channel to the specified AMQP Exchange")
                            .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_CHANNEL))
                            .addOption(OptionType.STRING, "amqp-uri", "The AMQP URL to connect to", true)
                            .addOption(OptionType.STRING, "exchange-name", "The AMQP exchange name to connect to", true),
                    Commands.slash("status", "See AMQP channel status")
                            .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_CHANNEL))
            ).queue();

            channels = Arrays.stream(Objects.requireNonNull(DirChannels.listFiles()))
                    .map(FileHandle::new)
                    .<DiscordChannelConnection.Config>map(file -> Jackson.JSON.parse(file.getContent(true))
                            .asObject()
                            .convert(DiscordChannelConnection.Config.class))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toMap(DiscordChannelConnection.Config::getUuid, DiscordChannelConnection::new));
            channels.values().forEach(Component.Base::initialize);

            bus.flatMap(SlashCommandInteractionEvent.class).listen()
                    .subscribeData(event -> cmdr.execute(event.getName(), event, event.getUser(), event.getGuild(), event.getChannel()));
            bus.flatMap(MessageReceivedEvent.class).listen().subscribeData(event -> {
                var author = event.getAuthor();
                if (author.isBot())
                    return;

                var channelId = new UUID(event.getGuild().getIdLong(), event.getChannel().getIdLong());
                if (!channels.containsKey(channelId))
                    return;

                var message = event.getMessage();
                var str = message.getContentStripped() + message.getAttachments().stream()
                        .map(Message.Attachment::getUrl)
                        .collect(Collectors.joining(" "," ",""));
                //str = TextDecoration.sanitize(str, Markdown.class);

                var comp = text("DISCORD ", TextColor.color(86, 98, 246))
                        .append(text(EmojiUtils.removeAllEmojis(author.getEffectiveName()), TextColor.color(Objects.requireNonNull(message.getMember()).getColorRaw())))
                        .append(text(": " + str, TextColor.color(0xFF_FF_FF)));
                channels.get(channelId).sendToRabbit(comp);
            });
        } catch (Throwable t) {
            throw new RuntimeException("Unable to start application", t);
        }
    }

    @Command(ephemeral = true)
    public String link(SlashCommandInteractionEvent event, Guild guild, MessageChannelUnion channel) {
        var uri = event.getOption("amqp-uri").getAsString();
        var exchange = event.getOption("exchange-name").getAsString();
        var config = new DiscordChannelConnection.Config(guild.getIdLong(), channel.getIdLong(), uri, exchange);
        var id = config.getUuid();

        DirChannels.createSubFile(id +".json").setContent(config.json());
        var conn = new DiscordChannelConnection(config);
        channels.put(id, conn);
        conn.initialize();
        return "Channel linked";
    }

    @Command(ephemeral = true)
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

    public static void main(String[] args) {
        /* this method can be a stub because enums self-initialize. the future! */
    }

    @Value
    public static class Config implements DataNode {
        String token;

        public Config(@Alias("token") String token) {
            this.token = token;
        }
    }
}