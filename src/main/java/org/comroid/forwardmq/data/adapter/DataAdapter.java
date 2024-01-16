package org.comroid.forwardmq.data.adapter;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.comroid.api.data.seri.DataNode;
import org.comroid.api.func.util.Event;
import org.comroid.forwardmq.entity.proto.adapter.ProtoAdapter;

@Data
@AllArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PROTECTED)
public abstract class DataAdapter<Proto extends ProtoAdapter, Data extends DataNode> extends Event.Bus<Data> {
    Proto proto;

    @Event.Subscriber("output")
    public abstract void handleOutput(DataNode node);
}
