package org.comroid.forwardmq.entity.data;

import jakarta.persistence.Basic;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
@jakarta.persistence.Entity
public abstract class DataScheme extends DataHandler {
    private @Basic Class<?> implType;
}
