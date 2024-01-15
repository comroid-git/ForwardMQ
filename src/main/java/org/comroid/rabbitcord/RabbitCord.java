package org.comroid.rabbitcord;

import jdk.jshell.JShell;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.Compression;
import org.comroid.api.data.seri.DataNode;
import org.comroid.api.data.seri.JSON;
import org.comroid.api.data.seri.Jackson;
import org.comroid.api.func.util.Event;
import org.comroid.api.io.FileHandle;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum RabbitCord {
    Instance;

    FileHandle DirBase;
    FileHandle DirChannels;
    FileHandle FileConfig;
    Config config;
    Event.Bus<GenericEvent> bus;
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
                    .addEventListeners((EventListener) bus::publish)
                    .build()
                    .awaitReady();
            channels = Arrays.stream(Objects.requireNonNull(DirChannels.listFiles()))
                    .map(FileHandle::new)
                    .<DiscordChannelConnection.Config>map(file -> Jackson.JSON.parse(file.getContent(true))
                            .asObject()
                            .convert(DiscordChannelConnection.Config.class))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toMap(DiscordChannelConnection.Config::getUuid, DiscordChannelConnection::new));
        } catch (Throwable t) {
            throw new RuntimeException("Unable to start application", t);
        }
    }

    public static void main(String[] args) {
        /* this method can be a stub because enums self-initialize. the future! */
    }

    @Value
    public static class Config implements DataNode {
        String token;
    }
}