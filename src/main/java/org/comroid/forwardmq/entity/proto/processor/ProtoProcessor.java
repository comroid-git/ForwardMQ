package org.comroid.forwardmq.entity.proto.processor;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.java.Log;
import lombok.extern.slf4j.Slf4j;
import org.comroid.forwardmq.entity.Entity;
import org.comroid.forwardmq.entity.proto.Proto;
import org.comroid.forwardmq.entity.proto.adapter.ProtoAdapter;

@Log
@Getter
@NoArgsConstructor
//@AllArgsConstructor
@jakarta.persistence.Entity
public abstract class ProtoProcessor extends Proto {
    public interface Repo<T extends ProtoProcessor> extends Proto.Repo<T> {}
}
