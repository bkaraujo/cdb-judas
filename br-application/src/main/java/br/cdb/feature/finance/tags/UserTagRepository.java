package br.cdb.feature.finance.tags;

import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@NullMarked
public interface UserTagRepository {
    List<UserTag> findAllByPerson(UUID personId);
    Optional<UserTag> findById(UUID id);
    UserTag save(UserTag tag);
    void deleteById(UUID id);
}
