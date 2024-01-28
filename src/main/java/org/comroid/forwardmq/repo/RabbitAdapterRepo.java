package org.comroid.forwardmq.repo;

import org.comroid.forwardmq.entity.proto.adapter.ProtoAdapter;
import org.comroid.forwardmq.entity.proto.adapter.rabbit.ProtoAdapter$Rabbit;

public interface RabbitAdapterRepo extends ProtoAdapter.Repo<ProtoAdapter$Rabbit> {
}
