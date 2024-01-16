package org.comroid.forwardmq.entity.proto.adapter;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.comroid.forwardmq.entity.Entity;

@Data
@Slf4j
@NoArgsConstructor
@AllArgsConstructor
@jakarta.persistence.Entity
public abstract class ProtoAdapter extends Entity {
}
