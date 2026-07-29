package br.cdb.feature.f000._0_domain.repository;

import br.cdb.feature.f000._0_domain.model.Person;
import br.commons.framework.persistence.json.Repository;
import org.jspecify.annotations.NullMarked;

import java.util.UUID;

@NullMarked
public interface PersonRepository extends Repository<Person, UUID> {
}
