package br.cdb.feature.f002._0_domain.repository;

import br.cdb.feature.f002._0_domain.model.Balance;
import br.commons.framework.persistence.json.Repository;
import org.jspecify.annotations.NullMarked;

import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@NullMarked
public interface BalanceRepository extends Repository<Balance, UUID> {

    List<Balance> findByAccount(UUID accountId);

    void delete(UUID uuid, YearMonth period);

    /** Marca todo snapshot da conta como sujo — no-op se não existir nenhum ainda (nada a corrigir;
     *  o próximo {@code save} normal já grava o valor certo). Consumido pela cascata de exclusão de
     *  conta (f002) como rede de segurança pro job de reconciliação (f999). */
    void markDirty(UUID accountId);

    /** Marca <b>todos</b> os snapshots como sujos — varredura de startup, para que uma mudança na
     *  regra de cálculo (ex.: fatura de cartão passando a debitar no vencimento) alcance dados já
     *  gravados sem exigir migração de schema. */
    void markAllDirty();

    /** IDs de conta com pelo menos um snapshot sujo — consumido pelo job de reconciliação (f999). */
    List<UUID> findDirtyAccountIds();

}
