package org.comroid.forwardmq.entity.proto.processor;

import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.java.Log;
import org.comroid.annotations.Instance;
import org.comroid.forwardmq.DataFlowManager;
import org.comroid.forwardmq.entity.proto.Proto;
import org.comroid.forwardmq.model.IDataProcessor;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Log
@Getter
@NoArgsConstructor
//@AllArgsConstructor
@jakarta.persistence.Entity
public class ProtoProcessor$Internal extends ProtoProcessor {
    public ProtoProcessor$Internal(UUID id, String name, String displayName) {
        setId(id);
        setName(name);
        setDisplayName(displayName);
    }

    @Instance
    public Optional<IDataProcessor<ProtoProcessor$Internal>> resolve(DataFlowManager dfm) {
        return dfm.getInternalProcessor(getName());
    }

    public interface Repo<T extends ProtoProcessor$Internal> extends Proto.Repo<T> {}
}
