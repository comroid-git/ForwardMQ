package org.comroid.forwardmq.data.adapter.discord;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.send.WebhookMessage;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import lombok.Value;
import lombok.extern.java.Log;
import org.comroid.api.data.seri.DataNode;
import org.comroid.forwardmq.DiscordAdapter;
import org.comroid.forwardmq.data.adapter.DataAdapter;
import org.comroid.forwardmq.entity.proto.adapter.discord.ProtoAdapter$DiscordWebhook;

@Log
@Value
public class DataAdapter$DiscordWebhook extends DataAdapter<ProtoAdapter$DiscordWebhook> {
    WebhookClient webhook;

    public DataAdapter$DiscordWebhook(ProtoAdapter$DiscordWebhook proto) {
        super(proto);

        this.webhook = WebhookClient.withUrl(proto.getWebhookUrl().toExternalForm());
    }

    @Override
    public void accept(DataNode data) {
        WebhookMessage send;
        if (data instanceof DiscordAdapter.Message msg) {
            var builder = new WebhookMessageBuilder()
                    .setContent(msg.getContent());
            var author = msg.getAuthor();
            if (author != null) {
                builder.setUsername(author.getEffectiveName());
                builder.setAvatarUrl(author.getAvatarUrl());
            }
            send = builder.build();
        } else send = new WebhookMessageBuilder()
                .setContent(data.toSerializedString())
                .build();
        webhook.send(send);
    }

    @Override
    public void closeSelf() throws Exception {
        webhook.close();
    }
}
