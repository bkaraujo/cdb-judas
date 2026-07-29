package br.cdb.feature.f002._0_domain.repository;

import br.cdb.feature.f002._0_domain.model.Account;
import br.commons.framework.persistence.json.Repository;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@NullMarked
public interface AccountRepository extends Repository<Account, UUID> {
    /** Guarda implícita: só as contas de {@code personId} — F002_ACCOUNT.COD_PERSON no WHERE. */
    List<Account> findAllByPerson(String personId);

    /** Guarda implícita: vazio se {@code id} existe mas não pertence a {@code personId} (404 natural). */
    Optional<Account> findByIdAndPerson(UUID id, String personId);
}
