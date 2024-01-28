package org.comroid.forwardmq.data.adapter.rabbit;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.experimental.NonFinal;
import lombok.extern.java.Log;
import org.comroid.api.data.seri.DataNode;
import org.comroid.forwardmq.data.adapter.DataAdapter;
import org.comroid.forwardmq.entity.proto.adapter.rabbit.ProtoAdapter$Rabbit;

import java.util.Objects;

@Log
@Value
public class DataAdapter$Rabbit extends DataAdapter<ProtoAdapter$Rabbit> {
    Connection connection;
    @NonFinal Channel channel;

    @SneakyThrows
    public DataAdapter$Rabbit(ProtoAdapter$Rabbit proto) {
        super(proto);

        var factory = new ConnectionFactory();
        factory.setUri(proto.getAmqpUri());
        this.connection = factory.newConnection();
        poll();
    }

    @Override
    @SneakyThrows
    public void accept(DataNode data) {
        channel.basicPublish(proto.getExchange(), proto.getRoutingKey(), null, data.toSerializedString().getBytes());
    }

    @Override
    public void closeSelf() throws Exception {
        channel.close();
        connection.close();
    }

    @SneakyThrows
    private void poll() {
        if (channel != null) {
            if (channel.isOpen())
                return;
            else channel.close();
        }

        channel = connection.createChannel();
        channel.exchangeDeclare(proto.getExchange(), Objects.requireNonNullElse(proto.getExchangeType(), "fanout"));

        var queue = channel.queueDeclare().getQueue();
        channel.queueBind(queue, proto.getExchange(), Objects.requireNonNullElse(proto.getRoutingKey(), ""));

        channel.basicConsume(queue, true, (consumerTag, message) -> source.publish(new DataNode.Value<>(new String(message.getBody()))), consumerTag -> {});
    }
}
