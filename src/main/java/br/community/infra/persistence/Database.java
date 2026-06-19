package br.community.infra.persistence;

import org.jspecify.annotations.NullMarked;

import java.util.List;

/**
 * Schema (DDL) do banco relacional, em SQL estritamente portável entre fornecedores.
 *
 * <p>Restrito a construções padrão (sem dialeto): {@code CREATE TABLE} sem {@code IF NOT EXISTS},
 * tipos {@code CHAR}/{@code VARCHAR}/{@code DECIMAL}/{@code DATE}/{@code INT}; booleanos como
 * {@code CHAR(1)} ('Y'/'N') — não há {@code BOOLEAN} em todos os bancos; textos grandes (JSON de
 * {@code additionalInfo}/{@code preferences}) como {@code VARCHAR(4000)} — não há {@code CLOB} em
 * todos os bancos. Tabelas de dados planas, sem {@code DEFAULT} (a aplicação sempre fornece os
 * valores).</p>
 *
 * <p>As tabelas de domínio (lookup) {@code MON_ACCOUNT_TYPE}, {@code MON_NATURE} e
 * {@code MON_TRANSACTION_STATUS} guardam os valores fixos dos {@code enum} Java
 * ({@code Account.Type}, {@code Transaction.Type}, {@code Transaction.Status}); são semeadas no
 * próprio schema e referenciadas por chaves estrangeiras a partir das colunas de tipo/natureza/
 * situação, garantindo integridade referencial no banco. As demais tabelas não têm FKs entre si.
 * As {@code CREATE}/{@code INSERT} das lookups vêm antes das tabelas que as referenciam.
 * {@link #reset()} dá os comandos de limpeza para isolar testes (apaga só dados, preserva as
 * lookups).</p>
 */
@NullMarked
public abstract class Database {

    private Database() {}

    public static List<String> model() {
        return List.of(
                """
                CREATE TABLE MON_ACCOUNT_TYPE (
                    ID VARCHAR(20) PRIMARY KEY,
                    TXT_DESCRIPTION VARCHAR(50) NOT NULL
                )
                """,
                """
                CREATE TABLE MON_NATURE (
                    ID VARCHAR(20) PRIMARY KEY,
                    TXT_DESCRIPTION VARCHAR(50) NOT NULL
                )
                """,
                """
                CREATE TABLE MON_TRANSACTION_STATUS (
                    ID VARCHAR(20) PRIMARY KEY,
                    TXT_DESCRIPTION VARCHAR(50) NOT NULL
                )
                """,
                "INSERT INTO MON_ACCOUNT_TYPE (ID, TXT_DESCRIPTION) VALUES ('CHECKING', 'Conta corrente')",
                "INSERT INTO MON_ACCOUNT_TYPE (ID, TXT_DESCRIPTION) VALUES ('INVESTMENT', 'Investimento')",
                "INSERT INTO MON_ACCOUNT_TYPE (ID, TXT_DESCRIPTION) VALUES ('CREDIT_CARD', 'Cartão de crédito')",
                "INSERT INTO MON_NATURE (ID, TXT_DESCRIPTION) VALUES ('EXPENSE', 'Despesa')",
                "INSERT INTO MON_NATURE (ID, TXT_DESCRIPTION) VALUES ('INCOME', 'Receita')",
                "INSERT INTO MON_TRANSACTION_STATUS (ID, TXT_DESCRIPTION) VALUES ('SCHEDULED', 'Agendado')",
                "INSERT INTO MON_TRANSACTION_STATUS (ID, TXT_DESCRIPTION) VALUES ('CONFIRMED', 'Confirmado')",
                "INSERT INTO MON_TRANSACTION_STATUS (ID, TXT_DESCRIPTION) VALUES ('PENDING', 'Pendente')",
                """
                CREATE TABLE MON_ACCOUNT (
                    ID CHAR(36) PRIMARY KEY,
                    TXT_NAME VARCHAR(255) NOT NULL,
                    DEC_BALANCE DECIMAL(19, 2) NOT NULL,
                    TXT_TYPE VARCHAR(20) NOT NULL REFERENCES MON_ACCOUNT_TYPE(ID),
                    TXT_COLOR VARCHAR(20) NOT NULL,
                    BOL_ACTIVE CHAR(1) NOT NULL,
                    COD_LINKED_ACCOUNT CHAR(36),
                    TXT_ADDITIONAL_INFO VARCHAR(4000)
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
                    TXT_DESCRIPTION VARCHAR(255) NOT NULL
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
                    DEC_AMOUNT DECIMAL(19, 2) NOT NULL,
                    DAT_DATE DATE NOT NULL,
                    COD_CATEGORY CHAR(36) NOT NULL,
                    COD_ACCOUNT CHAR(36) NOT NULL,
                    TXT_STATUS VARCHAR(20) NOT NULL REFERENCES MON_TRANSACTION_STATUS(ID),
                    TXT_TYPE VARCHAR(20) NOT NULL REFERENCES MON_NATURE(ID),
                    COD_COST_CENTER CHAR(36) NOT NULL,
                    DAT_PAYMENT DATE,
                    COD_GROUP CHAR(36),
                    NUM_INSTALLMENT INT NOT NULL,
                    NUM_TOTAL_INSTALLMENTS INT NOT NULL,
                    TXT_NOTES VARCHAR(1000)
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
                    TXT_LANGUAGE VARCHAR(20) NOT NULL
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
