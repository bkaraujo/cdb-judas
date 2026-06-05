package br.community.feature.user.accounts.statement.importer.preview;

import br.community.feature.user.accounts.statement.importer.provider.BtgBankStatementParser;
import org.jspecify.annotations.NullMarked;

import java.util.Map;
import java.util.Optional;

/** Resolves a detected {@link Issuer} to its {@link BankStatementParser}. */
@NullMarked
public class BankStatementParserRegistry {

    private final Map<Issuer, BankStatementParser> byIssuer;

    public BankStatementParserRegistry(BtgBankStatementParser btg) {
        this.byIssuer = Map.of(Issuer.BTG, btg);
    }

    public Optional<BankStatementParser> forIssuer(Issuer issuer) {
        return Optional.ofNullable(byIssuer.get(issuer));
    }
}
