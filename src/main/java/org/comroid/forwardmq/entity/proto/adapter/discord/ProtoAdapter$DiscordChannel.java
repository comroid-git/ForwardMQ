package org.comroid.forwardmq.entity.proto.adapter.discord;

import jakarta.persistence.Basic;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.java.Log;
import lombok.extern.slf4j.Slf4j;
import org.comroid.annotations.Related;
import org.comroid.forwardmq.data.adapter.discord.DataAdapter$DiscordChannel;
import org.comroid.forwardmq.entity.proto.adapter.ProtoAdapter;

@Log
@Getter
@NoArgsConstructor
@AllArgsConstructor
@jakarta.persistence.Entity
@Related(DataAdapter$DiscordChannel.class)
public final class ProtoAdapter$DiscordChannel extends ProtoAdapter {
    private @Basic long channelId;
}
