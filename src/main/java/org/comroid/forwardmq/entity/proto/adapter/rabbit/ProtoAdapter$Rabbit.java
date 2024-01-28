package org.comroid.forwardmq.entity.proto.adapter.rabbit;

import jakarta.persistence.Basic;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.java.Log;
import lombok.extern.slf4j.Slf4j;
import org.comroid.annotations.Related;
import org.comroid.forwardmq.data.adapter.rabbit.DataAdapter$Rabbit;
import org.comroid.forwardmq.entity.proto.adapter.ProtoAdapter;
import org.jetbrains.annotations.Nullable;

import java.net.URI;

@Log
@Getter
@NoArgsConstructor
@AllArgsConstructor
@jakarta.persistence.Entity
@Related(DataAdapter$Rabbit.class)
public final class ProtoAdapter$Rabbit extends ProtoAdapter {
    private @Basic URI amqpUri;
    private String exchange;
    private @Nullable String exchangeType;
    private @Nullable String routingKey;
}
