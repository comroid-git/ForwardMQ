package org.comroid.forwardmq.entity.proto.processor;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.java.Log;
import org.comroid.forwardmq.entity.proto.Proto;

@Log
@Getter
@NoArgsConstructor
@AllArgsConstructor
@jakarta.persistence.Entity
public abstract class ProtoProcessor extends Proto {
    public interface Repo<T extends ProtoProcessor> extends Proto.Repo<T> {}
}
