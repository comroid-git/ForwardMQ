package org.comroid.rabbitcord;

import emoji4j.EmojiUtils;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
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
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.TextColor;
import org.comroid.annotations.Alias;
import org.comroid.api.Polyfill;
import org.comroid.api.data.seri.DataNode;
import org.comroid.api.data.seri.adp.Jackson;
import org.comroid.api.func.util.Command;
import org.comroid.api.func.util.Event;
import org.comroid.api.func.util.Streams;
import org.comroid.api.info.Log;
import org.comroid.api.io.FileHandle;
import org.comroid.api.text.Markdown;
import org.comroid.api.text.TextDecoration;
import org.comroid.api.tree.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static net.kyori.adventure.text.Component.text;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum RabbitCord {
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

            jda = JDABuilder.createLight(Objects.requireNonNull(config).token,
                            GatewayIntent.getIntents(GatewayIntent.ALL_INTENTS))
                    .setCompression(Compression.ZLIB)
                    .addEventListeners((EventListener) bus::accept)
                    .build()
                    .awaitReady();
            jda.updateCommands().addCommands(
                    Commands.slash("amqp-link", "Link this channel to the specified AMQP Exchange")
                            .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_CHANNEL))
                            .addOption(OptionType.STRING, "amqp-uri", "The AMQP URL to connect to", true)
                            .addOption(OptionType.STRING, "exchange-name", "The AMQP exchange name to connect to", true),
                    Commands.slash("amqp", "See AMQP channel status")
                            .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_CHANNEL)),
                    Commands.slash("amqp-unlink", "Removes AMQP link from this channel")
                            .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_CHANNEL))
            ).queue();

            cmdr = new Command.Manager();
            cmdr.new Adapter$JDA(jda);
            cmdr.register(this);
            cmdr.initialize();

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
                //Log.at(Level.INFO, "Author ID: %d; Message: %s".formatted(author.getIdLong(), event.getMessage().getContentRaw()));
                if (author.isBot() && (author.getIdLong() == 1207401249922617445L || author.getIdLong() == 1239680369217765466L))
                    return;

                var channelId = new UUID(event.getGuild().getIdLong(), event.getChannel().getIdLong());
                if (!channels.containsKey(channelId))
                    return;

                var message = event.getMessage();
                var str = message.getContentStripped()
                        + message.getEmbeds().stream()
                        .map(MessageEmbed::getDescription)
                        .map(desc -> TextDecoration.sanitize(desc, Markdown.class))
                        .filter(Objects::nonNull)
                        .map(desc -> desc.replaceAll("\\[(.+)]\\(.+\\)", "$1"))
                        .collect(Collectors.joining(" ", " ", ""))
                        .trim()
                        + message.getAttachments().stream()
                        .map(Message.Attachment::getUrl)
                        .collect(Collectors.joining(" ", " ", ""))
                        .trim();
                //str = TextDecoration.sanitize(str, Markdown.class);

                var channel = channels.get(channelId);
                var inviteUrl = channel.getConfig().getInviteUrl();
                var comp = text();
                var discord = text(/*'#' + event.getChannel().getName() + " : */"DISCORD ", TextColor.color(86, 98, 246));
                if (inviteUrl != null)
                    discord = discord.clickEvent(ClickEvent.openUrl(inviteUrl));
                comp.append(discord)
                        .append(text(EmojiUtils.removeAllEmojis(author.getEffectiveName()).trim(),
                                Optional.ofNullable(message.getMember())
                                        .map(Member::getColorRaw)
                                        .map(TextColor::color)
                                        .orElse(TextColor.color(0xffffff))))
                        .append(text(": " + str, TextColor.color(0xFF_FF_FF))
                                .clickEvent(ClickEvent.openUrl(message.getJumpUrl())));
                channel.sendToRabbit(comp.build());
            });
        } catch (Throwable t) {
            throw new RuntimeException("Unable to start application", t);
        }
    }

    @Command(ephemeral = true)
    public Object amqp(Guild guild, MessageChannelUnion channel) {
        var id = new UUID(guild.getIdLong(), channel.getIdLong());
        if (!channels.containsKey(id))
            return "This channel is not linked";
        var conn = channels.get(id);
        var config = conn.getConfig();
        var uri = Polyfill.uri(config.getAmqpUri());
        return new EmbedBuilder()
                .setDescription("This channel is linked")
                .addField("RabbitMQ Host", uri.getHost(), true)
                .addField("Exchange", config.getExchange(), true)
                .addField("Status", conn.getChannel().isOpen() ? "Healthy" : "Troubled", true);
    }

    @Command(value = "amqp-link", ephemeral = true)
    public String amqpLink(
            @Command.Arg String amqpUri,
            @Command.Arg String exchangeName,
            @Command.Arg String channelName,
            @Command.Arg @Nullable String inviteUrl,
            SlashCommandInteractionEvent event,
            Guild guild,
            MessageChannelUnion channel
    ) {
        var uri = event.getOption("amqp-uri").getAsString();
        var exchange = event.getOption("exchange-name").getAsString();
        var config = new DiscordChannelConnection.Config(guild.getIdLong(), channel.getIdLong(), guild
                .retrieveVanityInvite()
                .map(Polyfill::<Invite>uncheckedCast)
                .map(List::of)
                .submit()
                .exceptionallyCompose(t -> guild.retrieveInvites().submit())
                .join()
                .stream()
                .findAny()
                .map(Invite::getUrl)
                .orElse(null),
                uri, exchange, channelName);
        var id = config.getUuid();

        var conn = new DiscordChannelConnection(config);
        conn.initialize();
        channels.put(id, conn);
        DirChannels.createSubFile(id + ".json").setContent(config.json());
        return "Channel linked";
    }

    @Command(value = "amqp-unlink", ephemeral = true)
    public String amqpUnlink(Guild guild, MessageChannelUnion channel) {
        var id = new UUID(guild.getIdLong(), channel.getIdLong());
        if (!channels.containsKey(id))
            return "This channel is not linked";
        var old = channels.remove(id);
        if (!DirChannels.createSubFile(id + ".json").delete())
            return "Link could not be removed";
        old.terminate();
        return "Channel was unlinked";
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