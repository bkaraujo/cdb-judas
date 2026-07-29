package br.cdb.infra.persistence.monetary;

import br.commons.framework.persistence.jdbc.DataSource;
import br.commons.framework.persistence.jdbc.primitives.JDBCParameter;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.UUID;

/**
 * Resolve {@code COD_PERSON} de {@code F002_ACCOUNT} a partir de um {@code accountId} — usado pelos
 * adaptadores que precisam gravar o dono num registro que só carrega a conta (cartão, transação),
 * já que nem {@link CreditCardJDBCRepository} nem {@link TransactionJDBCRepository} recebem o
 * {@code personId} da requisição (os use cases do contexto não o conhecem).
 */
@NullMarked
abstract class AccountOwnerLookup {

    private AccountOwnerLookup() {}

    @Nullable
    static String find(DataSource datasource, UUID accountId) {
        val results = datasource.query(
                "SELECT COD_PERSON FROM F002_ACCOUNT WHERE ID = ?",
                JDBCParameter.of(accountId.toString()),
                rs -> {
                    val list = new ArrayList<@Nullable String>();
                    while (rs.next().get()) list.add(rs.getString("COD_PERSON").get());
                    return list;
                }
        );
        return results.isEmpty() ? null : results.getFirst();
    }
}
