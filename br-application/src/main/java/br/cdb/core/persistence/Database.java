package br.cdb.core.persistence;

import br.commons.Logger;
import br.commons.Result;
import br.commons.framework.cdi.Context;
import br.commons.framework.persistence.jdbc.DataSource;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.util.List;

@NullMarked
public abstract class Database {

    private Database() {}

    /**
     * Comandos de limpeza para isolar testes — apaga só dados, preserva lookups/pessoa/login.
     * Ordem: dependentes primeiro (sem FK real entre dados, mas mantém organização pai→filho).
     */
    public static List<String> reset() {
        return List.of(
                "DELETE FROM F999_DELETION_QUEUE",
                "DELETE FROM F004_TRANSACTION_TAG",
                "DELETE FROM F005_TRANSACTION_CATEGORY",
                "DELETE FROM F006_TRANSACTION",
                "DELETE FROM F002_BALANCE",
                "DELETE FROM F003_CARD",
                "DELETE FROM F005_CATEGORY",
                "DELETE FROM F004_TAG",
                "DELETE FROM F000_PREFERENCES",
                "DELETE FROM F002_ACCOUNT"
        );
    }

    public static void initialize(List<String> instructions) {
        switch (Context.get(DataSource.class).begin()) {
            case Result.Failure(var error) -> throw new IllegalStateException(error);
            case Result.Success(var transaction) -> {
                if (transaction == null) throw new IllegalStateException("Transaction is null");
                try {
                    for (val command : instructions) {
                        transaction
                                .execute(command)
                                .ifFailure(reason -> Logger.error("Erro ao criar schema: %s", reason));
                    }
                    transaction.commit();
                } finally {
                    transaction.close();
                }
            }
        }
    }
}
