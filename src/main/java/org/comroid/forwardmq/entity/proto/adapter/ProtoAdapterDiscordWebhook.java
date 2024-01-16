package org.comroid.forwardmq.entity.proto.adapter;

import jakarta.persistence.Basic;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.net.URL;

@Data
@Slf4j
@NoArgsConstructor
@AllArgsConstructor
@jakarta.persistence.Entity
public final class ProtoAdapterDiscordWebhook extends ProtoAdapter {
    private @Basic URL webhookUrl;
}
