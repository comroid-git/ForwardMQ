package org.comroid.forwardmq.entity.proto.data;

import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.comroid.forwardmq.entity.Entity;
import org.comroid.forwardmq.entity.data.DataScheme;
import org.comroid.forwardmq.entity.proto.adapter.ProtoAdapter;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;

@Data
@Slf4j
@NoArgsConstructor
@AllArgsConstructor
@jakarta.persistence.Entity
public class ProtoDataFlow extends Entity {
    private @NotNull @ManyToOne ProtoAdapter input;
    private @NotNull @ManyToOne DataScheme inputScheme;
    private @NotNull @Language("JavaScript") String processor;
    private @NotNull @ManyToOne ProtoAdapter output;
}
