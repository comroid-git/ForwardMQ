package org.comroid.forwardmq.entity.proto.adapter;

import jakarta.persistence.Basic;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
@NoArgsConstructor
@AllArgsConstructor
@jakarta.persistence.Entity
public final class ProtoAdapterDiscordChannel extends ProtoAdapter {
    private @Basic long guildId;
    private long channelId;
}
