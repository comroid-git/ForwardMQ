package org.comroid.forwardmq.entity.proto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.java.Log;
import org.comroid.forwardmq.entity.Entity;

@Log
@Getter
@NoArgsConstructor
//@AllArgsConstructor
@jakarta.persistence.Entity
public abstract class Proto extends Entity {
    public interface Repo<T extends Proto> extends Entity.Repo<T> {}
}
