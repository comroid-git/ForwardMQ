package org.comroid.forwardmq.data.processor.internal;

import lombok.Value;
import org.comroid.api.data.seri.DataNode;
import org.comroid.api.info.Constraint;
import org.comroid.forwardmq.DiscordAdapter;
import org.comroid.forwardmq.entity.proto.processor.ProtoProcessor$Internal;
import org.comroid.forwardmq.model.AurionChat;
import org.comroid.forwardmq.model.IDataProcessor;
import org.comroid.forwardmq.util.Util;

import java.util.UUID;

import static net.kyori.adventure.text.serializer.gson.GsonComponentSerializer.gson;

@Value
public class DataProcessor$Internal$Aurion2Discord implements IDataProcessor<ProtoProcessor$Internal> {
    public static final UUID ID = UUID.fromString("a593c1f6-7073-4f72-b360-c767d284ba12");
    public static final ProtoProcessor$Internal PROTO = new ProtoProcessor$Internal(ID,
            "aurion2discord",
            "AurionChat to Discord message converter");
    ProtoProcessor$Internal proto = PROTO;

    @Override
    public DataNode apply(DataNode aurionMsg) {
        var event = aurionMsg.asObject().convert(AurionChat.Message.class);
        Constraint.notNull(event, "event").run();

        var component = gson().deserialize(event.getMessage());

        return DiscordAdapter.Message.builder()
                .content(Util.componentString(component)
                        .replaceAll("[§&]\\w", ""))
                .build();
    }
}
