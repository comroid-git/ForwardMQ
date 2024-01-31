package org.comroid.forwardmq.model;

import lombok.Builder;
import lombok.Data;
import lombok.Value;
import org.comroid.api.data.seri.DataNode;

public interface AurionChat {
    @Value
    @Builder
    class Message implements DataNode {
        String type;
        String source;
        String channel;
        String message;
    }
}
