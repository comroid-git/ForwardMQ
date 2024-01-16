package org.comroid.forwardmq.entity.data;

import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.comroid.api.attr.Named;
import org.comroid.api.data.seri.DataNode;
import org.comroid.api.data.seri.MimeType;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.Nullable;

@Data
@Slf4j
@NoArgsConstructor
@AllArgsConstructor
@jakarta.persistence.Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"name"}))
public final class DataScheme implements Named {
    private @Id String name;
    private Class<?> dataType = DataNode.class;
    private String mimeType = MimeType.JSON.toString();
    private @Nullable @Language("JavaScript") String convertInput;
    private @Nullable @Language("JavaScript") String convertOutput;
}
