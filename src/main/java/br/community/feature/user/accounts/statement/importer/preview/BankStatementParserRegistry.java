package br.community.feature.user.accounts.statement.importer.preview;

import br.community.feature.user.accounts.statement.importer.provider.BtgBankStatementParser;
import br.community.feature.user.accounts.statement.importer.provider.SantanderBankStatementParser;
import org.jspecify.annotations.NullMarked;

import java.util.Map;
import java.util.Optional;

/** Resolves a detected {@link Issuer} to its {@link BankStatementParser}. */
@NullMarked
public class BankStatementParserRegistry {

    private final Map<Issuer, BankStatementParser> byIssuer;

    public BankStatementParserRegistry(BtgBankStatementParser btg, SantanderBankStatementParser santander) {
        this.byIssuer = Map.of(Issuer.BTG, btg, Issuer.SANTANDER, santander);
    }

    public Optional<BankStatementParser> forIssuer(Issuer issuer) {
        return Optional.ofNullable(byIssuer.get(issuer));
    }
}
