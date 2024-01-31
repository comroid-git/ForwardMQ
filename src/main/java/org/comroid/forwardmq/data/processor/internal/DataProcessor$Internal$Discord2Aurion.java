package org.comroid.forwardmq.data.processor.internal;

import emoji4j.EmojiUtils;
import lombok.Value;
import net.kyori.adventure.text.format.TextColor;
import org.comroid.api.data.seri.DataNode;
import org.comroid.api.info.Constraint;
import org.comroid.forwardmq.DiscordAdapter;
import org.comroid.forwardmq.entity.proto.processor.ProtoProcessor$Internal;
import org.comroid.forwardmq.model.AurionChat;
import org.comroid.forwardmq.model.IDataProcessor;

import java.util.UUID;
import java.util.stream.Collectors;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.serializer.gson.GsonComponentSerializer.gson;

@Value
public class DataProcessor$Internal$Discord2Aurion implements IDataProcessor<ProtoProcessor$Internal> {
    public static final UUID ID = UUID.fromString("a16b03f3-807e-4c06-8571-b3dc18db8ac3");
    public static final ProtoProcessor$Internal PROTO = new ProtoProcessor$Internal(ID,
            "discord2aurion",
            "Discord to AurionChat message converter");
    ProtoProcessor$Internal proto = PROTO;

    @Override
    public DataNode apply(DataNode dcMsg) {
        var message = dcMsg.asObject().convert(DiscordAdapter.Message.class);
        Constraint.notNull(message, "message").run();
        var author = message.getAuthor();
        Constraint.notNull(author, "message.author").run();

        var str = message.getContent() + message.getAttachmentUrls().stream()
                .collect(Collectors.joining(" ", " ", ""))
                .trim();
        var base = text("DISCORD ", TextColor.color(86, 98, 246));
        //base.clickEvent(ClickEvent.openUrl()); todo: add meta config for discord invite url
        var color = author.getColor();
        var component = base
                .append(text(EmojiUtils.removeAllEmojis(author.getEffectiveName()).trim(),
                        TextColor.color(color != null ? color.getRGB() : 0xFF_FF_FF)))
                .append(text(": " + str, TextColor.color(0xFF_FF_FF)));

        return AurionChat.Message.builder()
                .type("chat")
                .source("discord")
                .channel("global")
                .message(gson().serializeToTree(component).toString())
                .build();
    }
}
