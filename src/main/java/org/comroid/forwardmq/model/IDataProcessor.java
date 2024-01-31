package org.comroid.forwardmq.model;

import org.comroid.api.data.seri.DataNode;
import org.comroid.forwardmq.entity.proto.processor.ProtoProcessor$Internal;

import java.util.function.UnaryOperator;

public interface IDataProcessor extends UnaryOperator<DataNode> {
    ProtoProcessor$Internal getProto();
}
