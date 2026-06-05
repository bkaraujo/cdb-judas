package br.community.feature.user.accounts.statement.importer.preview;

import br.community.feature.user.accounts.statement.importer.provider.BtgCreditCardStatementParser;
import br.community.feature.user.accounts.statement.importer.provider.SantanderCreditCardStatementParser;
import org.jspecify.annotations.NullMarked;

import java.util.Map;
import java.util.Optional;

/** Resolves a detected {@link Issuer} to its {@link CreditCardStatementParser}. */
@NullMarked
public class CreditCardStatementParserRegistry {

    private final Map<Issuer, CreditCardStatementParser> byIssuer;

    public CreditCardStatementParserRegistry(
            SantanderCreditCardStatementParser santander,
            BtgCreditCardStatementParser btg) {
        this.byIssuer = Map.of(
                Issuer.SANTANDER, santander,
                Issuer.BTG, btg);
    }

    public Optional<CreditCardStatementParser> forIssuer(Issuer issuer) {
        return Optional.ofNullable(byIssuer.get(issuer));
    }
}
