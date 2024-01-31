package org.comroid.forwardmq.model;

import lombok.Builder;
import lombok.Value;
import org.comroid.api.data.seri.DataNode;

import static net.kyori.adventure.text.serializer.gson.GsonComponentSerializer.gson;
import static org.comroid.forwardmq.util.Util.componentString;

public interface AurionChat {
    @Value
    @Builder
    class Message implements DataNode {
        String type;
        String source;
        String channel;
        String message;

        @Override
        public String toSerializedString() {
            return componentString(gson().deserialize(message));
        }
    }
}
