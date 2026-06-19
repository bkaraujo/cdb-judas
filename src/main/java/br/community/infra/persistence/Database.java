package br.community.infra.persistence;

import org.jspecify.annotations.NullMarked;

import java.util.List;

/**
 * Schema (DDL) do banco relacional, em SQL estritamente portável entre fornecedores.
 *
 * <p>Restrito a construções padrão (sem dialeto): {@code CREATE TABLE} sem {@code IF NOT EXISTS},
 * tipos {@code CHAR}/{@code VARCHAR}/{@code DECIMAL}/{@code DATE}/{@code INT}/{@code TIMESTAMP};
 * booleanos ativos como {@code CHAR(1)} ('Y'/'N') com prefixo {@code FLG_}; textos grandes (JSON
 * de {@code additionalInfo}) como {@code VARCHAR(4000)}. Tabelas de dados planas, sem
 * {@code DEFAULT} (a aplicação sempre fornece os valores).</p>
 *
 * <p>Lookup tables: {@code MON_ACCOUNT_TYPE} (IDs UUID estáveis — ver {@link AccountTypeMapper}),
 * {@code MON_NATURE} e {@code MON_STATUS} (IDs VARCHAR(20) com o nome do enum). As tabelas de
 * dados referenciam as lookups via FK; entre si não há FK.</p>
 *
 * <p>{@link #reset()} dá os comandos de limpeza para isolar testes (apaga só dados, preserva
 * lookups).</p>
 */
@NullMarked
public abstract class Database {

    private Database() {}

    public static List<String> model() {
        return List.of(
                """
                CREATE TABLE MON_ACCOUNT_TYPE (
                    ID CHAR(36) PRIMARY KEY,
                    TXT_DESCRIPTION VARCHAR(50) NOT NULL,
                    FLG_ACTIVE CHAR(1) NOT NULL
                )
                """,
                """
                CREATE TABLE MON_NATURE (
                    ID VARCHAR(20) PRIMARY KEY,
                    TXT_DESCRIPTION VARCHAR(50) NOT NULL
                )
                """,
                """
                CREATE TABLE MON_STATUS (
                    ID VARCHAR(20) PRIMARY KEY,
                    TXT_DESCRIPTION VARCHAR(50) NOT NULL,
                    FLG_ACTIVE CHAR(1) NOT NULL
                )
                """,
                "INSERT INTO MON_ACCOUNT_TYPE (ID, TXT_DESCRIPTION, FLG_ACTIVE) VALUES ('" + AccountTypeMapper.CHECKING_ID    + "', 'Conta corrente', 'Y')",
                "INSERT INTO MON_ACCOUNT_TYPE (ID, TXT_DESCRIPTION, FLG_ACTIVE) VALUES ('" + AccountTypeMapper.INVESTMENT_ID  + "', 'Investimento', 'Y')",
                "INSERT INTO MON_ACCOUNT_TYPE (ID, TXT_DESCRIPTION, FLG_ACTIVE) VALUES ('" + AccountTypeMapper.CREDIT_CARD_ID + "', 'Cartão de crédito', 'Y')",
                "INSERT INTO MON_NATURE (ID, TXT_DESCRIPTION) VALUES ('EXPENSE', 'Despesa')",
                "INSERT INTO MON_NATURE (ID, TXT_DESCRIPTION) VALUES ('INCOME', 'Receita')",
                "INSERT INTO MON_STATUS (ID, TXT_DESCRIPTION, FLG_ACTIVE) VALUES ('SCHEDULED', 'Agendado', 'Y')",
                "INSERT INTO MON_STATUS (ID, TXT_DESCRIPTION, FLG_ACTIVE) VALUES ('CONFIRMED', 'Confirmado', 'Y')",
                "INSERT INTO MON_STATUS (ID, TXT_DESCRIPTION, FLG_ACTIVE) VALUES ('PENDING', 'Pendente', 'Y')",
                """
                CREATE TABLE MON_ACCOUNT (
                    ID CHAR(36) PRIMARY KEY,
                    TXT_NAME VARCHAR(255) NOT NULL,
                    DEC_BALANCE DECIMAL(19, 2) NOT NULL,
                    TXT_TYPE CHAR(36) NOT NULL REFERENCES MON_ACCOUNT_TYPE(ID),
                    TXT_COLOR VARCHAR(20) NOT NULL,
                    FLG_ACTIVE CHAR(1) NOT NULL,
                    COD_LINKED_ACCOUNT CHAR(36),
                    TXT_ADDITIONAL_INFO VARCHAR(4000),
                    TMS_CREATE_AT TIMESTAMP NOT NULL,
                    TMS_UPDATED_AT TIMESTAMP NOT NULL
                )
                """,
                """
                CREATE TABLE MON_CATEGORY (
                    ID CHAR(36) PRIMARY KEY,
                    TXT_NATURE VARCHAR(20) NOT NULL REFERENCES MON_NATURE(ID),
                    TXT_NAME VARCHAR(255) NOT NULL,
                    COD_PARENT CHAR(36),
                    BOL_SYSTEM CHAR(1) NOT NULL
                )
                """,
                """
                CREATE TABLE MON_COST_CENTER (
                    ID CHAR(36) PRIMARY KEY,
                    TXT_DESCRIPTION VARCHAR(255) NOT NULL,
                    FLG_ACTIVE CHAR(1) NOT NULL,
                    TMS_CREATE_AT TIMESTAMP NOT NULL,
                    TMS_UPDATED_AT TIMESTAMP NOT NULL
                )
                """,
                """
                CREATE TABLE MON_TAG (
                    ID CHAR(36) PRIMARY KEY,
                    TXT_NAME VARCHAR(255) NOT NULL,
                    TXT_COLOR VARCHAR(20) NOT NULL
                )
                """,
                """
                CREATE TABLE MON_MONTHLY_BALANCE (
                    ID CHAR(36) PRIMARY KEY,
                    COD_ACCOUNT CHAR(36) NOT NULL,
                    TXT_PERIOD VARCHAR(7) NOT NULL,
                    DEC_BALANCE DECIMAL(19, 2) NOT NULL
                )
                """,
                """
                CREATE TABLE MON_TRANSACTION (
                    ID CHAR(36) PRIMARY KEY,
                    TXT_DESCRIPTION VARCHAR(255) NOT NULL,
                    NUM_SIGNAL INT NOT NULL,
                    DEC_AMOUNT DECIMAL(19, 2) NOT NULL,
                    TMS_PURCHASE TIMESTAMP NOT NULL,
                    COD_CATEGORY CHAR(36) NOT NULL,
                    COD_ACCOUNT CHAR(36) NOT NULL,
                    COD_STATUS VARCHAR(20) NOT NULL REFERENCES MON_STATUS(ID),
                    TXT_TYPE VARCHAR(20) NOT NULL REFERENCES MON_NATURE(ID),
                    COD_COST_CENTER CHAR(36) NOT NULL,
                    DAT_PAYMENT DATE,
                    GROUP_ID CHAR(36),
                    NUM_INSTALLMENT INT NOT NULL,
                    NUM_INSTALLMENT_TOTAL INT NOT NULL,
                    TXT_NOTES VARCHAR(1000),
                    TMS_CREATE_AT TIMESTAMP NOT NULL,
                    TMS_UPDATED_AT TIMESTAMP NOT NULL
                )
                """,
                """
                CREATE TABLE SEC_USER (
                    ID CHAR(36) PRIMARY KEY,
                    TXT_USERNAME VARCHAR(255) NOT NULL,
                    TXT_NAME VARCHAR(255),
                    TXT_PASSWORD VARCHAR(255) NOT NULL,
                    TXT_PREFERENCES VARCHAR(4000)
                )
                """,
                """
                CREATE TABLE PEP_PERSON (
                    ID CHAR(36) PRIMARY KEY,
                    TXT_NAME VARCHAR(255) NOT NULL,
                    TXT_LOCALE VARCHAR(20) NOT NULL,
                    TXT_LANGUAGE VARCHAR(20) NOT NULL,
                    TMS_CREATE_AT TIMESTAMP NOT NULL,
                    TMS_UPDATED_AT TIMESTAMP NOT NULL
                )
                """,
                """
                CREATE TABLE PEP_PERSON_ACCOUNT (
                    COD_PERSON CHAR(36) NOT NULL,
                    COD_ACCOUNT CHAR(36) NOT NULL,
                    PRIMARY KEY (COD_PERSON, COD_ACCOUNT)
                )
                """
        );
    }

    /** Comandos de limpeza de dados (isolamento entre testes). Sem FKs: ordem é indiferente. */
    public static List<String> reset() {
        return List.of(
                "DELETE FROM MON_ACCOUNT",
                "DELETE FROM MON_CATEGORY",
                "DELETE FROM MON_COST_CENTER",
                "DELETE FROM MON_TAG",
                "DELETE FROM MON_MONTHLY_BALANCE",
                "DELETE FROM MON_TRANSACTION",
                "DELETE FROM PEP_PERSON_ACCOUNT"
        );
    }
}
