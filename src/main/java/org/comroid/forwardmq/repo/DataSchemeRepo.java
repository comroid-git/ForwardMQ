package org.comroid.forwardmq.repo;

import org.comroid.api.func.util.AlmostComplete;
import org.comroid.forwardmq.entity.data.DataScheme;
import org.springframework.data.repository.CrudRepository;

import java.util.UUID;

public interface DataSchemeRepo extends CrudRepository<DataScheme, String> {
    default AlmostComplete<DataScheme> get(UUID id) {
        return findById(minecraftUsername).map(AlmostComplete::of).orElseGet(() -> new AlmostComplete<>(() -> {
            var usr = new User();
            var id = findMinecraftId(minecraftUsername);
            usr.setId(id);
            usr.setName(minecraftUsername);
            usr.setDisplayName(usr.getName() + " McUser");
            usr.setMinecraftId(id);
            return usr;
        }, this::save));
    }
}
