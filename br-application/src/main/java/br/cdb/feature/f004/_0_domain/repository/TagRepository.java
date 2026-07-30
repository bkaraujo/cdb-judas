package br.cdb.feature.f004._0_domain.repository;

import br.cdb.feature.f004._0_domain.model.Tag;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@NullMarked
public interface TagRepository {
    List<Tag> findAllByPerson(UUID personId);
    Optional<Tag> findByPersonAndId(UUID personId, UUID id);
    Optional<Tag> findById(UUID id);
    Tag save(Tag tag);
    void deleteById(UUID id);
}
