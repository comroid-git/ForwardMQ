package org.comroid.forwardmq.entity;

import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import lombok.Data;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

@Data
@Slf4j
@jakarta.persistence.Entity
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Entity {
    public static final int CurrentVersion = 1;
    private int version = CurrentVersion;
    private @Id UUID id = UUID.randomUUID();
    private @Nullable @Setter String name;
    private @Nullable @Setter String displayName;

    public String getBestName() {
        return Optional.ofNullable(displayName)
                .or(() -> Optional.ofNullable(name))
                .filter(Predicate.not("null"::equals))
                .orElseGet(id::toString);
    }

    public String toString() {
        return getClass().getSimpleName() + ' ' + getBestName();
    }

    public final boolean equals(Object other) {
        return other instanceof Entity entity && id.equals(entity.id);
    }

    public final int hashCode() {
        return id.hashCode();
    }

    public interface Repo<T extends Entity> extends CrudRepository<T, UUID> {
        Iterable<T> findAllByName(String name);

        @Query("SELECT e FROM #{#entityName} e WHERE e.version = null OR e.version <= :version")
        Iterable<T> findMigrationCandidates(@Param("version") int fromVersion);
    }
}
