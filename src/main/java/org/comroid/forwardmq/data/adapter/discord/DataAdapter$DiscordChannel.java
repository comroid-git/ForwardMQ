package org.comroid.forwardmq.data.adapter.discord;

import lombok.Value;
import lombok.extern.java.Log;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.comroid.api.data.seri.DataNode;
import org.comroid.api.data.seri.StandardValueType;
import org.comroid.api.func.util.Event;
import org.comroid.forwardmq.DiscordAdapter;
import org.comroid.forwardmq.data.adapter.DataAdapter;
import org.comroid.forwardmq.entity.proto.adapter.discord.ProtoAdapter$DiscordChannel;
import org.comroid.forwardmq.util.ApplicationContextProvider;

import static org.comroid.forwardmq.util.ApplicationContextProvider.bean;

@Log
@Value
public class DataAdapter$DiscordChannel extends DataAdapter<ProtoAdapter$DiscordChannel> {
    TextChannel channel;
    Event.Listener<DiscordAdapter.Message> listener;

    public DataAdapter$DiscordChannel(ProtoAdapter$DiscordChannel proto) {
        super(proto);

        var jda = bean(JDA.class);
        this.channel = jda.getTextChannelById(proto.getChannelId());
        this.listener = bean(DiscordAdapter.class).listen(proto.getChannelId(), source);
    }

    @Override
    public void accept(DataNode data) {
        String str;
        if (data instanceof DiscordAdapter.Message msg)
            str = msg.getContent();
        else str = data.toSerializedString();
        channel.sendMessage(str).queue();
    }
}
