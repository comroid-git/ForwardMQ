package org.comroid.forwardmq.data;

import lombok.Value;
import lombok.experimental.NonFinal;
import org.comroid.api.tree.Component;
import org.comroid.forwardmq.entity.Entity;

@Value
@NonFinal
public abstract class ProtoImplementation<Proto extends Entity> extends Component.Base {
    protected Proto proto;
}
