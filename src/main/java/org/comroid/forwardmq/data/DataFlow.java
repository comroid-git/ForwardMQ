package org.comroid.forwardmq.data;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.comroid.forwardmq.entity.proto.data.ProtoDataFlow;

@Data
@AllArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PROTECTED)
public class DataFlow {
    ProtoDataFlow proto;
}
