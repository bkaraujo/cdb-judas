package br.community.context.people._0_domain.repository;

import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.UUID;

/**
 * Porta da tabela auxiliar que vincula uma {@code Person} (dono) a contas do contexto monetário.
 * Opera apenas por identificadores ({@code personId}/{@code accountId}) — sem acoplar modelos de
 * outro contexto. A implementação JDBC mapeia a junção {@code PEP_PERSON_ACCOUNT}.
 */
@NullMarked
public interface PersonAccountRepository {

    /** Vincula uma conta a uma pessoa (idempotente). */
    void link(UUID personId, UUID accountId);

    /** Remove o vínculo entre pessoa e conta (no-op se inexistente). */
    void unlink(UUID personId, UUID accountId);

    /** Ids das contas pertencentes à pessoa. */
    List<UUID> accountIdsOf(UUID personId);

    /** Verdadeiro se a conta pertence à pessoa. */
    boolean owns(UUID personId, UUID accountId);
}
