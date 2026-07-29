package br.cdb.feature.f002._2_infrastructure.persistence;

import br.cdb.feature.f002._0_domain.UserAccount;
import br.cdb.feature.f002._0_domain.UserAccountRepository;
import br.commons.Registry;
import br.commons.framework.persistence.jdbc.DataSource;
import br.commons.framework.persistence.jdbc.primitives.JDBCParameter;
import br.commons.framework.persistence.jdbc.primitives.JDBCResultSet;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Adaptador JDBC (H2) da porta {@link UserAccountRepository}; tabela {@code F002_ACCOUNT} — mesma
 * linha que {@code AccountJDBCRepository} (contexto monetário) grava, colunas {@code COD_PERSON}/
 * {@code TXT_COLOR}. Não estende {@code JDBCRepository<UserAccount>}: este adaptador é dono de só 2
 * das colunas de uma tabela compartilhada, então opera direto sobre {@link DataSource}.
 * {@link #save} é sempre um UPDATE — a linha já existe, criada por
 * {@code AccountJDBCRepository.save} segundos antes, na mesma orquestração de
 * {@code f002.AccountUseCase}.
 */
@NullMarked
public final class UserAccountJDBCRepository implements UserAccountRepository {

    private final DataSource datasource = Registry.get(DataSource.class);

    @Override
    public Optional<UserAccount> find(String personId, UUID accountId) {
        return datasource.query(
                "SELECT COD_PERSON, ID, TXT_COLOR FROM F002_ACCOUNT WHERE ID = ? AND COD_PERSON = ?",
                JDBCParameter.of(accountId.toString(), personId),
                this::mapList
        ).stream().findFirst();
    }

    @Override
    public List<UserAccount> findByPerson(String personId) {
        return datasource.query(
                "SELECT COD_PERSON, ID, TXT_COLOR FROM F002_ACCOUNT WHERE COD_PERSON = ?",
                JDBCParameter.of(personId),
                this::mapList
        );
    }

    @Override
    public UserAccount save(UserAccount ua) {
        datasource.execute(
                "UPDATE F002_ACCOUNT SET COD_PERSON = ?, TXT_COLOR = ? WHERE ID = ?",
                JDBCParameter.of(ua.personId(), ua.color(), ua.accountId().toString())
        );
        return ua;
    }

    /** No-op seguro: a exclusão da conta (contexto) já apaga esta mesma linha por inteiro. */
    @Override
    public void delete(String personId, UUID accountId) {
        datasource.execute(
                "DELETE FROM F002_ACCOUNT WHERE ID = ? AND COD_PERSON = ?",
                JDBCParameter.of(accountId.toString(), personId)
        );
    }

    private List<UserAccount> mapList(JDBCResultSet rs) {
        val rows = new ArrayList<UserAccount>();
        while (rs.next().get()) {
            val personId = rs.getString("COD_PERSON").get();
            val accountId = rs.getString("ID").get();
            val color = rs.getString("TXT_COLOR").get();
            if (personId == null || accountId == null || color == null) continue;
            rows.add(new UserAccount(personId, UUID.fromString(accountId), color));
        }
        return rows;
    }
}
