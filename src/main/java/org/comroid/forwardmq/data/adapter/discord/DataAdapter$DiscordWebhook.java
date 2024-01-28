package org.comroid.forwardmq.data.adapter.discord;

import club.minnced.discord.webhook.WebhookClient;
import lombok.Value;
import lombok.extern.java.Log;
import org.comroid.api.data.seri.DataNode;
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
        webhook.send(data.toSerializedString());
    }

    @Override
    public void closeSelf() throws Exception {
        webhook.close();
    }
}
