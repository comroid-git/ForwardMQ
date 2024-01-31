package org.comroid.forwardmq.data.processor;

import org.comroid.api.data.seri.DataNode;
import org.comroid.forwardmq.data.ProtoImplementation;
import org.comroid.forwardmq.entity.proto.processor.ProtoProcessor;
import org.comroid.forwardmq.model.IDataProcessor;

import java.util.function.UnaryOperator;

public abstract class DataProcessor<Proto extends ProtoProcessor>
        extends ProtoImplementation<Proto>
        implements IDataProcessor<Proto> {
    public DataProcessor(Proto proto) {
        super(proto);
    }
}
