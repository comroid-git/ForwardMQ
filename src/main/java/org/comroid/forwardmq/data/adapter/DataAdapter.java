package org.comroid.forwardmq.data.adapter;

import lombok.Value;
import lombok.experimental.NonFinal;
import org.comroid.api.data.seri.DataNode;
import org.comroid.api.func.util.Event;
import org.comroid.forwardmq.data.ProtoImplementation;
import org.comroid.forwardmq.entity.proto.adapter.ProtoAdapter;

import java.util.function.Consumer;

@Value
@NonFinal
public abstract class DataAdapter<Proto extends ProtoAdapter>
        extends ProtoImplementation<Proto>
        implements Consumer<DataNode> {
    protected Event.Bus<DataNode> source = new Event.Bus<>();

    public DataAdapter(Proto proto) {
        super(proto);
    }
}
