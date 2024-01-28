package org.comroid.forwardmq.entity.proto.adapter.discord;

import jakarta.persistence.Basic;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.java.Log;
import lombok.extern.slf4j.Slf4j;
import org.comroid.annotations.Related;
import org.comroid.forwardmq.data.adapter.discord.DataAdapter$DiscordWebhook;
import org.comroid.forwardmq.entity.proto.adapter.ProtoAdapter;

import java.net.URL;

@Log
@Getter
@NoArgsConstructor
@AllArgsConstructor
@jakarta.persistence.Entity
@Related(DataAdapter$DiscordWebhook.class)
public final class ProtoAdapter$DiscordWebhook extends ProtoAdapter {
    private @Basic URL webhookUrl;
    public interface Repo extends ProtoAdapter.Repo<ProtoAdapter$DiscordWebhook> {}
}
