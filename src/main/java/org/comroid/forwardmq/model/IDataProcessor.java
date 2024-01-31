package org.comroid.forwardmq.model;

import org.comroid.api.data.seri.DataNode;
import org.comroid.forwardmq.entity.proto.processor.ProtoProcessor;

import java.util.function.UnaryOperator;

public interface IDataProcessor<Proto extends ProtoProcessor> extends UnaryOperator<DataNode> {
    Proto getProto();
}
