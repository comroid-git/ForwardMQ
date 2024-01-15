package org.comroid.forwardmq.entity.data;

import jakarta.persistence.Basic;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.comroid.api.attr.BitmaskAttribute;
import org.comroid.forwardmq.entity.Entity;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;

@Data
@Slf4j
@NoArgsConstructor
@AllArgsConstructor
@jakarta.persistence.Entity
public abstract class DataHandler extends Entity implements Comparable<DataHandler> {
    public static final Comparator<DataHandler> Comparator = java.util.Comparator.comparingInt(DataHandler::getOrder);
    private @Basic String mimeType;
    private Stage stage;
    private int order;

    @Override
    public int compareTo(@NotNull DataHandler other) {
        return Comparator.compare(this, other);
    }

    public enum Stage implements BitmaskAttribute<Stage> {
        None,
        Input,
        Process,
        Output
    }
}
