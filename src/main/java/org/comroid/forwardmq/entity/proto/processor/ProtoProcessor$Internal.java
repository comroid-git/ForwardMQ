package org.comroid.forwardmq.entity.proto.processor;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.java.Log;
import org.comroid.annotations.Instance;
import org.comroid.forwardmq.DataFlowManager;
import org.comroid.forwardmq.entity.proto.Proto;
import org.comroid.forwardmq.model.IDataProcessor;

import java.util.Objects;
import java.util.Optional;

@Log
@Getter
@NoArgsConstructor
@AllArgsConstructor
@jakarta.persistence.Entity
public abstract class ProtoProcessor$Internal extends ProtoProcessor {
    @Instance
    public Optional<IDataProcessor> resolve(DataFlowManager dfm) {
        return dfm.getInternalProcessorCache().stream()
                .filter(proc -> Objects.equals(getName(), proc.getProto().getName()))
                .findAny();
    }

    public interface Repo<T extends ProtoProcessor$Internal> extends Proto.Repo<T> {}
}
