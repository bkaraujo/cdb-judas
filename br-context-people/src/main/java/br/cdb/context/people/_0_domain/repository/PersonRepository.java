package br.cdb.context.people._0_domain.repository;

import br.cdb.context.people._0_domain.model.Person;
import br.commons.framework.persistence.json.Repository;
import org.jspecify.annotations.NullMarked;

import java.util.UUID;

@NullMarked
public interface PersonRepository extends Repository<Person, UUID> {
}
