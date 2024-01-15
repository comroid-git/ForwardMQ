package org.comroid.forwardmq;

import lombok.SneakyThrows;
import lombok.Value;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.Compression;
import org.comroid.api.func.util.Event;
import org.comroid.forwardmq.dto.Config;

@Value
public class DiscordAdapter extends Event.Bus<GenericEvent> {
    Config config;
    JDA jda;

    @SneakyThrows
    public DiscordAdapter(Config config) {
        this.config = config;
        this.jda = JDABuilder.create(config.getDiscordToken(), GatewayIntent.getIntents(GatewayIntent.ALL_INTENTS))
                .setCompression(Compression.ZLIB)
                .addEventListeners((EventListener) this::publish)
                .build()
                .awaitReady();
    }
}
