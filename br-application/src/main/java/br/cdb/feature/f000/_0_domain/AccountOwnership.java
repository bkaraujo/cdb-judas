package br.cdb.feature.f000._0_domain;

import org.jspecify.annotations.NullMarked;

import java.util.Set;
import java.util.UUID;

/**
 * Porta de propriedade de conta consumida por {@code UserGuards} (fatia-base {@code f000}) para a
 * checagem anti-IDOR, sem a base conhecer o overlay de conta. Implementada pela fatia que possui o
 * overlay {@code PERSON_ACCOUNT} (f002) — inversão de dependência: f002 depende de f000, nunca o
 * contrário.
 */
@NullMarked
public interface AccountOwnership {

    /** IDs das contas que pertencem à pessoa (via overlay {@code PERSON_ACCOUNT}). */
    Set<UUID> ownedAccountIds(String personId);
}
