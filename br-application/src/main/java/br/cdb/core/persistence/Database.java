package br.cdb.core.persistence;

import br.cdb.core.persistence.repository.AccountTypeMapper;
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
 * sem {@code DEFAULT} (a aplicação sempre fornece os valores).
 *
 * <p>Cada tabela de dados pertence a exatamente uma fatia ({@code FNNN_*}) e carrega {@code COD_PERSON}
 * — é a base da guarda anti-IDOR implícita (toda query de fatia filtra por ele). {@code F002_ACCOUNT}
 * funde o que antes era {@code MON_ACCOUNT} (contexto) + {@code PERSON_ACCOUNT} (overlay da feature,
 * só a cor); {@code F003_CARD} e {@code F006_TRANSACTION} ganham {@code COD_PERSON} próprio (antes só
 * alcançável indiretamente via {@code COD_ACCOUNT}). {@code F005_TRANSACTION_CATEGORY} é o vínculo
 * transação↔categoria extraído de {@code PERSON_TRANSACTION.COD_CATEGORY}, espelhando a forma de
 * {@code F004_TRANSACTION_TAG}. Cartão é entidade própria de {@code F003_CARD} (identificado só pelo
 * last4, vinculado a uma conta real); limite de crédito/cheque especial e ciclo de fatura (fechamento/
 * vencimento) são colunas da própria {@code F002_ACCOUNT}, compartilhadas por todos os cartões dela.
 * {@code F000_USER}/{@code F000_USER_CREDENTIAL} servem só ao login (identificam a pessoa).</p>
 *
 * <p>Lookup tables globais, prefixo {@code SYS_}: {@code SYS_ACCOUNT_TYPE} (IDs UUID estáveis — ver
 * {@link AccountTypeMapper}), {@code SYS_TRANSACTION_NATURE} e {@code SYS_STATUS} (IDs VARCHAR(20) com
 * o nome do enum); mais o catálogo {@code F000_COST_CENTER} (semeado, dono é a fatia f000). As FKs do
 * schema existem <strong>só</strong> para lookups (validade de enum/catálogo — inclusive
 * {@code F006_TRANSACTION→F000_COST_CENTER}). Entre as demais tabelas de dados — inclusive cross-slice
 * (ex.: {@code F000_USER→F000_PERSON}, {@code F005_TRANSACTION_CATEGORY→F006_TRANSACTION}) e a
 * self-ref de {@code F005_CATEGORY.COD_PARENT} — não há FK: a integridade referencial é garantida pela
 * aplicação via eventos de domínio (exclusão apaga a raiz e publica um {@code *Deleted}; o listener da
 * fatia dona do dependente reage, best-effort, ou grava em {@code F999_DELETION_QUEUE} para retry). Isso
 * permite apagar o pai antes dos dependentes sem {@code RESTRICT} bloqueando nem {@code CASCADE}
 * preemptando o listener. A ordem de criação em {@link #model()} e a de limpeza em {@link #reset()}
 * ainda respeita pai→filho por organização, não por exigência de FK.</p>
 *
 * <p>{@link #reset()} dá os comandos de limpeza para isolar testes (apaga só dados, preserva
 * lookups/pessoa/login).</p>
 */
@NullMarked
public abstract class Database {

    private Database() {}

    private static List<String> f000() {
        return List.of(
                """
                CREATE TABLE F000_PERSON (
                    ID CHAR(36) PRIMARY KEY,
                    TXT_NAME VARCHAR(255) NOT NULL,
                    TXT_LOCALE VARCHAR(20) NOT NULL,
                    TXT_LANGUAGE VARCHAR(20) NOT NULL,
                    TMS_CREATE_AT TIMESTAMP NOT NULL,
                    TMS_UPDATED_AT TIMESTAMP NOT NULL
                )
                """,
                """
                CREATE TABLE F000_USER (
                    ID CHAR(36) PRIMARY KEY,
                    COD_PERSON CHAR(36) NOT NULL,
                    TXT_USERNAME VARCHAR(120) NOT NULL,
                    FLG_ACTIVE CHAR(1) NOT NULL,
                    TMS_CREATE_AT TIMESTAMP NOT NULL,
                    TMS_UPDATED_AT TIMESTAMP NOT NULL
                )
                """,
                """
                CREATE TABLE F000_USER_CREDENTIAL (
                    ID CHAR(36) PRIMARY KEY,
                    COD_USER CHAR(36) NOT NULL,
                    TXT_PASSWORD VARCHAR(255) NOT NULL,
                    TMS_CREATE_AT TIMESTAMP NOT NULL
                )
                """,
                """
                CREATE TABLE F000_PREFERENCES (
                    COD_PERSON CHAR(36) NOT NULL,
                    TXT_KEY VARCHAR(50) NOT NULL,
                    TXT_VALUE VARCHAR(255),
                    PRIMARY KEY (COD_PERSON, TXT_KEY)
                )
                """,
                """
                CREATE TABLE F000_COST_CENTER (
                    ID CHAR(36) PRIMARY KEY,
                    TXT_DESCRIPTION VARCHAR(255) NOT NULL,
                    FLG_ACTIVE CHAR(1) NOT NULL
                )
                """,
                "INSERT INTO F000_COST_CENTER (ID, TXT_DESCRIPTION, FLG_ACTIVE) VALUES ('d0000000-0000-0000-0000-000000000001', 'Fixo', 'Y')",
                "INSERT INTO F000_COST_CENTER (ID, TXT_DESCRIPTION, FLG_ACTIVE) VALUES ('d0000000-0000-0000-0000-000000000002', 'Variável', 'Y')"
        );
    }

    private static List<String> sysLookups() {
        return List.of(
                """
                CREATE TABLE SYS_ACCOUNT_TYPE (
                    ID CHAR(36) PRIMARY KEY,
                    TXT_DESCRIPTION VARCHAR(50) NOT NULL,
                    FLG_ACTIVE CHAR(1) NOT NULL
                )
                """,
                "INSERT INTO SYS_ACCOUNT_TYPE (ID, TXT_DESCRIPTION, FLG_ACTIVE) VALUES ('" + AccountTypeMapper.CHECKING_ID    + "', 'Conta corrente', 'Y')",
                "INSERT INTO SYS_ACCOUNT_TYPE (ID, TXT_DESCRIPTION, FLG_ACTIVE) VALUES ('" + AccountTypeMapper.INVESTMENT_ID  + "', 'Investimento', 'Y')",
                """
                CREATE TABLE SYS_STATUS (
                    ID VARCHAR(20) PRIMARY KEY,
                    TXT_DESCRIPTION VARCHAR(50) NOT NULL,
                    FLG_ACTIVE CHAR(1) NOT NULL
                )
                """,
                "INSERT INTO SYS_STATUS (ID, TXT_DESCRIPTION, FLG_ACTIVE) VALUES ('SCHEDULED', 'Agendado', 'Y')",
                "INSERT INTO SYS_STATUS (ID, TXT_DESCRIPTION, FLG_ACTIVE) VALUES ('CONFIRMED', 'Confirmado', 'Y')",
                "INSERT INTO SYS_STATUS (ID, TXT_DESCRIPTION, FLG_ACTIVE) VALUES ('PENDING', 'Pendente', 'Y')",
                """
                CREATE TABLE SYS_TRANSACTION_NATURE (
                    ID VARCHAR(20) PRIMARY KEY,
                    TXT_DESCRIPTION VARCHAR(50) NOT NULL
                )
                """,
                "INSERT INTO SYS_TRANSACTION_NATURE (ID, TXT_DESCRIPTION) VALUES ('EXPENSE', 'Despesa')",
                "INSERT INTO SYS_TRANSACTION_NATURE (ID, TXT_DESCRIPTION) VALUES ('INCOME', 'Receita')"
        );
    }

    private static List<String> f002() {
        return List.of(
                """
                CREATE TABLE F002_ACCOUNT (
                    ID CHAR(36) PRIMARY KEY,
                    COD_PERSON CHAR(36),
                    TXT_TYPE CHAR(36) NOT NULL REFERENCES SYS_ACCOUNT_TYPE(ID),
                    TXT_NAME VARCHAR(80) NOT NULL,
                    TXT_COLOR VARCHAR(20),
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
                CREATE TABLE F002_BALANCE (
                    ID CHAR(36) PRIMARY KEY,
                    COD_PERSON CHAR(36) NOT NULL,
                    COD_ACCOUNT CHAR(36) NOT NULL,
                    NUM_PERIOD INT NOT NULL,
                    DEC_BALANCE DECIMAL(19, 2) NOT NULL,
                    FLG_DIRTY CHAR(1) NOT NULL
                )
                """
        );
    }

    private static List<String> f003() {
        return List.of(
                """
                CREATE TABLE F003_CARD (
                    ID CHAR(36) PRIMARY KEY,
                    COD_PERSON CHAR(36),
                    COD_ACCOUNT CHAR(36) NOT NULL,
                    TXT_LAST4 CHAR(4) NOT NULL,
                    FLG_ACTIVE CHAR(1) NOT NULL,
                    TMS_CREATE_AT TIMESTAMP NOT NULL,
                    TMS_UPDATED_AT TIMESTAMP NOT NULL
                )
                """
        );
    }

    private static List<String> f004() {
        return List.of(
                """
                CREATE TABLE F004_TAG (
                    ID CHAR(36) PRIMARY KEY,
                    COD_PERSON CHAR(36) NOT NULL,
                    TXT_DESCRIPTION VARCHAR(255) NOT NULL,
                    TXT_COLOR VARCHAR(20) NOT NULL,
                    TMS_CREATE_AT TIMESTAMP NOT NULL
                )
                """,
                """
                CREATE TABLE F004_TRANSACTION_TAG (
                    COD_TRANSACTION CHAR(36) NOT NULL,
                    COD_PERSON CHAR(36) NOT NULL,
                    COD_TAG CHAR(36) NOT NULL,
                    PRIMARY KEY (COD_TRANSACTION, COD_PERSON, COD_TAG)
                )
                """
        );
    }

    private static List<String> f005() {
        return List.of(
                """
                CREATE TABLE F005_CATEGORY (
                    ID CHAR(36) PRIMARY KEY,
                    COD_PERSON CHAR(36) NOT NULL,
                    COD_PARENT CHAR(36),
                    COD_NATURE VARCHAR(20) NOT NULL REFERENCES SYS_TRANSACTION_NATURE(ID),
                    TXT_NAME VARCHAR(80) NOT NULL,
                    FLG_SYSTEM CHAR(1) NOT NULL,
                    FLG_ACTIVE CHAR(1) NOT NULL,
                    TMS_CREATE_AT TIMESTAMP NOT NULL,
                    TMS_UPDATED_AT TIMESTAMP NOT NULL
                )
                """,
                """
                CREATE TABLE F005_TRANSACTION_CATEGORY (
                    COD_TRANSACTION CHAR(36) NOT NULL,
                    COD_PERSON CHAR(36) NOT NULL,
                    COD_CATEGORY CHAR(36) NOT NULL,
                    PRIMARY KEY (COD_TRANSACTION, COD_PERSON)
                )
                """
        );
    }

    private static List<String> f006() {
        return List.of(
                """
                CREATE TABLE F006_TRANSACTION (
                    ID CHAR(36) PRIMARY KEY,
                    COD_PERSON CHAR(36),
                    TXT_DESCRIPTION VARCHAR(255) NOT NULL,
                    NUM_SIGNAL INT NOT NULL,
                    DEC_AMOUNT DECIMAL(19, 2) NOT NULL,
                    TMS_PURCHASE TIMESTAMP NOT NULL,
                    COD_ACCOUNT CHAR(36) NOT NULL,
                    COD_CARD CHAR(36),
                    COD_STATUS VARCHAR(20) NOT NULL REFERENCES SYS_STATUS(ID),
                    COD_COST_CENTER CHAR(36) NOT NULL REFERENCES F000_COST_CENTER(ID),
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

    private static List<String> f999() {
        return List.of(
                """
                CREATE TABLE F999_DELETION_QUEUE (
                    ID CHAR(36) PRIMARY KEY,
                    TXT_TYPE VARCHAR(40) NOT NULL,
                    COD_TARGET CHAR(36) NOT NULL,
                    COD_PERSON CHAR(36) NOT NULL,
                    NUM_ATTEMPTS INT NOT NULL,
                    FLG_LOCKED CHAR(1) NOT NULL,
                    TMS_CREATE_AT TIMESTAMP NOT NULL,
                    TMS_UPDATED_AT TIMESTAMP NOT NULL
                )
                """
        );
    }

    public static List<String> model() {
        val instructions = new ArrayList<String>();
        instructions.addAll(f000());
        instructions.addAll(sysLookups());
        instructions.addAll(f002());
        instructions.addAll(f003());
        instructions.addAll(f004());
        instructions.addAll(f005());
        instructions.addAll(f006());
        instructions.addAll(f999());

        return instructions;
    }

    /**
     * Comandos de limpeza de dados (isolamento entre testes). Sem FK entre tabelas de dados, a ordem
     * não é mais exigida pelo banco — mantida pai→filho por organização. Preserva pessoa/login/lookups.
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
}
