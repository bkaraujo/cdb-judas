package br.cdb.infra.persistence;

import br.cdb.infra.persistence.monetary.AccountTypeMapper;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.util.ArrayList;
import java.util.List;

/**
 * Schema (DDL) do banco relacional, em SQL estritamente portável entre fornecedores.
 *
 * <p>Restrito a construções padrão (sem dialeto): {@code CREATE TABLE} sem {@code IF NOT EXISTS},
 * tipos {@code CHAR}/{@code VARCHAR}/{@code DECIMAL}/{@code DATE}/{@code INT}/{@code TIMESTAMP};
 * booleanos ativos como {@code CHAR(1)} ('Y'/'N') com prefixo {@code FLG_}. Tabelas de dados planas,
 * sem {@code DEFAULT} (a aplicação sempre fornece os valores). Cartão é entidade do contexto
 * monetário: {@code MON_CARD} (identificado só pelo last4, vinculado a uma conta real).
 * Limite de crédito/cheque especial e ciclo de fatura (fechamento/vencimento) são colunas da
 * própria {@code MON_ACCOUNT}, compartilhadas por todos os cartões dela. {@code USER_ACCOUNT}
 * carrega só a cor por utilizador — estado ativo e saldo já vêm do contexto monetário (o saldo
 * inicial histórico, quando existia, virou uma transação normal na migração).</p>
 *
 * <p>Lookup tables: {@code MON_ACCOUNT_TYPE} (IDs UUID estáveis — ver {@link AccountTypeMapper}),
 * {@code TRANSACTION_NATURE} e {@code MON_STATUS} (IDs VARCHAR(20) com o nome do enum). As FKs
 * conformam a todas as relações dos diagramas Mermaid: além das lookups, as tabelas de dados
 * referenciam-se entre si, inclusive entre contextos (ex.: {@code SEC_USER→PEP_PERSON},
 * {@code USER_TRANSACTION→MON_TRANSACTION}). Por isso a ordem de criação em {@link #model()} e a
 * de limpeza em {@link #reset()} respeitam a dependência pai→filho. As FKs dos overlays de feature
 * para {@code MON_TRANSACTION} ({@code USER_TRANSACTION}/{@code USER_TRANSACTION_TAG}) usam
 * {@code ON DELETE CASCADE}: o contexto monetário é dono da transação e a apaga primeiro; os
 * overlays somem em cascata (a limpeza reativa na feature vira idempotente). {@code USER_CATEGORY.COD_PARENT}
 * também usa {@code ON DELETE CASCADE} (apagar uma macro-categoria remove a subárvore).</p>
 *
 * <p>{@link #reset()} dá os comandos de limpeza para isolar testes (apaga só dados, preserva
 * lookups).</p>
 */
@NullMarked
public abstract class Database {

    private Database() {}

    private static List<String> ctxMonetary() {
        return List.of(
                """
                CREATE TABLE MON_ACCOUNT_TYPE (
                    ID CHAR(36) PRIMARY KEY,
                    TXT_DESCRIPTION VARCHAR(50) NOT NULL,
                    FLG_ACTIVE CHAR(1) NOT NULL
                )
                """,
                "INSERT INTO MON_ACCOUNT_TYPE (ID, TXT_DESCRIPTION, FLG_ACTIVE) VALUES ('" + AccountTypeMapper.CHECKING_ID    + "', 'Conta corrente', 'Y')",
                "INSERT INTO MON_ACCOUNT_TYPE (ID, TXT_DESCRIPTION, FLG_ACTIVE) VALUES ('" + AccountTypeMapper.INVESTMENT_ID  + "', 'Investimento', 'Y')",
                """
                CREATE TABLE MON_STATUS (
                    ID VARCHAR(20) PRIMARY KEY,
                    TXT_DESCRIPTION VARCHAR(50) NOT NULL,
                    FLG_ACTIVE CHAR(1) NOT NULL
                )
                """,
                "INSERT INTO MON_STATUS (ID, TXT_DESCRIPTION, FLG_ACTIVE) VALUES ('SCHEDULED', 'Agendado', 'Y')",
                "INSERT INTO MON_STATUS (ID, TXT_DESCRIPTION, FLG_ACTIVE) VALUES ('CONFIRMED', 'Confirmado', 'Y')",
                "INSERT INTO MON_STATUS (ID, TXT_DESCRIPTION, FLG_ACTIVE) VALUES ('PENDING', 'Pendente', 'Y')",
                """
                CREATE TABLE MON_ACCOUNT (
                    ID CHAR(36) PRIMARY KEY,
                    TXT_TYPE CHAR(36) NOT NULL REFERENCES MON_ACCOUNT_TYPE(ID),
                    TXT_NAME VARCHAR(80) NOT NULL,
                    DEC_CREDIT_LIMIT DECIMAL(19, 2),
                    DEC_OVERDRAFT_LIMIT DECIMAL(19, 2),
                    NUM_CLOSING_DAY INT,
                    NUM_DUE_DAY INT,
                    FLG_ACTIVE CHAR(1) NOT NULL,
                    TMS_CREATE_AT TIMESTAMP NOT NULL,
                    TMS_UPDATED_AT TIMESTAMP NOT NULL
                )
                """,
                """
                CREATE TABLE MON_COST_CENTER (
                    ID CHAR(36) PRIMARY KEY,
                    TXT_DESCRIPTION VARCHAR(255) NOT NULL,
                    FLG_ACTIVE CHAR(1) NOT NULL
                )
                """,
                "INSERT INTO MON_COST_CENTER (ID, TXT_DESCRIPTION, FLG_ACTIVE) VALUES ('d0000000-0000-0000-0000-000000000001', 'Fixo', 'Y')",
                "INSERT INTO MON_COST_CENTER (ID, TXT_DESCRIPTION, FLG_ACTIVE) VALUES ('d0000000-0000-0000-0000-000000000002', 'Variável', 'Y')",
                """
                CREATE TABLE MON_CARD (
                    ID CHAR(36) PRIMARY KEY,
                    COD_ACCOUNT CHAR(36) NOT NULL REFERENCES MON_ACCOUNT(ID),
                    TXT_LAST4 CHAR(4) NOT NULL,
                    FLG_ACTIVE CHAR(1) NOT NULL,
                    TMS_CREATE_AT TIMESTAMP NOT NULL,
                    TMS_UPDATED_AT TIMESTAMP NOT NULL
                )
                """,
                """
                CREATE TABLE MON_TRANSACTION (
                    ID CHAR(36) PRIMARY KEY,
                    TXT_DESCRIPTION VARCHAR(255) NOT NULL,
                    NUM_SIGNAL INT NOT NULL,
                    DEC_AMOUNT DECIMAL(19, 2) NOT NULL,
                    TMS_PURCHASE TIMESTAMP NOT NULL,
                    COD_ACCOUNT CHAR(36) NOT NULL REFERENCES MON_ACCOUNT(ID),
                    COD_CARD CHAR(36) REFERENCES MON_CARD(ID),
                    COD_STATUS VARCHAR(20) NOT NULL REFERENCES MON_STATUS(ID),
                    COD_COST_CENTER CHAR(36) NOT NULL REFERENCES MON_COST_CENTER(ID),
                    DAT_PAYMENT DATE,
                    GROUP_ID CHAR(36),
                    NUM_INSTALLMENT INT NOT NULL,
                    NUM_INSTALLMENT_TOTAL INT NOT NULL,
                    TXT_NOTES VARCHAR(1000),
                    TMS_CREATE_AT TIMESTAMP NOT NULL,
                    TMS_UPDATED_AT TIMESTAMP NOT NULL
                )
                """
        );
    }

    private static List<String> ctxPeople() {
        return List.of(
                """
                CREATE TABLE PEP_PERSON (
                    ID CHAR(36) PRIMARY KEY,
                    TXT_NAME VARCHAR(255) NOT NULL,
                    TXT_LOCALE VARCHAR(20) NOT NULL,
                    TXT_LANGUAGE VARCHAR(20) NOT NULL,
                    TMS_CREATE_AT TIMESTAMP NOT NULL,
                    TMS_UPDATED_AT TIMESTAMP NOT NULL
                )
                """
        );
    }

    private static List<String> security() {
        return List.of(
                """
                CREATE TABLE SEC_USER (
                    ID CHAR(36) PRIMARY KEY,
                    COD_PERSON CHAR(36) NOT NULL REFERENCES PEP_PERSON(ID),
                    TXT_USERNAME VARCHAR(120) NOT NULL,
                    FLG_ACTIVE CHAR(1) NOT NULL,
                    TMS_CREATE_AT TIMESTAMP NOT NULL,
                    TMS_UPDATED_AT TIMESTAMP NOT NULL
                )
                """
        );
    }

    private static List<String> features() {
        return List.of(
                """
                CREATE TABLE USER_CREDENTIAL (
                    ID CHAR(36) PRIMARY KEY,
                    COD_USER CHAR(36) NOT NULL REFERENCES SEC_USER(ID),
                    TXT_PASSWORD VARCHAR(255) NOT NULL,
                    TMS_CREATE_AT TIMESTAMP NOT NULL
                )
                """,
                """
                CREATE TABLE USER_PREFERENCES (
                    COD_USER CHAR(36) NOT NULL REFERENCES SEC_USER(ID),
                    TXT_KEY VARCHAR(50) NOT NULL,
                    TXT_VALUE VARCHAR(255),
                    PRIMARY KEY (COD_USER, TXT_KEY)
                )
                """,
                """
                CREATE TABLE USER_ACCOUNT (
                    COD_USER CHAR(36) NOT NULL REFERENCES SEC_USER(ID),
                    COD_ACCOUNT CHAR(36) NOT NULL REFERENCES MON_ACCOUNT(ID),
                    TXT_COLOR VARCHAR(20) NOT NULL,
                    PRIMARY KEY (COD_USER, COD_ACCOUNT)
                )
                """,
                """
                CREATE TABLE USER_ACCOUNT_BALANCE (
                    ID CHAR(36) PRIMARY KEY,
                    COD_USER CHAR(36) NOT NULL REFERENCES SEC_USER(ID),
                    COD_ACCOUNT CHAR(36) NOT NULL REFERENCES MON_ACCOUNT(ID),
                    NUM_PERIOD INT NOT NULL,
                    DEC_BALANCE DECIMAL(19, 2) NOT NULL
                )
                """,
                """
                CREATE TABLE TRANSACTION_NATURE (
                    ID VARCHAR(20) PRIMARY KEY,
                    TXT_DESCRIPTION VARCHAR(50) NOT NULL
                )
                """,
                "INSERT INTO TRANSACTION_NATURE (ID, TXT_DESCRIPTION) VALUES ('EXPENSE', 'Despesa')",
                "INSERT INTO TRANSACTION_NATURE (ID, TXT_DESCRIPTION) VALUES ('INCOME', 'Receita')",
                """
                CREATE TABLE USER_CATEGORY (
                    ID CHAR(36) PRIMARY KEY,
                    COD_USER CHAR(36) NOT NULL REFERENCES SEC_USER(ID),
                    COD_PARENT CHAR(36) REFERENCES USER_CATEGORY(ID) ON DELETE CASCADE,
                    COD_NATURE VARCHAR(20) NOT NULL REFERENCES TRANSACTION_NATURE(ID),
                    TXT_NAME VARCHAR(80) NOT NULL,
                    FLG_SYSTEM CHAR(1) NOT NULL,
                    FLG_ACTIVE CHAR(1) NOT NULL,
                    TMS_CREATE_AT TIMESTAMP NOT NULL,
                    TMS_UPDATED_AT TIMESTAMP NOT NULL
                )
                """,
                """
                CREATE TABLE USER_TAG (
                    ID CHAR(36) PRIMARY KEY,
                    COD_USER CHAR(36) NOT NULL REFERENCES SEC_USER(ID),
                    TXT_DESCRIPTION VARCHAR(255) NOT NULL,
                    TXT_COLOR VARCHAR(20) NOT NULL,
                    TMS_CREATE_AT TIMESTAMP NOT NULL
                )
                """,
                """
                CREATE TABLE USER_TRANSACTION (
                    COD_USER CHAR(36) NOT NULL REFERENCES SEC_USER(ID),
                    COD_ACCOUNT CHAR(36) NOT NULL REFERENCES MON_ACCOUNT(ID),
                    COD_TRANSACTION CHAR(36) NOT NULL REFERENCES MON_TRANSACTION(ID) ON DELETE CASCADE,
                    COD_CATEGORY CHAR(36) NOT NULL REFERENCES USER_CATEGORY(ID),
                    TMS_CREATE_AT TIMESTAMP NOT NULL,
                    TMS_UPDATED_AT TIMESTAMP NOT NULL,
                    PRIMARY KEY (COD_USER, COD_ACCOUNT, COD_TRANSACTION)
                )
                """,
                """
                CREATE TABLE USER_TRANSACTION_TAG (
                    COD_TRANSACTION CHAR(36) NOT NULL REFERENCES MON_TRANSACTION(ID) ON DELETE CASCADE,
                    COD_USER CHAR(36) NOT NULL REFERENCES SEC_USER(ID),
                    COD_TAG CHAR(36) NOT NULL REFERENCES USER_TAG(ID),
                    PRIMARY KEY (COD_TRANSACTION, COD_USER, COD_TAG)
                )
                """
        );
    }


    public static List<String> model() {
        val instructions = new ArrayList<String>();
        instructions.addAll(ctxPeople());
        instructions.addAll(ctxMonetary());

        instructions.addAll(security());
        instructions.addAll(features());

        return instructions;
    }

    /**
     * Comandos de limpeza de dados (isolamento entre testes). Ordem importa: cada tabela é apagada
     * antes das que ela referencia via FK (filho→pai). {@code USER_CATEGORY} tem auto-referência
     * ({@code COD_PARENT}), então as filhas são removidas antes das raízes em dois passos — sem
     * recorrer a dialeto ({@code SET REFERENTIAL_INTEGRITY}), mantendo o DDL portável.
     */
    public static List<String> reset() {
        return List.of(
                "DELETE FROM USER_TRANSACTION_TAG",
                "DELETE FROM USER_TRANSACTION",
                "DELETE FROM USER_ACCOUNT_BALANCE",
                "DELETE FROM USER_ACCOUNT",
                "DELETE FROM USER_CATEGORY WHERE COD_PARENT IS NOT NULL",
                "DELETE FROM USER_CATEGORY",
                "DELETE FROM USER_TAG",
                "DELETE FROM USER_PREFERENCES",
                "DELETE FROM MON_TRANSACTION",
                "DELETE FROM MON_CARD",
                "DELETE FROM MON_ACCOUNT"
        );
    }
}
