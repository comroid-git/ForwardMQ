package org.comroid.forwardmq.entity;

import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.java.Log;
import lombok.extern.slf4j.Slf4j;
import org.comroid.forwardmq.entity.proto.adapter.ProtoAdapter;
import org.comroid.forwardmq.entity.proto.processor.ProtoProcessor;

import java.util.List;

@Log
@Getter
@NoArgsConstructor
@AllArgsConstructor
@jakarta.persistence.Entity
public class DataFlow extends Entity {
    @ManyToOne ProtoAdapter source;
    @ManyToOne ProtoAdapter destination;
    @ManyToMany List<ProtoProcessor> processors;

    public interface Repo extends Entity.Repo<DataFlow> {}
}
