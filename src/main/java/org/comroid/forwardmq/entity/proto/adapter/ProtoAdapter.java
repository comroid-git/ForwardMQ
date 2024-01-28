package org.comroid.forwardmq.entity.proto.adapter;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.java.Log;
import lombok.extern.slf4j.Slf4j;
import org.comroid.forwardmq.entity.DataFlow;
import org.comroid.forwardmq.entity.Entity;
import org.comroid.forwardmq.entity.proto.Proto;

@Log
@Getter
@NoArgsConstructor
//@AllArgsConstructor
@jakarta.persistence.Entity
public abstract class ProtoAdapter extends Proto {
    public interface Repo<T extends ProtoAdapter> extends Proto.Repo<T> {}
}
